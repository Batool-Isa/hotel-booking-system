package com.ga.hotel_booking_app.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "users")
@ToString(exclude = {
        "password",
        "userProfile",
        "emailVerificationTokens",
        "passwordResetTokens",
        "logs"
})
public class User {
    @Id
    @Column
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(unique = true, nullable = false)
    private String username;
    @Column(unique = true, nullable = false)
    private String email;
    @Column(nullable = false)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;
    @Column(nullable = false)
    private boolean emailVerified = false;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.UNVERIFIED;
    public enum Status {
        ACTIVE,
        INACTIVE,
        UNVERIFIED
    }
    @OneToOne(cascade = CascadeType.ALL, fetch=FetchType.LAZY )
    @JoinColumn(name = "profile_id", referencedColumnName = "id")
    private UserProfile userProfile;
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name="role_id")
    private Role role;
    @JsonIgnore
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private Set<EmailVerificationToken> emailVerificationTokens;
    @JsonIgnore
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private Set<PasswordResetToken> passwordResetTokens;
    @JsonIgnore
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private Set<AuditLog> logs ;
    public String getPassword() {
        return password;
    }
}
