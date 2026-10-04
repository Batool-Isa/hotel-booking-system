package com.ga.hotel_booking_app.config;

import com.ga.hotel_booking_app.model.Amenity;
import com.ga.hotel_booking_app.model.Role;
import com.ga.hotel_booking_app.model.User;
import com.ga.hotel_booking_app.model.UserProfile;
import com.ga.hotel_booking_app.repository.AmenityRepository;
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
    private final AmenityRepository amenityRepository;

    @Bean
    CommandLineRunner seedData() {
        return args -> {

            // Seed roles
            seedRoles();

            // Seed users
            seedAdmin();
            seedHotelManager();

            seedAmenities();
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


            if (userRepository.existsByEmail("admin@vibestay.com")) {
                return;
            }

            // Find admin role
            Role adminRole = roleRepository.findByName(Role.RoleName.ADMIN)
                    .orElseThrow();

            // Create profile
            UserProfile profile = new UserProfile();
            profile.setFirstName("VibeStay");
            profile.setLastName("Admin");
            profile.setPhone("+97330000000");

            // Create admin
            User admin = new User();
            admin.setUsername("admin");
            admin.setEmail("admin@vibestay.com");
            admin.setPassword(passwordEncoder.encode("Admin123!"));
            admin.setEmailVerified(true);
            admin.setStatus(User.Status.ACTIVE);
            admin.setRole(adminRole);

            // Assign profile
            admin.setUserProfile(profile);

            userRepository.save(admin);

    }

    private void seedHotelManager() {

        if (userRepository.existsByEmail("hotel-manager1@vibestay.com")) {
            return;
        }

        // Find manager role
        Role managerRole = roleRepository.findByName(Role.RoleName.HOTEL_MANAGER)
                .orElseThrow();

        // Create profile
        UserProfile profile = new UserProfile();
        profile.setFirstName("Hotel");
        profile.setLastName("Manager");
        profile.setPhone("+97331111111");

        // Create manager
        User manager = new User();
        manager.setUsername("hotel-manager1");
        manager.setEmail("hotel-manager1@vibestay.com");
        manager.setPassword(passwordEncoder.encode("hotel-manager123!"));
        manager.setEmailVerified(true);
        manager.setStatus(User.Status.ACTIVE);
        manager.setRole(managerRole);

        // Assign profile
        manager.setUserProfile(profile);

        userRepository.save(manager);
    }

    private void seedAmenities() {

        String[] amenities = {
                "Wi-Fi",
                "Swimming Pool",
                "Gym",
                "Parking",
                "Restaurant",
                "Spa",
                "Breakfast",
                "Beach Access",
                "Room Service",
                "Air Conditioning"
        };

        for (String name : amenities) {
            if (amenityRepository.findByName(name).isEmpty()) {
                Amenity amenity = new Amenity();
                amenity.setName(name);
                amenityRepository.save(amenity);
            }
        }
    }
}