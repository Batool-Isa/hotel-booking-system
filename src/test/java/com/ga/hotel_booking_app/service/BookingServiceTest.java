package com.ga.hotel_booking_app.service;

import com.ga.hotel_booking_app.dto.request.BookingGuestRequest;
import com.ga.hotel_booking_app.dto.request.BookingRequest;
import com.ga.hotel_booking_app.dto.request.BookingRoomRequest;
import com.ga.hotel_booking_app.dto.request.BookingStatusRequest;
import com.ga.hotel_booking_app.exception.custom.InvalidInformationException;
import com.ga.hotel_booking_app.exception.custom.UnauthorizedActionException;
import com.ga.hotel_booking_app.model.*;
import com.ga.hotel_booking_app.repository.BookingRepository;
import com.ga.hotel_booking_app.repository.HotelRepository;
import com.ga.hotel_booking_app.repository.RoomRepository;
import com.ga.hotel_booking_app.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private HotelRepository hotelRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private BookingService bookingService;

    @Test
    @DisplayName("Create booking when room is available")
    void createNewBooking() {
        User customer = new User();
        customer.setId(1L);
        customer.setEmail("test@example.com");
        customer.setStatus(User.Status.ACTIVE);
        Role role = new Role();
        role.setName(Role.RoleName.CUSTOMER);
        customer.setRole(role);
        Hotel hotel = new Hotel();
        hotel.setId(1L);
        hotel.setName("VibeStay-hotel");
        ChildPolicy childPolicy = new ChildPolicy();
        childPolicy.setInfantMaxAge(2);
        childPolicy.setChildMaxAge(12);
        childPolicy.setInfantsCountTowardOccupancy(false);
        hotel.setChildPolicy(childPolicy);
        Room room = new Room();
        room.setId(1L);
        room.setHotel(hotel);
        room.setStatus(Room.Status.ACTIVE);
        room.setPricePerNight(new BigDecimal("50.00"));
        room.setMaxAdults(2);
        room.setMaxChildren(2);
        room.setMaxOccupancy(4);
        BookingGuestRequest guestRequest = new BookingGuestRequest();
        guestRequest.setName("Test User");
        guestRequest.setAge(25);
        BookingRoomRequest roomRequest = new BookingRoomRequest();
        roomRequest.setRoomId(1L);
        roomRequest.setGuests(List.of(guestRequest));

        BookingRequest request = new BookingRequest();
        request.setHotelId(1L);
        request.setCheckIn(LocalDate.now().plusDays(5));
        request.setCheckOut(LocalDate.now().plusDays(7));
        request.setRooms(List.of(roomRequest));

        when(hotelRepository.findById(1L))
                .thenReturn(Optional.of(hotel));

        when(roomRepository.findById(1L))
                .thenReturn(Optional.of(room));

        when(bookingRepository.findOverlappingBookings(
                eq(1L),
                eq(Booking.Status.CONFIRMED),
                any(LocalDate.class),
                any(LocalDate.class)))
                .thenReturn(Collections.emptyList());

        when(userRepository.findUserByEmail("test@example.com"))
                .thenReturn(customer);

        when(bookingRepository.save(any(Booking.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "test@example.com",
                        null
                )
        );

        var response = bookingService.createNewBooking(request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());

        verify(bookingRepository).save(any(Booking.class));
        verify(notificationService).sendBookingNotification(any(Booking.class));
        verify(emailService).sendBookingConfirmationEmail(any(Booking.class));
        verify(auditLogService).log(
                eq(customer),
                eq("BOOKING_CONFIRMED"),
                eq("BOOKING"),
                any(),
                anyString()
        );

        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Create booking with overlapping room throws custom InvalidInformationException")
    void createBookingWithUnavailableRoom() {
        Hotel hotel = new Hotel();
        hotel.setId(1L);
        hotel.setName("VibeStay-hotel");
        ChildPolicy childPolicy = new ChildPolicy();
        childPolicy.setInfantMaxAge(2);
        childPolicy.setChildMaxAge(12);
        childPolicy.setInfantsCountTowardOccupancy(false);
        hotel.setChildPolicy(childPolicy);
        Room room = new Room();
        room.setId(1L);
        room.setHotel(hotel);
        room.setStatus(Room.Status.ACTIVE);
        room.setPricePerNight(new BigDecimal("50.00"));
        room.setMaxAdults(2);
        room.setMaxChildren(2);
        room.setMaxOccupancy(4);
        BookingGuestRequest guestRequest = new BookingGuestRequest();
        guestRequest.setName("Test User");
        guestRequest.setAge(25);
        BookingRoomRequest roomRequest = new BookingRoomRequest();
        roomRequest.setRoomId(1L);
        roomRequest.setGuests(List.of(guestRequest));
        BookingRequest request = new BookingRequest();
        request.setHotelId(1L);
        request.setCheckIn(LocalDate.now().plusDays(5));
        request.setCheckOut(LocalDate.now().plusDays(7));
        request.setRooms(List.of(roomRequest));
        Booking existingBooking = new Booking();
        when(hotelRepository.findById(1L)).thenReturn(Optional.of(hotel));
        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));
        when(bookingRepository.findOverlappingBookings(eq(1L), eq(Booking.Status.CONFIRMED),
                any(LocalDate.class), any(LocalDate.class))).thenReturn(List.of(existingBooking));

        assertThrows(InvalidInformationException.class, () -> bookingService.createNewBooking(request));
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    @DisplayName("Create booking with invalid dates throws InvalidInformationException")
    void createBookingWithInvalidDates() {
        BookingRequest request = new BookingRequest();
        request.setHotelId(1L);
        request.setCheckIn(LocalDate.now().plusDays(5));
        request.setCheckOut(LocalDate.now().plusDays(3));
        assertThrows(InvalidInformationException.class, () -> bookingService.createNewBooking(request));
        verifyNoInteractions(hotelRepository);
        verifyNoInteractions(roomRepository);
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    @DisplayName("Cancel confirmed booking successfully")
    void cancelBooking() {
        User customer = new User();
        customer.setId(1L);
        customer.setEmail("test@example.com");
        customer.setStatus(User.Status.ACTIVE);
        Role role = new Role();
        role.setName(Role.RoleName.CUSTOMER);
        customer.setRole(role);
        Hotel hotel = new Hotel();
        hotel.setId(1L);
        hotel.setName("VibeStay");
        Booking booking = new Booking();
        booking.setId(1L);
        booking.setBookingReference("VI1234");
        booking.setUser(customer);
        booking.setHotel(hotel);
        booking.setStatus(Booking.Status.CONFIRMED);
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(userRepository.findUserByEmail("test@example.com")).thenReturn(customer);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "test@example.com", null)
        );

        var response = bookingService.cancelBooking(1L);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(Booking.Status.CANCELLED, booking.getStatus());
        verify(bookingRepository).save(booking);
        verify(notificationService).sendBookingNotification(booking);
        verify(emailService).sendBookingCancellationEmail(booking);
        verify(auditLogService).log(
                eq(customer), eq("BOOKING_CANCELLED"), eq("BOOKING"),
                eq(1L), anyString()
        );

        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Cancel already cancelled booking throws InvalidInformationException")
    void cancelAlreadyCancelledBooking() {
        User customer = new User();
        customer.setId(1L);
        customer.setEmail("test@example.com");
        Role role = new Role();
        role.setName(Role.RoleName.CUSTOMER);
        customer.setRole(role);
        Booking booking = new Booking();
        booking.setId(1L);
        booking.setUser(customer);
        booking.setStatus(Booking.Status.CANCELLED);
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(userRepository.findUserByEmail("test@example.com")).thenReturn(customer);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("test@example.com", null
                )
        );
        assertThrows(InvalidInformationException.class, () -> bookingService.cancelBooking(1L)
        );

        verify(bookingRepository, never()).save(any(Booking.class));

        SecurityContextHolder.clearContext();
    }


    @Test
    @DisplayName("Customer cannot cancel another customer's booking")
    void customerCannotCancelAnotherUsersBooking() {
        User customer = new User();
        customer.setId(1L);
        customer.setEmail("test@example.com");
        Role role = new Role();
        role.setName(Role.RoleName.CUSTOMER);
        customer.setRole(role);
        User bookingOwner = new User();
        bookingOwner.setId(2L);
        Booking booking = new Booking();
        booking.setId(1L);
        booking.setUser(bookingOwner);
        booking.setStatus(Booking.Status.CONFIRMED);
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(userRepository.findUserByEmail("test@example.com")).thenReturn(customer);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "test@example.com",
                        null
                )
        );
        assertThrows(
                UnauthorizedActionException.class,
                () -> bookingService.cancelBooking(1L)
        );
        verify(bookingRepository, never()).save(any(Booking.class));

        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Hotel manager can update confirmed booking to completed")
    void updateBookingStatus() {
        User manager = new User();
        manager.setId(1L);
        manager.setEmail("manager@example.com");
        Role role = new Role();
        role.setName(Role.RoleName.HOTEL_MANAGER);
        manager.setRole(role);
        Hotel hotel = new Hotel();
        hotel.setId(1L);
        hotel.setName("VibeStay");
        hotel.setManagers(Set.of(manager));

        Booking booking = new Booking();
        booking.setId(1L);
        booking.setBookingReference("VI1234");
        booking.setHotel(hotel);
        booking.setStatus(Booking.Status.CONFIRMED);
        BookingStatusRequest request = new BookingStatusRequest();
        request.setStatus(Booking.Status.COMPLETED);
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(userRepository.findUserByEmail("manager@example.com")).thenReturn(manager);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "manager@example.com", null)
        );
        var response = bookingService.updateBookingStatus(1L, request);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(Booking.Status.COMPLETED, booking.getStatus());
        verify(bookingRepository).save(booking);
        verify(notificationService).sendBookingNotification(booking);
        verify(auditLogService).log(eq(manager), eq("BOOKING_UPDATE"),
                eq("BOOKING"), eq(1L), anyString()
        );

        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Customer cannot update booking status")
    void customerCannotUpdateBookingStatus() {
        User customer = new User();
        customer.setId(1L);
        customer.setEmail("test@example.com");
        Role role = new Role();
        role.setName(Role.RoleName.CUSTOMER);
        customer.setRole(role);
        Booking booking = new Booking();
        booking.setId(1L);
        booking.setStatus(Booking.Status.CONFIRMED);

        BookingStatusRequest request = new BookingStatusRequest();
        request.setStatus(Booking.Status.COMPLETED);
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

        when(userRepository.findUserByEmail("test@example.com")).thenReturn(customer);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "test@example.com", null
                )
        );
        assertThrows(
                UnauthorizedActionException.class, () -> bookingService.updateBookingStatus(1L, request)
        );
        verify(bookingRepository, never()).save(any(Booking.class));
        SecurityContextHolder.clearContext();
    }
}