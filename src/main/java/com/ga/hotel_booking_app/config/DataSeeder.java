package com.ga.hotel_booking_app.config;

import com.ga.hotel_booking_app.model.*;
import com.ga.hotel_booking_app.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;


@Configuration
@RequiredArgsConstructor
public class DataSeeder {
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AmenityRepository amenityRepository;
    private final HotelRepository hotelRepository;
    private final HotelAmenityRepository hotelAmenityRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final RoomRepository roomRepository;
    private final ChildPolicyRepository childPolicyRepository;
    private final BookingRepository bookingRepository;

    @Bean
    CommandLineRunner seedData() {
        return args -> {
            seedRoles();
            seedStaffUsers();
            seedAmenities();
            seedRoomTypes();
            seedCustomers();
            seedHotels();
            seedBookings();
        };
    }

    private void seedRoles() {
        for (Role.RoleName name : Role.RoleName.values()) {
            if (roleRepository.findByName(name).isEmpty()) {
                roleRepository.save(new Role(null, name));
            }
        }
    }

    private void seedStaffUsers() {
        createUser("admin", "admin@vibestay.com", "Admin123!", Role.RoleName.ADMIN, "VibeStay", "Admin", "+97330000000");
        createUser("staff1", "staff1@vibestay.com", "Staff123!", Role.RoleName.STAFF, "Mariam", "Yousif", "+97330000001");

        String[][] managers = {
                {"Hotel", "Manager One", "+97331111111"},
                {"Hotel", "Manager Two", "+97332222222"},
                {"Khalid", "Al Mansoori", "+97331000003"},
                {"Noura", "Al Hashimi", "+97331000004"},
                {"Yusuf", "Karimi", "+97331000005"},
                {"Layla", "Haddad", "+97331000006"}
        };
        for (int i = 0; i < managers.length; i++) {
            int n = i + 1;
            createUser("manager" + n, "manager" + n + "@vibestay.com", "Manager123!",
                    Role.RoleName.HOTEL_MANAGER, managers[i][0], managers[i][1], managers[i][2]);
        }
    }

    private void seedCustomers() {
        String[][] customers = {
                {"Sara", "Ahmed"}, {"Omar", "Khalid"}, {"Fatima", "Al Zayani"}, {"Hassan", "Jaffar"},
                {"Maryam", "Salman"}, {"Ali", "Hussain"}, {"Zainab", "Mahmood"}, {"Ahmed", "Fakhro"},
                {"Reem", "Nasser"}, {"Yousef", "Darwish"}, {"Huda", "Abdulla"}, {"Tariq", "Mansoor"},
                {"Aisha", "Rashid"}, {"Jasim", "Saleh"}, {"Dana", "Mubarak"}, {"Karim", "Haddad"}
        };
        for (int i = 0; i < customers.length; i++) {
            int n = i + 1;
            createUser("customer" + n, "customer" + n + "@vibestay.com", "Customer123!", Role.RoleName.CUSTOMER,
                    customers[i][0], customers[i][1], String.format("+9733400%04d", n));
        }
    }

    private final Map<String, String> encodedPasswords = new HashMap<>();

    private void createUser(String username, String email, String rawPassword, Role.RoleName roleName,
                            String first, String last, String phone) {
        if (userRepository.existsByEmail(email) || userRepository.existsByUsername(username)) {
            return;
        }
        Role role = roleRepository.findByName(roleName).orElseThrow();

        UserProfile profile = new UserProfile();
        profile.setFirstName(first);
        profile.setLastName(last);
        profile.setPhone(phone);

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        // BCrypt is slow, so each distinct password is encoded once and reused
        user.setPassword(encodedPasswords.computeIfAbsent(rawPassword, passwordEncoder::encode));
        user.setEmailVerified(true);
        user.setStatus(User.Status.ACTIVE);
        user.setRole(role);
        user.setUserProfile(profile);
        userRepository.save(user);
    }

    private static final String[] AMENITIES = {
            "Wi-Fi", "Swimming Pool", "Gym", "Parking", "Restaurant", "Spa", "Breakfast", "Beach Access",
            "Room Service", "Air Conditioning", "Airport Shuttle", "Laundry", "Kids Club", "Pet Friendly",
            "Business Center", "Bar"
    };

