package com.ga.userservice.config;

import com.ga.userservice.model.Role;
import com.ga.userservice.model.User;
import com.ga.userservice.model.UserProfile;
import com.ga.userservice.repository.RoleRepository;
import com.ga.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@Configuration
@RequiredArgsConstructor
public class DataSeeder {
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;

    private final Map<String, String> encodedPasswords = new HashMap<>();

    private static final String[][] MANAGERS = {
            {"Mohammed", "Al Sayed", "+97331111111"},
            {"Fatema", "Janahi", "+97332222222"},
            {"Khalid", "Al Mansoori", "+97331000003"},
            {"Noura", "Al Hashimi", "+97331000004"},
            {"Yusuf", "Karimi", "+97331000005"},
            {"Layla", "Haddad", "+97331000006"}
    };

    private static final String[][] CUSTOMERS = {
            {"Sara", "Ahmed"}, {"Omar", "Khalid"}, {"Fatima", "Al Zayani"}, {"Hassan", "Jaffar"},
            {"Maryam", "Salman"}, {"Ali", "Hussain"}, {"Zainab", "Mahmood"}, {"Ahmed", "Fakhro"},
            {"Reem", "Nasser"}, {"Yousef", "Darwish"}, {"Huda", "Abdulla"}, {"Tariq", "Mansoor"},
            {"Aisha", "Rashid"}, {"Jasim", "Saleh"}, {"Dana", "Mubarak"}, {"Karim", "Haddad"},
            {"Hamad", "Al Ansari"}, {"Lulwa", "Al Doseri"}, {"Ebrahim", "Shehab"}, {"Noora", "Al Qassab"},
            {"Saleh", "Al Amer"}, {"Mariam", "Hasan"}, {"Faisal", "Matar"}, {"Shaikha", "Al Mannai"}
    };

    @Bean
    CommandLineRunner seedUsers() {
        return args -> {
            if (userRepository.count() > 0) {
                return;
            }
            seedRoles();
            createUser(usernameFor("Yasmeen", "Al Khaja"), "admin@vibestay.com", "Admin123!",
                    Role.RoleName.ADMIN, "Yasmeen", "Al Khaja", "+97330000000");
            for (int i = 0; i < MANAGERS.length; i++) {
                createUser(usernameFor(MANAGERS[i][0], MANAGERS[i][1]), "manager" + (i + 1) + "@vibestay.com",
                        "Manager123!", Role.RoleName.HOTEL_MANAGER, MANAGERS[i][0], MANAGERS[i][1], MANAGERS[i][2]);
            }
            for (int i = 0; i < CUSTOMERS.length; i++) {
                createUser(usernameFor(CUSTOMERS[i][0], CUSTOMERS[i][1]), "customer" + (i + 1) + "@vibestay.com",
                        "Customer123!", Role.RoleName.CUSTOMER, CUSTOMERS[i][0], CUSTOMERS[i][1],
                        String.format("+9733400%04d", i + 1));
            }
            backdateUsers();
        };
    }

    private void seedRoles() {
        for (Role.RoleName name : Role.RoleName.values()) {
            if (roleRepository.findByName(name).isEmpty()) {
                roleRepository.save(new Role(null, name));
            }
        }
    }

    // "Fatima", "Al Zayani" -> "fatima.alzayani"
    private static String usernameFor(String first, String last) {
        return (first + "." + last).toLowerCase().replaceAll("[^a-z.]", "");
    }

    private void createUser(String username, String email, String rawPassword, Role.RoleName roleName,
                            String first, String last, String phone) {
        if (userRepository.existsByEmail(email) || userRepository.existsByUsername(username)) {
            return;
        }
        Role role = roleRepository.findByName(roleName).orElseThrow();

        UserProfile profile = new UserProfile();
        profile.setFirstName(first);
        profile.setLastName(last);
        profile.setPhone(phone);

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(encodedPasswords.computeIfAbsent(rawPassword, passwordEncoder::encode));
        user.setEmailVerified(true);
        user.setStatus(User.Status.ACTIVE);
        user.setRole(role);
        user.setUserProfile(profile);
        userRepository.save(user);
    }

    private void backdateUsers() {
        Random rnd = new Random(20261104L);
        LocalDateTime now = LocalDateTime.now();
        for (User user : userRepository.findAll()) {
            boolean staff = user.getEmail().startsWith("admin") || user.getEmail().startsWith("manager");
            LocalDateTime joined = now.minusDays((staff ? 400 : 180) + rnd.nextInt(200)).withHour(9 + rnd.nextInt(10));
            jdbcTemplate.update("UPDATE users SET created_at = ?, updated_at = ? WHERE id = ?",
                    Timestamp.valueOf(joined), Timestamp.valueOf(joined), user.getId());
        }
    }
}
