package com.kedarnath.controller;

import com.kedarnath.dto.AllotmentPreviewResponse;
import com.kedarnath.dto.BookingRequest;
import com.kedarnath.dto.PaymentOrderResponse;
import com.kedarnath.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @GetMapping("/preview")
    public ResponseEntity<AllotmentPreviewResponse> preview(
            @RequestParam int groupSize,
            @RequestParam String checkIn,
            @RequestParam String checkOut) {
        return ResponseEntity.ok(bookingService.previewAllotment(groupSize, checkIn, checkOut));
    }

    @PostMapping("/create")
    public ResponseEntity<PaymentOrderResponse> createBooking(
            @Valid @RequestBody BookingRequest req) throws Exception {
        return ResponseEntity.ok(bookingService.createBooking(req));
    }
}
