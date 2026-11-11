
package com.ga.bookingservice.service;

import com.ga.bookingservice.dto.request.LoginRequest;
import com.ga.bookingservice.dto.request.RegisterRequest;
import com.ga.bookingservice.exception.custom.InformationExistException;
import com.ga.bookingservice.exception.custom.InvalidCredentialsException;
import com.ga.bookingservice.exception.custom.PasswordMismatchException;
import com.ga.bookingservice.model.Role;
import com.ga.bookingservice.model.User;
import com.ga.bookingservice.repository.EmailVerificationTokenRepository;
import com.ga.bookingservice.repository.RoleRepository;
import com.ga.bookingservice.repository.UserRepository;
import com.ga.bookingservice.security.JwtUtils;
import com.ga.bookingservice.security.MyUserDetails;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
class UserServiceTest {
    @MockitoBean
    private UserRepository userRepository;
    @MockitoBean
    private PasswordEncoder passwordEncoder;
    @MockitoBean
    private EmailService emailService;
    @MockitoBean
    private RoleRepository roleRepository;
    @MockitoBean
    private EmailVerificationTokenRepository emailVerificationTokenRepository;
    @MockitoBean
    private AuthenticationManager authenticationManager;
    @MockitoBean
    private JwtUtils jwtUtils;
    @MockitoBean
    private AuditLogService auditLogService;
    @MockitoBean(name = "seedData")
    private CommandLineRunner seedData;
    private final MyUserDetails myUserDetails = mock(MyUserDetails.class);
    @Autowired
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
        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(request.getPassword())).thenReturn("hashedPassword");
        when(roleRepository.findByName(Role.RoleName.CUSTOMER)).thenReturn(Optional.of(customerRole));
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(emailVerificationTokenRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
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
        when(jwtUtils.generateJwtToken(myUserDetails)).thenReturn("jwt-token");
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
                any(UsernamePasswordAuthenticationToken.class))).thenThrow(new BadCredentialsException("Invalid credentials"));
        assertThrows(InvalidCredentialsException.class, () -> userService.loginUser(request)
        );
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
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
        assertThrows(InformationExistException.class, () -> userService.register(request, null)
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
        assertThrows(PasswordMismatchException.class, () -> userService.register(request, null));
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
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);
        when(userRepository.findUserByEmail(request.getEmail())).thenReturn(user);
        assertThrows(InvalidCredentialsException.class, () -> userService.loginUser(request)
        );
        verify(userRepository).findUserByEmail(request.getEmail());
        verifyNoInteractions(jwtUtils);
    }
}