package com.kedarnath.controller;

import com.kedarnath.dto.CampAvailabilityResponse;
import com.kedarnath.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/camps")
@RequiredArgsConstructor
public class CampController {

    private final BookingService bookingService;

    @GetMapping("/availability")
    public ResponseEntity<List<CampAvailabilityResponse>> getAvailability() {
        return ResponseEntity.ok(bookingService.getAllCampsAvailability());
    }
}