    /** Short text and extra cost shown when an amenity is linked to a hotel. */
    private static final Map<String, String> AMENITY_TEXT = Map.ofEntries(
            Map.entry("Wi-Fi", "Free high-speed Wi-Fi in all rooms"),
            Map.entry("Swimming Pool", "Outdoor pool, open 7am - 10pm"),
            Map.entry("Gym", "24-hour fitness room"),
            Map.entry("Parking", "Secure on-site parking"),
            Map.entry("Restaurant", "All-day dining"),
            Map.entry("Spa", "Massage and wellness treatments"),
            Map.entry("Breakfast", "Buffet breakfast, 6:30am - 10:30am"),
            Map.entry("Beach Access", "Private beach area"),
            Map.entry("Room Service", "Available 24 hours"),
            Map.entry("Air Conditioning", "Individual climate control"),
            Map.entry("Airport Shuttle", "Pick-up and drop-off on request"),
            Map.entry("Laundry", "Same-day laundry service"),
            Map.entry("Kids Club", "Supervised activities for children"),
            Map.entry("Pet Friendly", "Small pets welcome"),
            Map.entry("Business Center", "Meeting rooms and printing"),
            Map.entry("Bar", "Lounge bar, evenings"));

    private static final Map<String, String> AMENITY_COST = Map.of(
            "Spa", "12.00", "Breakfast", "4.50", "Airport Shuttle", "8.00", "Laundry", "3.00",
            "Pet Friendly", "6.00", "Kids Club", "5.00");

    private void seedAmenities() {
        for (String name : AMENITIES) {
            if (amenityRepository.findByName(name).isEmpty()) {
                Amenity amenity = new Amenity();
                amenity.setName(name);
                amenity.setDescription(AMENITY_TEXT.get(name));
                amenityRepository.save(amenity);
            }
        }
    }

    private static final String[][] ROOM_TYPES = {
            {"Standard Room", "Comfortable room with all the basics", "35.00", "2", "1", "3"},
            {"Twin Room", "Two single beds, ideal for friends or colleagues", "40.00", "2", "1", "3"},
            {"Deluxe Room", "Larger room with a better view and extra comfort", "55.00", "2", "2", "4"},
            {"Executive Suite", "Suite with a separate lounge area and work desk", "80.00", "3", "2", "4"},
            {"Family Suite", "Spacious suite for families with children", "90.00", "4", "3", "6"},
            {"Presidential Suite", "Top floor suite with panoramic views", "180.00", "4", "2", "6"}
    };

    private void seedRoomTypes() {
        for (String[] t : ROOM_TYPES) {
            if (!roomTypeRepository.existsByName(t[0])) {
                RoomType type = new RoomType();
                type.setName(t[0]);
                type.setDescription(t[1]);
                type.setStatus(RoomType.Status.ACTIVE);
                roomTypeRepository.save(type);
            }
        }
    }


    private record HotelSeed(String name, String description, String address, String city, String country,
                             String phone, String lat, String lng, Hotel.Status status, List<String> managers,
                             int infantMaxAge, int childMaxAge, boolean infantsCount, double priceFactor,
                             List<String> amenities) {
    }

