package com.kedarnath.dto;

import lombok.Data;
import java.util.UUID;

@Data
public class PaymentOrderResponse {
    private String razorpayOrderId;
    private Double amount;
    private String currency;
    private String keyId;
    private UUID bookingId;
    private String customerName;
    private String customerEmail;
    private String customerPhone;
}
