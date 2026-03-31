package com.kedarnath.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "camp_allocations")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CampAllocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "camp_id", nullable = false)
    private Camp camp;

    @Column(nullable = false)
    private Integer quantity = 1;

    public CampAllocation(Booking booking, Camp camp, int quantity) {
        this.booking = booking;
        this.camp = camp;
        this.quantity = quantity;
    }
}
