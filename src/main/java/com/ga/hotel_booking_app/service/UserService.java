package com.ga.hotel_booking_app.service;

import com.ga.hotel_booking_app.exception.custom.InvalidCredentialsException;
import com.ga.hotel_booking_app.model.reponse.LoginResponse;
import com.ga.hotel_booking_app.model.request.LoginRequest;
import com.ga.hotel_booking_app.security.JwtUtils;
import com.ga.hotel_booking_app.exception.custom.InformationExistException;
import com.ga.hotel_booking_app.model.User;
import com.ga.hotel_booking_app.repository.UserRepository;
import com.ga.hotel_booking_app.security.MyUserDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;


@Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final AuthenticationManager authenticationManager;
    private MyUserDetails myUserDetails;

    @Autowired
    public UserService(UserRepository userRepository,
                       @Lazy PasswordEncoder passwordEncoder,
                       JwtUtils jwtUtils,
                       @Lazy AuthenticationManager authenticationManager,
                       @Lazy MyUserDetails myUserDetails) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtUtils = jwtUtils;
        this.myUserDetails = myUserDetails;
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
            return userRepository.save(user);
        } else {
            throw new InformationExistException("User with email " + user.getEmail() + " already exist!");
        }
    }

    public ResponseEntity<?> loginUser(LoginRequest loginRequest) {
      try {
          Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword()));
          SecurityContextHolder.getContext().setAuthentication(authentication);
          myUserDetails = (MyUserDetails) authentication.getPrincipal();
          final String JWT = jwtUtils.generateJwtToken(myUserDetails);
          return ResponseEntity.ok(new LoginResponse(JWT));
      } catch (Exception e) {
         throw new InvalidCredentialsException("Invalid email or password");
      }

    }
}
