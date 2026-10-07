package com.ga.hotel_booking_app.config;

import com.ga.hotel_booking_app.model.*;
import com.ga.hotel_booking_app.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Configuration
@Profile("dev")
@RequiredArgsConstructor
public class DataSeeder {
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AmenityRepository amenityRepository;
    private final HotelRepository hotelRepository;
    private final HotelImageRepository hotelImageRepository;
    private final HotelAmenityRepository hotelAmenityRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final RoomRepository roomRepository;
    private final ChildPolicyRepository childPolicyRepository;
    private final BookingRepository bookingRepository;
    private final BookingRoomRepository bookingRoomRepository;
    private final BookingGuestRepository bookingGuestRepository;

    @Bean
    CommandLineRunner seedData() {
        return args -> {
            seedRoles();
            seedAdmin();
            seedHotelManagers();
            seedCustomers();
            seedAmenities();
            seedRoomTypes();
            seedHotels();
            seedManagerHotelAssignments();
            seedChildPolicies();
            seedRooms();
            seedHotelAmenities();
            seedHotelImages();
            seedBookings();
        };
    }

    private void seedRoles() {
        createRoleIfNotExists(Role.RoleName.CUSTOMER);
        createRoleIfNotExists(Role.RoleName.HOTEL_MANAGER);
        createRoleIfNotExists(Role.RoleName.ADMIN);
        createRoleIfNotExists(Role.RoleName.STAFF);
    }

    private void createRoleIfNotExists(Role.RoleName roleName) {
        if (roleRepository.findByName(roleName).isEmpty()) {
            Role role = new Role();
            role.setName(roleName);
            roleRepository.save(role);
        }
    }

    private void seedAdmin() {
        if (userRepository.existsByEmail("admin@vibestay.com")) {
            return;
        }
        Role adminRole = roleRepository.findByName(Role.RoleName.ADMIN).orElseThrow();
        UserProfile profile = new UserProfile();
        profile.setFirstName("VibeStay");
        profile.setLastName("Admin");
        profile.setPhone("+97330000000");
        User admin = new User();
        admin.setUsername("admin");
        admin.setEmail("admin@vibestay.com");
        admin.setPassword(passwordEncoder.encode("Admin123!"));
        admin.setEmailVerified(true);
        admin.setStatus(User.Status.ACTIVE);
        admin.setRole(adminRole);
        admin.setUserProfile(profile);
        userRepository.save(admin);
    }

    private void seedHotelManagers() {
        Role managerRole = roleRepository.findByName(Role.RoleName.HOTEL_MANAGER).orElseThrow();
        createManager("manager1", "manager1@vibestay.com", "Ahmed", "Al Khalifa", "+97331111111", managerRole);
        createManager("manager2", "manager2@vibestay.com", "Sara", "Hassan", "+97332222222", managerRole);
        createManager("manager3", "manager3@vibestay.com", "Omar", "Ali", "+97333333333", managerRole);
        createManager("manager4", "manager4@vibestay.com", "Mariam", "Yousef", "+97334444444", managerRole);
        createManager("manager5", "manager5@vibestay.com", "Daniel", "Smith", "+971500000005", managerRole);
    }

    private void createManager(String username, String email, String firstName, String lastName, String phone, Role role) {
        if (userRepository.existsByEmail(email)) {
            return;
        }
        UserProfile profile = new UserProfile();
        profile.setFirstName(firstName);
        profile.setLastName(lastName);
        profile.setPhone(phone);
        User manager = new User();
        manager.setUsername(username);
        manager.setEmail(email);
        manager.setPassword(passwordEncoder.encode("Manager123!"));
        manager.setEmailVerified(true);
        manager.setStatus(User.Status.ACTIVE);
        manager.setRole(role);
        manager.setUserProfile(profile);
        userRepository.save(manager);
    }

