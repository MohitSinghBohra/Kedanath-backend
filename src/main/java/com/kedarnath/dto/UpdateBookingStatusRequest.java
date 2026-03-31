package com.kedarnath.dto;

import com.kedarnath.model.Booking;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateBookingStatusRequest {
    @NotNull private Booking.BookingStatus status;
}
