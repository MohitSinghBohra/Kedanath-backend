package com.kedarnath.repository;

import com.kedarnath.model.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface BookingRepository extends JpaRepository<Booking, UUID> {
    List<Booking> findAllByOrderByCreatedAtDesc();
    List<Booking> findByStatus(Booking.BookingStatus status);
    List<Booking> findByCustomerEmail(String email);

    @Query("SELECT b FROM Booking b WHERE b.status NOT IN ('CANCELLED') " +
           "AND b.checkIn < :checkOut AND b.checkOut > :checkIn")
    List<Booking> findOverlappingBookings(@Param("checkIn") LocalDate checkIn,
                                          @Param("checkOut") LocalDate checkOut);
}
