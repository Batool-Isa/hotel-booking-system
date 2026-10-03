package com.ga.hotel_booking_app.config;

import com.ga.hotel_booking_app.model.Role;
import com.ga.hotel_booking_app.model.User;
import com.ga.hotel_booking_app.repository.RoleRepository;
import com.ga.hotel_booking_app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@RequiredArgsConstructor
public class DataSeeder {


    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    CommandLineRunner seedData() {
        return args -> {

            // Seed roles
            seedRoles();

            // Seed users
            seedAdmin();
            seedHotelManager();
        };
    }

    private void seedRoles() {

        if (roleRepository.findByName(Role.RoleName.CUSTOMER).isEmpty()) {
            roleRepository.save(new Role(null, Role.RoleName.CUSTOMER));
        }

        if (roleRepository.findByName(Role.RoleName.HOTEL_MANAGER).isEmpty()) {
            roleRepository.save(new Role(null, Role.RoleName.HOTEL_MANAGER));
        }

        if (roleRepository.findByName(Role.RoleName.ADMIN).isEmpty()) {
            roleRepository.save(new Role(null, Role.RoleName.ADMIN));
        }
    }

    private void seedAdmin() {
        // create admin
        Role adminRole = roleRepository.findByName(Role.RoleName.ADMIN)
                .orElseThrow();

        User admin = new User();
        admin.setUsername("admin");
        admin.setEmail("admin@vibestay.com");
        admin.setPassword(passwordEncoder.encode("Admin123!"));
        admin.setEmailVerified(true);
        admin.setStatus(User.Status.ACTIVE);
        admin.getRoles().add(adminRole);

        userRepository.save(admin);
    }

    private void seedHotelManager() {
        // create manager
        Role managerRole = roleRepository.findByName(Role.RoleName.HOTEL_MANAGER)
                .orElseThrow();

        User manger = new User();
        manger.setUsername("hotel-manager1");
        manger.setEmail("hotel-manager1@vibestay.com");
        manger.setPassword(passwordEncoder.encode("hotel-manager123!"));
        manger.setEmailVerified(true);
        manger.setStatus(User.Status.ACTIVE);
        manger.getRoles().add(managerRole);

        userRepository.save(manger);
    }
}