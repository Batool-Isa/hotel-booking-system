package com.ga.hotel_booking_app.service;

import com.ga.hotel_booking_app.dto.reponse.MessageResponse;
import com.ga.hotel_booking_app.dto.request.CreateHotelRequest;
import com.ga.hotel_booking_app.exception.custom.InformationExistException;
import com.ga.hotel_booking_app.exception.custom.InformationNotFoundException;
import com.ga.hotel_booking_app.exception.custom.InvalidInformationException;
import com.ga.hotel_booking_app.model.Hotel;
import com.ga.hotel_booking_app.model.Role;
import com.ga.hotel_booking_app.model.User;
import com.ga.hotel_booking_app.repository.HotelRepository;
import com.ga.hotel_booking_app.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.autoconfigure.WebMvcProperties;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Optional;

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
        return ResponseEntity.ok(new MessageResponse("Hotel added successfully"));
    }
}
