package com.ga.hotel_booking_app.service;

import com.ga.hotel_booking_app.exception.custom.InformationNotFoundException;
import com.ga.hotel_booking_app.exception.custom.InvalidCredentialsException;
import com.ga.hotel_booking_app.exception.custom.InvalidTokenException;
import com.ga.hotel_booking_app.model.EmailVerificationToken;
import com.ga.hotel_booking_app.dto.reponse.MessageResponse;
import com.ga.hotel_booking_app.dto.request.LoginRequest;
import com.ga.hotel_booking_app.repository.EmailVerificationTokenRepository;
import com.ga.hotel_booking_app.repository.PasswordResetTokenRepository;
import com.ga.hotel_booking_app.security.JwtUtils;
import com.ga.hotel_booking_app.exception.custom.InformationExistException;
import com.ga.hotel_booking_app.model.User;
import com.ga.hotel_booking_app.repository.UserRepository;
import com.ga.hotel_booking_app.security.MyUserDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Service responsible for all operations realtede to user
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
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private EmailService emailService;


    /**
     * creates user service and injects all required dependancies
     */
    @Autowired
    public UserService(UserRepository userRepository,
                       @Lazy PasswordEncoder passwordEncoder,
                       JwtUtils jwtUtils,
                       @Lazy AuthenticationManager authenticationManager,
                       @Lazy MyUserDetails myUserDetails,
                       EmailVerificationTokenRepository emailVerificationTokenRepository,
                       EmailService emailService,
                       PasswordResetTokenRepository passwordResetTokenRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtUtils = jwtUtils;
        this.myUserDetails = myUserDetails;
        this.emailVerificationTokenRepository = emailVerificationTokenRepository;
        this.emailService = emailService;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
    }


    /**
     * Finds the user by its email address
     * @param email User's email address
     * @return the user that has the given email address
     */
    public User findUserByEmailAddress(String email) {
        return userRepository.findUserByEmail(email);
    }

    /**
     * Register a new user into the system
     *
     * Check if the email address doesn't exists, otherwise throw an exists 409 error.
     * Password is being encoded before saving
     * User status set to unverified at the start
     * An email verification sent with verification token
     *
     * @param user user information provided during registeration
     * @return saved user
     */
    public User register(User user) {
        System.out.println("User service calling ----> register");
        if (!userRepository.existsByEmail(user.getEmail())) {
            user.setPassword(
                    passwordEncoder.encode(user.getPassword())
            );
            user.setStatus(User.Status.UNVERIFIED);
            User savedUser = userRepository.save(user);

            // create email verification token
            EmailVerificationToken token = createEmailVerificationToken(user);
            //send email
            emailService.sendVerificationEmail(token);
            return savedUser;
        } else {
            throw new InformationExistException("User with email " + user.getEmail() + " already exist!");
        }
    }

    /**
     * Creates an email verification token for a user
     * This token is only valid for 24 hours
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
        return "Email Verified Successfully";

    }

}
