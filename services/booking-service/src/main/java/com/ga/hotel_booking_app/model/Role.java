package com.ga.hotel_booking_app.model;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name="roles")
public class Role {
    @Id
    @Column
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Enumerated(EnumType.STRING)
    @Column
    private RoleName name;

    public enum RoleName{
        ADMIN,
        CUSTOMER,
        HOTEL_MANAGER,
        STAFF
    }
}
