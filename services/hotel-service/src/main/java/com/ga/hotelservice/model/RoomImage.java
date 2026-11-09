package com.ga.hotelservice.model;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.ga.hotelservice.model.Room;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "room_images")
@ToString
public class RoomImage {
    @Id
    @Column
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(unique = true, nullable = false)
    private String imageURL;
    @Column(updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;
    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

}

