//package com.ga.bookingservice.config;
//
//import com.ga.bookingservice.model.*;
//import com.ga.bookingservice.repository.*;
//import lombok.RequiredArgsConstructor;
//import org.springframework.boot.CommandLineRunner;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.security.crypto.password.PasswordEncoder;
//
//import org.springframework.core.io.ClassPathResource;
//import org.springframework.jdbc.core.JdbcTemplate;
//
//import java.io.IOException;
//import java.io.InputStream;
//import java.math.BigDecimal;
//import java.nio.file.Files;
//import java.nio.file.Path;
//import java.nio.file.Paths;
//import java.time.LocalDate;
//import java.sql.Timestamp;
//import java.time.LocalDateTime;
//import java.time.temporal.ChronoUnit;
//import java.util.*;
//
//@Configuration
//@RequiredArgsConstructor
//public class DataSeeder {
//    private final RoleRepository roleRepository;
//    private final UserRepository userRepository;
//    private final PasswordEncoder passwordEncoder;
//    private final AmenityRepository amenityRepository;
//    private final HotelRepository hotelRepository;
//    private final HotelAmenityRepository hotelAmenityRepository;
//    private final RoomTypeRepository roomTypeRepository;
//    private final RoomRepository roomRepository;
//    private final ChildPolicyRepository childPolicyRepository;
//    private final BookingRepository bookingRepository;
//    private final HotelImageRepository hotelImageRepository;
//    private final RoomImageRepository roomImageRepository;
//    private final PaymentRepository paymentRepository;
//    private final ReviewRepository reviewRepository;
//    private final JdbcTemplate jdbcTemplate;
//
//    @Bean
//    CommandLineRunner seedData() {
//        return args -> {
//            if (userRepository.count() > 0 || hotelRepository.count() > 0) {
//                return;
//            }
//            seedRoles();
//            seedStaffUsers();
//            seedAmenities();
//            seedRoomTypes();
//            seedCustomers();
//            seedHotels();
//            seedBookings();
//        };
//    }
//
//    private void seedRoles() {
//        for (Role.RoleName name : Role.RoleName.values()) {
//            if (roleRepository.findByName(name).isEmpty()) {
//                roleRepository.save(new Role(null, name));
//            }
//        }
//    }
//
//    private void seedStaffUsers() {
//        createUser(usernameFor("Yasmeen", "Al Khaja"), "admin@vibestay.com", "Admin123!", Role.RoleName.ADMIN,
//                "Yasmeen", "Al Khaja", "+97330000000");
//
//        String[][] managers = {
//                {"Mohammed", "Al Sayed", "+97331111111"},
//                {"Fatema", "Janahi", "+97332222222"},
//                {"Khalid", "Al Mansoori", "+97331000003"},
//                {"Noura", "Al Hashimi", "+97331000004"},
//                {"Yusuf", "Karimi", "+97331000005"},
//                {"Layla", "Haddad", "+97331000006"}
//        };
//        for (int i = 0; i < managers.length; i++) {
//            int n = i + 1;
//            createUser(usernameFor(managers[i][0], managers[i][1]), "manager" + n + "@vibestay.com", "Manager123!",
//                    Role.RoleName.HOTEL_MANAGER, managers[i][0], managers[i][1], managers[i][2]);
//        }
//    }
//
//    private static final String[][] CUSTOMERS = {
//            {"Sara", "Ahmed"}, {"Omar", "Khalid"}, {"Fatima", "Al Zayani"}, {"Hassan", "Jaffar"},
//            {"Maryam", "Salman"}, {"Ali", "Hussain"}, {"Zainab", "Mahmood"}, {"Ahmed", "Fakhro"},
//            {"Reem", "Nasser"}, {"Yousef", "Darwish"}, {"Huda", "Abdulla"}, {"Tariq", "Mansoor"},
//            {"Aisha", "Rashid"}, {"Jasim", "Saleh"}, {"Dana", "Mubarak"}, {"Karim", "Haddad"},
//            {"Hamad", "Al Ansari"}, {"Lulwa", "Al Doseri"}, {"Ebrahim", "Shehab"}, {"Noora", "Al Qassab"},
//            {"Saleh", "Al Amer"}, {"Mariam", "Hasan"}, {"Faisal", "Matar"}, {"Shaikha", "Al Mannai"}
//    };
//
//    private void seedCustomers() {
//        for (int i = 0; i < CUSTOMERS.length; i++) {
//            int n = i + 1;
//            createUser(usernameFor(CUSTOMERS[i][0], CUSTOMERS[i][1]), "customer" + n + "@vibestay.com", "Customer123!", Role.RoleName.CUSTOMER,
//                    CUSTOMERS[i][0], CUSTOMERS[i][1], String.format("+9733400%04d", n));
//        }
//    }
//
//    // "Fatima", "Al Zayani" -> "fatima.alzayani"
//    private static String usernameFor(String first, String last) {
//        return (first + "." + last).toLowerCase().replaceAll("[^a-z.]", "");
//    }
//
//    private final Map<String, String> encodedPasswords = new HashMap<>();
//
//    private void createUser(String username, String email, String rawPassword, Role.RoleName roleName,
//                            String first, String last, String phone) {
//        if (userRepository.existsByEmail(email) || userRepository.existsByUsername(username)) {
//            return;
//        }
//        Role role = roleRepository.findByName(roleName).orElseThrow();
//
//        UserProfile profile = new UserProfile();
//        profile.setFirstName(first);
//        profile.setLastName(last);
//        profile.setPhone(phone);
//
//        User user = new User();
//        user.setUsername(username);
//        user.setEmail(email);
//        user.setPassword(encodedPasswords.computeIfAbsent(rawPassword, passwordEncoder::encode));
//        user.setEmailVerified(true);
//        user.setStatus(User.Status.ACTIVE);
//        user.setRole(role);
//        user.setUserProfile(profile);
//        userRepository.save(user);
//    }
//
//    private static final String[] AMENITIES = {
//            "Wi-Fi", "Swimming Pool", "Gym", "Parking", "Restaurant", "Spa", "Breakfast", "Beach Access",
//            "Room Service", "Air Conditioning", "Airport Shuttle", "Laundry", "Kids Club", "Pet Friendly",
//            "Business Center", "Bar"
//    };
//
//    private static final Map<String, String> AMENITY_TEXT = Map.ofEntries(
//            Map.entry("Wi-Fi", "Free high-speed Wi-Fi in all rooms"),
//            Map.entry("Swimming Pool", "Outdoor pool, open 7am - 10pm"),
//            Map.entry("Gym", "24-hour fitness room"),
//            Map.entry("Parking", "Secure on-site parking"),
//            Map.entry("Restaurant", "All-day dining"),
//            Map.entry("Spa", "Massage and wellness treatments"),
//            Map.entry("Breakfast", "Buffet breakfast, 6:30am - 10:30am"),
//            Map.entry("Beach Access", "Private beach area"),
//            Map.entry("Room Service", "Available 24 hours"),
//            Map.entry("Air Conditioning", "Individual climate control"),
//            Map.entry("Airport Shuttle", "Pick-up and drop-off on request"),
//            Map.entry("Laundry", "Same-day laundry service"),
//            Map.entry("Kids Club", "Supervised activities for children"),
//            Map.entry("Pet Friendly", "Small pets welcome"),
//            Map.entry("Business Center", "Meeting rooms and printing"),
//            Map.entry("Bar", "Lounge bar, evenings"));
//
//    private static final Map<String, String> AMENITY_COST = Map.of(
//            "Spa", "12.00", "Breakfast", "4.50", "Airport Shuttle", "8.00", "Laundry", "3.00",
//            "Pet Friendly", "6.00", "Kids Club", "5.00");
//
//    private void seedAmenities() {
//        for (String name : AMENITIES) {
//            if (amenityRepository.findByName(name).isEmpty()) {
//                Amenity amenity = new Amenity();
//                amenity.setName(name);
//                amenity.setDescription(AMENITY_TEXT.get(name));
//                amenityRepository.save(amenity);
//            }
//        }
//    }
//
//    private static final String[][] ROOM_TYPES = {
//            {"Standard Room", "Comfortable room with all the basics", "35.00", "2", "1", "3"},
//            {"Twin Room", "Two single beds, ideal for friends or colleagues", "40.00", "2", "1", "3"},
//            {"Deluxe Room", "Larger room with a better view and extra comfort", "55.00", "2", "2", "4"},
//            {"Executive Suite", "Suite with a separate lounge area and work desk", "80.00", "3", "2", "4"},
//            {"Family Suite", "Spacious suite for families with children", "90.00", "4", "3", "6"},
//            {"Presidential Suite", "Top floor suite with panoramic views", "180.00", "4", "2", "6"}
//    };
//
//    private void seedRoomTypes() {
//        for (String[] t : ROOM_TYPES) {
//            if (!roomTypeRepository.existsByName(t[0])) {
//                RoomType type = new RoomType();
//                type.setName(t[0]);
//                type.setDescription(t[1]);
//                type.setStatus(RoomType.Status.ACTIVE);
//                roomTypeRepository.save(type);
//            }
//        }
//    }
//
//    private record HotelSeed(String name, String description, String address, String city, String country,
//                             String phone, String lat, String lng, Hotel.Status status, List<String> managers,
//                             int infantMaxAge, int childMaxAge, boolean infantsCount, double priceFactor,
//                             List<String> amenities) {
//    }
//
//    private static List<HotelSeed> hotelSeeds() {
//        return List.of(
//                new HotelSeed("VibeStay Manama Grand", "A modern city hotel with sea views in the heart of Manama.",
//                        "Road 1705, Diplomatic Area", "Manama", "Bahrain", "+97317000001", "26.2361", "50.5831",
//                        Hotel.Status.ACTIVE, List.of("manager1@vibestay.com"), 2, 12, false, 1.0,
//                        List.of("Wi-Fi", "Swimming Pool", "Gym", "Restaurant", "Air Conditioning", "Parking", "Business Center")),
//                new HotelSeed("VibeStay Beach Resort", "A relaxed beachfront resort for families and couples.",
//                        "Amwaj Islands", "Muharraq", "Bahrain", "+97317000002", "26.2872", "50.6600",
//                        Hotel.Status.ACTIVE, List.of("manager2@vibestay.com"), 2, 12, false, 1.3,
//                        List.of("Wi-Fi", "Swimming Pool", "Beach Access", "Breakfast", "Spa", "Parking", "Kids Club", "Bar")),
//                new HotelSeed("VibeStay Desert Lodge", "A quiet lodge near the desert, waiting for approval.",
//                        "Sakhir Road", "Sakhir", "Bahrain", "+97317000003", "26.0325", "50.5106",
//                        Hotel.Status.PENDING_APPROVAL, List.of("manager1@vibestay.com"), 2, 12, false, 0.8,
//                        List.of("Wi-Fi", "Parking")),
//                new HotelSeed("VibeStay Riffa Heights", "Hilltop hotel near the Royal Golf Club with calm gardens.",
//                        "Riffa Views", "Riffa", "Bahrain", "+97317000004", "26.1300", "50.5550",
//                        Hotel.Status.ACTIVE, List.of("manager3@vibestay.com"), 3, 11, true, 0.9,
//                        List.of("Wi-Fi", "Swimming Pool", "Gym", "Restaurant", "Parking", "Laundry")),
//                new HotelSeed("VibeStay Dubai Marina", "High-rise stay a short walk from the Marina promenade.",
//                        "Marina Walk, Dubai Marina", "Dubai", "United Arab Emirates", "+97144000005", "25.0800", "55.1400",
//                        Hotel.Status.ACTIVE, List.of("manager4@vibestay.com"), 2, 12, false, 2.2,
//                        List.of("Wi-Fi", "Swimming Pool", "Gym", "Spa", "Restaurant", "Bar", "Airport Shuttle", "Room Service")),
//                new HotelSeed("VibeStay Abu Dhabi Corniche", "Elegant rooms on the Corniche with views of the Gulf.",
//                        "Corniche Road West", "Abu Dhabi", "United Arab Emirates", "+97126000006", "24.4764", "54.3300",
//                        Hotel.Status.ACTIVE, List.of("manager4@vibestay.com"), 2, 12, false, 1.9,
//                        List.of("Wi-Fi", "Swimming Pool", "Beach Access", "Breakfast", "Gym", "Parking")),
//                new HotelSeed("VibeStay Doha Pearl", "Waterfront hotel next to Souq Waqif and the Museum of Islamic Art.",
//                        "Corniche Street", "Doha", "Qatar", "+97444000007", "25.2900", "51.5300",
//                        Hotel.Status.ACTIVE, List.of("manager5@vibestay.com"), 2, 12, true, 1.7,
//                        List.of("Wi-Fi", "Swimming Pool", "Restaurant", "Spa", "Airport Shuttle", "Business Center")),
//                new HotelSeed("VibeStay Riyadh Tower", "Business hotel in the financial district with meeting rooms.",
//                        "King Fahd Road, Olaya", "Riyadh", "Saudi Arabia", "+96611000008", "24.6900", "46.6850",
//                        Hotel.Status.ACTIVE, List.of("manager5@vibestay.com"), 2, 12, false, 1.5,
//                        List.of("Wi-Fi", "Gym", "Restaurant", "Business Center", "Parking", "Air Conditioning", "Laundry")),
//                new HotelSeed("VibeStay Muscat Bay", "Boutique hotel between the mountains and the sea in Muscat.",
//                        "Qurum Beach Road", "Muscat", "Oman", "+96824000009", "23.6100", "58.4800",
//                        Hotel.Status.ACTIVE, List.of("manager6@vibestay.com"), 2, 12, false, 1.2,
//                        List.of("Wi-Fi", "Beach Access", "Swimming Pool", "Breakfast", "Pet Friendly", "Restaurant")),
//                new HotelSeed("VibeStay Kuwait Gate", "Central hotel close to Kuwait Towers and the Avenues mall.",
//                        "Gulf Road", "Kuwait City", "Kuwait", "+96522000010", "29.3759", "47.9774",
//                        Hotel.Status.PENDING_APPROVAL, List.of("manager6@vibestay.com"), 2, 12, false, 1.4,
//                        List.of("Wi-Fi", "Gym", "Restaurant")),
//                new HotelSeed("VibeStay Old Town Inn", "Closed for renovation, kept here to test inactive hotels.",
//                        "Bab Al Bahrain Avenue", "Manama", "Bahrain", "+97317000011", "26.2400", "50.5800",
//                        Hotel.Status.INACTIVE, List.of("manager2@vibestay.com"), 2, 12, false, 0.7,
//                        List.of("Wi-Fi", "Air Conditioning")));
//    }
//
//    private void seedHotels() {
//        Map<String, RoomType> types = new HashMap<>();
//        for (RoomType t : roomTypeRepository.findAll()) {
//            types.put(t.getName(), t);
//        }
//        for (HotelSeed seed : hotelSeeds()) {
//            if (!hotelRepository.existsByName(seed.name())) {
//                createHotel(seed, types);
//            }
//        }
//    }
//
//    private void createHotel(HotelSeed seed, Map<String, RoomType> types) {
//        Hotel hotel = new Hotel();
//        hotel.setName(seed.name());
//        hotel.setDescription(seed.description());
//        hotel.setAddress(seed.address());
//        hotel.setCity(seed.city());
//        hotel.setCountry(seed.country());
//        hotel.setPhone(seed.phone());
//        hotel.setLatitude(new BigDecimal(seed.lat()));
//        hotel.setLongitude(new BigDecimal(seed.lng()));
//        hotel.setStatus(seed.status());
//        for (String email : seed.managers()) {
//            User manager = userRepository.findUserByEmail(email);
//            if (manager != null) {
//                hotel.getManagers().add(manager);
//            }
//        }
//        hotel = hotelRepository.save(hotel);
//
//        ChildPolicy policy = new ChildPolicy();
//        policy.setInfantMaxAge(seed.infantMaxAge());
//        policy.setChildMaxAge(seed.childMaxAge());
//        policy.setInfantsCountTowardOccupancy(seed.infantsCount());
//        policy.setHotel(hotel);
//        childPolicyRepository.save(policy);
//
//        for (String amenityName : seed.amenities()) {
//            Amenity amenity = amenityRepository.findByName(amenityName).orElse(null);
//            if (amenity == null) {
//                continue;
//            }
//            HotelAmenity link = new HotelAmenity();
//            link.setHotel(hotel);
//            link.setAmenity(amenity);
//            link.setDescription(AMENITY_TEXT.get(amenityName));
//            String cost = AMENITY_COST.get(amenityName);
//            link.setAdditionalCost(cost == null ? null : new BigDecimal(cost));
//            hotelAmenityRepository.save(link);
//        }
//
//        createRooms(hotel, seed.priceFactor(), types);
//        createHotelImages(hotel);
//    }
//
//    private void createRooms(Hotel hotel, double factor, Map<String, RoomType> types) {
//        String[][] layout = {
//                {"101", "1", "Standard Room"}, {"102", "1", "Standard Room"}, {"103", "1", "Standard Room"},
//                {"104", "1", "Twin Room"},
//                {"201", "2", "Deluxe Room"}, {"202", "2", "Deluxe Room"}, {"203", "2", "Twin Room"},
//                {"301", "3", "Family Suite"}, {"302", "3", "Executive Suite"}, {"303", "3", "Family Suite"},
//                {"401", "4", "Presidential Suite"}
//        };
//        for (String[] r : layout) {
//            String[] t = Arrays.stream(ROOM_TYPES).filter(x -> x[0].equals(r[2])).findFirst().orElseThrow();
//            Room room = new Room();
//            room.setRoomNumber(r[0]);
//            room.setFloorNumber(r[1]);
//            room.setStatus(Room.Status.ACTIVE);
//            room.setPricePerNight(new BigDecimal(t[2]).multiply(BigDecimal.valueOf(factor))
//                    .setScale(2, java.math.RoundingMode.HALF_UP));
//            room.setMaxAdults(Integer.parseInt(t[3]));
//            room.setMaxChildren(Integer.parseInt(t[4]));
//            room.setMaxOccupancy(Integer.parseInt(t[5]));
//            room.setHotel(hotel);
//            room.setRoomType(types.get(r[2]));
//            room = roomRepository.save(room);
//            createRoomImages(room, r[2]);
//        }
//        Room maintenance = roomRepository.findByHotelId(hotel.getId()).stream()
//                .filter(r -> r.getRoomNumber().equals("104")).findFirst().orElse(null);
//        if (maintenance != null && hotel.getStatus() == Hotel.Status.ACTIVE
//                && hotel.getName().equals("VibeStay Beach Resort")) {
//            maintenance.setStatus(Room.Status.UNDER_MAINTENANCE);
//            roomRepository.save(maintenance);
//        }
//    }
//
//    private static final Map<String, List<String>> HOTEL_PHOTOS = Map.ofEntries(Map.entry("VibeStay Manama Grand", List.of("hotel-manama-grand", "extra-lobby-reception", "extra-restaurant-2", "extra-gym-1")), Map.entry("VibeStay Beach Resort", List.of("hotel-beach-resort", "extra-infinity-beach", "extra-cabanas", "extra-spa-2")), Map.entry("VibeStay Desert Lodge", List.of("hotel-desert-lodge", "extra-desert-view", "extra-lodge-pool")), Map.entry("VibeStay Riffa Heights", List.of("hotel-riffa-heights", "extra-garden-pool-2", "extra-garden-pool")), Map.entry("VibeStay Dubai Marina", List.of("hotel-dubai-marina", "extra-pool-aerial", "extra-pool-night", "extra-lobby-lounge")), Map.entry("VibeStay Abu Dhabi Corniche", List.of("hotel-abu-dhabi-corniche", "extra-loungers", "extra-lobby", "extra-spa-1")), Map.entry("VibeStay Doha Pearl", List.of("hotel-doha-pearl", "extra-pool-dusk", "extra-lobby-entrance")), Map.entry("VibeStay Riyadh Tower", List.of("hotel-riyadh-tower", "extra-gym-2", "extra-corridor")), Map.entry("VibeStay Muscat Bay", List.of("hotel-muscat-bay", "extra-tropical-pool", "extra-palms", "extra-resort-sunset")), Map.entry("VibeStay Kuwait Gate", List.of("hotel-kuwait-gate", "extra-courtyard-pool", "extra-umbrellas")), Map.entry("VibeStay Old Town Inn", List.of("hotel-old-town-inn", "extra-restaurant-1")));
//    private static final Map<String, List<String>> ROOM_PHOTOS = Map.of("Standard Room", List.of("room-standard-1", "room-standard-2", "room-standard-3", "room-standard-4", "room-standard-5", "room-standard-6"), "Twin Room", List.of("room-twin-1", "room-twin-2", "room-twin-3", "room-twin-4"), "Deluxe Room", List.of("room-deluxe-1", "room-deluxe-2", "room-deluxe-3", "room-deluxe-4", "room-deluxe-5", "room-deluxe-6", "room-deluxe-7"), "Executive Suite", List.of("room-executive-1", "room-executive-2", "room-executive-3", "room-executive-4", "room-executive-5", "room-executive-6"), "Family Suite", List.of("room-family-1", "room-family-2", "room-family-3", "room-family-4", "room-family-5"), "Presidential Suite", List.of("room-presidential-1", "room-presidential-2", "room-presidential-3"));
//    private static final List<String> SUITES = List.of("Executive Suite", "Family Suite", "Presidential Suite");
//    private static final int LOUNGE_PHOTOS = 12;
//    private static final int BATH_PHOTOS = 6;
//    private final Map<String, byte[]> imageCache = new HashMap<>();
//
//    private void createHotelImages(Hotel hotel) {
//        List<String> photos = HOTEL_PHOTOS.getOrDefault(hotel.getName(), List.of("hotel-manama-grand"));
//        for (int i = 0; i < photos.size(); i++) {
//            String path = saveSeedImage(photos.get(i) + ".jpg", "uploads/hotels/" + hotel.getId(), "seed-" + (i + 1) + ".jpg");
//            if (path != null) {
//                HotelImage image = new HotelImage();
//                image.setImageURL(path);
//                image.setAltText(hotel.getName() + " photo " + (i + 1));
//                image.setHotel(hotel);
//                hotelImageRepository.save(image);
//            }
//        }
//    }
//
//    private void createRoomImages(Room room, String typeName) {
//        int h = room.getHotel().getId().intValue();
//        int t = Arrays.stream(ROOM_TYPES).map(x -> x[0]).toList().indexOf(typeName);
//        List<String> beds = ROOM_PHOTOS.getOrDefault(typeName, ROOM_PHOTOS.get("Standard Room"));
//        List<String> photos = new ArrayList<>(List.of(beds.get(h % beds.size()), beds.get((h + 1) % beds.size())));
//        if (SUITES.contains(typeName)) {
//            photos.add("room-lounge-" + ((h * 3 + t) % LOUNGE_PHOTOS + 1));
//        }
//        photos.add("room-bath-" + ((h + t) % BATH_PHOTOS + 1));
//        for (int i = 0; i < photos.size(); i++) {
//            String path = saveSeedImage(photos.get(i) + ".jpg", "uploads/rooms/" + h + "/" + room.getId(), "seed-" + (i + 1) + ".jpg");
//            if (path != null) {
//                RoomImage image = new RoomImage();
//                image.setImageURL(path);
//                image.setRoom(room);
//                roomImageRepository.save(image);
//            }
//        }
//    }
//
//    private String saveSeedImage(String bundledName, String directory, String fileName) {
//        byte[] data = imageCache.computeIfAbsent(bundledName, this::readBundled);
//        if (data.length == 0) {
//            return null;
//        }
//        try {
//            Path dir = Paths.get(directory);
//            Files.createDirectories(dir);
//            Files.write(dir.resolve(fileName), data);
//            return directory + "/" + fileName;
//        } catch (IOException e) {
//            return null;
//        }
//    }
//
//    private byte[] readBundled(String name) {
//        try (InputStream in = new ClassPathResource("seed-images/" + name).getInputStream()) {
//            return in.readAllBytes();
//        } catch (IOException e) {
//            return new byte[0];
//        }
//    }
//
//    // ---------------------------------------------------------------- bookings
//    // Generated with a fixed random seed, so every fresh database gets the same data.
//    // Covers every state: completed (most with a review), paid and confirmed, staying now,
//    // arriving / leaving today, cancelled (paid or unpaid) and a few pending payment holds.
//
//    private static final Set<String> FEMALE = Set.of("Sara", "Fatima", "Maryam", "Zainab", "Reem", "Huda",
//            "Aisha", "Dana", "Lulwa", "Noora", "Mariam", "Shaikha");
//    private static final String[] WIVES = {"Noor", "Latifa", "Hessa", "Mona", "Amal", "Rana", "Sumaya", "Batool"};
//    private static final String[] HUSBANDS = {"Hussain", "Jassim", "Mahmood", "Nabeel", "Adel", "Waleed", "Majed", "Sami"};
//    private static final String[] KIDS = {"Adam", "Jana", "Isa", "Malak", "Hamad", "Talia", "Zayed", "Nada",
//            "Yaqoob", "Lina", "Omar", "Ruqaya"};
//    private static final String[] FRIENDS = {"Ali Al Ansari", "Hessa Al Doseri", "Khalil Ebrahim", "Mona Fardan",
//            "Rashed Al Amer", "Noor Taqi", "Bader Al Sayegh", "Layla Mattar"};
//
//    private static final String[] REQUESTS = {
//            "Late check-in, we land around midnight", "High floor if possible", "Quiet room away from the lift",
//            "Early check-in please", "Celebrating our anniversary", "Airport pick-up needed",
//            "Twin beds instead of a double", "Invoice in the company name please", "Sea-view room if available",
//            "Late check-out until 2pm", "Non-smoking room", "Birthday trip - a small cake would be lovely"};
//
//    private static final Map<Integer, String[]> REVIEW_TEXT = Map.of(
//            5, new String[]{"Amazing stay, the staff were friendly and the room was spotless.",
//                    "Perfect for families, the kids loved the pool. We will come back!",
//                    "Everything was excellent from arrival to check-out.",
//                    "We celebrated our anniversary here and the staff made it really special.",
//                    "Quiet, calm and beautifully designed. Housekeeping was excellent.",
//                    "Best hotel breakfast I have had in a long time. Very helpful reception team."},
//            4, new String[]{"Great location and comfortable beds. Breakfast could have more choice.",
//                    "Lovely view and quiet rooms. Parking was easy to find.",
//                    "Clean, modern and close to everything. Wi-Fi was fast.",
//                    "Spacious room and a great shower. The gym was small but well equipped.",
//                    "Good for a business trip. Fast Wi-Fi and a proper desk in the room.",
//                    "Easy check-in, comfortable bed and good value. Would stay again."},
//            3, new String[]{"Good value for money, but check-in took longer than expected.",
//                    "Nice hotel, but the pool area was crowded on the weekend.",
//                    "Room was clean, but the view was of the parking lot.",
//                    "Decent stay. The room was a bit smaller than in the photos."},
//            2, new String[]{"The room was fine but the air conditioning was noisy at night.",
//                    "Housekeeping missed our room twice. Location is good though."},
//            1, new String[]{"Our room was not ready until 6pm and nobody apologised."});
//    private static final int[] RATINGS = {5, 5, 5, 5, 4, 4, 4, 4, 4, 3, 3, 2, 1};
//
//    private final Map<Long, String[]> customerNames = new HashMap<>();
//
//    private void seedBookings() {
//        Random rnd = new Random(20261104L);
//        LocalDate today = LocalDate.now();
//        LocalDateTime now = LocalDateTime.now();
//        Set<String> references = new HashSet<>();
//
//        List<User> customers = new ArrayList<>();
//        for (int i = 1; i <= CUSTOMERS.length; i++) {
//            User customer = userRepository.findUserByEmail("customer" + i + "@vibestay.com");
//            customers.add(customer);
//            customerNames.put(customer.getId(), CUSTOMERS[i - 1]);
//        }
//        backdateUsers(rnd, now);
//
//        List<Hotel> hotels = hotelRepository.findAll().stream()
//                .filter(h -> h.getStatus() == Hotel.Status.ACTIVE)
//                .sorted(Comparator.comparing(Hotel::getId)).toList();
//        int showcase = 0;
//        for (Hotel hotel : hotels) {
//            ChildPolicy policy = childPolicyRepository.findByHotelId(hotel.getId()).orElse(null);
//            List<Room> rooms = roomRepository.findByHotelId(hotel.getId()).stream()
//                    .filter(r -> r.getStatus() == Room.Status.ACTIVE)
//                    .sorted(Comparator.comparing(Room::getRoomNumber)).limit(6).toList();
//            for (int i = 0; i < rooms.size(); i++) {
//                Room room = rooms.get(i);
//                // the first three rooms of every hotel show today's front-desk work:
//                // a guest arriving today, a guest staying now, and a guest leaving today (not completed yet)
//                LocalDate[] fixed = switch (i) {
//                    case 0 -> new LocalDate[]{today, today.plusDays(3)};
//                    case 1 -> new LocalDate[]{today.minusDays(2), today.plusDays(2)};
//                    case 2 -> new LocalDate[]{today.minusDays(3), today};
//                    default -> null;
//                };
//                if (fixed != null) {
//                    User guest = customers.get(showcase++ % customers.size());
//                    createBooking(rnd, references, hotel, room, policy, guest, fixed[0], fixed[1],
//                            Booking.Status.CONFIRMED, now);
//                }
//                LocalDate t = today.minusDays(150 + rnd.nextInt(20));
//                while (true) {
//                    t = t.plusDays(6 + rnd.nextInt(30));
//                    int nights = 1 + rnd.nextInt(rnd.nextInt(4) == 0 ? 7 : 4);
//                    LocalDate in = t, out = t.plusDays(nights);
//                    if (in.isAfter(today.plusDays(75))) {
//                        break;
//                    }
//                    if (fixed != null && in.isBefore(fixed[1]) && out.isAfter(fixed[0])) {
//                        t = fixed[1];
//                        continue;
//                    }
//                    // the first three customers book a lot, so the demo accounts have a full history
//                    User guest = rnd.nextInt(10) < 2 ? customers.get(rnd.nextInt(3))
//                            : customers.get(rnd.nextInt(customers.size()));
//                    createBooking(rnd, references, hotel, room, policy, guest, in, out,
//                            pickStatus(rnd, in, out, today), now);
//                    t = out;
//                }
//            }
//        }
//    }
//
//    private static Booking.Status pickStatus(Random rnd, LocalDate in, LocalDate out, LocalDate today) {
//        int roll = rnd.nextInt(100);
//        if (!out.isAfter(today)) {
//            return roll < 12 ? Booking.Status.CANCELLED : Booking.Status.COMPLETED;
//        }
//        if (!in.isAfter(today)) {
//            return Booking.Status.CONFIRMED;
//        }
//        if (roll < 6 && in.isAfter(today.plusDays(3))) {
//            return Booking.Status.PENDING;
//        }
//        return roll < 18 ? Booking.Status.CANCELLED : Booking.Status.CONFIRMED;
//    }
//
//    private void createBooking(Random rnd, Set<String> references, Hotel hotel, Room room, ChildPolicy policy,
//                               User customer, LocalDate in, LocalDate out, Booking.Status status,
//                               LocalDateTime now) {
//        int nights = (int) ChronoUnit.DAYS.between(in, out);
//        String reference;
//        do {
//            reference = "VIBE" + Integer.toHexString(0x10000 + rnd.nextInt(0xEFFFF)).toUpperCase();
//        } while (!references.add(reference));
//
//        // when the guest booked: one day to six weeks ahead, never in the future
//        LocalDateTime created = in.minusDays(1 + rnd.nextInt(42)).atTime(8 + rnd.nextInt(14), rnd.nextInt(60));
//        if (created.isAfter(now)) {
//            // future stays: booked some time in the last two months
//            created = now.minusHours(2 + rnd.nextInt(24 * 60));
//        }
//        if (status == Booking.Status.PENDING) {
//            created = now.minusMinutes(1 + rnd.nextInt(8));
//        }
//
//        Booking booking = new Booking();
//        booking.setBookingReference(reference);
//        booking.setCheckIn(in);
//        booking.setCheckOut(out);
//        booking.setTotalAmount(room.getPricePerNight().multiply(BigDecimal.valueOf(nights)));
//        booking.setStatus(status);
//        booking.setHotel(hotel);
//        booking.setUser(customer);
//        if (status == Booking.Status.PENDING) {
//            // same 15-minute payment hold the app uses; the expiry job cancels it if nobody pays
//            booking.setHoldExpiresAt(created.plusMinutes(15));
//        }
//
//        BookingRoom bookingRoom = new BookingRoom();
//        bookingRoom.setBooking(booking);
//        bookingRoom.setRoom(room);
//        bookingRoom.setPricePerNight(room.getPricePerNight());
//        int[] party = addGuests(rnd, bookingRoom, room, policy, customer);
//        booking.setAdults(party[0]);
//        booking.setChildren(party[1]);
//        booking.getBookingRooms().add(bookingRoom);
//        if (rnd.nextInt(10) < 3) {
//            booking.setSpecialRequest(party[2] > 0 && rnd.nextBoolean()
//                    ? "Baby cot in the room please" : REQUESTS[rnd.nextInt(REQUESTS.length)]);
//        }
//        booking = bookingRepository.save(booking);
//
//        LocalDateTime updated = created.plusMinutes(3);
//        switch (status) {
//            case CONFIRMED -> createPayment(booking, Payment.STATUS.SUCCEED, created.plusMinutes(2));
//            case COMPLETED -> {
//                createPayment(booking, Payment.STATUS.SUCCEED, created.plusMinutes(2));
//                updated = out.atTime(11, rnd.nextInt(60));
//                // most guests leave a review a few days after they check out
//                LocalDateTime reviewed = out.plusDays(rnd.nextInt(6)).atTime(9 + rnd.nextInt(12), rnd.nextInt(60));
//                if (rnd.nextInt(100) < 75 && reviewed.isBefore(now)) {
//                    createReview(rnd, booking, reviewed);
//                }
//            }
//            case CANCELLED -> {
//                // some guests paid and then cancelled, some never finished paying
//                int roll = rnd.nextInt(10);
//                if (roll < 6) {
//                    createPayment(booking, Payment.STATUS.SUCCEED, created.plusMinutes(2));
//                    LocalDateTime latest = in.atStartOfDay().isBefore(now) ? in.atStartOfDay() : now;
//                    LocalDateTime cancelled = created.plusDays(1 + rnd.nextInt(10));
//                    updated = cancelled.isBefore(latest) ? cancelled : latest.minusHours(1);
//                } else {
//                    if (roll < 8) {
//                        createPayment(booking, Payment.STATUS.FAILED, created.plusMinutes(4));
//                    }
//                    updated = created.plusMinutes(15);
//                }
//            }
//            default -> updated = created;
//        }
//        if (updated.isBefore(created)) {
//            updated = created;
//        }
//        jdbcTemplate.update("UPDATE bookings SET created_at = ?, updated_at = ? WHERE id = ?",
//                Timestamp.valueOf(created), Timestamp.valueOf(updated), booking.getId());
//    }
//
//    // fills the room with a realistic party that fits it; returns {adults, children, infants}
//    private int[] addGuests(Random rnd, BookingRoom bookingRoom, Room room, ChildPolicy policy, User customer) {
//        String[] fullName = customerNames.get(customer.getId());
//        String first = fullName[0], last = fullName[1];
//        int maxAdults = room.getMaxAdults(), maxOccupancy = room.getMaxOccupancy();
//        int adults = maxAdults >= 2 && rnd.nextInt(10) < 7 ? 2 : 1 + rnd.nextInt(maxAdults);
//        int kids = rnd.nextInt(10) < 4
//                ? rnd.nextInt(Math.max(0, Math.min(room.getMaxChildren(), maxOccupancy - adults)) + 1) : 0;
//        int infantMax = policy == null ? 2 : policy.getInfantMaxAge();
//
//        // each customer has one fixed family (same spouse, children and ages in every booking)
//        Random family = new Random(customer.getId() * 7919L);
//        int age = 28 + family.nextInt(25);
//        String spouse = (FEMALE.contains(first) ? HUSBANDS : WIVES)[family.nextInt(8)];
//        int spouseAge = age - 4 + family.nextInt(9);
//        List<String> kidNames = new ArrayList<>(List.of(KIDS));
//        Collections.shuffle(kidNames, family);
//        int[] kidAges = {3 + family.nextInt(9), family.nextInt(8), family.nextInt(3)}; // all within every hotel's child range
//
//        bookingRoom.getGuests().add(guest(bookingRoom, first + " " + last, age, BookingGuest.GuestType.ADULT));
//        for (int a = 1; a < adults; a++) {
//            String name = a == 1 ? spouse + " " + last : FRIENDS[rnd.nextInt(FRIENDS.length)];
//            bookingRoom.getGuests().add(guest(bookingRoom, name, a == 1 ? spouseAge : 25 + rnd.nextInt(40),
//                    BookingGuest.GuestType.ADULT));
//        }
//        int children = 0, infants = 0;
//        for (int c = 0; c < kids; c++) {
//            int kidAge = kidAges[c];
//            boolean infant = kidAge <= infantMax;
//            bookingRoom.getGuests().add(guest(bookingRoom, kidNames.get(c) + " " + last, kidAge,
//                    infant ? BookingGuest.GuestType.INFANT : BookingGuest.GuestType.CHILD));
//            if (infant) {
//                infants++;
//            } else {
//                children++;
//            }
//        }
//        return new int[]{adults, children, infants};
//    }
//
//    private void createPayment(Booking booking, Payment.STATUS status, LocalDateTime at) {
//        String ref = booking.getBookingReference();
//        Payment payment = new Payment();
//        payment.setBooking(booking);
//        payment.setAmount(booking.getTotalAmount());
//        payment.setMethod(Payment.METHOD.CARD);
//        payment.setStatus(status);
//        payment.setProviderReference("cs_seed_" + ref);
//        payment.setIdempotencyKey("seed-" + ref);
//        payment.setPaymentIntent(status == Payment.STATUS.SUCCEED ? "pi_seed_" + ref : null);
//        payment = paymentRepository.save(payment);
//        jdbcTemplate.update("UPDATE payments SET created_at = ?, updated_at = ? WHERE id = ?",
//                Timestamp.valueOf(at), Timestamp.valueOf(at.plusMinutes(1)), payment.getId());
//    }
//
//    private void createReview(Random rnd, Booking booking, LocalDateTime at) {
//        int rating = RATINGS[rnd.nextInt(RATINGS.length)];
//        String[] texts = REVIEW_TEXT.get(rating);
//        Review review = new Review();
//        review.setRating(rating);
//        review.setComment(rnd.nextInt(10) == 0 ? null : texts[rnd.nextInt(texts.length)]);
//        review.setBooking(booking);
//        review = reviewRepository.save(review);
//        jdbcTemplate.update("UPDATE reviews SET created_at = ? WHERE id = ?", Timestamp.valueOf(at), review.getId());
//    }
//
//    // accounts look like they joined over the past year or two rather than all at once
//    private void backdateUsers(Random rnd, LocalDateTime now) {
//        for (User user : userRepository.findAll()) {
//            boolean staff = user.getEmail().startsWith("admin") || user.getEmail().startsWith("manager");
//            LocalDateTime joined = now.minusDays((staff ? 400 : 180) + rnd.nextInt(200)).withHour(9 + rnd.nextInt(10));
//            jdbcTemplate.update("UPDATE users SET created_at = ?, updated_at = ? WHERE id = ?",
//                    Timestamp.valueOf(joined), Timestamp.valueOf(joined), user.getId());
//        }
//    }
//
//    private BookingGuest guest(BookingRoom bookingRoom, String name, int age, BookingGuest.GuestType type) {
//        BookingGuest guest = new BookingGuest();
//        guest.setName(name);
//        guest.setAge(age);
//        guest.setGuestType(type);
//        guest.setBookingRoom(bookingRoom);
//        return guest;
//    }
//}
