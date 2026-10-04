package com.ga.hotel_booking_app.service;

import com.ga.hotel_booking_app.dto.reponse.AmenityResponse;
import com.ga.hotel_booking_app.dto.reponse.HotelImageResponse;
import com.ga.hotel_booking_app.dto.reponse.HotelResponse;
import com.ga.hotel_booking_app.dto.reponse.MessageResponse;
import com.ga.hotel_booking_app.dto.request.CreateHotelRequest;
import com.ga.hotel_booking_app.dto.request.UpdateHotelRequest;
import com.ga.hotel_booking_app.dto.request.UpdateHotelStatusRequest;
import com.ga.hotel_booking_app.exception.custom.InformationExistException;
import com.ga.hotel_booking_app.exception.custom.InformationNotFoundException;
import com.ga.hotel_booking_app.exception.custom.InvalidInformationException;
import com.ga.hotel_booking_app.exception.custom.UnauthorizedActionException;
import com.ga.hotel_booking_app.model.*;
import com.ga.hotel_booking_app.repository.HotelRepository;
import com.ga.hotel_booking_app.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class HotelService {
    @Autowired
    private HotelRepository hotelRepository;

    @Autowired
    private UserRepository userRepository;

    public User getCurrentLoggedInUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        return userRepository.findUserByEmail(email);
    }

    public ResponseEntity<?> create(CreateHotelRequest request) {
        if (hotelRepository.existsByName(request.getName())) {
            throw new InformationExistException("Hotel " + request.getName() + " already exists");
        }

        User user = (getCurrentLoggedInUser());
        System.out.println("User  ->" + user.getId());
        Hotel hotel = new Hotel();
        hotel.setName(request.getName());
        hotel.setAddress(request.getAddress());
        hotel.setCity(request.getCity());
        hotel.setCountry(request.getCountry());
        hotel.setLatitude(request.getLatitude());
        hotel.setLongitude(request.getLongitude());
        hotel.setDescription(request.getDescription());
        hotel.setPhone(request.getPhone());

        if (user.getRole().getName() == Role.RoleName.HOTEL_MANAGER) {
            hotel.getManagers().add(user);
            hotel.setStatus(Hotel.Status.PENDING_APPROVAL);
            hotelRepository.save(hotel);
            return ResponseEntity.status(HttpStatus.CREATED).body(new MessageResponse("Hotel added successfully, Please wait for admin approval"));

        } else {
            if (request.getManagerId() == null) {
                throw new InvalidInformationException("Manager ID is required when an admin creates a hotel");
            }
            User manager = userRepository.findById(request.getManagerId())
                    .orElseThrow(() ->
                            new InformationNotFoundException(
                                    "Manager with id " + request.getManagerId() + " not found"
                            ));
            if (manager.getRole().getName() != Role.RoleName.HOTEL_MANAGER) {
                throw new InvalidInformationException("The selected user is not a hotel manager");
            }
            hotel.setStatus(Hotel.Status.ACTIVE);
            hotel.getManagers().add(manager);
            hotelRepository.save(hotel);
            return ResponseEntity.status(HttpStatus.CREATED).body(new MessageResponse("Hotel added successfully"));

        }
    }

    public ResponseEntity<?> getHotel(Long id) {
        Hotel hotel = hotelRepository.findById(id)
                .orElseThrow(() -> new InformationNotFoundException("Hotel with id " + id + " not found"));

        return ResponseEntity.ok(buildHotelResponse(hotel));

    }

    public HotelResponse buildHotelResponse(Hotel hotel) {
        HotelResponse response = new HotelResponse();
        response.setName(hotel.getName());
        response.setDescription(hotel.getDescription());
        response.setAddress(hotel.getAddress());
        response.setCity(hotel.getCity());
        response.setCountry(hotel.getCountry());
        response.setLatitude(hotel.getLatitude());
        response.setLongitude(hotel.getLongitude());
        response.setPhone(hotel.getPhone());
        response.setUpdatedAt(hotel.getUpdatedAt());
        //response.getReviewCount(hotel.getreview);
        List<AmenityResponse> amenities = new ArrayList<>();

        for (HotelAmenity hotelAmenity : hotel.getAmenities()) {
            Amenity amenity = hotelAmenity.getAmenity();

            AmenityResponse amenityResponse = new AmenityResponse();

            amenityResponse.setId(amenity.getId());
            amenityResponse.setName(amenity.getName());
            amenityResponse.setDescription(hotelAmenity.getDescription());
            amenityResponse.setAdditionalCost(hotelAmenity.getAdditionalCost());

            amenities.add(amenityResponse);
        }
        response.setAmenities(amenities);
        List<HotelImageResponse> images = new ArrayList<>();
        for (HotelImage image : hotel.getImages()) {
            HotelImageResponse hotelImageResponse = new HotelImageResponse();
            hotelImageResponse.setId(image.getId());
            hotelImageResponse.setImageUrl(image.getImageURL());
            images.add(hotelImageResponse);
        }
        response.setImages(images);
        return response;
    }

    public ResponseEntity<?> getHotels(String search, String name,
                                       String country, String city) {
        List<Hotel> hotels = hotelRepository.findByStatus(Hotel.Status.ACTIVE);
        if (search!=null) {
            hotels.stream().filter(h -> h.getName().toLowerCase().contains(search)).toList();
        }
        if (name!=null) {
            hotels.stream().filter(h -> h.getName().equalsIgnoreCase(name)).toList();
        }
        if (city!=null) {
            hotels.stream().filter(h -> h.getCity().equalsIgnoreCase(city)).toList();
        }
        if (country!=null) {
            hotels.stream().filter(h -> h.getCountry().equalsIgnoreCase(country)).toList();
        }

        // later add filter by availability and prices and so on.

        List<HotelResponse> responses = new ArrayList<>();

        for (Hotel hotel : hotels) {
            HotelResponse hotelResponse = buildHotelResponse(hotel);
            responses.add(hotelResponse);
        }
        return ResponseEntity.ok(responses);


    }

    public ResponseEntity<?> getPendingHotels() {
        List<Hotel> hotels = hotelRepository.findByStatus(Hotel.Status.PENDING_APPROVAL);
        List<HotelResponse> responses = new ArrayList<>();
        for (Hotel hotel : hotels) {
            HotelResponse hotelResponse = buildHotelResponse(hotel);
            responses.add(hotelResponse);
        }
        return ResponseEntity.ok(responses);
    }

    public ResponseEntity<?> getMangerHotels() {
        User manager = getCurrentLoggedInUser();
        List<Hotel> hotels = hotelRepository.findByManagers_Id(manager.getId());
        List<HotelResponse> responses = new ArrayList<>();
        for (Hotel hotel : hotels) {
            HotelResponse hotelResponse = buildHotelResponse(hotel);
            responses.add(hotelResponse);
        }
        return ResponseEntity.ok(responses);
    }

    public ResponseEntity<?> updateHotel(Long id, UpdateHotelRequest request) {
        Hotel hotel = hotelRepository.findById(id)
                .orElseThrow(() -> new InformationNotFoundException("Hotel with id " + id + " not found"));

        User user = (getCurrentLoggedInUser());
        System.out.println("User Logged in  ->" + user.getId());


        if (user.getRole().getName().equals(Role.RoleName.HOTEL_MANAGER)) {
            boolean isMangerHotel = hotel.getManagers().stream()
                    .anyMatch(m -> m.getId().equals(user.getId()));
            if (!isMangerHotel) {
                throw new UnauthorizedActionException(
                        "You are not authorized to update this hotel"
                );
            }
        } else if (user.getRole().getName() == Role.RoleName.ADMIN) {
            if (request.getManagerId() != null) {
                User manager = userRepository.findById(request.getManagerId())
                        .orElseThrow(() ->
                                new InformationNotFoundException(
                                        "Manager with id " + request.getManagerId() + " not found"
                                ));
                if (manager.getRole().getName() != Role.RoleName.HOTEL_MANAGER) {
                    throw new InvalidInformationException("The selected user is not a hotel manager");
                }
                hotel.getManagers().add(manager);

            }
        } else {
            throw new UnauthorizedActionException(
                    "You are not authorized to update this hotel"
            );
        }

        hotel.setName(request.getName());
        hotel.setAddress(request.getAddress());
        hotel.setCity(request.getCity());
        hotel.setCountry(request.getCountry());
        hotel.setLatitude(request.getLatitude());
        hotel.setLongitude(request.getLongitude());
        hotel.setDescription(request.getDescription());
        hotel.setPhone(request.getPhone());

        hotelRepository.save(hotel);
        return ResponseEntity.ok(new MessageResponse("Hotel updated successfully"));
    }

    public ResponseEntity<?> updateHotelStatus(Long id, UpdateHotelStatusRequest request) {
        Hotel hotel = hotelRepository.findById(id)
                .orElseThrow(() -> new InformationNotFoundException("Hotel with id " + id + " not found"));
        Hotel.Status currentStatus = hotel.getStatus();
        Hotel.Status newStatus = request.getStatus();
        boolean validTransition = (currentStatus == Hotel.Status.PENDING_APPROVAL &&
                (newStatus == Hotel.Status.ACTIVE ||
                        newStatus == Hotel.Status.REJECTED))
                ||
                (currentStatus == Hotel.Status.ACTIVE &&
                        newStatus == Hotel.Status.INACTIVE)
                ||
                (currentStatus == Hotel.Status.INACTIVE &&
                        newStatus == Hotel.Status.ACTIVE);

        if (!validTransition) {
            throw new InvalidInformationException(
                    "Invalid hotel status transition from "
                            + currentStatus + " to " + newStatus
            );
        }
        hotel.setStatus(request.getStatus());
        hotelRepository.save(hotel);
        return ResponseEntity.ok(new MessageResponse("Hotel Updated successfully"));
    }

    public ResponseEntity<?> assignManager(Long hotelId, Long managerId) {
        Hotel hotel = hotelRepository.findById(hotelId)
                .orElseThrow(() -> new InformationNotFoundException("Hotel with id " + hotelId + " not found"));
        User manager = userRepository.findById(managerId)
                .orElseThrow(() -> new InformationNotFoundException("User with id " + managerId + " not found"));
        if (!manager.getRole().getName().equals(Role.RoleName.HOTEL_MANAGER)) {
            throw new InvalidInformationException("This use id not a hotel manager");
        }
        if (hotel.getManagers().contains(manager)) {
            throw new InvalidInformationException("This manager is already assigned to this hotel");
        }
        hotel.getManagers().add(manager);
        hotelRepository.save(hotel);
        return ResponseEntity.ok(new MessageResponse("Manager assigned successfully to hotel " + hotel.getName()));

    }
}
