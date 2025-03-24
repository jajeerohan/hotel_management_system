package com.codingshuttle.projects.airBnbApp.repository;

import com.codingshuttle.projects.airBnbApp.dto.RoomPriceDto;
import com.codingshuttle.projects.airBnbApp.entity.Hotel;
import com.codingshuttle.projects.airBnbApp.entity.Inventory;
import com.codingshuttle.projects.airBnbApp.entity.Room;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {
    void deleteByDateAfterAndRoom(LocalDate today, Room room);

    void deleteByRoom(Room room);

    // This method is get all the hotels in the city that has at least one roomType available for all the dates between startDate and endDate.
    // Here reservedCount is number of rooms which are currently being booked but not yet confirmed
    //      bookedCount is number of rooms which are already booked and confirmed
    //      totalCount is number of rooms still available
    @Query("""
            SELECT i.hotel
            FROM Inventory i
            WHERE i.city = :city
              AND (i.date BETWEEN :startDate AND :endDate)
              AND i.closed = false
              AND (i.totalCount-i.bookedCount - i.reservedCount) >= :roomsCount
            GROUP BY i.hotel
            HAVING COUNT(i.date) >= :dateCount
            """)
    Page<Hotel> findHotelWithAvailableInventory(
            @Param("city") String city,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("roomsCount") Integer roomsCount,
            @Param("dateCount") Long dateCount,
            Pageable pageable);

    // This Query gives us the inventory for the user selected room and date range
    // get the inventory of available number of rooms excluding reserved as well
    // Also locks those rows so that concurrent user cannot modify those records
    // here -reservedCount is done...because we don't want to show this inventory to others as this is being reserved by other user for booking
    @Query("""
            SELECT i
            FROM Inventory i
            WHERE i.room.id = :roomId
              AND (i.date BETWEEN :startDate AND :endDate)
              AND i.closed = false
              AND (i.totalCount-i.bookedCount-i.reservedCount) >= :roomsCount
            """)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<Inventory> findAndLockAvailableInventory(
            @Param("roomId") Long roomId,
            @Param("startDate") LocalDate checkInDate,
            @Param("endDate") LocalDate checkOutDate,
            @Param("roomsCount") Integer roomsCount);

    // This query updated the reserved count when a user initializes/starts the booking process
    @Modifying
    @Query("""
                UPDATE Inventory i
                SET i.reservedCount = i.reservedCount + :numberOfRooms
                WHERE i.room.id = :roomId
                  AND i.date BETWEEN :startDate AND :endDate
                  AND (i.totalCount - i.bookedCount - i.reservedCount) >= :numberOfRooms
                  AND i.closed = false
            """)
    void initBooking(@Param("roomId") Long roomId,
                     @Param("startDate") LocalDate startDate,
                     @Param("endDate") LocalDate endDate,
                     @Param("numberOfRooms") int numberOfRooms);

    // This query gets the inventory which are reserved because now booking is done, and we want to update the reserved count
    // if we do -reservedCount as well then we get the inventory which is available for booking
    @Query("""
                SELECT i
                FROM Inventory i
                WHERE i.room.id = :roomId
                  AND i.date BETWEEN :startDate AND :endDate
                  AND (i.totalCount - i.bookedCount) >= :numberOfRooms
                  AND i.closed = false
            """)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<Inventory> findAndLockReservedInventory(@Param("roomId") Long roomId,
                                                 @Param("startDate") LocalDate startDate,
                                                 @Param("endDate") LocalDate endDate,
                                                 @Param("numberOfRooms") int numberOfRooms);

    // after the successful payment updating the Inventory
    // The reserved count is decreased, booking count is increased by number of rooms mentioned for booking request
    @Modifying
    @Query("""
                UPDATE Inventory i
                SET i.reservedCount = i.reservedCount - :numberOfRooms,
                    i.bookedCount = i.bookedCount + :numberOfRooms
                WHERE i.room.id = :roomId
                  AND i.date BETWEEN :startDate AND :endDate
                  AND (i.totalCount - i.bookedCount) >= :numberOfRooms
                  AND i.reservedCount >= :numberOfRooms
                  AND i.closed = false
            """)
    void confirmBooking(@Param("roomId") Long roomId,
                        @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate,
                        @Param("numberOfRooms") int numberOfRooms);

    // after cancelling the confirmed booking updating the inventory
    // Note that only booked count is updated...not the reserved count(this count is updated while booking only)
    @Modifying
    @Query("""
                UPDATE Inventory i
                SET i.bookedCount = i.bookedCount - :numberOfRooms
                WHERE i.room.id = :roomId
                  AND i.date BETWEEN :startDate AND :endDate
                  AND (i.totalCount - i.bookedCount) >= :numberOfRooms
                  AND i.closed = false
            """)
    void cancelBooking(@Param("roomId") Long roomId,
                       @Param("startDate") LocalDate startDate,
                       @Param("endDate") LocalDate endDate,
                       @Param("numberOfRooms") int numberOfRooms);


    List<Inventory> findByHotelAndDateBetween(Hotel hotel, LocalDate startDate, LocalDate endDate);

    List<Inventory> findByRoomOrderByDate(Room room);

    // Acquiring Lock for updating the inventory
    @Query("""
                SELECT i
                FROM Inventory i
                WHERE i.room.id = :roomId
                  AND i.date BETWEEN :startDate AND :endDate
            """)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<Inventory> getInventoryAndLockBeforeUpdate(@Param("roomId") Long roomId,
                                                    @Param("startDate") LocalDate startDate,
                                                    @Param("endDate") LocalDate endDate);

    // Updating the Inventory by the Admin
    @Modifying
    @Query("""
                UPDATE Inventory i
                SET i.surgeFactor = :surgeFactor,
                    i.closed = :closed
                WHERE i.room.id = :roomId
                  AND i.date BETWEEN :startDate AND :endDate
            """)
    void updateInventory(@Param("roomId") Long roomId,
                         @Param("startDate") LocalDate startDate,
                         @Param("endDate") LocalDate endDate,
                         @Param("closed") boolean closed,
                         @Param("surgeFactor") BigDecimal surgeFactor);

    @Query("""
       SELECT new com.codingshuttle.projects.airBnbApp.dto.RoomPriceDto(
            i.room,
            CASE
                WHEN COUNT(i) = :dateCount THEN AVG(i.price)
                ELSE NULL
            END
        )
       FROM Inventory i
       WHERE i.hotel.id = :hotelId
             AND i.date BETWEEN :startDate AND :endDate
             AND (i.totalCount - i.bookedCount) >= :roomsCount
             AND i.closed = false
       GROUP BY i.room
       """)
    List<RoomPriceDto> findRoomAveragePrice(
            @Param("hotelId") Long hotelId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("roomsCount") Long roomsCount,
            @Param("dateCount") Long dateCount
    );
}


