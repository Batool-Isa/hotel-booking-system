package com.ga.hotelservice.service;

import com.ga.hotelservice.client.UserServiceClient;
import com.ga.hotelservice.client.UserSummary;
import com.ga.hotelservice.model.HotelAmenity;
import com.ga.hotelservice.security.AuthUser;
import com.ga.hotelservice.dto.request.CreateHotelRequest;
import com.ga.hotelservice.dto.request.UpdateHotelRequest;
import com.ga.hotelservice.dto.request.UpdateHotelStatusRequest;
import com.ga.hotelservice.dto.response.AmenityResponse;
import com.ga.hotelservice.dto.response.HotelImageResponse;
import com.ga.hotelservice.dto.response.HotelResponse;
import com.ga.hotelservice.dto.response.MessageResponse;
import com.ga.hotelservice.exception.custom.InformationExistException;
import com.ga.hotelservice.exception.custom.InformationNotFoundException;
import com.ga.hotelservice.exception.custom.InvalidInformationException;
import com.ga.hotelservice.exception.custom.UnauthorizedActionException;
import com.ga.hotelservice.model.Amenity;
import com.ga.hotelservice.model.Hotel;
import com.ga.hotelservice.model.HotelImage;
import com.ga.hotelservice.repository.HotelRepository;
import com.ga.hotelservice.security.CurrentUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class HotelService {
    @Autowired
    private HotelRepository hotelRepository;

    @Autowired
    private CurrentUser currentUser;

    @Autowired
    private UserServiceClient userServiceClient;

    public ResponseEntity<?> createHotel(CreateHotelRequest request) {
        if (hotelRepository.existsByName(request.getName())) {
            throw new InformationExistException("Hotel " + request.getName() + " already exists");
        }

        AuthUser user = (currentUser.get());
        Hotel hotel = new Hotel();
        hotel.setName(request.getName());
        hotel.setAddress(request.getAddress());
        hotel.setCity(request.getCity());
        hotel.setCountry(request.getCountry());
        hotel.setLatitude(request.getLatitude());
        hotel.setLongitude(request.getLongitude());
        hotel.setDescription(request.getDescription());
        hotel.setPhone(request.getPhone());

        if (user.isManager()) {
            hotel.getManagerIds().add(user.getId());
            hotel.setStatus(Hotel.Status.PENDING_APPROVAL);
            hotelRepository.save(hotel);
            return ResponseEntity.status(HttpStatus.CREATED).body(new MessageResponse("Hotel added successfully, Please wait for admin approval"));

        } else {
            if (request.getManagerId() == null) {
                throw new InvalidInformationException("Manager ID is required when an admin creates a hotel");
            }
            requireHotelManager(request.getManagerId());
            hotel.setStatus(Hotel.Status.ACTIVE);
            hotel.getManagerIds().add(request.getManagerId());
            hotelRepository.save(hotel);
            return ResponseEntity.status(HttpStatus.CREATED).body(new MessageResponse("Hotel added successfully"));

        }
    }

    public ResponseEntity<?> getHotel(Long id) {
        Hotel hotel = hotelRepository.findById(id)
                .orElseThrow(() -> new InformationNotFoundException("Hotel with id " + id + " not found"));
        // show only active hotels to customer while all hotels for admin and manger is he assigned to it
        if (!hotel.getStatus().equals(Hotel.Status.ACTIVE) && !canViewNonActiveHotels(hotel)) {
            throw new InformationNotFoundException("Hotel with id " + id + " not found");
        }
        return ResponseEntity.ok(buildHotelResponse(hotel));

    }

    public boolean canViewNonActiveHotels(Hotel hotel) {
        AuthUser user = currentUser.get();
        if (user == null) {
            return false;
        }
        if (user.isAdmin()) {
            return true;
        }
        // a manager only sees the hotels they are assigned to
        return user.isManager() && hotel.isManagedBy(user.getId());
    }

    public HotelResponse buildHotelResponse(Hotel hotel) {
        HotelResponse response = new HotelResponse();
        response.setId(hotel.getId());
        response.setName(hotel.getName());
        response.setDescription(hotel.getDescription());
        response.setAddress(hotel.getAddress());
        response.setCity(hotel.getCity());
        response.setCountry(hotel.getCountry());
        response.setLatitude(hotel.getLatitude());
        response.setLongitude(hotel.getLongitude());
        response.setPhone(hotel.getPhone());
        response.setStatus(hotel.getStatus());
        response.setUpdatedAt(hotel.getUpdatedAt());
        response.setCreatedAt(hotel.getCreatedAt());
        //response.getReviewCount(hotel.getreview);
        // response.getAverageRating(hotel.get)
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
                                       String country, String city,
                                       Pageable pageable) {
        Page<Hotel> hotels = hotelRepository.findFilteredHotels(
                search, name, city, country, pageable
        );
        List<HotelResponse> responses = new ArrayList<>();


        for (Hotel hotel : hotels) {
            HotelResponse hotelResponse = buildHotelResponse(hotel);
            responses.add(hotelResponse);
        }
        return ResponseEntity.ok(responses);


    }

    public ResponseEntity<?> getAllHotels(String search, String name,
                                          String country, String city,
                                          Pageable pageable) {
        Page<Hotel> hotels = hotelRepository.getAllHotels(
                search, name, city, country, pageable
        );
        List<HotelResponse> responses = new ArrayList<>();


        for (Hotel hotel : hotels) {
            HotelResponse hotelResponse = buildHotelResponse(hotel);
            responses.add(hotelResponse);
        }
        return ResponseEntity.ok(responses);


    }

    public ResponseEntity<?> getPendingHotels(Pageable pageable) {
        Page<Hotel> hotels = hotelRepository.findByStatus(Hotel.Status.PENDING_APPROVAL, pageable);
        List<HotelResponse> responses = new ArrayList<>();
        for (Hotel hotel : hotels) {
            HotelResponse hotelResponse = buildHotelResponse(hotel);
            responses.add(hotelResponse);
        }
        return ResponseEntity.ok(responses);
    }

    public ResponseEntity<?> getMangerHotels(Pageable pageable) {
        AuthUser manager = currentUser.get();
        Page<Hotel> hotels = hotelRepository.findByManagerIdsContains(manager.getId(), pageable);
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

        AuthUser user = (currentUser.get());
        log.info("User Logged in  ->{}", user.getId());

        if (user.isManager()) {
            boolean isMangerHotel = hotel.isManagedBy(user.getId());
            if (!isMangerHotel) {
                throw new UnauthorizedActionException(
                        "You are not authorized to update this hotel"
                );
            }
        } else if (user.isAdmin()) {
            if (request.getManagerId() != null) {
                requireHotelManager(request.getManagerId());
                hotel.getManagerIds().add(request.getManagerId());

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

    private void requireHotelManager(Long userId) {
        UserSummary user = userServiceClient.findUser(userId, currentUser.token())
                .orElseThrow(() -> new InformationNotFoundException("User " + userId + " not found"));
        if (!"HOTEL_MANAGER".equals(user.getRole())) {
            throw new InvalidInformationException("User " + userId + " is not a hotel manager");
        }
    }

    public ResponseEntity<?> assignManager(Long hotelId, Long managerId) {
        Hotel hotel = hotelRepository.findById(hotelId)
                .orElseThrow(() -> new InformationNotFoundException("Hotel with id " + hotelId + " not found"));
        if (hotel.isManagedBy(managerId)) {
            throw new InvalidInformationException("This manager is already assigned to this hotel");
        }
        requireHotelManager(managerId);
        hotel.getManagerIds().add(managerId);
        hotelRepository.save(hotel);
        return ResponseEntity.ok(new MessageResponse("Manager assigned successfully to hotel " + hotel.getName()));

    }
}
