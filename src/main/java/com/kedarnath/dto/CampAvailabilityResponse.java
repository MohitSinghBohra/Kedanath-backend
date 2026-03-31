package com.kedarnath.dto;

import lombok.Data;

@Data
public class CampAvailabilityResponse {
    private Long id;
    private String name;
    private Integer capacity;
    private Integer totalCount;
    private Integer bookedCount;
    private Integer availableCount;
    private Double pricePerNight;
    private String description;
}
