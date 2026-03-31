package com.kedarnath.repository;

import com.kedarnath.model.Booking;
import com.kedarnath.model.CampAllocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CampAllocationRepository extends JpaRepository<CampAllocation, Long> {
    List<CampAllocation> findByBooking(Booking booking);
}
