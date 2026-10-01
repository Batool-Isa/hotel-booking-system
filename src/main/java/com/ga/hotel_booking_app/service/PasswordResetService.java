package com.ga.hotel_booking_app.service;

import com.ga.hotel_booking_app.dto.reponse.MessageResponse;
import com.ga.hotel_booking_app.dto.request.ForgotPasswordRequest;
import com.ga.hotel_booking_app.dto.request.ResetPasswordRequest;
import com.ga.hotel_booking_app.exception.custom.InformationNotFoundException;
import com.ga.hotel_booking_app.exception.custom.InvalidTokenException;
import com.ga.hotel_booking_app.model.PasswordResetToken;
import com.ga.hotel_booking_app.model.User;
import com.ga.hotel_booking_app.repository.EmailVerificationTokenRepository;
import com.ga.hotel_booking_app.repository.PasswordResetTokenRepository;
import com.ga.hotel_booking_app.repository.UserRepository;
import com.ga.hotel_booking_app.security.JwtUtils;
import com.ga.hotel_booking_app.security.MyUserDetails;
import org.apache.coyote.BadRequestException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Service responsible for all operations related to password managment
 */
@Service
public class PasswordResetService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final AuthenticationManager authenticationManager;
    private MyUserDetails myUserDetails;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private EmailService emailService;


    @Autowired
    public PasswordResetService(UserRepository userRepository,
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

    public ResponseEntity<?> forgotPassword(ForgotPasswordRequest request) {
        User user = userRepository.findUserByEmail(request.getEmail());
        if (user != null) {
            //create reset link
            PasswordResetToken resetPasswordLink = createResetPasswordLink(user);
            // send email with reset link
            emailService.sendResetLink(resetPasswordLink, user);
        }
        return ResponseEntity.ok(new MessageResponse("if an account exists with that email, a reset link has been sent"));

    }

    public PasswordResetToken createResetPasswordLink(User user) {
        PasswordResetToken token = new PasswordResetToken();
        token.setToken(UUID.randomUUID().toString());
        token.setUser(user);
        token.setExpiresAt(LocalDateTime.now().plusMinutes(15));
        return passwordResetTokenRepository.save(token);
    }




    public ResponseEntity<?> resetPassword(String token, ResetPasswordRequest request) throws BadRequestException {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(token)
                .orElseThrow(() -> new InformationNotFoundException("Reset Token not found"));
        if (resetToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new InvalidTokenException("Reset Token has been expired");
        }

        if (resetToken.getUsedAt() != null) {
            throw new InvalidTokenException("Reset Token has been already used");
        }
        if (!request.getPassword().equals(request.getConfirmedPassword())) {
            throw new BadRequestException("Password not matching");
        }
        resetToken.setUsedAt(LocalDateTime.now());
        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        passwordResetTokenRepository.save(resetToken);
        userRepository.save(user);
        return ResponseEntity.ok(new MessageResponse("Password reset successfully"));
    }
}
