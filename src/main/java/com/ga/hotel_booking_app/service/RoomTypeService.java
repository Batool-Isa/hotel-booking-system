package com.ga.hotel_booking_app.service;

import com.ga.hotel_booking_app.dto.reponse.MessageResponse;
import com.ga.hotel_booking_app.dto.reponse.RoomTypeResponse;
import com.ga.hotel_booking_app.dto.request.RoomTypeRequest;
import com.ga.hotel_booking_app.dto.request.UpdateRoomTypeStatusRequest;
import com.ga.hotel_booking_app.exception.custom.InformationExistException;
import com.ga.hotel_booking_app.exception.custom.InformationNotFoundException;
import com.ga.hotel_booking_app.model.RoomType;
import com.ga.hotel_booking_app.repository.RoomTypeRepository;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class RoomTypeService {
    @Autowired
    private RoomTypeRepository roomTypeRepository;
    @Autowired
    private AuditLogService auditLogService;
    private static final Logger logger =
            LoggerFactory.getLogger(UserService.class);

    public ResponseEntity<?> getRoomTypes() {
        //List<RoomType> list = roomTypeRepository.findAll();
        List<RoomType> list = roomTypeRepository.findByStatus(RoomType.Status.ACTIVE);

        List<RoomTypeResponse> responses = new ArrayList<>();
        for (RoomType type : list) {
            RoomTypeResponse response = new RoomTypeResponse();
            response.setId(type.getId());
            response.setName(type.getName());
            response.setDescription(type.getDescription());
            responses.add(response);
        }

        return ResponseEntity.ok(responses);

    }

    public ResponseEntity<?> getRoomType(Long id) {
        RoomType type = roomTypeRepository.findByIdAndStatus(id, RoomType.Status.ACTIVE)
                .orElseThrow(() -> new InformationNotFoundException("Room type with id " + id + " not found"));
        RoomTypeResponse response = new RoomTypeResponse();
        response.setId(type.getId());
        response.setName(type.getName());
        response.setDescription(type.getDescription());
        return ResponseEntity.ok(response);
    }

    public ResponseEntity<?> createRoomType(RoomTypeRequest request) {
        if (roomTypeRepository.existsByName(request.getName())) {
            throw new InformationExistException("Room Type with name " + request.getName() + " already exists.");
        }
        RoomType type = new RoomType();
        type.setName(request.getName());
        type.setDescription(request.getDescription());
        roomTypeRepository.save(type);
        return ResponseEntity.status(HttpStatus.CREATED).body(new MessageResponse("Room type added successfully"));

    }


    public ResponseEntity<?> updateRoomType(
            Long id,
            RoomTypeRequest request) {
        RoomType type = roomTypeRepository.findById(id)
                .orElseThrow(() -> new InformationNotFoundException("Room type with id " + id + " not found"));

        if (!type.getName().equalsIgnoreCase(request.getName())
                && roomTypeRepository.existsByName(request.getName())) {
            throw new InformationExistException("Room type with name " + request.getName() + " already exists.");
        }

        type.setName(request.getName());
        type.setDescription(request.getDescription());
        roomTypeRepository.save(type);
        return ResponseEntity.ok(new MessageResponse("Room type updated successfully"));
    }

    public ResponseEntity<?> updateRoomTypeStatus(Long id, @Valid UpdateRoomTypeStatusRequest request) {
        RoomType type = roomTypeRepository.findById(id)
                .orElseThrow(() -> new InformationNotFoundException("Room type with id " + id + " not found"));
        type.setStatus(request.getStatus());
        roomTypeRepository.save(type);
        return ResponseEntity.ok(new MessageResponse("Room type status updated successfully"));


    }
}
