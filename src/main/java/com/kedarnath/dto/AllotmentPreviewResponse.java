package com.kedarnath.dto;

import lombok.Data;
import java.util.List;

@Data
public class AllotmentPreviewResponse {
    private boolean available;
    private String message;
    private List<CampAvailabilityResponse> suggestedAllocations;
    private Double estimatedTotal;
    private Double estimatedAdvance;
}
