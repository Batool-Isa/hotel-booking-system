package com.ga.hotel_booking_app.service;

import com.ga.hotel_booking_app.dto.reponse.AmenityResponse;
import com.ga.hotel_booking_app.dto.reponse.HotelImageResponse;
import com.ga.hotel_booking_app.dto.reponse.HotelResponse;
import com.ga.hotel_booking_app.dto.reponse.MessageResponse;
import com.ga.hotel_booking_app.dto.request.CreateHotelRequest;
import com.ga.hotel_booking_app.exception.custom.InformationExistException;
import com.ga.hotel_booking_app.exception.custom.InformationNotFoundException;
import com.ga.hotel_booking_app.exception.custom.InvalidInformationException;
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
        System.out.println("useeeeeeeeeeer ->" + user.getId());
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
            hotel.getManagers().add(manager);
        }
        hotelRepository.save(hotel);
        return ResponseEntity.status(HttpStatus.CREATED).body(new MessageResponse("Hotel added successfully"));
    }

    public ResponseEntity<?> getHotel(Long id) {
        Hotel hotel = hotelRepository.findById(id)
                .orElseThrow(() -> new InformationNotFoundException("Hotel with id "+id+" not found"));

        return ResponseEntity.ok(buildHotelResponse(hotel));

    }

    public HotelResponse buildHotelResponse(Hotel hotel){
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

        for(HotelAmenity hotelAmenity: hotel.getAmenities()){
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
        for(HotelImage image: hotel.getImages()){
            HotelImageResponse hotelImageResponse= new HotelImageResponse();
            hotelImageResponse.setId(image.getId());
            hotelImageResponse.setImageUrl(image.getImageURL());
            images.add(hotelImageResponse);
        }
        response.setImages(images);
    return  response;
    }
    public ResponseEntity<?> getHotels() {
        List<Hotel> hotels = hotelRepository.findAll();
        List<HotelResponse> responses = new ArrayList<>();
        for (Hotel hotel:hotels){
            HotelResponse hotelResponse= buildHotelResponse(hotel);
            responses.add(hotelResponse);
        }
        return ResponseEntity.ok(responses);
    }
}
