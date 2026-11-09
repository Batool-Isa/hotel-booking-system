package com.ga.hotelservice.model;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.ga.hotelservice.model.Hotel;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "hotel_images")
@ToString
public class HotelImage {
    @Id
    @Column
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String imageURL;
    @Column
    private String altText ;
    @Column(nullable = false)
    private boolean isPrimary = false;
    @CreationTimestamp
    @Column
    private LocalDateTime createdAt;
    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "hotel_id", nullable = false)
    private Hotel hotel;
}

