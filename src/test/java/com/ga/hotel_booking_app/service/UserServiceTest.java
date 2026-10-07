
package com.ga.hotel_booking_app.service;

import com.ga.hotel_booking_app.dto.request.LoginRequest;
import com.ga.hotel_booking_app.dto.request.RegisterRequest;
import com.ga.hotel_booking_app.exception.custom.InformationExistException;
import com.ga.hotel_booking_app.exception.custom.InvalidCredentialsException;
import com.ga.hotel_booking_app.exception.custom.PasswordMismatchException;
import com.ga.hotel_booking_app.model.Role;
import com.ga.hotel_booking_app.model.User;
import com.ga.hotel_booking_app.repository.EmailVerificationTokenRepository;
import com.ga.hotel_booking_app.repository.RoleRepository;
import com.ga.hotel_booking_app.repository.UserRepository;
import com.ga.hotel_booking_app.security.JwtUtils;
import com.ga.hotel_booking_app.security.MyUserDetails;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EmailService emailService;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private EmailVerificationTokenRepository emailVerificationTokenRepository;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private MyUserDetails myUserDetails;

    @InjectMocks
    private UserService userService;

    @Test
    @DisplayName("Register a new customer")
    void registerAsACustomer() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("testuser");
        request.setEmail("test@example.com");
        request.setPassword("Password123!");
        request.setConfirmedPassword("Password123!");
        request.setFirstName("Test");
        request.setLastName("User");
        request.setPhone("12345678");
        MockMultipartFile image = new MockMultipartFile("image", "profile.jpg", "image/jpeg", "fake image".getBytes()
        );
        Role customerRole = new Role();
        customerRole.setName(Role.RoleName.CUSTOMER);
        User savedUser = new User();
        savedUser.setId(1L);
        savedUser.setEmail("test@example.com");
        when(userRepository.existsByEmail(request.getEmail()))
                .thenReturn(false);
        when(passwordEncoder.encode(request.getPassword()))
                .thenReturn("hashedPassword");
        when(roleRepository.findByName(Role.RoleName.CUSTOMER))
                .thenReturn(Optional.of(customerRole));
        when(userRepository.save(any(User.class)))
                .thenReturn(savedUser);
        when(emailVerificationTokenRepository.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        var response = userService.register(request, image);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        verify(userRepository).save(any(User.class));
        verify(passwordEncoder).encode(request.getPassword());
        verify(emailService).sendVerificationEmail(any());
        verify(emailVerificationTokenRepository).save(any());
    }


    @Test
    @DisplayName("Login with valid credentials")
    void loginWithValidCredentials() {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("Password123!");
        User user = new User();
        user.setEmail("test@example.com");
        user.setEmailVerified(true);
        user.setStatus(User.Status.ACTIVE);
        Authentication authentication = mock(Authentication.class);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(myUserDetails);
        when(userRepository.findUserByEmail(request.getEmail())).thenReturn(user);
        when(jwtUtils.generateJwtToken(myUserDetails))
                .thenReturn("fake-jwt-token");

        var response = userService.loginUser(request);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(userRepository).findUserByEmail(request.getEmail());
        verify(jwtUtils).generateJwtToken(myUserDetails);
    }

    @Test
    @DisplayName("Login with invalid credentials throws InvalidCredentialsException")
    void loginWithInvalidCredentials() {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("123123");
        when(authenticationManager.authenticate(
                any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Invalid credentials"));
        assertThrows(InvalidCredentialsException.class, () -> userService.loginUser(request)
        );
        verify(authenticationManager)
                .authenticate(any(UsernamePasswordAuthenticationToken.class));
        verifyNoInteractions(userRepository);
        verifyNoInteractions(jwtUtils);
    }

        @Test
        @DisplayName("Register with existing email throws InformationExistException")
        void registerWithExistingEmail() {
            RegisterRequest request = new RegisterRequest();
            request.setUsername("testuser");
            request.setEmail("test@example.com");
            request.setPassword("Password123!");
            request.setConfirmedPassword("Password123!");
            when(userRepository.existsByEmail(request.getEmail())).thenReturn(true);
            assertThrows(InformationExistException.class,
                    () -> userService.register(request, null)
            );
            verify(userRepository).existsByEmail(request.getEmail());
            verifyNoInteractions(passwordEncoder);
            verifyNoInteractions(emailService);
        }

        @Test
        @DisplayName("Register with mismatched passwords throws PasswordMismatchException")
        void registerWithMismatchedPasswords() {
            RegisterRequest request = new RegisterRequest();
            request.setUsername("testuser");
            request.setEmail("test@example.com");
            request.setPassword("Password123!");
            request.setConfirmedPassword("Different123!");
            when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
            assertThrows(PasswordMismatchException.class, () -> userService.register(request, null)
            );
            verify(userRepository).existsByEmail(request.getEmail());
            verifyNoInteractions(passwordEncoder);
            verifyNoInteractions(emailService);
        }



        @Test
        @DisplayName("Login with inactive user throws InvalidCredentialsException")
        void loginWithInactiveUser() {
            LoginRequest request = new LoginRequest();
            request.setEmail("test@example.com");
            request.setPassword("Password123!");
            User user = new User();
            user.setEmail("test@example.com");
            user.setEmailVerified(true);
            user.setStatus(User.Status.INACTIVE);
            Authentication authentication = mock(Authentication.class);
            when(authenticationManager.authenticate(
                    any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);
            when(userRepository.findUserByEmail(request.getEmail()))
                    .thenReturn(user);
            assertThrows(InvalidCredentialsException.class, () -> userService.loginUser(request)
            );
            verify(userRepository)
                    .findUserByEmail(request.getEmail());
            verifyNoInteractions(jwtUtils);
        }

}