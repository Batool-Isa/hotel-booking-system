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

        Role managerRole = roleRepository.findByName(Role.RoleName.HOTEL_MANAGER)
                .orElseThrow();

        if (!userRepository.existsByEmail("manager1@vibestay.com")) {

            UserProfile profile1 = new UserProfile();
            profile1.setFirstName("Hotel");
            profile1.setLastName("Manager One");
            profile1.setPhone("+97331111111");

            User manager1 = new User();
            manager1.setUsername("manager1");
            manager1.setEmail("manager1@vibestay.com");
            manager1.setPassword(passwordEncoder.encode("Manager123!"));
            manager1.setEmailVerified(true);
            manager1.setStatus(User.Status.ACTIVE);
            manager1.setRole(managerRole);
            manager1.setUserProfile(profile1);

            userRepository.save(manager1);
        }

        if (!userRepository.existsByEmail("manager2@vibestay.com")) {

            UserProfile profile2 = new UserProfile();
            profile2.setFirstName("Hotel");
            profile2.setLastName("Manager Two");
            profile2.setPhone("+97332222222");

            User manager2 = new User();
            manager2.setUsername("manager2");
            manager2.setEmail("manager2@vibestay.com");
            manager2.setPassword(passwordEncoder.encode("Manager123!"));
            manager2.setEmailVerified(true);
            manager2.setStatus(User.Status.ACTIVE);
            manager2.setRole(managerRole);
            manager2.setUserProfile(profile2);

            userRepository.save(manager2);
        }

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