package com.ga.hotel_booking_app.service;

import com.ga.hotel_booking_app.dto.reponse.AmenityCatalogResponse;
import com.ga.hotel_booking_app.dto.reponse.AmenityResponse;
import com.ga.hotel_booking_app.dto.reponse.MessageResponse;
import com.ga.hotel_booking_app.dto.request.AmenityRequest;
import com.ga.hotel_booking_app.dto.request.HotelAmenityRequest;
import com.ga.hotel_booking_app.exception.custom.InformationExistException;
import com.ga.hotel_booking_app.exception.custom.InformationNotFoundException;
import com.ga.hotel_booking_app.exception.custom.InvalidInformationException;
import com.ga.hotel_booking_app.exception.custom.UnauthorizedActionException;
import com.ga.hotel_booking_app.model.*;
import com.ga.hotel_booking_app.repository.AmenityRepository;
import com.ga.hotel_booking_app.repository.HotelAmenityRepository;
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

/**
 * Two things live here:
 *  1. the amenity catalog (Wi-Fi, Pool ...) that every hotel can pick from
 *  2. the link between a hotel and an amenity (with its own description and extra cost)
 */
@Service
public class AmenityService {
    @Autowired
    private AmenityRepository amenityRepository;
    @Autowired
    private HotelAmenityRepository hotelAmenityRepository;
    @Autowired
    private HotelRepository hotelRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private AuditLogService auditLogService;

