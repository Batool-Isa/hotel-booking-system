package com.ga.hotelservice.service;

import com.ga.hotelservice.dto.request.AmenityRequest;
import com.ga.hotelservice.dto.request.HotelAmenityRequest;
import com.ga.hotelservice.dto.response.AmenityCatalogResponse;
import com.ga.hotelservice.dto.response.AmenityResponse;
import com.ga.hotelservice.dto.response.MessageResponse;
import com.ga.hotelservice.exception.custom.InformationExistException;
import com.ga.hotelservice.exception.custom.InformationNotFoundException;
import com.ga.hotelservice.exception.custom.InvalidInformationException;
import com.ga.hotelservice.exception.custom.UnauthorizedActionException;
import com.ga.hotelservice.model.Amenity;
import com.ga.hotelservice.model.Hotel;
import com.ga.hotelservice.model.HotelAmenity;
import com.ga.hotelservice.repository.AmenityRepository;
import com.ga.hotelservice.repository.HotelAmenityRepository;
import com.ga.hotelservice.repository.HotelRepository;
import com.ga.hotelservice.security.AuthUser;
import com.ga.hotelservice.security.CurrentUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    private AuditLogService auditLogService;
    @Autowired
    private CurrentUser currentUser;

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

        AuthUser user = currentUser.get();
        auditLogService.log(user.getId(), "AMENITY_CREATED", "AMENITY", amenity.getId(),
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

        AuthUser user = currentUser.get();
        auditLogService.log(user.getId(), "AMENITY_UPDATED", "AMENITY", id,
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

        AuthUser user = currentUser.get();
        auditLogService.log(user.getId(), "AMENITY_DELETED", "AMENITY", id,
                "User " + user.getId() + " deleted amenity " + id);
        return ResponseEntity.ok(new MessageResponse("Amenity deleted successfully"));
    }


    private Hotel findManagedHotel(Long hotelId, AuthUser user) {
        Hotel hotel = hotelRepository.findById(hotelId)
                .orElseThrow(() -> new InformationNotFoundException("Hotel with id " + hotelId + " not found"));
        if (user.isManager() && !hotel.isManagedBy(user.getId())) {
            throw new UnauthorizedActionException("You are not authorized to change amenities of this hotel");
        }
        return hotel;
    }

    public ResponseEntity<?> addAmenityToHotel(Long hotelId, HotelAmenityRequest request) {
        AuthUser user = currentUser.get();
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

        auditLogService.log(user.getId(), "HOTEL_AMENITY_ADDED", "HOTEL", hotelId,
                "User " + user.getId() + " added amenity " + amenity.getId() + " to hotel " + hotelId);
        return ResponseEntity.status(HttpStatus.CREATED).body(toHotelAmenity(link));
    }

    public ResponseEntity<?> updateHotelAmenity(Long hotelId, Long amenityId, HotelAmenityRequest request) {
        AuthUser user = currentUser.get();
        findManagedHotel(hotelId, user);
        HotelAmenity link = hotelAmenityRepository.findByHotelIdAndAmenityId(hotelId, amenityId)
                .orElseThrow(() -> new InformationNotFoundException("This hotel does not have amenity " + amenityId));
        link.setDescription(request.getDescription());
        link.setAdditionalCost(request.getAdditionalCost());
        hotelAmenityRepository.save(link);

        auditLogService.log(user.getId(), "HOTEL_AMENITY_UPDATED", "HOTEL", hotelId,
                "User " + user.getId() + " updated amenity " + amenityId + " of hotel " + hotelId);
        return ResponseEntity.ok(toHotelAmenity(link));
    }

    public ResponseEntity<?> removeAmenityFromHotel(Long hotelId, Long amenityId) {
        AuthUser user = currentUser.get();
        findManagedHotel(hotelId, user);
        HotelAmenity link = hotelAmenityRepository.findByHotelIdAndAmenityId(hotelId, amenityId)
                .orElseThrow(() -> new InformationNotFoundException("This hotel does not have amenity " + amenityId));
        hotelAmenityRepository.delete(link);
        auditLogService.log(user.getId(), "HOTEL_AMENITY_REMOVED", "HOTEL", hotelId,
                "User " + user.getId() + " removed amenity " + amenityId + " from hotel " + hotelId);
        return ResponseEntity.ok(new MessageResponse("Amenity removed from hotel"));
    }
}
