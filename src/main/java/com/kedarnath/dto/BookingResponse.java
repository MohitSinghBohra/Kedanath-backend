package com.kedarnath.dto;

import lombok.Data;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
public class BookingResponse {
    private UUID id;
    private String customerName;
    private String customerEmail;
    private String customerPhone;
    private Integer groupSize;
    private LocalDate checkIn;
    private LocalDate checkOut;
    private Integer totalNights;
    private Double totalAmount;
    private Double advanceAmount;
    private String status;
    private List<AllocationDetail> allocations;
    private String razorpayOrderId;

    @Data
    public static class AllocationDetail {
        private String campName;
        private Integer capacity;
        private Integer quantity;
        private Double pricePerNight;
    }
}
