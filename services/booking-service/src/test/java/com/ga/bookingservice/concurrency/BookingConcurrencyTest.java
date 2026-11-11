package com.ga.bookingservice.concurrency;
import com.ga.bookingservice.dto.request.BookingGuestRequest;
import com.ga.bookingservice.dto.request.BookingRequest;
import com.ga.bookingservice.dto.request.BookingRoomRequest;
import com.ga.bookingservice.exception.custom.RoomUnavailableException;
import com.ga.bookingservice.repository.*;
import com.ga.bookingservice.service.AuditLogService;
import com.ga.bookingservice.service.BookingService;
import com.ga.bookingservice.service.EmailService;
import com.ga.bookingservice.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
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
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.assertEquals;

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

    private Hotel hotel;
    private Room room;
    private Room room2;
    private Room room3;
private final List<String> emails = new ArrayList<>();
    private  final List<Room> rooms = new ArrayList<>();

    @BeforeEach
    void setup(){
        // add unique id to avoid having error of existing
        String uniqueId = UUID.randomUUID().toString().substring(0,4);
         hotel = new Hotel();
        hotel.setName("Test Hotel "+uniqueId);
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
        roomType.setName("Deluxe "+uniqueId);
        roomType.setDescription("test");
        roomType = roomTypeRepository.save(roomType);

        room = new Room();
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
        room2 = new Room();
        room2.setRoomNumber("102");
        room2.setFloorNumber("1");
        room2.setStatus(Room.Status.ACTIVE);
        room2.setPricePerNight(new BigDecimal("50.00"));
        room2.setMaxAdults(2);
        room2.setMaxChildren(2);
        room2.setMaxOccupancy(4);
        room2.setHotel(hotel);
        room2.setRoomType(roomType);
        room2 = roomRepository.save(room2);
        room3 = new Room();
        room3.setRoomNumber("103");
        room3.setFloorNumber("1");
        room3.setStatus(Room.Status.ACTIVE);
        room3.setPricePerNight(new BigDecimal("50.00"));
        room3.setMaxAdults(2);
        room3.setMaxChildren(2);
        room3.setMaxOccupancy(4);
        room3.setHotel(hotel);
        room3.setRoomType(roomType);
        room3 = roomRepository.save(room3);
        rooms.addAll(Arrays.asList(room, room2, room3));
        // create 5 users
        for (int i = 0; i < 5; i++) {
            User u = new User();
            u.setUsername("user" + i +"-"+uniqueId);
            u.setEmail("user" + i +"-"+uniqueId+ "@test.com");
            u.setPassword("not-used");
            u.setStatus(User.Status.ACTIVE);
            userRepository.save(u);
            emails.add(u.getEmail());
        }

    }
    @Test
    void FiveUsersBookingSameRoomOnlyOneSuccssed() throws Exception {
        long bookingsBefore = bookingRepository.count();

        ExecutorService executor = Executors.newFixedThreadPool(5);
        CountDownLatch startGate = new CountDownLatch(1);
        AtomicInteger success = new AtomicInteger();
        AtomicInteger errors = new AtomicInteger();
        AtomicInteger conflicts = new AtomicInteger();

        for (int i = 0; i < 5; i++) {
            String email = emails.get(i);
            BookingRequest request = buildRequest(hotel.getId(), room.getId(),
                    LocalDate.now().plusDays(2),  LocalDate.now().plusDays(5));
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
                } finally {
                    SecurityContextHolder.clearContext();
                }
            });
        }
        startGate.countDown();
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.MINUTES);
        assertEquals(0, errors.get(), "unexpected errors");
        assertEquals(1, success.get(), "exactly one booking must succeed");
        assertEquals(4, conflicts.get(), "all others must get a conflict");
        assertEquals(bookingsBefore + 1, bookingRepository.count(), "only one booking row in the database");
    }

    @Test
    void ThreeUsersBookingDifferentRoomForSameDateAllSucceed() throws Exception {

        long bookingsBefore = bookingRepository.count();

        ExecutorService executor = Executors.newFixedThreadPool(3);
        CountDownLatch startGate = new CountDownLatch(1);
        AtomicInteger success = new AtomicInteger();
        AtomicInteger errors = new AtomicInteger();
        AtomicInteger conflicts = new AtomicInteger();

        for (int i = 0; i < 3; i++) {
            String email = emails.get(i);
            BookingRequest request = buildRequest(hotel.getId(), rooms.get(i).getId(),
            LocalDate.now().plusDays(1), LocalDate.now().plusDays(4));
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
        startGate.countDown();
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.MINUTES);
        assertEquals(0, errors.get(), "unexpected errors");
        assertEquals(3, success.get(), "All three bookings must succeed");
        assertEquals(0, conflicts.get(), "no conflict");
        assertEquals(bookingsBefore + 3, bookingRepository.count(), "three booking row in the database");
    }
    @Test
    void ThreeUsersBookingSameRoomForDifferentDatesAllSucceed() throws Exception {

        long bookingsBefore = bookingRepository.count();

        ExecutorService executor = Executors.newFixedThreadPool(3);
        CountDownLatch startGate = new CountDownLatch(1);
        AtomicInteger success = new AtomicInteger();
        AtomicInteger errors = new AtomicInteger();
        AtomicInteger conflicts = new AtomicInteger();

        for (int i = 0; i < 3; i++) {
            String email = emails.get(i);
            BookingRequest request = buildRequest(hotel.getId(), room2.getId(),
                    LocalDate.now().plusDays(i+5), LocalDate.now().plusDays(i+6) );
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
        startGate.countDown();
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.MINUTES);
        assertEquals(0, errors.get(), "unexpected errors");
        assertEquals(3, success.get(), "All three bookings must succeed");
        assertEquals(0, conflicts.get(), "no conflict");
        assertEquals(bookingsBefore + 3, bookingRepository.count(), "three booking row in the database");
    }


    private BookingRequest buildRequest(Long hotelId, Long roomId, LocalDate checkin, LocalDate checkout) {
        BookingGuestRequest guest = new BookingGuestRequest();
        guest.setName("Guest");
        guest.setAge(25);
        BookingRoomRequest roomRequest = new BookingRoomRequest();
        roomRequest.setRoomId(roomId);
        roomRequest.setGuests(List.of(guest));
        BookingRequest request = new BookingRequest();
        request.setHotelId(hotelId);
        request.setCheckIn(checkin);
        request.setCheckOut(checkout);
        request.setRooms(List.of(roomRequest));
        return request;
    }
}