    private static List<HotelSeed> hotelSeeds() {
        return List.of(
                new HotelSeed("VibeStay Manama Grand", "A modern city hotel with sea views in the heart of Manama.",
                        "Road 1705, Diplomatic Area", "Manama", "Bahrain", "+97317000001", "26.2361", "50.5831",
                        Hotel.Status.ACTIVE, List.of("manager1@vibestay.com"), 2, 12, false, 1.0,
                        List.of("Wi-Fi", "Swimming Pool", "Gym", "Restaurant", "Air Conditioning", "Parking", "Business Center")),
                new HotelSeed("VibeStay Beach Resort", "A relaxed beachfront resort for families and couples.",
                        "Amwaj Islands", "Muharraq", "Bahrain", "+97317000002", "26.2872", "50.6600",
                        Hotel.Status.ACTIVE, List.of("manager2@vibestay.com"), 2, 12, false, 1.3,
                        List.of("Wi-Fi", "Swimming Pool", "Beach Access", "Breakfast", "Spa", "Parking", "Kids Club", "Bar")),
                new HotelSeed("VibeStay Desert Lodge", "A quiet lodge near the desert, waiting for approval.",
                        "Sakhir Road", "Sakhir", "Bahrain", "+97317000003", "26.0325", "50.5106",
                        Hotel.Status.PENDING_APPROVAL, List.of("manager1@vibestay.com"), 2, 12, false, 0.8,
                        List.of("Wi-Fi", "Parking")),
                new HotelSeed("VibeStay Riffa Heights", "Hilltop hotel near the Royal Golf Club with calm gardens.",
                        "Riffa Views", "Riffa", "Bahrain", "+97317000004", "26.1300", "50.5550",
                        Hotel.Status.ACTIVE, List.of("manager3@vibestay.com"), 3, 11, true, 0.9,
                        List.of("Wi-Fi", "Swimming Pool", "Gym", "Restaurant", "Parking", "Laundry")),
                new HotelSeed("VibeStay Dubai Marina", "High-rise stay a short walk from the Marina promenade.",
                        "Marina Walk, Dubai Marina", "Dubai", "United Arab Emirates", "+97144000005", "25.0800", "55.1400",
                        Hotel.Status.ACTIVE, List.of("manager4@vibestay.com"), 2, 12, false, 2.2,
                        List.of("Wi-Fi", "Swimming Pool", "Gym", "Spa", "Restaurant", "Bar", "Airport Shuttle", "Room Service")),
                new HotelSeed("VibeStay Abu Dhabi Corniche", "Elegant rooms on the Corniche with views of the Gulf.",
                        "Corniche Road West", "Abu Dhabi", "United Arab Emirates", "+97126000006", "24.4764", "54.3300",
                        Hotel.Status.ACTIVE, List.of("manager4@vibestay.com"), 2, 12, false, 1.9,
                        List.of("Wi-Fi", "Swimming Pool", "Beach Access", "Breakfast", "Gym", "Parking")),
                new HotelSeed("VibeStay Doha Pearl", "Waterfront hotel next to Souq Waqif and the Museum of Islamic Art.",
                        "Corniche Street", "Doha", "Qatar", "+97444000007", "25.2900", "51.5300",
                        Hotel.Status.ACTIVE, List.of("manager5@vibestay.com"), 2, 12, true, 1.7,
                        List.of("Wi-Fi", "Swimming Pool", "Restaurant", "Spa", "Airport Shuttle", "Business Center")),
                new HotelSeed("VibeStay Riyadh Tower", "Business hotel in the financial district with meeting rooms.",
                        "King Fahd Road, Olaya", "Riyadh", "Saudi Arabia", "+96611000008", "24.6900", "46.6850",
                        Hotel.Status.ACTIVE, List.of("manager5@vibestay.com"), 2, 12, false, 1.5,
                        List.of("Wi-Fi", "Gym", "Restaurant", "Business Center", "Parking", "Air Conditioning", "Laundry")),
                new HotelSeed("VibeStay Muscat Bay", "Boutique hotel between the mountains and the sea in Muscat.",
                        "Qurum Beach Road", "Muscat", "Oman", "+96824000009", "23.6100", "58.4800",
                        Hotel.Status.ACTIVE, List.of("manager6@vibestay.com"), 2, 12, false, 1.2,
                        List.of("Wi-Fi", "Beach Access", "Swimming Pool", "Breakfast", "Pet Friendly", "Restaurant")),
                new HotelSeed("VibeStay Kuwait Gate", "Central hotel close to Kuwait Towers and the Avenues mall.",
                        "Gulf Road", "Kuwait City", "Kuwait", "+96522000010", "29.3759", "47.9774",
                        Hotel.Status.PENDING_APPROVAL, List.of("manager6@vibestay.com"), 2, 12, false, 1.4,
                        List.of("Wi-Fi", "Gym", "Restaurant")),
                new HotelSeed("VibeStay Old Town Inn", "Closed for renovation, kept here to test inactive hotels.",
                        "Bab Al Bahrain Avenue", "Manama", "Bahrain", "+97317000011", "26.2400", "50.5800",
                        Hotel.Status.INACTIVE, List.of("manager2@vibestay.com"), 2, 12, false, 0.7,
                        List.of("Wi-Fi", "Air Conditioning")));
    }

    private void seedHotels() {
        Map<String, RoomType> types = new HashMap<>();
        for (RoomType t : roomTypeRepository.findAll()) {
            types.put(t.getName(), t);
        }
        for (HotelSeed seed : hotelSeeds()) {
            if (!hotelRepository.existsByName(seed.name())) {
                createHotel(seed, types);
            }
        }
    }

