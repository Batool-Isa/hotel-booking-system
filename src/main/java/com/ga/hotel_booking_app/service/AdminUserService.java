package com.ga.hotel_booking_app.service;

import com.ga.hotel_booking_app.dto.reponse.AdminUserResponse;
import com.ga.hotel_booking_app.dto.reponse.MessageResponse;
import com.ga.hotel_booking_app.dto.reponse.PageResponse;
import com.ga.hotel_booking_app.exception.custom.InformationNotFoundException;
import com.ga.hotel_booking_app.exception.custom.InvalidInformationException;
import com.ga.hotel_booking_app.model.Role;
import com.ga.hotel_booking_app.model.User;
import com.ga.hotel_booking_app.model.UserProfile;
import com.ga.hotel_booking_app.repository.UserRepository;
import jakarta.persistence.criteria.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Administrative user management: list/search users and activate or deactivate them.
 * Users are never physically deleted (soft delete): deactivating sets the status to INACTIVE.
 */
@Service
public class AdminUserService {
    private static final Logger logger = LoggerFactory.getLogger(AdminUserService.class);

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserService userService;
    @Autowired
    private AuditLogService auditLogService;


    public ResponseEntity<PageResponse<AdminUserResponse>> getUsers(User.Status status, Role.RoleName role,
                                                                    String search, Pageable pageable) {
        Specification<User> spec = (root, query, cb) -> {
            List<Predicate> filters = new ArrayList<>();
            if (status != null) {
                filters.add(cb.equal(root.get("status"), status));
            }
            if (role != null) {
                filters.add(cb.equal(root.get("role").get("name"), role));
            }
            if (search != null && !search.isBlank()) {
                String like = "%" + search.trim().toLowerCase() + "%";
                filters.add(cb.or(cb.like(cb.lower(root.get("username")), like), cb.like(cb.lower(root.get("email")), like)));
            }
            return cb.and(filters.toArray(new Predicate[0]));
        };
        Page<User> users = userRepository.findAll(spec, pageable);
        return ResponseEntity.ok(PageResponse.of(users, this::maptoResponse));
    }

    public ResponseEntity<AdminUserResponse> getUser(Long userId) {
        return ResponseEntity.ok(maptoResponse(findUser(userId)));
    }

    public ResponseEntity<MessageResponse> updateStatus(Long userId, User.Status newStatus) {
        User admin = userService.getCurrentLoggedInUser();
        User target = findUser(userId);

        if (newStatus == User.Status.UNVERIFIED) {
            throw new InvalidInformationException("Status can only be changed to ACTIVE or INACTIVE");
        }
        if (target.getId().equals(admin.getId())) {
            throw new InvalidInformationException("You can't change the status of your own account");
        }
        if (target.getStatus() == newStatus) {
            throw new InvalidInformationException("User is already " + newStatus);
        }
        if (target.getStatus() == User.Status.UNVERIFIED) {
            throw new InvalidInformationException(
                    "This user hasn't verified their email yet, so the account can't be changed");
        }
        target.setStatus(newStatus);
        userRepository.save(target);
        String verb = newStatus == User.Status.INACTIVE ? "deactivated" : "activated";
        auditLogService.log(admin, "USER_" + newStatus, "USER", target.getId(),
                "Admin " + admin.getId() + " " + verb + " user " + target.getId());
        logger.info("Admin {} {} user {}", admin.getId(), verb, target.getId());
        return ResponseEntity.ok(new MessageResponse("User " + verb + " successfully"));
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId).orElseThrow(() -> new InformationNotFoundException("User with id " + userId + " not found"));
    }

    private AdminUserResponse maptoResponse(User user) {
        AdminUserResponse response = new AdminUserResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        response.setRole(user.getRole().getName().name());
        response.setStatus(user.getStatus().name());
        response.setEmailVerified(user.isEmailVerified());
        response.setCreatedAt(user.getCreatedAt());
        UserProfile profile = user.getUserProfile();
        if (profile != null) {
            response.setFirstName(profile.getFirstName());
            response.setLastName(profile.getLastName());
            response.setPhone(profile.getPhone());
            response.setProfileImageUrl(profile.getProfileImageUrl());
        }
        return response;
    }

    public ResponseEntity<Void> deleteUser(Long id) {
        updateStatus(id, User.Status.INACTIVE);
        return ResponseEntity.noContent().build();
    }
}
