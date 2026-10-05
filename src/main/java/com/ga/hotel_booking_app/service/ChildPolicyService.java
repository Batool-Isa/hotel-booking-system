package com.ga.hotel_booking_app.service;

import com.ga.hotel_booking_app.dto.reponse.ChildPolicyResponse;
import com.ga.hotel_booking_app.dto.reponse.MessageResponse;
import com.ga.hotel_booking_app.dto.request.CreateChildPolicyRequest;
import com.ga.hotel_booking_app.exception.custom.InformationExistException;
import com.ga.hotel_booking_app.exception.custom.InformationNotFoundException;
import com.ga.hotel_booking_app.exception.custom.InvalidInformationException;
import com.ga.hotel_booking_app.exception.custom.UnauthorizedActionException;
import com.ga.hotel_booking_app.model.ChildPolicy;
import com.ga.hotel_booking_app.model.Hotel;
import com.ga.hotel_booking_app.model.Role;
import com.ga.hotel_booking_app.model.User;
import com.ga.hotel_booking_app.repository.ChildPolicyRepository;
import com.ga.hotel_booking_app.repository.HotelRepository;
import com.ga.hotel_booking_app.repository.UserRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class ChildPolicyService {
    @Autowired
    private HotelRepository hotelRepository;
    @Autowired
    private ChildPolicyRepository childPolicyRepository;
    @Autowired
    private UserRepository userRepository;

    public User getCurrentLoggedInUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        return userRepository.findUserByEmail(email);
    }

    public ResponseEntity<?> createChildPolicy(
            Long hotelId,
            CreateChildPolicyRequest request) {

        Hotel hotel = hotelRepository.findById(hotelId)
                .orElseThrow(() -> new InformationNotFoundException("Hotel with id " + hotelId + " not found"));

        User user = getCurrentLoggedInUser();

        // hotel manager can only manage their own hotel
        if (user.getRole().getName().equals(Role.RoleName.HOTEL_MANAGER)) {
            boolean isHotelManager = hotel.getManagers().stream()
                    .anyMatch(manager ->
                            manager.getId().equals(user.getId()));
            if (!isHotelManager) {
                throw new UnauthorizedActionException("You are not authorized to manage this hotel's child policy");
            }
        }

        // 1 policy only for each hotel
        if (childPolicyRepository.existsByHotelId(hotelId)) {
            throw new InformationExistException("This hotel already has a child policy");
        }

        // validate age ranges
        if (request.getInfantMaxAge() >= request.getChildMaxAge()) {
            throw new InvalidInformationException("Infant maximum age must be less than child maximum age");
        }

        ChildPolicy childPolicy = new ChildPolicy();

        childPolicy.setInfantMaxAge(request.getInfantMaxAge());
        childPolicy.setChildMaxAge(request.getChildMaxAge());
        childPolicy.setHotel(hotel);

        childPolicyRepository.save(childPolicy);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new MessageResponse("Child policy created successfully"));
    }

    public ResponseEntity<?> getChildPolicy(Long hotelId) {
        Hotel hotel = hotelRepository.findById(hotelId)
                .orElseThrow(() -> new InformationNotFoundException("Hotel with id " + hotelId + " not found"));

        ChildPolicy childPolicy = childPolicyRepository
                .findByHotelId(hotelId)
                .orElseThrow(() -> new InformationNotFoundException("Child policy for hotel with id "
                        + hotelId + " not found"));

        ChildPolicyResponse response = new ChildPolicyResponse();
        response.setUpdatedAt(childPolicy.getUpdatedAt());
        response.setInfantMaxAge(childPolicy.getInfantMaxAge());
        response.setChildMaxAge(childPolicy.getChildMaxAge());

        return ResponseEntity.ok(response);
    }


    public ResponseEntity<?> updateChildPolicy(
            Long hotelId,
            CreateChildPolicyRequest request) {

        Hotel hotel = hotelRepository.findById(hotelId)
                .orElseThrow(() -> new InformationNotFoundException("Hotel with id " + hotelId + " not found"));

        User user = getCurrentLoggedInUser();

        // hotel manager can only update their own hotel's policy
        if (user.getRole().getName().equals(Role.RoleName.HOTEL_MANAGER)) {
            boolean isHotelManager = hotel.getManagers().stream()
                    .anyMatch(manager ->
                            manager.getId().equals(user.getId()));
            if (!isHotelManager) {
                throw new UnauthorizedActionException(
                        "You are not authorized to manage this hotel's child policy");
            }
        }
        ChildPolicy childPolicy = childPolicyRepository
                .findByHotelId(hotelId)
                .orElseThrow(() -> new InformationNotFoundException("Child policy for hotel with id " + hotelId + " not found"));
        if (request.getInfantMaxAge() >= request.getChildMaxAge()) {
            throw new InvalidInformationException("Infant maximum age must be less than child maximum age");
        }
        childPolicy.setInfantMaxAge(request.getInfantMaxAge());
        childPolicy.setChildMaxAge(request.getChildMaxAge());
        childPolicyRepository.save(childPolicy);

        return ResponseEntity.ok(new MessageResponse("Child policy updated successfully"));
    }
}
