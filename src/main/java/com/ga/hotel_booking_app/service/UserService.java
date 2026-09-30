package com.ga.hotel_booking_app.service;

import com.ga.hotel_booking_app.dto.EmailDetails;
import com.ga.hotel_booking_app.dto.request.ForgotPasswordRequest;
import com.ga.hotel_booking_app.dto.request.ResetPasswordRequest;
import com.ga.hotel_booking_app.exception.custom.InformationNotFoundException;
import com.ga.hotel_booking_app.exception.custom.InvalidCredentialsException;
import com.ga.hotel_booking_app.exception.custom.InvalidTokenException;
import com.ga.hotel_booking_app.model.EmailVerificationToken;
import com.ga.hotel_booking_app.dto.reponse.MessageResponse;
import com.ga.hotel_booking_app.dto.request.LoginRequest;
import com.ga.hotel_booking_app.model.PasswordResetToken;
import com.ga.hotel_booking_app.repository.EmailVerificationTokenRepository;
import com.ga.hotel_booking_app.repository.PasswordResetTokenRepository;
import com.ga.hotel_booking_app.security.JwtUtils;
import com.ga.hotel_booking_app.exception.custom.InformationExistException;
import com.ga.hotel_booking_app.model.User;
import com.ga.hotel_booking_app.repository.UserRepository;
import com.ga.hotel_booking_app.security.MyUserDetails;
import org.apache.coyote.BadRequestException;
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


    public User findUserByEmailAddress(String email) {
        return userRepository.findUserByEmail(email);
    }

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
            sendVerificationEmail(token);
            return savedUser;
        } else {
            throw new InformationExistException("User with email " + user.getEmail() + " already exist!");
        }
    }

    public EmailVerificationToken createEmailVerificationToken(User user) {
        System.out.println("User Service Calling create Verification Token");
        EmailVerificationToken emailVerificationToken = new EmailVerificationToken();
        emailVerificationToken.setUser(user);
        emailVerificationToken.setToken(UUID.randomUUID().toString());
        emailVerificationToken.setExpiresAt(LocalDateTime.now().plusHours(24));
        return emailVerificationTokenRepository.save(emailVerificationToken);

    }

    public void sendVerificationEmail(EmailVerificationToken emailVerificationToken) {
        String verificationLink =
                "http://localhost:8000/auth/users/verify-email?token="
                        + emailVerificationToken.getToken();
        String emailBody = """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <title>Verify your VibeStay account</title>
                </head>
                
                <body style="
                    margin: 0;
                    padding: 0;
                    background-color: #f4f6f8;
                    font-family: Arial, sans-serif;
                ">
                
                    <div style="
                        max-width: 600px;
                        margin: 40px auto;
                        background-color: white;
                        border-radius: 12px;
                        overflow: hidden;
                        box-shadow: 0 4px 15px rgba(0,0,0,0.08);
                    ">
                
                        <div style="
                            background-color: #6c63ff;
                            padding: 30px;
                            text-align: center;
                            color: white;
                        ">
                            <h1 style="margin: 0;">
                                VibeStay
                            </h1>
                
                            <p style="margin-top: 10px;">
                                Your stay starts here
                            </p>
                        </div>
                
                        <div style="padding: 35px;">
                
                            <h2>
                                Verify your email
                            </h2>
                
                            <p>
                                Hello,
                            </p>
                
                            <p>
                                Thank you for creating your VibeStay account.
                                Please verify your email address to activate
                                your account.
                            </p>
                
                            <div style="text-align: center; margin: 30px 0;">
                
                                <a href="%s"
                                   style="
                                       display: inline-block;
                                       padding: 14px 28px;
                                       background-color: #6c63ff;
                                       color: white;
                                       text-decoration: none;
                                       border-radius: 8px;
                                       font-weight: bold;
                                   ">
                                    Verify My Email
                                </a>
                
                            </div>
                
                            <p>
                                This verification link will expire in
                                <strong>24 hours</strong>.
                            </p>
                
                            <p>
                                If you did not create a VibeStay account,
                                you can safely ignore this email.
                            </p>
                
                        </div>
                
                        <div style="
                            background-color: #f4f6f8;
                            padding: 20px;
                            text-align: center;
                            color: #777;
                            font-size: 12px;
                        ">
                            © 2026 VibeStay. All rights reserved.
                        </div>
                
                    </div>
                
                </body>
                </html>
                """.formatted(verificationLink);

        EmailDetails emailDetails = new EmailDetails();
        emailDetails.setSubject("Verify your VibeStay Account");
        emailDetails.setMsgBody(emailBody);
        emailDetails.setRecipient(emailVerificationToken.getUser().getEmail());
        System.out.println("sending verification email");
        emailService.sendSimpleMail(emailDetails);

    }

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

    public ResponseEntity<?> forgotPassword(ForgotPasswordRequest request) {
        User user = userRepository.findUserByEmail(request.getEmail());
        if (user != null) {
            //create reset link
            PasswordResetToken resetPasswordLink = createResetPasswordLink(user);
            // send email with reset link
            sendResetLink(resetPasswordLink, user);
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

    public void sendResetLink(PasswordResetToken token, User user) {
        String link = "http://localhost:8000/auth/users/reset-link?token=" + token.getToken();

        String emailBody = """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>Reset Your VibeStay Password</title>
                </head>
                
                <body style="
                    margin: 0;
                    padding: 0;
                    background-color: #f5f7fb;
                    font-family: Arial, Helvetica, sans-serif;
                    color: #333333;
                ">
                
                    <div style="
                        width: 100%%;
                        padding: 40px 0;
                    ">
                
                        <div style="
                            max-width: 600px;
                            margin: 0 auto;
                            background-color: #ffffff;
                            border-radius: 12px;
                            overflow: hidden;
                            box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
                        ">
                
                            <!-- Header -->
                            <div style="
                                background-color: #6c63ff;
                                padding: 32px 20px;
                                text-align: center;
                                color: #ffffff;
                            ">
                                <h1 style="
                                    margin: 0;
                                    font-size: 30px;
                                    font-weight: bold;
                                ">
                                    VibeStay
                                </h1>
                
                                <p style="
                                    margin: 8px 0 0;
                                    font-size: 14px;
                                    opacity: 0.9;
                                ">
                                    Your stay starts here
                                </p>
                            </div>
                
                
                            <!-- Main Content -->
                            <div style="
                                padding: 40px 35px;
                            ">
                
                                <h2 style="
                                    margin: 0 0 20px;
                                    font-size: 24px;
                                    color: #222222;
                                ">
                                    Reset Your Password
                                </h2>
                
                                <p style="
                                    margin: 0 0 16px;
                                    font-size: 15px;
                                    line-height: 1.7;
                                ">
                                    Hello,
                                </p>
                
                                <p style="
                                    margin: 0 0 20px;
                                    font-size: 15px;
                                    line-height: 1.7;
                                    color: #555555;
                                ">
                                    We received a request to reset the password for your
                                    VibeStay account.
                                </p>
                
                                <p style="
                                    margin: 0 0 30px;
                                    font-size: 15px;
                                    line-height: 1.7;
                                    color: #555555;
                                ">
                                    Click the button below to create a new password
                                    and regain access to your account.
                                </p>
                
                
                                <!-- Reset Button -->
                                <div style="
                                    text-align: center;
                                    margin: 30px 0;
                                ">
                
                                    <a href="%s"
                                       style="
                                           display: inline-block;
                                           padding: 14px 32px;
                                           background-color: #6c63ff;
                                           color: #ffffff;
                                           text-decoration: none;
                                           border-radius: 8px;
                                           font-size: 15px;
                                           font-weight: bold;
                                       ">
                                        Reset My Password
                                    </a>
                
                                </div>
                
                
                                <!-- Expiry Notice -->
                                <div style="
                                    margin: 30px 0;
                                    padding: 16px 18px;
                                    background-color: #f4f3ff;
                                    border-left: 4px solid #6c63ff;
                                    border-radius: 6px;
                                ">
                
                                    <p style="
                                        margin: 0;
                                        font-size: 14px;
                                        line-height: 1.6;
                                        color: #555555;
                                    ">
                                        <strong style="color: #333333;">
                                            Important:
                                        </strong>
                                        This password reset link will expire in
                                        <strong>15 minutes</strong>.
                                    </p>
                
                                </div>
                
                
                                <!-- Security Message -->
                                <p style="
                                    margin: 25px 0 0;
                                    font-size: 13px;
                                    line-height: 1.7;
                                    color: #777777;
                                ">
                                    If you did not request a password reset, you can
                                    safely ignore this email. Your password will remain
                                    unchanged.
                                </p>
                
                            </div>
                
                
                            <!-- Footer -->
                            <div style="
                                background-color: #f5f7fb;
                                padding: 20px;
                                text-align: center;
                            ">
                
                                <p style="
                                    margin: 0;
                                    font-size: 12px;
                                    color: #888888;
                                ">
                                    © 2026 VibeStay. All rights reserved.
                                </p>
                
                                <p style="
                                    margin: 6px 0 0;
                                    font-size: 12px;
                                    color: #aaaaaa;
                                ">
                                    Your stay starts here.
                                </p>
                
                            </div>
                
                        </div>
                
                    </div>
                
                </body>
                </html>
                """.formatted(link);
        EmailDetails emailDetails = new EmailDetails();
        emailDetails.setSubject("Reset");
        emailDetails.setMsgBody(emailBody);
        emailDetails.setRecipient(user.getEmail());
        emailService.sendSimpleMail(emailDetails);
        System.out.println("sending reset password email");
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
