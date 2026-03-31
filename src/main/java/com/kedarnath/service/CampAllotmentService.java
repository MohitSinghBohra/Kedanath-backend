package com.kedarnath.service;

import com.kedarnath.model.Camp;
import com.kedarnath.repository.CampRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class CampAllotmentService {

    private final CampRepository campRepository;

    public record AllotmentResult(boolean success, String message,
                                   Map<Camp, Integer> allocations) {}

    /**
     * Find best camp combination for the given group size.
     * Returns a map of Camp → quantity needed.
     */
    public AllotmentResult findBestAllotment(int groupSize) {
        List<Camp> available = campRepository.findAllByOrderByCapacityAsc()
                .stream()
                .filter(c -> c.getAvailableCount() > 0)
                .sorted(Comparator.comparingInt(Camp::getCapacity).reversed())
                .toList();

        if (available.isEmpty()) {
            return new AllotmentResult(false, "No camps available for these dates.", Map.of());
        }

        Map<Camp, Integer> result = new LinkedHashMap<>();
        int remaining = groupSize;

        for (Camp camp : available) {
            if (remaining <= 0) break;
            int needed = (int) Math.ceil((double) remaining / camp.getCapacity());
            int canUse = Math.min(needed, camp.getAvailableCount());
            if (canUse > 0) {
                result.put(camp, canUse);
                remaining -= canUse * camp.getCapacity();
            }
        }

        if (remaining > 0) {
            return new AllotmentResult(false,
                "Not enough camp capacity available for " + groupSize + " people. Please try different dates.",
                Map.of());
        }

        return new AllotmentResult(true, "Camps allocated successfully.", result);
    }

    /**
     * Calculate total cost for the given nights.
     */
    public double calculateTotal(Map<Camp, Integer> allocations, int nights) {
        return allocations.entrySet().stream()
                .mapToDouble(e -> e.getKey().getPricePerNight() * e.getValue() * nights)
                .sum();
    }
}