    private User getCurrentLoggedInUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return userRepository.findUserByEmail(authentication.getName());
    }

    private AmenityCatalogResponse toCatalog(Amenity amenity) {
        AmenityCatalogResponse response = new AmenityCatalogResponse();
        response.setId(amenity.getId());
        response.setName(amenity.getName());
        response.setDescription(amenity.getDescription());
        return response;
    }

    private AmenityResponse toHotelAmenity(HotelAmenity link) {
        AmenityResponse response = new AmenityResponse();
        response.setId(link.getAmenity().getId());
        response.setName(link.getAmenity().getName());
        response.setDescription(link.getDescription());
        response.setAdditionalCost(link.getAdditionalCost());
        return response;
    }

    // ---------- catalog ----------

    public ResponseEntity<?> getAmenities() {
        List<AmenityCatalogResponse> list = new ArrayList<>();
        for (Amenity amenity : amenityRepository.findAll()) {
            list.add(toCatalog(amenity));
        }
        return ResponseEntity.ok(list);
    }

    public ResponseEntity<?> createAmenity(AmenityRequest request) {
        String name = request.getName().trim();
        if (amenityRepository.existsByNameIgnoreCase(name)) {
            throw new InformationExistException("Amenity " + name + " already exists");
        }
        Amenity amenity = new Amenity();
        amenity.setName(name);
        amenity.setDescription(request.getDescription());
        amenityRepository.save(amenity);

        User user = getCurrentLoggedInUser();
        auditLogService.log(user, "AMENITY_CREATED", "AMENITY", amenity.getId(),
                "User " + user.getId() + " created amenity " + name);
        return ResponseEntity.status(HttpStatus.CREATED).body(toCatalog(amenity));
    }

    public ResponseEntity<?> updateAmenity(Long id, AmenityRequest request) {
        Amenity amenity = amenityRepository.findById(id)
                .orElseThrow(() -> new InformationNotFoundException("Amenity with id " + id + " not found"));
        String name = request.getName().trim();
        if (!amenity.getName().equalsIgnoreCase(name) && amenityRepository.existsByNameIgnoreCase(name)) {
            throw new InformationExistException("Amenity " + name + " already exists");
        }
        amenity.setName(name);
        amenity.setDescription(request.getDescription());
        amenityRepository.save(amenity);

        User user = getCurrentLoggedInUser();
        auditLogService.log(user, "AMENITY_UPDATED", "AMENITY", id,
                "User " + user.getId() + " updated amenity " + id);
        return ResponseEntity.ok(toCatalog(amenity));
    }

    public ResponseEntity<?> deleteAmenity(Long id) {
        Amenity amenity = amenityRepository.findById(id)
                .orElseThrow(() -> new InformationNotFoundException("Amenity with id " + id + " not found"));
        if (hotelAmenityRepository.existsByAmenityId(id)) {
            throw new InvalidInformationException("This amenity is used by at least one hotel. Remove it from the hotels first");
        }
        amenityRepository.delete(amenity);

        User user = getCurrentLoggedInUser();
        auditLogService.log(user, "AMENITY_DELETED", "AMENITY", id,
                "User " + user.getId() + " deleted amenity " + id);
        return ResponseEntity.ok(new MessageResponse("Amenity deleted successfully"));
    }

    // ---------- hotel <-> amenity ----------

    private Hotel findManagedHotel(Long hotelId, User user) {
        Hotel hotel = hotelRepository.findById(hotelId)
                .orElseThrow(() -> new InformationNotFoundException("Hotel with id " + hotelId + " not found"));
        if (user.getRole().getName() == Role.RoleName.HOTEL_MANAGER) {
            boolean isThisHotelManager = hotel.getManagers().stream()
                    .anyMatch(m -> m.getId().equals(user.getId()));
            if (!isThisHotelManager) {
                throw new UnauthorizedActionException("You are not authorized to change amenities of this hotel");
            }
        }
        return hotel;
    }

    public ResponseEntity<?> addAmenityToHotel(Long hotelId, HotelAmenityRequest request) {
        User user = getCurrentLoggedInUser();
        Hotel hotel = findManagedHotel(hotelId, user);
        Amenity amenity = amenityRepository.findById(request.getAmenityId())
                .orElseThrow(() -> new InformationNotFoundException("Amenity with id " + request.getAmenityId() + " not found"));
        if (hotelAmenityRepository.findByHotelIdAndAmenityId(hotelId, amenity.getId()).isPresent()) {
            throw new InformationExistException("This hotel already has the amenity " + amenity.getName());
        }
        HotelAmenity link = new HotelAmenity();
        link.setHotel(hotel);
        link.setAmenity(amenity);
        link.setDescription(request.getDescription());
        link.setAdditionalCost(request.getAdditionalCost());
        hotelAmenityRepository.save(link);

        auditLogService.log(user, "HOTEL_AMENITY_ADDED", "HOTEL", hotelId,
                "User " + user.getId() + " added amenity " + amenity.getId() + " to hotel " + hotelId);
        return ResponseEntity.status(HttpStatus.CREATED).body(toHotelAmenity(link));
    }

    public ResponseEntity<?> updateHotelAmenity(Long hotelId, Long amenityId, HotelAmenityRequest request) {
        User user = getCurrentLoggedInUser();
        findManagedHotel(hotelId, user);
        HotelAmenity link = hotelAmenityRepository.findByHotelIdAndAmenityId(hotelId, amenityId)
                .orElseThrow(() -> new InformationNotFoundException("This hotel does not have amenity " + amenityId));
        link.setDescription(request.getDescription());
        link.setAdditionalCost(request.getAdditionalCost());
        hotelAmenityRepository.save(link);

        auditLogService.log(user, "HOTEL_AMENITY_UPDATED", "HOTEL", hotelId,
                "User " + user.getId() + " updated amenity " + amenityId + " of hotel " + hotelId);
        return ResponseEntity.ok(toHotelAmenity(link));
    }

    public ResponseEntity<?> removeAmenityFromHotel(Long hotelId, Long amenityId) {
        User user = getCurrentLoggedInUser();
        findManagedHotel(hotelId, user);
        HotelAmenity link = hotelAmenityRepository.findByHotelIdAndAmenityId(hotelId, amenityId)
                .orElseThrow(() -> new InformationNotFoundException("This hotel does not have amenity " + amenityId));
        hotelAmenityRepository.delete(link);

        auditLogService.log(user, "HOTEL_AMENITY_REMOVED", "HOTEL", hotelId,
                "User " + user.getId() + " removed amenity " + amenityId + " from hotel " + hotelId);
        return ResponseEntity.ok(new MessageResponse("Amenity removed from hotel"));
    }
}