    private void createHotel(HotelSeed seed, Map<String, RoomType> types) {
        Hotel hotel = new Hotel();
        hotel.setName(seed.name());
        hotel.setDescription(seed.description());
        hotel.setAddress(seed.address());
        hotel.setCity(seed.city());
        hotel.setCountry(seed.country());
        hotel.setPhone(seed.phone());
        hotel.setLatitude(new BigDecimal(seed.lat()));
        hotel.setLongitude(new BigDecimal(seed.lng()));
        hotel.setStatus(seed.status());
        for (String email : seed.managers()) {
            User manager = userRepository.findUserByEmail(email);
            if (manager != null) {
                hotel.getManagers().add(manager);
            }
        }
        hotel = hotelRepository.save(hotel);

        ChildPolicy policy = new ChildPolicy();
        policy.setInfantMaxAge(seed.infantMaxAge());
        policy.setChildMaxAge(seed.childMaxAge());
        policy.setInfantsCountTowardOccupancy(seed.infantsCount());
        policy.setHotel(hotel);
        childPolicyRepository.save(policy);

        for (String amenityName : seed.amenities()) {
            Amenity amenity = amenityRepository.findByName(amenityName).orElse(null);
            if (amenity == null) {
                continue;
            }
            HotelAmenity link = new HotelAmenity();
            link.setHotel(hotel);
            link.setAmenity(amenity);
            link.setDescription(AMENITY_TEXT.get(amenityName));
            String cost = AMENITY_COST.get(amenityName);
            link.setAdditionalCost(cost == null ? null : new BigDecimal(cost));
            hotelAmenityRepository.save(link);
        }

        createRooms(hotel, seed.priceFactor(), types);
    }


    private void createRooms(Hotel hotel, double factor, Map<String, RoomType> types) {
        String[][] layout = {
                {"101", "1", "Standard Room"}, {"102", "1", "Standard Room"}, {"103", "1", "Standard Room"},
                {"104", "1", "Twin Room"},
                {"201", "2", "Deluxe Room"}, {"202", "2", "Deluxe Room"}, {"203", "2", "Twin Room"},
                {"301", "3", "Family Suite"}, {"302", "3", "Executive Suite"}, {"303", "3", "Family Suite"},
                {"401", "4", "Presidential Suite"}
        };
        for (String[] r : layout) {
            String[] t = Arrays.stream(ROOM_TYPES).filter(x -> x[0].equals(r[2])).findFirst().orElseThrow();
            Room room = new Room();
            room.setRoomNumber(r[0]);
            room.setFloorNumber(r[1]);
            room.setStatus(Room.Status.ACTIVE);
            room.setPricePerNight(new BigDecimal(t[2]).multiply(BigDecimal.valueOf(factor))
                    .setScale(2, java.math.RoundingMode.HALF_UP));
            room.setMaxAdults(Integer.parseInt(t[3]));
            room.setMaxChildren(Integer.parseInt(t[4]));
            room.setMaxOccupancy(Integer.parseInt(t[5]));
            room.setHotel(hotel);
            room.setRoomType(types.get(r[2]));
            roomRepository.save(room);
        }
        // one room under maintenance so that status is visible too
        Room maintenance = roomRepository.findByHotelId(hotel.getId()).stream()
                .filter(r -> r.getRoomNumber().equals("104")).findFirst().orElse(null);
        if (maintenance != null && hotel.getStatus() == Hotel.Status.ACTIVE
                && hotel.getName().equals("VibeStay Beach Resort")) {
            maintenance.setStatus(Room.Status.UNDER_MAINTENANCE);
            roomRepository.save(maintenance);
        }
    }


    private record BookingSeed(String hotel, String room, int customer, int startDay, int nights,
                               int adults, int children, Booking.Status status, String request) {
    }

    private static List<BookingSeed> bookingSeeds() {
        String m = "VibeStay Manama Grand", b = "VibeStay Beach Resort", r = "VibeStay Riffa Heights",
                d = "VibeStay Dubai Marina", a = "VibeStay Abu Dhabi Corniche", q = "VibeStay Doha Pearl",
                y = "VibeStay Riyadh Tower", u = "VibeStay Muscat Bay";
        Booking.Status C = Booking.Status.CONFIRMED, P = Booking.Status.PENDING,
                X = Booking.Status.CANCELLED, D = Booking.Status.COMPLETED;
        return List.of(
                // the very first sample booking (kept the same as before)
                new BookingSeed(m, "201", 1, 14, 2, 2, 1, C, "Sample booking created by the data seeder"),
                // upcoming
                new BookingSeed(m, "301", 2, 7, 3, 3, 2, C, "Extra bed for the children please"),
                new BookingSeed(m, "101", 3, 3, 2, 2, 0, C, null),
                new BookingSeed(m, "302", 4, 20, 4, 2, 0, P, "Late check-in around midnight"),
                new BookingSeed(b, "201", 5, 10, 5, 2, 2, C, "Sea-view room if possible"),
                new BookingSeed(b, "301", 6, 30, 7, 4, 2, C, "Celebrating a family birthday"),
                new BookingSeed(b, "102", 7, 5, 2, 2, 0, C, null),
                new BookingSeed(r, "202", 8, 12, 3, 2, 1, C, null),
                new BookingSeed(d, "401", 9, 25, 3, 2, 0, C, "Anniversary - flowers in the room"),
                new BookingSeed(d, "201", 10, 9, 4, 2, 1, C, null),
                new BookingSeed(d, "103", 11, 2, 2, 1, 0, P, "Quiet room, I work from the hotel"),
                new BookingSeed(a, "302", 12, 15, 5, 3, 1, C, null),
                new BookingSeed(q, "203", 13, 18, 3, 2, 0, C, "Airport pick-up needed"),
                new BookingSeed(y, "202", 14, 6, 2, 2, 0, C, "Invoice for the company"),
                new BookingSeed(u, "301", 15, 40, 6, 4, 2, C, null),
                // finished stays
                new BookingSeed(m, "201", 1, -30, 3, 2, 0, D, null),
                new BookingSeed(m, "101", 5, -21, 2, 2, 0, D, null),
                new BookingSeed(b, "301", 2, -45, 4, 4, 2, D, "Early check-in"),
                new BookingSeed(d, "203", 3, -18, 5, 2, 0, D, null),
                new BookingSeed(a, "101", 8, -12, 3, 2, 1, D, null),
                new BookingSeed(q, "301", 4, -60, 4, 3, 2, D, null),
                new BookingSeed(u, "201", 16, -9, 2, 2, 0, D, null),
                // cancelled
                new BookingSeed(m, "301", 6, 7, 3, 2, 0, X, "Cancelled - plans changed"),
                new BookingSeed(b, "201", 9, 10, 2, 2, 0, X, "Cancelled - found a different date"),
                new BookingSeed(d, "401", 12, 25, 2, 2, 0, X, "Cancelled by guest"),
                new BookingSeed(r, "301", 11, -5, 3, 4, 1, X, "Cancelled before arrival")
        );
    }

