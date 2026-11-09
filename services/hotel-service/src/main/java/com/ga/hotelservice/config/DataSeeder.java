package com.ga.hotelservice.config;

import com.ga.hotelservice.model.*;
import com.ga.hotelservice.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@Configuration
@RequiredArgsConstructor
public class DataSeeder {

    private final AmenityRepository amenityRepository;
    private final HotelRepository hotelRepository;
    private final HotelAmenityRepository hotelAmenityRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final RoomRepository roomRepository;
    private final ChildPolicyRepository childPolicyRepository;
    private final HotelImageRepository hotelImageRepository;
    private final RoomImageRepository roomImageRepository;

    // Hotel-service does not own users. It only stores manager ids.
    // These ids MUST match the users created by user-service's seeder.
    private static final Map<String, Long> MANAGER_IDS = Map.of(
            "manager1@vibestay.com", 2L,
            "manager2@vibestay.com", 3L,
            "manager3@vibestay.com", 4L,
            "manager4@vibestay.com", 5L,
            "manager5@vibestay.com", 6L,
            "manager6@vibestay.com", 7L);

    @Bean
    CommandLineRunner seedData() {
        return args -> {
            if (hotelRepository.count() > 0) {
                return;
            }
            seedAmenities();
            seedRoomTypes();
            seedHotels();
        };
    }

