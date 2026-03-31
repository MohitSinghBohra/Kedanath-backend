package com.kedarnath.controller;

import com.kedarnath.dto.BookingResponse;
import com.kedarnath.dto.UpdateBookingStatusRequest;
import com.kedarnath.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final BookingService bookingService;

    @GetMapping("/bookings")
    public ResponseEntity<List<BookingResponse>> getAllBookings() {
        return ResponseEntity.ok(bookingService.getAllBookings());
    }

    @GetMapping("/bookings/{id}")
    public ResponseEntity<BookingResponse> getBooking(@PathVariable UUID id) {
        return ResponseEntity.ok(bookingService.getAllBookings().stream()
            .filter(b -> b.getId().equals(id))
            .findFirst()
            .orElseThrow(() -> new NoSuchElementException("Booking not found")));
    }

    @PutMapping("/bookings/{id}/status")
    public ResponseEntity<BookingResponse> updateStatus(
            @PathVariable UUID id,
            @RequestBody UpdateBookingStatusRequest req) {
        return ResponseEntity.ok(bookingService.updateBookingStatus(id, req.getStatus()));
    }
}
