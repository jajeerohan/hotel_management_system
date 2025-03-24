package com.codingshuttle.projects.airBnbApp.repository;

import com.codingshuttle.projects.airBnbApp.entity.Booking;
import com.codingshuttle.projects.airBnbApp.entity.Hotel;
import com.codingshuttle.projects.airBnbApp.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.awt.print.Book;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    Optional<Booking> findByPaymentSessionId(String sessionId);

    List<Booking> findByHotel(Hotel hotel);

    List<Booking> findByHotelAndCreatedAtBetween(Hotel hotel, LocalDateTime startDateTime, LocalDateTime endDateTime);

    List<Booking> findByUser(User user);
}