    private void seedCustomers() {
        Role customerRole = roleRepository.findByName(Role.RoleName.CUSTOMER).orElseThrow();
        createCustomer("customer1", "customer1@vibestay.com", "Layla", "Ahmed", "+97335000001", customerRole);
        createCustomer("customer2", "customer2@vibestay.com", "Mohammed", "Hassan", "+97335000002", customerRole);
        createCustomer("customer3", "customer3@vibestay.com", "Fatima", "Ali", "+97335000003", customerRole);
        createCustomer("customer4", "customer4@vibestay.com", "Yousef", "Salman", "+97335000004", customerRole);
        createCustomer("customer5", "customer5@vibestay.com", "Noor", "Khalid", "+97335000005", customerRole);
        createCustomer("customer6", "customer6@vibestay.com", "Hamad", "Jassim", "+97335000006", customerRole);
        createCustomer("customer7", "customer7@vibestay.com", "Aisha", "Nasser", "+97335000007", customerRole);
        createCustomer("customer8", "customer8@vibestay.com", "Daniel", "Wilson", "+97335000008", customerRole);
    }

    private void createCustomer(String username, String email, String firstName, String lastName, String phone, Role role) {
        if (userRepository.existsByEmail(email)) {
            return;
        }
        UserProfile profile = new UserProfile();
        profile.setFirstName(firstName);
        profile.setLastName(lastName);
        profile.setPhone(phone);
        User customer = new User();
        customer.setUsername(username);
        customer.setEmail(email);
        customer.setPassword(passwordEncoder.encode("Customer123!"));
        customer.setEmailVerified(true);
        customer.setStatus(User.Status.ACTIVE);
        customer.setRole(role);
        customer.setUserProfile(profile);
        userRepository.save(customer);
    }

    private void seedAmenities() {
        String[] amenities = {
                "Wi-Fi",
                "Swimming Pool",
                "Gym",
                "Parking",
                "Restaurant",
                "Spa",
                "Breakfast",
                "Beach Access",
                "Room Service",
                "Air Conditioning",
                "Airport Shuttle",
                "24-Hour Front Desk"
        };
        for (String name : amenities) {
            if (amenityRepository.findByName(name).isEmpty()) {
                Amenity amenity = new Amenity();
                amenity.setName(name);
                amenityRepository.save(amenity);
            }
        }
    }

    private void seedRoomTypes() {
        createRoomType("Standard", "Comfortable room suitable for short stays.");
        createRoomType("Deluxe", "Spacious room with upgraded facilities.");
        createRoomType("Suite", "Luxury suite with a separate living area.");
        createRoomType("Family", "Large room designed for families.");
        createRoomType("Executive", "Premium room designed for business and extended stays.");
    }

    private void createRoomType(String name, String description) {
        if (roomTypeRepository.existsByName(name)) {
            return;
        }
        RoomType roomType = new RoomType();
        roomType.setName(name);
        roomType.setDescription(description);
        roomTypeRepository.save(roomType);
    }

    private void seedHotels() {
        if (hotelRepository.count() > 0) {
            return;
        }
        createHotel("VibeStay Grand Manama", "A modern luxury hotel in the heart of Manama.", "Government Avenue", "Manama", "Bahrain", "26.2235", "50.5876", "+97317000001");
        createHotel("VibeStay Seef Resort", "A relaxing resort close to Bahrain's Seef district.", "Seef District", "Manama", "Bahrain", "26.2330", "50.5350", "+97317000002");
        createHotel("VibeStay Amwaj", "A waterfront hotel overlooking Amwaj Islands.", "Amwaj Islands", "Muharraq", "Bahrain", "26.2876", "50.6578", "+97317000003");
        createHotel("VibeStay Juffair", "A contemporary city hotel in Juffair.", "Juffair", "Manama", "Bahrain", "26.2100", "50.6080", "+97317000004");
        createHotel("VibeStay Dubai Marina", "A stylish waterfront hotel in Dubai Marina.", "Dubai Marina", "Dubai", "UAE", "25.0800", "55.1400", "+97140000001");
        createHotel("VibeStay Downtown Dubai", "A premium hotel near Downtown Dubai.", "Downtown Dubai", "Dubai", "UAE", "25.1972", "55.2744", "+97140000002");
        createHotel("VibeStay Doha Pearl", "A modern waterfront hotel in The Pearl.", "The Pearl", "Doha", "Qatar", "25.3700", "51.5500", "+97440000001");
        createHotel("VibeStay Istanbul", "A comfortable city hotel close to Istanbul's historic center.", "Sultanahmet", "Istanbul", "Türkiye", "41.0082", "28.9784", "+902120000001");
        createHotel("VibeStay London", "A modern hotel in central London.", "Westminster", "London", "UK", "51.4975", "-0.1357", "+442000000001");
        createHotel("VibeStay Paris", "A boutique stay in central Paris.", "1st Arrondissement", "Paris", "France", "48.8606", "2.3376", "+33100000001");
    }