    private void seedBookings() {
        Map<String, Hotel> hotels = new HashMap<>();
        for (Hotel h : hotelRepository.findAll()) {
            hotels.put(h.getName(), h);
        }
        List<BookingSeed> seeds = bookingSeeds();
        for (int i = 0; i < seeds.size(); i++) {
            createBooking(String.format("SEED-%04d", i + 1), seeds.get(i), hotels);
        }
    }

    private void createBooking(String reference, BookingSeed seed, Map<String, Hotel> hotels) {
        if (bookingRepository.existsByBookingReference(reference)) {
            return;
        }
        Hotel hotel = hotels.get(seed.hotel());
        User customer = userRepository.findUserByEmail("customer" + seed.customer() + "@vibestay.com");
        if (hotel == null || customer == null) {
            return;
        }
        Room room = roomRepository.findByHotelId(hotel.getId()).stream()
                .filter(r -> r.getRoomNumber().equals(seed.room()))
                .findFirst().orElse(null);
        if (room == null) {
            return;
        }

        LocalDate checkIn = LocalDate.now().plusDays(seed.startDay());
        LocalDate checkOut = checkIn.plusDays(seed.nights());

        Booking booking = new Booking();
        booking.setBookingReference(reference);
        booking.setCheckIn(checkIn);
        booking.setCheckOut(checkOut);
        booking.setAdults(seed.adults());
        booking.setChildren(seed.children());
        booking.setTotalAmount(room.getPricePerNight().multiply(BigDecimal.valueOf(seed.nights())));
        booking.setStatus(seed.status());
        booking.setSpecialRequest(seed.request());
        booking.setHotel(hotel);
        booking.setUser(customer);

        BookingRoom bookingRoom = new BookingRoom();
        bookingRoom.setBooking(booking);
        bookingRoom.setRoom(room);
        bookingRoom.setPricePerNight(room.getPricePerNight());

        String lastName = customer.getUserProfile() != null ? customer.getUserProfile().getLastName() : "Guest";
        for (int a = 0; a < seed.adults(); a++) {
            bookingRoom.getGuests().add(guest(bookingRoom, (a == 0 ? customer.getUserProfile().getFirstName() : "Guest " + (a + 1)) + " " + lastName,
                    30 + a * 3, BookingGuest.GuestType.ADULT));
        }
        for (int c = 0; c < seed.children(); c++) {
            bookingRoom.getGuests().add(guest(bookingRoom, "Child " + (c + 1) + " " + lastName,
                    5 + c * 3, BookingGuest.GuestType.CHILD));
        }
        booking.getBookingRooms().add(bookingRoom);
        bookingRepository.save(booking);
    }

    private BookingGuest guest(BookingRoom bookingRoom, String name, int age, BookingGuest.GuestType type) {
        BookingGuest guest = new BookingGuest();
        guest.setName(name);
        guest.setAge(age);
        guest.setGuestType(type);
        guest.setBookingRoom(bookingRoom);
        return guest;
    }
}
