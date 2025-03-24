package com.codingshuttle.projects.airBnbApp.service;

import com.codingshuttle.projects.airBnbApp.dto.*;
import com.codingshuttle.projects.airBnbApp.entity.*;
import com.codingshuttle.projects.airBnbApp.entity.enums.BookingStatus;
import com.codingshuttle.projects.airBnbApp.exception.ResourceNotFoundException;
import com.codingshuttle.projects.airBnbApp.exception.UnAuthorisedException;
import com.codingshuttle.projects.airBnbApp.repository.*;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.Refund;
import com.stripe.model.checkout.Session;
import com.stripe.param.RefundCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

import static com.codingshuttle.projects.airBnbApp.util.AppUtils.getCurrentUser;

@Service
@Slf4j
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService{

    private final BookingRepository bookingRepository;
    private final HotelRepository hotelRepository;
    private final RoomRepository roomRepository;
    private final InventoryRepository inventoryRepository;
    private final GuestRepository guestRepository;
    private final ModelMapper modelMapper;
    private final CheckoutService checkoutService;

    @Value("${frontend.url}")
    private String frontendUrl;

    @Override
    @Transactional
    public BookingDto initializeBooking(BookingRequest bookingRequest) {
        log.info("Initialising booking for hotel : {}, room: {}, date from {} to {}", bookingRequest.getHotelId(),
                bookingRequest.getRoomId(), bookingRequest.getCheckInDate(), bookingRequest.getCheckOutDate());
        Hotel hotel = hotelRepository.findById(bookingRequest.getHotelId())
                .orElseThrow(() -> new ResourceNotFoundException("Hotel not found with Id " + bookingRequest.getHotelId()));
        Room room = roomRepository.findById(bookingRequest.getRoomId())
                .orElseThrow(() -> new ResourceNotFoundException("Room not found with Id " + bookingRequest.getHotelId()));

        // Getting the Inventory for the user selected room
        List<Inventory> inventoryList = inventoryRepository.findAndLockAvailableInventory(bookingRequest.getRoomId(),
                bookingRequest.getCheckInDate(), bookingRequest.getCheckOutDate(), bookingRequest.getRoomsCount());
        long daysBetween = ChronoUnit.DAYS.between(bookingRequest.getCheckInDate(), bookingRequest.getCheckOutDate())+1;
        if (inventoryList.size() < daysBetween) {
            throw new IllegalStateException("Room is not available anymore");
        }

        // Updating the Inventories - reserved count
        /*for (Inventory inventory : inventoryList) {
            inventory.setReservedCount(inventory.getReservedCount() + bookingRequest.getRoomsCount());
        }
        inventoryRepository.saveAll(inventoryList);*/
        // We can update using above for loop or below query
        inventoryRepository.initBooking(room.getId(), bookingRequest.getCheckInDate(),
                bookingRequest.getCheckOutDate(), bookingRequest.getRoomsCount());

        BigDecimal priceOfOneRoom = inventoryList.stream()
                .map(Inventory::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalPrice = priceOfOneRoom.multiply(BigDecimal.valueOf(bookingRequest.getRoomsCount()));

        // Create the Booking
        // TODO: calculate the amount
        Booking booking = Booking.builder()
                .bookingStatus(BookingStatus.RESERVED)
                .hotel(hotel)
                .room(room)
                .checkInDate(bookingRequest.getCheckInDate())
                .checkOutDate(bookingRequest.getCheckOutDate())
                .user(getCurrentUser())
                .roomsCount(bookingRequest.getRoomsCount())
                .amount(totalPrice)
                .build();
        bookingRepository.save(booking);
        return modelMapper.map(booking, BookingDto.class);
    }

    @Override
    @Transactional
    public BookingDto addGuests(Long bookingId, List<Long> guestIdList) {
        log.info("Adding guests to the booking {} ", bookingId );
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with Id " + bookingId));

        User user = getCurrentUser();
        if (!user.equals(booking.getUser())) {
            throw new UnAuthorisedException("Booking does not belong to this user with id : " + user.getId());
        }

        if (isBookingExpired(booking)) {
            throw new IllegalStateException("Booking is already expired");
        }

        if (booking.getBookingStatus() != BookingStatus.RESERVED) {
            throw new IllegalStateException("Booking is not under RESERVED status so you cannot add guests. Please create a new Booking again");
        }

        for (Long guestId: guestIdList) {
            Guest guest = guestRepository.findById(guestId)
                    .orElseThrow(() -> new ResourceNotFoundException("Guest not found with id: "+guestId));
            booking.getGuests().add(guest);
        }

        booking.setBookingStatus(BookingStatus.GUESTS_ADDED);
        booking = bookingRepository.save(booking);
        return modelMapper.map(booking, BookingDto.class);
    }

    // returns a session url usimng which we can make the payment
    @Override
    @Transactional
    public String initiatePayments(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with Id " + bookingId));

        User user = getCurrentUser();
        if (!user.equals(booking.getUser())) {
            throw new UnAuthorisedException("Booking does not belong to this user with id : " + user.getId());
        }

        if (isBookingExpired(booking)) {
            throw new IllegalStateException("Booking is already expired");
        }

        String sessionUrl = checkoutService.getCheckoutSession(booking,
                frontendUrl+"/payments/" +bookingId +"/status",
                frontendUrl+"/payments/" +bookingId +"/status");
        booking.setBookingStatus(BookingStatus.PAYMENT_PENDING);
        bookingRepository.save(booking);

        return sessionUrl;
    }

    // Captures the payment and checks for the success event webhook
    // If success then updates the Inventory
    @Override
    @Transactional
    public void capturePayment(Event event) {
        if ("checkout.session.completed".equals(event.getType())) {
            Session session = (Session) event.getDataObjectDeserializer().getObject().orElse(null);
            String sessionId = session.getId();
            Booking booking = bookingRepository.findByPaymentSessionId(sessionId).orElseThrow(
                    () -> new ResourceNotFoundException("Booking with given payment Id not present"));
            booking.setBookingStatus(BookingStatus.CONFIRMED);
            bookingRepository.save(booking);

            //One payment is confirmed update the inventory
            //This call will lock the reserved inventory
            inventoryRepository.findAndLockReservedInventory(booking.getRoom().getId(), booking.getCheckInDate(),
                    booking.getCheckOutDate(), booking.getRoomsCount());
            //This call increases the bookedCount and decreases the reservedCount by roomsCount
            inventoryRepository.confirmBooking(booking.getRoom().getId(), booking.getCheckInDate(),
                    booking.getCheckOutDate(), booking.getRoomsCount());
            log.info("Successfully confirmed the booking for Booking ID: {}", booking.getId());
        } else {
            log.warn("Unhandled event type: {}", event.getType());
        }
    }

    // Updates the inventory in case booking is cancelled
    // Refund initiation logic
    @Override
    @Transactional
    public String cancelBooking(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with Id " + bookingId));
        User user = getCurrentUser();
        if (!user.equals(booking.getUser())) {
            throw new UnAuthorisedException("Booking does not belong to this user with id : " + user.getId());
        }
        if (booking.getBookingStatus() != BookingStatus.CONFIRMED) {
            throw  new IllegalStateException("Only confirmed bookings can be cancelled");
        }
        booking.setBookingStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);

        // After booking is set to cancelled update the inventory
        // First get the lock for those inventory and the update the count
        inventoryRepository.findAndLockReservedInventory(booking.getRoom().getId(), booking.getCheckInDate(),
                booking.getCheckOutDate(), booking.getRoomsCount());
        inventoryRepository.cancelBooking(booking.getRoom().getId(), booking.getCheckInDate(),
                booking.getCheckOutDate(), booking.getRoomsCount());

        //Refund Logic
        try {
            Session session = Session.retrieve(booking.getPaymentSessionId());
            RefundCreateParams refundCreateParams = RefundCreateParams.builder()
                    .setPaymentIntent(session.getPaymentIntent())
                    .build();
            Refund.create(refundCreateParams);
        } catch (StripeException e) {
            throw new RuntimeException(e);
        }
        return "Booking has been cancelled and refund is successful";
    }

    @Override
    public BookingStatus getBookingStatus(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId).orElseThrow(
                () -> new ResourceNotFoundException("Booking not found with id: "+bookingId)
        );
        User user = getCurrentUser();
        if (!user.equals(booking.getUser())) {
            throw new UnAuthorisedException("Booking does not belong to this user with id: "+user.getId());
        }

        return booking.getBookingStatus();
    }

    @Override
    public List<BookingDto> getAllBookingsByHotelId(Long hotelId) {
        log.info("Getting all the booking for a hotel id : {}", hotelId);
        Hotel hotel = hotelRepository.findById(hotelId).orElseThrow(
                () -> new ResourceNotFoundException("Hotel Not found with Id : " + hotelId));
        User user = getCurrentUser();
        if(!user.equals(hotel.getOwner())){
            throw new AccessDeniedException("You are not the owner of this hotel");
        }
        List<Booking> bookings = bookingRepository.findByHotel(hotel);
        return bookings.stream()
                .map(booking -> modelMapper.map(booking, BookingDto.class))
                .toList();
    }

    @Override
    public HotelReportDto getHotelReport(Long hotelId, LocalDate startDate, LocalDate endDate) {
        log.info("Getting report for the hotel with id : {}", hotelId);
        Hotel hotel = hotelRepository.findById(hotelId).orElseThrow(
                () -> new ResourceNotFoundException("Hotel Not found with Id : " + hotelId));
        User user = getCurrentUser();
        if(!user.equals(hotel.getOwner())){
            throw new AccessDeniedException("You are not the owner of this hotel");
        }

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        List<Booking> bookings = bookingRepository.findByHotelAndCreatedAtBetween(hotel, startDateTime, endDateTime);

        long confirmedBookingCount = bookings.stream()
                .filter(booking -> booking.getBookingStatus() == BookingStatus.CONFIRMED)
                .count();
        BigDecimal totalRevenueOfConfirmedBookings = bookings.stream()
                .filter(booking -> booking.getBookingStatus() == BookingStatus.CONFIRMED)
                .map(Booking::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal avgRevenue = confirmedBookingCount == 0 ? BigDecimal.ZERO :
                totalRevenueOfConfirmedBookings.divide(BigDecimal.valueOf(confirmedBookingCount), RoundingMode.HALF_UP);

        return new HotelReportDto(confirmedBookingCount, totalRevenueOfConfirmedBookings, avgRevenue);
    }

    @Override
    public List<BookingDto> getMyBookings() {
        User user = getCurrentUser();
        log.info("Getting all the bookings of user : {}", user.toString());
        return bookingRepository.findByUser(user)
                .stream().
                map((element) -> modelMapper.map(element, BookingDto.class))
                .collect(Collectors.toList());
    }

    public boolean isBookingExpired(Booking booking) {
        return booking.getCreatedAt().plusMinutes(10).isBefore(LocalDateTime.now());
    }


}
