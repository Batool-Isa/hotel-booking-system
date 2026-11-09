package com.ga.hotelservice.service;

import com.ga.hotelservice.dto.request.CreateChildPolicyRequest;
import com.ga.hotelservice.dto.response.ChildPolicyResponse;
import com.ga.hotelservice.dto.response.MessageResponse;
import com.ga.hotelservice.exception.custom.InformationExistException;
import com.ga.hotelservice.exception.custom.InformationNotFoundException;
import com.ga.hotelservice.exception.custom.InvalidInformationException;
import com.ga.hotelservice.exception.custom.UnauthorizedActionException;
import com.ga.hotelservice.model.ChildPolicy;
import com.ga.hotelservice.model.Hotel;
import com.ga.hotelservice.repository.ChildPolicyRepository;
import com.ga.hotelservice.repository.HotelRepository;
import com.ga.hotelservice.security.AuthUser;
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

    // the logged-in user, built from the JWT by JwtRequestFilter (no database lookup)
    private AuthUser getCurrentLoggedInUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return principal instanceof AuthUser user ? user : null;
    }

    public ResponseEntity<?> createChildPolicy(
            Long hotelId,
            CreateChildPolicyRequest request) {

        Hotel hotel = hotelRepository.findById(hotelId)
                .orElseThrow(() -> new InformationNotFoundException("Hotel with id " + hotelId + " not found"));

        AuthUser user = getCurrentLoggedInUser();

        // hotel manager can only manage their own hotel
        if (user.isManager()) {
            checkManagerAuthority(user, hotel);
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
        childPolicy.setChildMaxAge(request.getChildMaxAge());
        childPolicy.setHotel(hotel);

        childPolicyRepository.save(childPolicy);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new MessageResponse("Child policy created successfully"));
    }

    public ResponseEntity<?> getChildPolicy(Long hotelId) {
        hotelRepository.findById(hotelId)
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


    public void checkManagerAuthority(AuthUser user,Hotel hotel){
        if (!hotel.isManagedBy(user.getId())) {
            throw new UnauthorizedActionException(
                    "You are not authorized to manage this hotel's child policy");
        }
    }
    public ResponseEntity<?> updateChildPolicy(
            Long hotelId,
            CreateChildPolicyRequest request) {

        Hotel hotel = hotelRepository.findById(hotelId)
                .orElseThrow(() -> new InformationNotFoundException("Hotel with id " + hotelId + " not found"));

        AuthUser user = getCurrentLoggedInUser();

        // hotel manager can only update their own hotel's policy
        if (user.isManager()) {
            checkManagerAuthority(user, hotel);
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
