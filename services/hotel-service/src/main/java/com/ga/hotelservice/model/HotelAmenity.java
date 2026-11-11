package com.ga.hotelservice.model;

import jakarta.persistence.*;
import com.ga.hotelservice.model.Amenity;
import com.ga.hotelservice.model.Hotel;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "hotels_amenities")
@ToString
public class HotelAmenity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column
    private Long id;

    @ManyToOne
    @JoinColumn(name = "hotel_id", nullable = false)
    private Hotel hotel;

    @ManyToOne
    @JoinColumn(name = "amenity_id", nullable = false)
    private Amenity amenity;

    @Column
    private String description;
    @Column(precision = 10, scale = 2)
    private BigDecimal additionalCost;
}