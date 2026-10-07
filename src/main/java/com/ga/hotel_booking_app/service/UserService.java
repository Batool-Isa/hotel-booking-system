package com.ga.hotel_booking_app.service;

import com.ga.hotel_booking_app.dto.reponse.UserProfileResponse;
import com.ga.hotel_booking_app.dto.request.RegisterRequest;
import com.ga.hotel_booking_app.dto.request.UpdateProfileRequest;
import com.ga.hotel_booking_app.exception.custom.*;
import com.ga.hotel_booking_app.model.EmailVerificationToken;
import com.ga.hotel_booking_app.dto.reponse.MessageResponse;
import com.ga.hotel_booking_app.dto.request.LoginRequest;
import com.ga.hotel_booking_app.model.Role;
import com.ga.hotel_booking_app.model.UserProfile;
import com.ga.hotel_booking_app.repository.EmailVerificationTokenRepository;
import com.ga.hotel_booking_app.repository.RoleRepository;
import com.ga.hotel_booking_app.security.JwtUtils;
import com.ga.hotel_booking_app.model.User;
import com.ga.hotel_booking_app.repository.UserRepository;
import com.ga.hotel_booking_app.security.MyUserDetails;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

/**
 * Service responsible for all operations related to user
 * such as login, register and verify email.
 */
@Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final AuthenticationManager authenticationManager;
    private MyUserDetails myUserDetails;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final EmailService emailService;
    private final RoleRepository roleRepository;
    @Autowired
    private AuditLogService auditLogService;
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    /**
     * creates user service and injects all required dependencies
     */
    @Autowired
    public UserService(UserRepository userRepository,
                       @Lazy PasswordEncoder passwordEncoder,
                       JwtUtils jwtUtils,
                       @Lazy AuthenticationManager authenticationManager,
                       @Lazy MyUserDetails myUserDetails,
                       EmailVerificationTokenRepository emailVerificationTokenRepository,
                       EmailService emailService,
                       RoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtUtils = jwtUtils;
        this.myUserDetails = myUserDetails;
        this.emailVerificationTokenRepository = emailVerificationTokenRepository;
        this.emailService = emailService;
        this.roleRepository = roleRepository;
    }

    public User getCurrentLoggedInUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        return userRepository.findUserByEmail(email);
    }

    /**
     * Finds the user by its email address
     *
     * @param email User's email address
     * @return the user that has the given email address
     */
    public User findUserByEmailAddress(String email) {
        return userRepository.findUserByEmail(email);
    }

    private final String UPLOAD_DIR = "uploads/";

    /**
     * Register a new user into the system
     * <p>
     * Check if the email address doesn't exists, otherwise throw an exists 409 error.
     * Password is being encoded before saving
     * User status set to unverified at the start
     * An email verification sent with verification token
     *
     * @param request that holds all user information
     * @param image   profile image
     * @return saved user
     */
    public ResponseEntity<?> register(RegisterRequest request, MultipartFile image) {
        System.out.println("User service calling ----> register");
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new InformationExistException(
                    "User with email " + request.getEmail() + " already exists!"
            );
        }
        if (!request.getPassword().equals(request.getConfirmedPassword())) {
            throw new PasswordMismatchException(
                    "Password and confirmation password do not match"
            );
        }
        UserProfile profile = new UserProfile();
        profile.setFirstName(request.getFirstName());
        profile.setLastName(request.getLastName());
        profile.setPhone(request.getPhone());

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setStatus(User.Status.UNVERIFIED);

        String profileImage = uploadeImage(image);
        profile.setProfileImageUrl(profileImage);

        Role role = roleRepository.findByName(Role.RoleName.CUSTOMER)
                .orElseThrow(()-> new InformationExistException("Customer role not found"));
        user.setRole(role);
        user.setUserProfile(profile);
        User savedUser = userRepository.save(user);
        EmailVerificationToken token = createEmailVerificationToken(savedUser);
        emailService.sendVerificationEmail(token);
        auditLogService.log(savedUser, "REGISTER", "User", savedUser.getId(),
                "User "+savedUser.getUsername()+" registered successfully"
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(new MessageResponse(
                "Registered successfully, please verify your email"
        ));
    }

    public String uploadeImage(MultipartFile image) {
        try {
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            String originalNameFile = image.getOriginalFilename();
            //generate uniq id
            String uniqueId = UUID.randomUUID().toString().substring(10);
            // create file name
            String imageFileName = uniqueId + "-" + originalNameFile;
            // create file path
            Path filePath = uploadPath.resolve(imageFileName);

            image.transferTo(filePath);
            return (UPLOAD_DIR + imageFileName);
        } catch (IOException e) {
            throw new RuntimeException("Could not save image", e);
        }
    }

    /**
     * Creates an email verification token for a user
     * This token is only valid for 24 hours
     *
     * @param user new user
     * @return token created
     */
    public EmailVerificationToken createEmailVerificationToken(User user) {
        System.out.println("User Service Calling create Verification Token");
        EmailVerificationToken emailVerificationToken = new EmailVerificationToken();
        emailVerificationToken.setUser(user);
        emailVerificationToken.setToken(UUID.randomUUID().toString());
        emailVerificationToken.setExpiresAt(LocalDateTime.now().plusHours(24));
        return emailVerificationTokenRepository.save(emailVerificationToken);

    }


    /**
     * Authenticates a user and generate a JWT token
     *
     * @param loginRequest request with user credentials ( email and password )
     * @return login response with Jwt token
     */
    public ResponseEntity<?> loginUser(LoginRequest loginRequest) {
        try {
            Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword()));
            SecurityContextHolder.getContext().setAuthentication(authentication);
            myUserDetails = (MyUserDetails) authentication.getPrincipal();
            User user = findUserByEmailAddress(loginRequest.getEmail());
            System.out.println("Status  :" + user.getStatus());
            if (user.getStatus() == (User.Status.UNVERIFIED)) {
                throw new InvalidCredentialsException("Please verify your email first");

            }
            if (user.getStatus() == (User.Status.INACTIVE)) {
                throw new InvalidCredentialsException("Your account is inactive, please contact admin");

            }
            final String JWT = jwtUtils.generateJwtToken(myUserDetails);
            auditLogService.log(
                    user,
                    "Login",
                    "User",
                    user.getId(),
                    "User "+user.getUsername()+" logged in successfully"
            );
            return ResponseEntity.ok(new MessageResponse(JWT));
        } catch (BadCredentialsException e) {
            throw new InvalidCredentialsException("Invalid email or password");
        }

    }


    public String verifyEmail(String token) {
        EmailVerificationToken verificationToken = emailVerificationTokenRepository.findByToken(token)
                .orElseThrow(() -> new InformationNotFoundException("Email Verification with token " + token + " not found!"));
        if (verificationToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new InvalidTokenException("Verification Token has been expired");

        }

        if (verificationToken.getUsedAt() != (null)) {
            throw new InvalidTokenException("Verification token has already been used");
        }

        User user = verificationToken.getUser();
        user.setEmailVerified(true);
        user.setStatus(User.Status.ACTIVE);
        // update verification token
        verificationToken.setUsedAt(LocalDateTime.now());
        userRepository.save(user);
        emailVerificationTokenRepository.save(verificationToken);
        auditLogService.log(
                user,
                "EMAIL VERIFICATION",
                "User",
                user.getId(),
                "User "+user.getUsername()+" email verified successfully"
        );
        return "Email Verified Successfully";

    }

    public ResponseEntity<?> getUserProfile() {
        User user = getCurrentLoggedInUser();
        UserProfile profile = user.getUserProfile();
        UserProfileResponse response = new UserProfileResponse();
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        response.setFirstName(profile.getFirstName());
        response.setLastName(profile.getLastName());
        response.setPhone(profile.getPhone());
        response.setProfileImageUrl(profile.getProfileImageUrl());
        response.setRole(user.getRole());
        return ResponseEntity.ok(response);
    }

    public ResponseEntity<?> updateUserProfile(@Valid UpdateProfileRequest request) {
        User user = getCurrentLoggedInUser();
        UserProfile profile = user.getUserProfile();
        user.setUsername(request.getUsername());
        profile.setFirstName(request.getFirstName());
        profile.setLastName(request.getLastName());
        profile.setPhone(request.getPhone());
        user.setUserProfile(profile);
        userRepository.save(user);
        auditLogService.log(
                user, "UPDATE PROFILE", "User", user.getId(),
                "User "+user.getUsername()+" updated his/her profile successfully"
        );
        return ResponseEntity.ok(new MessageResponse("Profile updated successfully"));
    }

    public ResponseEntity<?> updateProfileImage(MultipartFile image) {
        User user = getCurrentLoggedInUser();
        UserProfile profile = user.getUserProfile();
        String profileImage = uploadeImage(image);
        profile.setProfileImageUrl(profileImage);
        user.setUserProfile(profile);
        userRepository.save(user);
        auditLogService.log(
                user, "UPDATE IMAGE PROFILE", "User", user.getId(),
                "User "+user.getUsername()+" updated his/her image profile successfully"
        );
        return ResponseEntity.ok(new MessageResponse("Image Profile updated successfully"));
    }
}
