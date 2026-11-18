package com.ga.bookingservice.concurrency;

import com.ga.bookingservice.client.ChildPolicySummary;
import com.ga.bookingservice.client.HotelServiceClient;
import com.ga.bookingservice.client.HotelSummary;
import com.ga.bookingservice.client.RoomSummary;
import com.ga.bookingservice.client.UserServiceClient;
import com.ga.bookingservice.client.UserSummary;
import com.ga.bookingservice.dto.request.BookingGuestRequest;
import com.ga.bookingservice.dto.request.BookingRequest;
import com.ga.bookingservice.dto.request.BookingRoomRequest;
import com.ga.bookingservice.exception.custom.RoomUnavailableException;
import com.ga.bookingservice.kafka.BookingEventProducer;
import com.ga.bookingservice.repository.BookingRepository;
import com.ga.bookingservice.security.AuthUser;
import com.ga.bookingservice.security.CurrentUser;
import com.ga.bookingservice.service.AuditLogService;
import com.ga.bookingservice.service.BookingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntFunction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
@EnabledIfEnvironmentVariable(named = "TEST_DB_URL", matches = ".+")
@TestPropertySource(properties = {
        "spring.datasource.url=${TEST_DB_URL}",
        "spring.datasource.username=${TEST_DB_USERNAME:postgres}",
        "spring.datasource.password=${TEST_DB_PASSWORD:}",
        "spring.datasource.driver-class-name=org.postgresql.Driver"
})
public class BookingConcurrencyTest {

    private static final String TOKEN = "Bearer test-token";
    private static final long HOTEL_ID = 1L;

    @Autowired
    BookingService bookingService;
    @Autowired
    BookingRepository bookingRepository;

    @MockitoBean
    HotelServiceClient hotelServiceClient;
    @MockitoBean
    UserServiceClient userServiceClient;
    @MockitoBean
    CurrentUser currentUser;
    @MockitoBean
    AuditLogService auditLogService;
    @MockitoBean
    BookingEventProducer bookingEventProducer;

    private final ThreadLocal<AuthUser> threadUser = new ThreadLocal<>();
    private long roomBase;

    @BeforeEach
    void setup() {
        roomBase = System.nanoTime() % 1_000_000_000L * 10;

        HotelSummary hotel = new HotelSummary();
        hotel.setId(HOTEL_ID);
        hotel.setName("Test Hotel");
        hotel.setStatus("ACTIVE");
        when(hotelServiceClient.getHotelSummary(HOTEL_ID)).thenReturn(hotel);
        when(hotelServiceClient.getChildPolicySummary(HOTEL_ID)).thenReturn(new ChildPolicySummary(2, 12, false));
        when(hotelServiceClient.getRoomSummary(any(), anyLong(), any())).thenAnswer(inv -> room(inv.getArgument(1)));

        when(currentUser.get()).thenAnswer(inv -> threadUser.get());
        when(currentUser.token()).thenReturn(TOKEN);
        when(userServiceClient.findUser(anyLong(), any())).thenAnswer(inv -> {
            UserSummary user = new UserSummary();
            user.setId(inv.getArgument(0));
            user.setFirstName("Guest");
            user.setLastName(String.valueOf(inv.getArgument(0, Long.class)));
            return Optional.of(user);
        });
    }

    private RoomSummary room(Long roomId) {
        RoomSummary room = new RoomSummary();
        room.setId(roomId);
        room.setHotelId(HOTEL_ID);
        room.setRoomNumber(String.valueOf(roomId));
        room.setRoomTypeName("Deluxe");
        room.setStatus("ACTIVE");
        room.setPricePerNight(new BigDecimal("50.00"));
        room.setMaxAdults(2);
        room.setMaxChildren(2);
        room.setMaxOccupancy(4);
        return room;
    }

    @Test
    void FiveUsersBookingSameRoomOnlyOneSuccssed() throws Exception {
        long roomId = roomBase + 1;
        Result result = runConcurrently(5, i -> buildRequest(roomId,
                LocalDate.now().plusDays(2), LocalDate.now().plusDays(5)));

        assertEquals(0, result.errors.get(), "unexpected errors");
        assertEquals(1, result.success.get(), "exactly one booking must succeed");
        assertEquals(4, result.conflicts.get(), "all others must get a conflict");
        assertEquals(result.bookingsBefore + 1, bookingRepository.count(), "only one booking row in the database");
    }

    @Test
    void ThreeUsersBookingDifferentRoomForSameDateAllSucceed() throws Exception {
        Result result = runConcurrently(3, i -> buildRequest(roomBase + 1 + i,
                LocalDate.now().plusDays(1), LocalDate.now().plusDays(4)));

        assertEquals(0, result.errors.get(), "unexpected errors");
        assertEquals(3, result.success.get(), "All three bookings must succeed");
        assertEquals(0, result.conflicts.get(), "no conflict");
        assertEquals(result.bookingsBefore + 3, bookingRepository.count(), "three booking row in the database");
    }

    @Test
    void ThreeUsersBookingSameRoomForDifferentDatesAllSucceed() throws Exception {
        long roomId = roomBase + 2;
        Result result = runConcurrently(3, i -> buildRequest(roomId,
                LocalDate.now().plusDays(i + 5), LocalDate.now().plusDays(i + 6)));

        assertEquals(0, result.errors.get(), "unexpected errors");
        assertEquals(3, result.success.get(), "All three bookings must succeed");
        assertEquals(0, result.conflicts.get(), "no conflict");
        assertEquals(result.bookingsBefore + 3, bookingRepository.count(), "three booking row in the database");
    }

    private record Result(long bookingsBefore, AtomicInteger success, AtomicInteger conflicts, AtomicInteger errors) {
    }

    private Result runConcurrently(int users, IntFunction<BookingRequest> requestFor) throws InterruptedException {
        Result result = new Result(bookingRepository.count(), new AtomicInteger(), new AtomicInteger(), new AtomicInteger());
        ExecutorService executor = Executors.newFixedThreadPool(users);
        CountDownLatch startGate = new CountDownLatch(1);
        for (int i = 0; i < users; i++) {
            AuthUser user = new AuthUser(roomBase + 100 + i, "user" + i + "@test.com", "CUSTOMER", "ACTIVE");
            BookingRequest request = requestFor.apply(i);
            executor.execute(() -> {
                threadUser.set(user);
                try {
                    startGate.await();
                    bookingService.createNewBooking(request);
                    result.success.incrementAndGet();
                } catch (RoomUnavailableException e) {
                    result.conflicts.incrementAndGet();
                } catch (Exception e) {
                    result.errors.incrementAndGet();
                } finally {
                    threadUser.remove();
                }
            });
        }
        startGate.countDown();
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.MINUTES);
        return result;
    }

    private BookingRequest buildRequest(Long roomId, LocalDate checkin, LocalDate checkout) {
        BookingGuestRequest guest = new BookingGuestRequest();
        guest.setName("Guest");
        guest.setAge(25);
        BookingRoomRequest roomRequest = new BookingRoomRequest();
        roomRequest.setRoomId(roomId);
        roomRequest.setGuests(List.of(guest));
        BookingRequest request = new BookingRequest();
        request.setHotelId(HOTEL_ID);
        request.setCheckIn(checkin);
        request.setCheckOut(checkout);
        request.setRooms(List.of(roomRequest));
        return request;
    }
}
