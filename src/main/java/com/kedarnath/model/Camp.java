package com.kedarnath.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "camps")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Camp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Integer capacity;

    @Column(name = "total_count", nullable = false)
    private Integer totalCount;

    @Column(name = "booked_count", nullable = false)
    private Integer bookedCount = 0;

    @Column(name = "price_per_night", nullable = false)
    private Double pricePerNight;

    @Column(columnDefinition = "TEXT")
    private String description;

    public Integer getAvailableCount() {
        return totalCount - bookedCount;
    }
}
