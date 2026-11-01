package com.ga.hotel_booking_app.concurrency;
import com.ga.hotel_booking_app.dto.request.BookingGuestRequest;
import com.ga.hotel_booking_app.dto.request.BookingRequest;
import com.ga.hotel_booking_app.dto.request.BookingRoomRequest;
import com.ga.hotel_booking_app.exception.custom.RoomUnavailableException;
import com.ga.hotel_booking_app.model.*;
import com.ga.hotel_booking_app.repository.*;
import com.ga.hotel_booking_app.service.AuditLogService;
import com.ga.hotel_booking_app.service.BookingService;
import com.ga.hotel_booking_app.service.EmailService;
import com.ga.hotel_booking_app.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


@SpringBootTest
@ActiveProfiles("test")
public class BookingConcurrencyTest {

    @Autowired BookingService bookingService;
    @Autowired BookingRepository bookingRepository;
    @Autowired
    HotelRepository hotelRepository;
    @Autowired
    ChildPolicyRepository childPolicyRepository;
    @Autowired
    RoomTypeRepository roomTypeRepository;
    @Autowired RoomRepository roomRepository;
    @Autowired
    UserRepository userRepository;

    @MockitoBean
    EmailService emailService;
    @MockitoBean
    NotificationService notificationService;
    @MockitoBean
    AuditLogService auditLogService;

    @Test
    void FiveUsersBookingSameRoomOnlyOneSuccssed() throws Exception {
        Hotel hotel = new Hotel();
        hotel.setName("Test Hotel");
        hotel.setDescription("test");
        hotel.setAddress("Street 1");
        hotel.setCity("Manama");
        hotel.setCountry("Bahrain");
        hotel.setPhone("12345678");
        hotel.setStatus(Hotel.Status.ACTIVE);
        hotel = hotelRepository.save(hotel);

        ChildPolicy policy = new ChildPolicy();
        policy.setInfantMaxAge(2);
        policy.setChildMaxAge(12);
        policy.setInfantsCountTowardOccupancy(false);
        policy.setHotel(hotel);
        childPolicyRepository.save(policy);

        RoomType roomType = new RoomType();
        roomType.setName("Deluxe");
        roomType.setDescription("test");
        roomType = roomTypeRepository.save(roomType);

        Room room = new Room();
        room.setRoomNumber("101");
        room.setFloorNumber("1");
        room.setStatus(Room.Status.ACTIVE);
        room.setPricePerNight(new BigDecimal("50.00"));
        room.setMaxAdults(2);
        room.setMaxChildren(2);
        room.setMaxOccupancy(4);
        room.setHotel(hotel);
        room.setRoomType(roomType);
        room = roomRepository.save(room);

        List<String> emails = new ArrayList<>();
        // create 5 users
        for (int i = 0; i < 5; i++) {
            User u = new User();
            u.setUsername("user" + i );
            u.setEmail("user" + i + "@test.com");
            u.setPassword("not-used");
            u.setStatus(User.Status.ACTIVE);
            userRepository.save(u);
            emails.add(u.getEmail());
        }

        long bookingsBefore = bookingRepository.count();

        ExecutorService executor = Executors.newFixedThreadPool(5);
        CountDownLatch startGate = new CountDownLatch(1);
        AtomicInteger success = new AtomicInteger();
        AtomicInteger errors = new AtomicInteger();
        AtomicInteger conflicts = new AtomicInteger();

        for (int i = 0; i < 5; i++) {
            String email = emails.get(i);
            BookingRequest request = buildRequest(hotel.getId(), room.getId());
            executor.execute(() -> {
                try {
                    SecurityContextHolder.getContext().setAuthentication(
                            new UsernamePasswordAuthenticationToken(email, null));
                    startGate.await();
                    bookingService.createNewBooking(request);
                    success.incrementAndGet();
                } catch (RoomUnavailableException e) {
                    conflicts.incrementAndGet();
                } catch (Exception e) {
                    errors.incrementAndGet();
                    e.printStackTrace();
                } finally {
                    SecurityContextHolder.clearContext();
                }
            });
        }

        // release all threads at the same moment
        startGate.countDown();
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.MINUTES);

        assertEquals(0, errors.get(), "unexpected errors");
        assertEquals(1, success.get(), "exactly one booking must succeed");
        assertEquals(4, conflicts.get(), "all others must get a conflict");
        assertEquals(bookingsBefore + 1, bookingRepository.count(), "only one booking row in the database");
    }

    private BookingRequest buildRequest(Long hotelId, Long roomId) {
        BookingGuestRequest guest = new BookingGuestRequest();
        guest.setName("Guest");
        guest.setAge(25);

        BookingRoomRequest roomRequest = new BookingRoomRequest();
        roomRequest.setRoomId(roomId);
        roomRequest.setGuests(List.of(guest));

        BookingRequest request = new BookingRequest();
        request.setHotelId(hotelId);
        request.setCheckIn(LocalDate.now().plusDays(5));
        request.setCheckOut(LocalDate.now().plusDays(7));
        request.setRooms(List.of(roomRequest));
        return request;
    }
}