    private static final String[] AMENITIES = {
            "Wi-Fi", "Swimming Pool", "Gym", "Parking", "Restaurant", "Spa", "Breakfast", "Beach Access",
            "Room Service", "Air Conditioning", "Airport Shuttle", "Laundry", "Kids Club", "Pet Friendly",
            "Business Center", "Bar"
    };

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
            Long managerId = MANAGER_IDS.get(email);
            if (managerId != null) {
                hotel.getManagerIds().add(managerId);
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
        createHotelImages(hotel);
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
                    .setScale(2, RoundingMode.HALF_UP));
            room.setMaxAdults(Integer.parseInt(t[3]));
            room.setMaxChildren(Integer.parseInt(t[4]));
            room.setMaxOccupancy(Integer.parseInt(t[5]));
            room.setHotel(hotel);
            room.setRoomType(types.get(r[2]));
            room = roomRepository.save(room);
            createRoomImages(room, r[2]);
        }
        Room maintenance = roomRepository.findByHotelId(hotel.getId()).stream()
                .filter(r -> r.getRoomNumber().equals("104")).findFirst().orElse(null);
        if (maintenance != null && hotel.getStatus() == Hotel.Status.ACTIVE
                && hotel.getName().equals("VibeStay Beach Resort")) {
            maintenance.setStatus(Room.Status.UNDER_MAINTENANCE);
            roomRepository.save(maintenance);
        }
    }

    private static final Map<String, List<String>> HOTEL_PHOTOS = Map.ofEntries(Map.entry("VibeStay Manama Grand", List.of("hotel-manama-grand", "extra-lobby-reception", "extra-restaurant-2", "extra-gym-1")), Map.entry("VibeStay Beach Resort", List.of("hotel-beach-resort", "extra-infinity-beach", "extra-cabanas", "extra-spa-2")), Map.entry("VibeStay Desert Lodge", List.of("hotel-desert-lodge", "extra-desert-view", "extra-lodge-pool")), Map.entry("VibeStay Riffa Heights", List.of("hotel-riffa-heights", "extra-garden-pool-2", "extra-garden-pool")), Map.entry("VibeStay Dubai Marina", List.of("hotel-dubai-marina", "extra-pool-aerial", "extra-pool-night", "extra-lobby-lounge")), Map.entry("VibeStay Abu Dhabi Corniche", List.of("hotel-abu-dhabi-corniche", "extra-loungers", "extra-lobby", "extra-spa-1")), Map.entry("VibeStay Doha Pearl", List.of("hotel-doha-pearl", "extra-pool-dusk", "extra-lobby-entrance")), Map.entry("VibeStay Riyadh Tower", List.of("hotel-riyadh-tower", "extra-gym-2", "extra-corridor")), Map.entry("VibeStay Muscat Bay", List.of("hotel-muscat-bay", "extra-tropical-pool", "extra-palms", "extra-resort-sunset")), Map.entry("VibeStay Kuwait Gate", List.of("hotel-kuwait-gate", "extra-courtyard-pool", "extra-umbrellas")), Map.entry("VibeStay Old Town Inn", List.of("hotel-old-town-inn", "extra-restaurant-1")));
    private static final Map<String, List<String>> ROOM_PHOTOS = Map.of("Standard Room", List.of("room-standard-1", "room-standard-2", "room-standard-3", "room-standard-4", "room-standard-5", "room-standard-6"), "Twin Room", List.of("room-twin-1", "room-twin-2", "room-twin-3", "room-twin-4"), "Deluxe Room", List.of("room-deluxe-1", "room-deluxe-2", "room-deluxe-3", "room-deluxe-4", "room-deluxe-5", "room-deluxe-6", "room-deluxe-7"), "Executive Suite", List.of("room-executive-1", "room-executive-2", "room-executive-3", "room-executive-4", "room-executive-5", "room-executive-6"), "Family Suite", List.of("room-family-1", "room-family-2", "room-family-3", "room-family-4", "room-family-5"), "Presidential Suite", List.of("room-presidential-1", "room-presidential-2", "room-presidential-3"));
    private static final List<String> SUITES = List.of("Executive Suite", "Family Suite", "Presidential Suite");
    private static final int LOUNGE_PHOTOS = 12;
    private static final int BATH_PHOTOS = 6;
    private final Map<String, byte[]> imageCache = new HashMap<>();

    private void createHotelImages(Hotel hotel) {
        List<String> photos = HOTEL_PHOTOS.getOrDefault(hotel.getName(), List.of("hotel-manama-grand"));
        for (int i = 0; i < photos.size(); i++) {
            String path = saveSeedImage(photos.get(i) + ".jpg", "uploads/hotels/" + hotel.getId(), "seed-" + (i + 1) + ".jpg");
            if (path != null) {
                HotelImage image = new HotelImage();
                image.setImageURL(path);
                image.setAltText(hotel.getName() + " photo " + (i + 1));
                image.setHotel(hotel);
                hotelImageRepository.save(image);
            }
        }
    }

    private void createRoomImages(Room room, String typeName) {
        int h = room.getHotel().getId().intValue();
        int t = Arrays.stream(ROOM_TYPES).map(x -> x[0]).toList().indexOf(typeName);
        List<String> beds = ROOM_PHOTOS.getOrDefault(typeName, ROOM_PHOTOS.get("Standard Room"));
        List<String> photos = new ArrayList<>(List.of(beds.get(h % beds.size()), beds.get((h + 1) % beds.size())));
        if (SUITES.contains(typeName)) {
            photos.add("room-lounge-" + ((h * 3 + t) % LOUNGE_PHOTOS + 1));
        }
        photos.add("room-bath-" + ((h + t) % BATH_PHOTOS + 1));
        for (int i = 0; i < photos.size(); i++) {
            String path = saveSeedImage(photos.get(i) + ".jpg", "uploads/rooms/" + h + "/" + room.getId(), "seed-" + (i + 1) + ".jpg");
            if (path != null) {
                RoomImage image = new RoomImage();
                image.setImageURL(path);
                image.setRoom(room);
                roomImageRepository.save(image);
            }
        }
    }

    private String saveSeedImage(String bundledName, String directory, String fileName) {
        byte[] data = imageCache.computeIfAbsent(bundledName, this::readBundled);
        if (data.length == 0) {
            return null;
        }
        try {
            Path dir = Paths.get(directory);
            Files.createDirectories(dir);
            Files.write(dir.resolve(fileName), data);
            return directory + "/" + fileName;
        } catch (IOException e) {
            return null;
        }
    }

    private byte[] readBundled(String name) {
        try (InputStream in = new ClassPathResource("seed-images/" + name).getInputStream()) {
            return in.readAllBytes();
        } catch (IOException e) {
            return new byte[0];
        }
    }
}