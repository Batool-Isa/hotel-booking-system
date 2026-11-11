package com.ga.hotelservice.model;

import jakarta.persistence.*;
import com.ga.hotelservice.model.Room;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "hotels")
@ToString(exclude = {"images", "amenities", "managerIds"})
public class Hotel {
    @Id
    @Column
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(unique = true, nullable = false)
    private String name;
    @Column(nullable = false)
    private String description;
    @Column(nullable = false)
    private String address;
    @Column(nullable = false)
    private String city;
    @Column(nullable = false)
    private String country;
    @Column
    private BigDecimal latitude;
    @Column
    private BigDecimal longitude ;
    @Column(nullable = false)
    private String phone;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.PENDING_APPROVAL;
    public enum Status {
        PENDING_APPROVAL,
        ACTIVE,
        REJECTED,
        INACTIVE
    }
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "hotel_managers", joinColumns = @JoinColumn(name = "hotel_id"))
    @Column(name = "user_id")
    private Set<Long> managerIds = new HashSet<>();

    public boolean isManagedBy(Long userId) {
        return managerIds.contains(userId);
    }

    @OneToMany(mappedBy = "hotel", fetch = FetchType.LAZY)
    private Set<HotelImage> images = new HashSet<>();

    @OneToMany(mappedBy = "hotel",fetch = FetchType.LAZY)
    private Set<HotelAmenity> amenities = new HashSet<>();

    @OneToMany(mappedBy = "hotel", fetch = FetchType.LAZY)
    private Set<Room> rooms = new HashSet<>();
    @OneToOne(mappedBy = "hotel", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private ChildPolicy childPolicy;
}
