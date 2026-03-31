package com.kedarnath.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDate;

@Data
public class BookingRequest {
    @NotBlank(message = "Name is required")
    private String customerName;
    @Email @NotBlank
    private String customerEmail;
    @NotBlank @Pattern(regexp = "^[6-9]\\d{9}$", message = "Valid Indian mobile required")
    private String customerPhone;
    @NotNull @Min(1) @Max(100)
    private Integer groupSize;
    @NotNull
    private LocalDate checkIn;
    @NotNull
    private LocalDate checkOut;
    private String specialRequests;
}
