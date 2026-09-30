package com.ga.hotel_booking_app.service;

import com.ga.hotel_booking_app.dto.EmailDetails;
import com.ga.hotel_booking_app.dto.request.ForgotPasswordRequest;
import com.ga.hotel_booking_app.exception.custom.InformationNotFoundException;
import com.ga.hotel_booking_app.exception.custom.InvalidCredentialsException;
import com.ga.hotel_booking_app.exception.custom.InvalidVerificationTokenException;
import com.ga.hotel_booking_app.model.EmailVerificationToken;
import com.ga.hotel_booking_app.dto.reponse.LoginResponse;
import com.ga.hotel_booking_app.dto.request.LoginRequest;
import com.ga.hotel_booking_app.repository.EmailVerificationTokenRepository;
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


@Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final AuthenticationManager authenticationManager;
    private MyUserDetails myUserDetails;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private EmailService emailService;

    @Autowired
    public UserService(UserRepository userRepository,
                       @Lazy PasswordEncoder passwordEncoder,
                       JwtUtils jwtUtils,
                       @Lazy AuthenticationManager authenticationManager,
                       @Lazy MyUserDetails myUserDetails,
                       EmailVerificationTokenRepository emailVerificationTokenRepository,
                       EmailService emailService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtUtils = jwtUtils;
        this.myUserDetails = myUserDetails;
        this.emailVerificationTokenRepository = emailVerificationTokenRepository;
        this.emailService = emailService;
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
            System.out.println("Status  :"+user.getStatus());
            if (user.getStatus() == (User.Status.UNVERIFIED)) {
                throw new InvalidCredentialsException("Please verify your email first");

            }
            if (user.getStatus() == (User.Status.INACTIVE)) {
                throw new InvalidCredentialsException("Your account is inactive, please contact admin");

            }
            final String JWT = jwtUtils.generateJwtToken(myUserDetails);
            return ResponseEntity.ok(new LoginResponse(JWT));
        } catch (BadCredentialsException e) {
            throw new InvalidCredentialsException("Invalid email or password");
        }

    }

    public String verifyEmail(String token) {
        EmailVerificationToken verificationToken = emailVerificationTokenRepository.findByToken(token)
                .orElseThrow(() -> new InformationNotFoundException("Email Verification with token " + token + " not found!"));
        if (verificationToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new InvalidVerificationTokenException("Verification Token has been expired");

        }

        if (verificationToken.getUsedAt() != (null)) {
            throw new InvalidVerificationTokenException("Verification token has already been used");
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

    public ResponseEntity<?> forgetPassword(ForgotPasswordRequest request) {
        User user = userRepository.findUserByEmail(request.getEmail());
        if (user != null){
          //  emailService.sendSimpleMail();


            //create reset link
            String resetPasswordLink = createResetPasswordLink();

            // send email
            emailService.sendSimpleMail();

      //If the email is registered, a password reset link has been sent
        }else{
            throw new InformationNotFoundException("User with email "+request.getEmail()+"not found");
        }

    }

    public void createResetPasswordLink(){
        String token = UUID.randomUUID().toString();
        String resetLink = "http://localhost:8000/auth/users/reset-link?token?"+token;

    }
    public ResponseEntity<?> resetPassword(String token) {


    }
}
