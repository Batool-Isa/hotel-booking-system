package com.ga.hotel_booking_app.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "amenities")
@ToString
public class Amenity {
    @Id
    @Column
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String name;
    @Column(nullable = true)
    private String description;
    @OneToMany(mappedBy = "amenity")
    private Set<HotelAmenity> hotels = new HashSet<>();
}