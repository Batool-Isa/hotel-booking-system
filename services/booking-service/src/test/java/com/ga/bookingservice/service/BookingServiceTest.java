package com.ga.bookingservice.service;

import com.ga.bookingservice.dto.request.BookingGuestRequest;
import com.ga.bookingservice.dto.request.BookingRequest;
import com.ga.bookingservice.dto.request.BookingRoomRequest;
import com.ga.bookingservice.dto.request.BookingStatusRequest;
import com.ga.bookingservice.exception.custom.InvalidInformationException;
import com.ga.bookingservice.exception.custom.RoomUnavailableException;
import com.ga.bookingservice.exception.custom.UnauthorizedActionException;
import com.ga.bookingservice.model.*;
import com.ga.bookingservice.repository.BookingRepository;
import com.ga.bookingservice.repository.HotelRepository;
import com.ga.bookingservice.repository.RoomRepository;
import com.ga.bookingservice.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
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

    @AfterEach
    void clearSecurityContext() {
        // runs even when a test fails, so a login never leaks into the next test
        SecurityContextHolder.clearContext();
    }

    private User loginActiveCustomer() {
        User customer = new User();
        customer.setId(1L);
        customer.setEmail("test@example.com");
        customer.setStatus(User.Status.ACTIVE);
        Role role = new Role();
        role.setName(Role.RoleName.CUSTOMER);
        customer.setRole(role);
        when(userRepository.findUserByEmail("test@example.com")).thenReturn(customer);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("test@example.com", null)
        );
        return customer;
    }

    @Test
    @DisplayName("Create booking when room is available")
    void createNewBooking() {
        User customer = loginActiveCustomer();
        Hotel hotel = new Hotel();
        hotel.setId(1L);
        hotel.setName("VibeStay-hotel");
        hotel.setStatus(Hotel.Status.ACTIVE);
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
        RoomType roomType = new RoomType();
        roomType.setName("Deluxe");
        room.setRoomType(roomType);
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

        when(roomRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(room));

        when(roomRepository.findById(1L))
                .thenReturn(Optional.of(room));

        when(bookingRepository.findOverlappingBookings(
                eq(1L),
                any(LocalDate.class),
                any(LocalDate.class),
                any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());

        when(bookingRepository.save(any(Booking.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = bookingService.createNewBooking(request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());

        // the room row must be locked before availability is checked
        verify(roomRepository).findByIdForUpdate(1L);

        ArgumentCaptor<Booking> saved = ArgumentCaptor.forClass(Booking.class);
        verify(bookingRepository).save(saved.capture());
        // booking is only held until payment, it is confirmed later by the Stripe webhook
        assertEquals(Booking.Status.PENDING, saved.getValue().getStatus());
        assertNotNull(saved.getValue().getHoldExpiresAt());
        assertEquals(new BigDecimal("100.00"), saved.getValue().getTotalAmount());

        verifyNoInteractions(notificationService, emailService);
        verify(auditLogService).log(
                eq(customer),
                eq("BOOKING_CREATED"),
                eq("BOOKING"),
                any(),
                anyString()
        );
    }

    @Test
    @DisplayName("Create booking with overlapping room throws RoomUnavailableException")
    void createBookingWithUnavailableRoom() {
        loginActiveCustomer();
        Hotel hotel = new Hotel();
        hotel.setId(1L);
        hotel.setName("VibeStay-hotel");
        hotel.setStatus(Hotel.Status.ACTIVE);
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
        when(roomRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(room));
        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));
        when(bookingRepository.findOverlappingBookings(eq(1L),
                any(LocalDate.class), any(LocalDate.class), any(LocalDateTime.class))).thenReturn(List.of(existingBooking));

        assertThrows(RoomUnavailableException.class, () -> bookingService.createNewBooking(request));
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    @DisplayName("Create booking with invalid dates throws InvalidInformationException")
    void createBookingWithInvalidDates() {
        loginActiveCustomer();
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
        when(bookingRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(booking));
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
        when(bookingRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(booking));
        when(userRepository.findUserByEmail("test@example.com")).thenReturn(customer);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("test@example.com", null
                )
        );
        assertThrows(InvalidInformationException.class, () -> bookingService.cancelBooking(1L)
        );

        verify(bookingRepository, never()).save(any(Booking.class));

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
        when(bookingRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(booking));
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
        User customer = new User();
        customer.setId(2L);
        booking.setUser(customer);
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
    }

    @Test
    @DisplayName("Create booking in inactive hotel throws InvalidInformationException")
    void createBookingInInactiveHotel() {
        loginActiveCustomer();
        Hotel hotel = new Hotel();
        hotel.setId(1L);
        hotel.setName("VibeStay-hotel");
        hotel.setStatus(Hotel.Status.INACTIVE);
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
        when(hotelRepository.findById(1L)).thenReturn(Optional.of(hotel));

        InvalidInformationException exception = assertThrows(InvalidInformationException.class,
                () -> bookingService.createNewBooking(request));
        assertEquals("This hotel is not available for booking", exception.getMessage());
        verifyNoInteractions(roomRepository);
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    @DisplayName("Create booking with more guests than room capacity throws InvalidInformationException")
    void createBookingExceedingRoomCapacity() {
        loginActiveCustomer();
        Hotel hotel = new Hotel();
        hotel.setId(1L);
        hotel.setName("VibeStay-hotel");
        hotel.setStatus(Hotel.Status.ACTIVE);
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
        BookingGuestRequest guest1 = new BookingGuestRequest();
        guest1.setName("Adult 1");
        guest1.setAge(25);
        BookingGuestRequest guest2 = new BookingGuestRequest();
        guest2.setName("Adult 2");
        guest2.setAge(30);
        BookingGuestRequest guest3 = new BookingGuestRequest();
        guest3.setName("Adult 3");
        guest3.setAge(35);
        BookingRoomRequest roomRequest = new BookingRoomRequest();
        roomRequest.setRoomId(1L);
        roomRequest.setGuests(List.of(guest1, guest2, guest3));
        BookingRequest request = new BookingRequest();
        request.setHotelId(1L);
        request.setCheckIn(LocalDate.now().plusDays(5));
        request.setCheckOut(LocalDate.now().plusDays(7));
        request.setRooms(List.of(roomRequest));
        when(hotelRepository.findById(1L)).thenReturn(Optional.of(hotel));
        when(roomRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(room));
        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));

        InvalidInformationException exception = assertThrows(InvalidInformationException.class,
                () -> bookingService.createNewBooking(request));
        assertEquals("Invalid number of guests, room can't accommodate this number of guests", exception.getMessage());
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    @DisplayName("Cancel completed booking throws InvalidInformationException")
    void cancelCompletedBooking() {
        User customer = new User();
        customer.setId(1L);
        customer.setEmail("test@example.com");
        Role role = new Role();
        role.setName(Role.RoleName.CUSTOMER);
        customer.setRole(role);
        Booking booking = new Booking();
        booking.setId(1L);
        booking.setUser(customer);
        booking.setStatus(Booking.Status.COMPLETED);
        when(bookingRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(booking));
        when(userRepository.findUserByEmail("test@example.com")).thenReturn(customer);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("test@example.com", null)
        );

        InvalidInformationException exception = assertThrows(InvalidInformationException.class,
                () -> bookingService.cancelBooking(1L));
        assertEquals("A completed booking can't be cancelled", exception.getMessage());
        assertEquals(Booking.Status.COMPLETED, booking.getStatus());
        verify(bookingRepository, never()).save(any(Booking.class));
        verify(emailService, never()).sendBookingCancellationEmail(any());
    }
}
