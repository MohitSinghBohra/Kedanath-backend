package com.kedarnath.controller;

import com.kedarnath.dto.BookingResponse;
import com.kedarnath.dto.PaymentVerifyRequest;
import com.kedarnath.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final BookingService bookingService;

    @PostMapping("/verify")
    public ResponseEntity<BookingResponse> verifyPayment(
            @Valid @RequestBody PaymentVerifyRequest req) throws Exception {
        return ResponseEntity.ok(bookingService.verifyPayment(req));
    }
}