    private void createHotel(String name, String description, String address, String city, String country, String latitude, String longitude, String phone) {
        Hotel hotel = new Hotel();
        hotel.setName(name);
        hotel.setDescription(description);
        hotel.setAddress(address);
        hotel.setCity(city);
        hotel.setCountry(country);
        hotel.setLatitude(new BigDecimal(latitude));
        hotel.setLongitude(new BigDecimal(longitude));
        hotel.setPhone(phone);
        hotel.setStatus(Hotel.Status.ACTIVE);
        hotelRepository.save(hotel);
    }

    @Transactional
    private void seedManagerHotelAssignments() {
        assignManager("manager1@vibestay.com", "VibeStay Grand Manama");
        assignManager("manager1@vibestay.com", "VibeStay Seef Resort");
        assignManager("manager2@vibestay.com", "VibeStay Amwaj");
        assignManager("manager2@vibestay.com", "VibeStay Juffair");
        assignManager("manager3@vibestay.com", "VibeStay Dubai Marina");
        assignManager("manager3@vibestay.com", "VibeStay Downtown Dubai");
        assignManager("manager4@vibestay.com", "VibeStay Doha Pearl");
        assignManager("manager5@vibestay.com", "VibeStay Istanbul");
        assignManager("manager5@vibestay.com", "VibeStay London");
        assignManager("manager5@vibestay.com", "VibeStay Paris");
    }

    private void assignManager(String managerEmail, String hotelName) {
        Hotel hotel = hotelRepository.findByName(hotelName);
        User manager = userRepository.findUserByEmail(managerEmail);
        if (hotel == null) {
            System.err.println("Error seeding assignment: Hotel not found -> " + hotelName);
            return;
        }
        if (manager == null) {
            System.err.println("Error seeding assignment: User/Manager not found -> " + managerEmail);
            return;
        }
        if (hotel.getManagers() == null) {
            hotel.setManagers(new java.util.HashSet<>());
        }
        hotel.getManagers().add(manager);
        hotelRepository.save(hotel);
    }

    private void seedChildPolicies() {
        if (childPolicyRepository.count() > 0) {
            return;
        }
        List<Hotel> hotels = hotelRepository.findAll();
        for (Hotel hotel : hotels) {
            createChildPolicy(hotel, 2, 12, false);
        }
    }

    private void createChildPolicy(Hotel hotel, int infantMaxAge, int childMaxAge, boolean infantsCountTowardOccupancy) {
        ChildPolicy policy = new ChildPolicy();
        policy.setHotel(hotel);
        policy.setInfantMaxAge(infantMaxAge);
        policy.setChildMaxAge(childMaxAge);
        policy.setInfantsCountTowardOccupancy(infantsCountTowardOccupancy);
        childPolicyRepository.save(policy);
    }

    private void seedRooms() {
        if (roomRepository.count() > 0) {
            return;
        }
        RoomType standard = roomTypeRepository.findByName("Standard");
        RoomType deluxe = roomTypeRepository.findByName("Deluxe");
        RoomType suite = roomTypeRepository.findByName("Suite");
        RoomType family = roomTypeRepository.findByName("Family");
        RoomType executive = roomTypeRepository.findByName("Executive");
        List<Hotel> hotels = hotelRepository.findAll();
        for (Hotel hotel : hotels) {
            BigDecimal standardPrice = getBasePrice(hotel, 1);
            BigDecimal deluxePrice = getBasePrice(hotel, 2);
            BigDecimal suitePrice = getBasePrice(hotel, 3);
            BigDecimal familyPrice = getBasePrice(hotel, 4);
            BigDecimal executivePrice = getBasePrice(hotel, 5);
            createRoom(hotel, standard, "101", 1, standardPrice, 2, 1, 3);
            createRoom(hotel, deluxe, "102", 1, deluxePrice, 2, 2, 4);
            createRoom(hotel, suite, "201", 2, suitePrice, 2, 2, 4);
            createRoom(hotel, family, "202", 2, familyPrice, 2, 3, 5);
            createRoom(hotel, executive, "301", 3, executivePrice, 2, 1, 3);
        }
    }

