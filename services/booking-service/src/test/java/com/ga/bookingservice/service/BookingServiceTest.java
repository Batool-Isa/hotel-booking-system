package com.ga.bookingservice.service;

import com.ga.bookingservice.client.ChildPolicySummary;
import com.ga.bookingservice.client.HotelServiceClient;
import com.ga.bookingservice.client.HotelSummary;
import com.ga.bookingservice.client.RoomSummary;
import com.ga.bookingservice.client.UserServiceClient;
import com.ga.bookingservice.client.UserSummary;
import com.ga.bookingservice.dto.request.BookingGuestRequest;
import com.ga.bookingservice.dto.request.BookingRequest;
import com.ga.bookingservice.dto.request.BookingRoomRequest;
import com.ga.bookingservice.dto.request.BookingStatusRequest;
import com.ga.bookingservice.event.BookingChanged;
import com.ga.bookingservice.exception.custom.InvalidInformationException;
import com.ga.bookingservice.exception.custom.RoomUnavailableException;
import com.ga.bookingservice.exception.custom.UnauthorizedActionException;
import com.ga.bookingservice.model.Booking;
import com.ga.bookingservice.repository.BookingRepository;
import com.ga.bookingservice.security.AuthUser;
import com.ga.bookingservice.security.CurrentUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    private static final String TOKEN = "Bearer test-token";

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private HotelServiceClient hotelServiceClient;

    @Mock
    private UserServiceClient userServiceClient;

    @Mock
    private CurrentUser currentUser;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private BookingService bookingService;

    private AuthUser login(Long id, String role) {
        AuthUser user = new AuthUser(id, "user" + id + "@example.com", role, "ACTIVE");
        when(currentUser.get()).thenReturn(user);
        lenient().when(currentUser.token()).thenReturn(TOKEN);
        return user;
    }

    private AuthUser loginActiveCustomer() {
        return login(1L, "CUSTOMER");
    }

    private HotelSummary hotel(String status) {
        HotelSummary hotel = new HotelSummary();
        hotel.setId(1L);
        hotel.setName("VibeStay-hotel");
        hotel.setStatus(status);
        return hotel;
    }

    private ChildPolicySummary childPolicy() {
        return new ChildPolicySummary(2, 12, false);
    }

    private RoomSummary room() {
        RoomSummary room = new RoomSummary();
        room.setId(1L);
        room.setHotelId(1L);
        room.setRoomNumber("101");
        room.setRoomTypeName("Deluxe");
        room.setStatus("ACTIVE");
        room.setPricePerNight(new BigDecimal("50.00"));
        room.setMaxAdults(2);
        room.setMaxChildren(2);
        room.setMaxOccupancy(4);
        return room;
    }

    private UserSummary userSummary(Long id) {
        UserSummary user = new UserSummary();
        user.setId(id);
        user.setFirstName("Test");
        user.setLastName("User");
        user.setEmail("user" + id + "@example.com");
        return user;
    }

    private BookingRequest bookingRequest(LocalDate checkIn, LocalDate checkOut, Integer... guestAges) {
        List<BookingGuestRequest> guests = new java.util.ArrayList<>();
        for (int i = 0; i < guestAges.length; i++) {
            BookingGuestRequest guest = new BookingGuestRequest();
            guest.setName("Guest " + (i + 1));
            guest.setAge(guestAges[i]);
            guests.add(guest);
        }
        BookingRoomRequest roomRequest = new BookingRoomRequest();
        roomRequest.setRoomId(1L);
        roomRequest.setGuests(guests);
        BookingRequest request = new BookingRequest();
        request.setHotelId(1L);
        request.setCheckIn(checkIn);
        request.setCheckOut(checkOut);
        request.setRooms(List.of(roomRequest));
        return request;
    }

    private Booking booking(Long ownerId, Booking.Status status) {
        Booking booking = new Booking();
        booking.setId(1L);
        booking.setBookingReference("VIBE1234");
        booking.setUserId(ownerId);
        booking.setHotelId(1L);
        booking.setStatus(status);
        return booking;
    }

    private void stubHotelAndRoom() {
        when(hotelServiceClient.getHotelSummary(1L)).thenReturn(hotel("ACTIVE"));
        when(hotelServiceClient.getChildPolicySummary(1L)).thenReturn(childPolicy());
        when(hotelServiceClient.getRoomSummary(1L, 1L, TOKEN)).thenReturn(room());
    }

    @Test
    @DisplayName("Create booking when room is available")
    void createNewBooking() {
        loginActiveCustomer();
        stubHotelAndRoom();
        when(bookingRepository.findOverlappingBookings(eq(1L), any(LocalDate.class), any(LocalDate.class),
                any(LocalDateTime.class))).thenReturn(Collections.emptyList());
        when(userServiceClient.findUser(1L, TOKEN)).thenReturn(Optional.of(userSummary(1L)));
        BookingRequest request = bookingRequest(LocalDate.now().plusDays(5), LocalDate.now().plusDays(7), 25);

        var response = bookingService.createNewBooking(request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        verify(bookingRepository).lockRoom(1L);
        ArgumentCaptor<Booking> saved = ArgumentCaptor.forClass(Booking.class);
        verify(bookingRepository).save(saved.capture());
        assertEquals(Booking.Status.PENDING, saved.getValue().getStatus());
        assertNotNull(saved.getValue().getHoldExpiresAt());
        assertEquals(0, new BigDecimal("100.00").compareTo(saved.getValue().getTotalAmount()));
        assertEquals(1L, saved.getValue().getUserId());
        assertEquals(1L, saved.getValue().getHotelId());
        verify(applicationEventPublisher).publishEvent(argThat((Object e) ->
                e instanceof BookingChanged changed && "BOOKING_CREATED".equals(changed.type())));
        verify(auditLogService).log(eq(1L), eq("BOOKING_CREATED"), eq("BOOKING"), any(), anyString());
    }

    @Test
    @DisplayName("Create booking with overlapping room throws RoomUnavailableException")
    void createBookingWithUnavailableRoom() {
        loginActiveCustomer();
        stubHotelAndRoom();
        when(bookingRepository.findOverlappingBookings(eq(1L), any(LocalDate.class), any(LocalDate.class),
                any(LocalDateTime.class))).thenReturn(List.of(new Booking()));
        BookingRequest request = bookingRequest(LocalDate.now().plusDays(5), LocalDate.now().plusDays(7), 25);

        assertThrows(RoomUnavailableException.class, () -> bookingService.createNewBooking(request));

        verify(bookingRepository, never()).save(any(Booking.class));
        verifyNoInteractions(applicationEventPublisher, auditLogService);
    }

    @Test
    @DisplayName("Create booking with invalid dates throws InvalidInformationException")
    void createBookingWithInvalidDates() {
        loginActiveCustomer();
        BookingRequest request = bookingRequest(LocalDate.now().plusDays(5), LocalDate.now().plusDays(3), 25);

        assertThrows(InvalidInformationException.class, () -> bookingService.createNewBooking(request));

        verifyNoInteractions(hotelServiceClient);
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    @DisplayName("Inactive customer cannot create a booking")
    void inactiveCustomerCannotCreateBooking() {
        when(currentUser.get()).thenReturn(new AuthUser(1L, "user1@example.com", "CUSTOMER", "INACTIVE"));
        BookingRequest request = bookingRequest(LocalDate.now().plusDays(5), LocalDate.now().plusDays(7), 25);

        InvalidInformationException exception = assertThrows(InvalidInformationException.class,
                () -> bookingService.createNewBooking(request));

        assertEquals("Only active customer can book hotels", exception.getMessage());
        verifyNoInteractions(hotelServiceClient);
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    @DisplayName("Create booking in inactive hotel throws InvalidInformationException")
    void createBookingInInactiveHotel() {
        loginActiveCustomer();
        when(hotelServiceClient.getHotelSummary(1L)).thenReturn(hotel("INACTIVE"));
        BookingRequest request = bookingRequest(LocalDate.now().plusDays(5), LocalDate.now().plusDays(7), 25);

        InvalidInformationException exception = assertThrows(InvalidInformationException.class,
                () -> bookingService.createNewBooking(request));

        assertEquals("This hotel is not available for booking", exception.getMessage());
        verify(hotelServiceClient, never()).getRoomSummary(anyLong(), anyLong(), any());
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    @DisplayName("Create booking with more guests than room capacity throws InvalidInformationException")
    void createBookingExceedingRoomCapacity() {
        loginActiveCustomer();
        stubHotelAndRoom();
        BookingRequest request = bookingRequest(LocalDate.now().plusDays(5), LocalDate.now().plusDays(7), 25, 30, 35);

        InvalidInformationException exception = assertThrows(InvalidInformationException.class,
                () -> bookingService.createNewBooking(request));

        assertEquals("Invalid number of guests, room can't accommodate this number of guests", exception.getMessage());
        verify(bookingRepository, never()).lockRoom(anyLong());
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    @DisplayName("Cancel confirmed booking successfully")
    void cancelBooking() {
        loginActiveCustomer();
        Booking booking = booking(1L, Booking.Status.CONFIRMED);
        when(bookingRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(booking));
        when(userServiceClient.findUser(1L, TOKEN)).thenReturn(Optional.of(userSummary(1L)));

        var response = bookingService.cancelBooking(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(Booking.Status.CANCELLED, booking.getStatus());
        verify(bookingRepository).save(booking);
        verify(applicationEventPublisher).publishEvent(new BookingChanged(booking, "BOOKING_CANCELLED"));
        verify(auditLogService).log(eq(1L), eq("BOOKING_CANCELLED"), eq("BOOKING"), eq(1L), anyString());
    }

    @Test
    @DisplayName("Cancel already cancelled booking throws InvalidInformationException")
    void cancelAlreadyCancelledBooking() {
        loginActiveCustomer();
        when(bookingRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(booking(1L, Booking.Status.CANCELLED)));

        assertThrows(InvalidInformationException.class, () -> bookingService.cancelBooking(1L));

        verify(bookingRepository, never()).save(any(Booking.class));
        verifyNoInteractions(applicationEventPublisher);
    }

    @Test
    @DisplayName("Customer cannot cancel another customer's booking")
    void customerCannotCancelAnotherUsersBooking() {
        loginActiveCustomer();
        when(bookingRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(booking(2L, Booking.Status.CONFIRMED)));

        assertThrows(UnauthorizedActionException.class, () -> bookingService.cancelBooking(1L));

        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    @DisplayName("Cancel completed booking throws InvalidInformationException")
    void cancelCompletedBooking() {
        loginActiveCustomer();
        Booking booking = booking(1L, Booking.Status.COMPLETED);
        when(bookingRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(booking));

        InvalidInformationException exception = assertThrows(InvalidInformationException.class,
                () -> bookingService.cancelBooking(1L));

        assertEquals("A completed booking can't be cancelled", exception.getMessage());
        assertEquals(Booking.Status.COMPLETED, booking.getStatus());
        verify(bookingRepository, never()).save(any(Booking.class));
        verifyNoInteractions(applicationEventPublisher);
    }

    @Test
    @DisplayName("Hotel manager can update confirmed booking to completed")
    void updateBookingStatus() {
        login(5L, "HOTEL_MANAGER");
        Booking booking = booking(2L, Booking.Status.CONFIRMED);
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(hotelServiceClient.getManagersHotel(TOKEN)).thenReturn(List.of(hotel("ACTIVE")));
        when(userServiceClient.findUser(5L, TOKEN)).thenReturn(Optional.of(userSummary(5L)));
        BookingStatusRequest request = new BookingStatusRequest();
        request.setStatus(Booking.Status.COMPLETED);

        var response = bookingService.updateBookingStatus(1L, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(Booking.Status.COMPLETED, booking.getStatus());
        verify(bookingRepository).save(booking);
        verify(applicationEventPublisher).publishEvent(new BookingChanged(booking, "BOOKING_COMPLETED"));
        verify(auditLogService).log(eq(5L), eq("BOOKING_UPDATE"), eq("BOOKING"), eq(1L), anyString());
    }

    @Test
    @DisplayName("Manager of another hotel cannot update booking status")
    void managerOfAnotherHotelCannotUpdateBookingStatus() {
        login(5L, "HOTEL_MANAGER");
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking(2L, Booking.Status.CONFIRMED)));
        HotelSummary otherHotel = hotel("ACTIVE");
        otherHotel.setId(99L);
        when(hotelServiceClient.getManagersHotel(TOKEN)).thenReturn(List.of(otherHotel));
        BookingStatusRequest request = new BookingStatusRequest();
        request.setStatus(Booking.Status.COMPLETED);

        assertThrows(UnauthorizedActionException.class, () -> bookingService.updateBookingStatus(1L, request));

        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    @DisplayName("Customer cannot update booking status")
    void customerCannotUpdateBookingStatus() {
        loginActiveCustomer();
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking(1L, Booking.Status.CONFIRMED)));
        BookingStatusRequest request = new BookingStatusRequest();
        request.setStatus(Booking.Status.COMPLETED);

        assertThrows(UnauthorizedActionException.class, () -> bookingService.updateBookingStatus(1L, request));

        verify(bookingRepository, never()).save(any(Booking.class));
    }
}
