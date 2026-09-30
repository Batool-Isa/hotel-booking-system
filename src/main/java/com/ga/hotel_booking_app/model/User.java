package com.ga.hotel_booking_app.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@Entity
@Table(name = "users")
@ToString(exclude = {"password","userProfile"})
public class User {
    @Id
    @Column
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(unique = true)
    private String username;
    @Column
    private String email;
    @Column
    private String password;
    @Column(nullable = false, columnDefinition = "varchar(255) DEFAULT 'UNVERIFIED' ")
    private boolean emailVerified;
    @Enumerated(EnumType.STRING)
    @Column
    private Status status;
    public enum Status {
        ACTIVE,
        INACTIVE,
        UNVERIFIED
    }
    @OneToOne(cascade = CascadeType.ALL, fetch=FetchType.LAZY )
    @JoinColumn(name = "profile_id", referencedColumnName = "id")
    private UserProfile userProfile;
    @CreationTimestamp
    @Column
    private LocalDateTime createdAt;
    @UpdateTimestamp
    @Column
    private LocalDateTime updatedAt;
    @ManyToMany
    @JoinTable(
            name="user_role",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles;
}