    private BigDecimal getBasePrice(Hotel hotel, int roomType) {
        boolean bahrain = hotel.getCountry().equalsIgnoreCase("Bahrain");
        boolean uae = hotel.getCountry().equalsIgnoreCase("UAE");
        BigDecimal base;
        if (bahrain) {
            base = new BigDecimal("35.00");
        } else if (uae) {
            base = new BigDecimal("90.00");
        } else {
            base = new BigDecimal("70.00");
        }
        return switch (roomType) {
            case 1 -> base;
            case 2 -> base.multiply(new BigDecimal("1.35"));
            case 3 -> base.multiply(new BigDecimal("2.00"));
            case 4 -> base.multiply(new BigDecimal("1.55"));
            case 5 -> base.multiply(new BigDecimal("1.70"));
            default -> base;
        };
    }

    private void createRoom(Hotel hotel, RoomType roomType, String roomNumber, Integer floorNumber, BigDecimal price, Integer maxAdults, Integer maxChildren, Integer maxOccupancy) {
        Room room = new Room();
        room.setHotel(hotel);
        room.setRoomType(roomType);
        room.setRoomNumber(roomNumber);
        room.setFloorNumber(String.valueOf(floorNumber));
        room.setPricePerNight(price);
        room.setMaxAdults(maxAdults);
        room.setMaxChildren(maxChildren);
        room.setMaxOccupancy(maxOccupancy);
        room.setStatus(Room.Status.ACTIVE);
        roomRepository.save(room);
    }

    private void seedHotelAmenities() {
        if (hotelAmenityRepository.count() > 0) {
            return;
        }
        List<Hotel> hotels = hotelRepository.findAll();
        for (Hotel hotel : hotels) {
            addAmenity(hotel, "Wi-Fi");
            addAmenity(hotel, "Air Conditioning");
            addAmenity(hotel, "24-Hour Front Desk");
            addAmenity(hotel, "Room Service");
            String country = hotel.getCountry();
            if (country.equalsIgnoreCase("Bahrain")) {
                addAmenity(hotel, "Swimming Pool");
                addAmenity(hotel, "Parking");
            } else if (country.equalsIgnoreCase("UAE")) {
                addAmenity(hotel, "Swimming Pool");
                addAmenity(hotel, "Gym");
                addAmenity(hotel, "Restaurant");
                addAmenity(hotel, "Spa");
            } else {
                addAmenity(hotel, "Breakfast");
                addAmenity(hotel, "Restaurant");
                addAmenity(hotel, "Gym");
            }
        }
    }

    private void addAmenity(Hotel hotel, String amenityName) {
        Amenity amenity = amenityRepository.findByName(amenityName).orElseThrow();
        HotelAmenity hotelAmenity = new HotelAmenity();
        hotelAmenity.setHotel(hotel);
        hotelAmenity.setAmenity(amenity);
        hotelAmenityRepository.save(hotelAmenity);
    }

    private void seedHotelImages() {
        if (hotelImageRepository.count() > 0) {
            return;
        }
        List<Hotel> hotels = hotelRepository.findAll();
        for (Hotel hotel : hotels) {
            createHotelImage(hotel, getHotelImage(hotel, 1), hotel.getName() + " exterior", true);
            createHotelImage(hotel, getHotelImage(hotel, 2), hotel.getName() + " room", false);
            createHotelImage(hotel, getHotelImage(hotel, 3), hotel.getName() + " pool", false);
            createHotelImage(hotel, getHotelImage(hotel, 4), hotel.getName() + " interior", false);
        }
    }

    private String getHotelImage(Hotel hotel, int imageNumber) {
        String[] images = {
                "https://images.unsplash.com/photo-1566073771259-6a8506099945",
                "https://images.unsplash.com/photo-1566665797739-1674de7a421a",
                "https://images.unsplash.com/photo-1540541338287-41700207dee6",
                "https://images.unsplash.com/photo-1564501049412-61c2a3083791"
        };
        return images[(imageNumber - 1) % images.length];
    }

    private void createHotelImage(Hotel hotel, String imageUrl, String altText, boolean isPrimary) {
        if (hotelImageRepository.existsByImageURL(imageUrl)) {
            return;
        }
        HotelImage image = new HotelImage();
        image.setHotel(hotel);
        image.setImageURL(imageUrl);
        image.setAltText(altText);
        image.setPrimary(isPrimary);
        hotelImageRepository.save(image);
    }

    private void seedBookings() {
        if (bookingRepository.count() > 0) {
            return;
        }
        List<User> customers = userRepository.findAll()
                .stream()
                .filter(user -> user.getRole().getName() == Role.RoleName.CUSTOMER)
                .toList();
        List<Hotel> hotels = hotelRepository.findAll();
        if (customers.isEmpty() || hotels.isEmpty()) {
            return;
        }
        LocalDate startDate = LocalDate.of(2026, 7, 1);
        int bookingNumber = 1;
        for (int i = 0; i < 30; i++) {
            Hotel hotel = hotels.get(i % hotels.size());
            List<Room> hotelRooms = roomRepository.findByHotel(hotel);
            if (hotelRooms.isEmpty()) {
                continue;
            }
            Room room = hotelRooms.get(i % hotelRooms.size());
            User customer = customers.get(i % customers.size());
            LocalDate checkIn = startDate.plusDays(i * 4L);
            LocalDate checkOut = checkIn.plusDays(2);
            Booking.Status status;
            if (i % 6 == 0) {
                status = Booking.Status.CANCELLED;
            } else if (i % 4 == 0) {
                status = Booking.Status.COMPLETED;
            } else {
                status = Booking.Status.CONFIRMED;
            }
            createBooking(bookingNumber, customer, hotel, room, checkIn, checkOut, status);
            bookingNumber++;
        }
    }

    private void createBooking(int bookingNumber, User customer, Hotel hotel, Room room, LocalDate checkIn, LocalDate checkOut, Booking.Status status) {
        long nights = java.time.temporal.ChronoUnit.DAYS.between(checkIn, checkOut);
        BigDecimal total = room.getPricePerNight().multiply(BigDecimal.valueOf(nights));
        Booking booking = new Booking();
        booking.setBookingReference("VS" + String.format("%04d", bookingNumber));
        booking.setUser(customer);
        booking.setHotel(hotel);
        booking.setCheckIn(checkIn);
        booking.setCheckOut(checkOut);
        booking.setStatus(status);
        booking.setAdults(2);
        booking.setChildren(bookingNumber % 3 == 0 ? 1 : 0);
        booking.setTotalAmount(total);
        booking.setSpecialRequest(bookingNumber % 5 == 0 ? "Late check-in requested" : null);
        Booking savedBooking = bookingRepository.save(booking);
        BookingRoom bookingRoom = new BookingRoom();
        bookingRoom.setBooking(savedBooking);
        bookingRoom.setRoom(room);
        bookingRoom.setPricePerNight(room.getPricePerNight());
        BookingRoom savedBookingRoom = bookingRoomRepository.save(bookingRoom);
        createBookingGuest(savedBookingRoom, "Guest " + bookingNumber, 28, BookingGuest.GuestType.ADULT);
        createBookingGuest(savedBookingRoom, "Guest " + bookingNumber + " Partner", 30, BookingGuest.GuestType.ADULT);
        if (bookingNumber % 3 == 0) {
            createBookingGuest(savedBookingRoom, "Child " + bookingNumber, 8, BookingGuest.GuestType.CHILD);
        }
    }

    private void createBookingGuest(BookingRoom bookingRoom, String name, int age, BookingGuest.GuestType guestType) {
        BookingGuest guest = new BookingGuest();
        guest.setBookingRoom(bookingRoom);
        guest.setName(name);
        guest.setAge(age);
        guest.setGuestType(guestType);
        bookingGuestRepository.save(guest);
    }
}