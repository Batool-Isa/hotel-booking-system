package com.ga.hotel_booking_app.service;

import com.ga.hotel_booking_app.dto.reponse.MessageResponse;
import com.ga.hotel_booking_app.dto.reponse.RoomResponse;
import com.ga.hotel_booking_app.dto.request.BulkCreateRoomsRequest;
import com.ga.hotel_booking_app.dto.request.CreateRoomRequest;
import com.ga.hotel_booking_app.dto.request.UpdateRoomStatusRequest;
import com.ga.hotel_booking_app.exception.custom.InformationExistException;
import com.ga.hotel_booking_app.exception.custom.InformationNotFoundException;
import com.ga.hotel_booking_app.exception.custom.InvalidInformationException;
import com.ga.hotel_booking_app.exception.custom.UnauthorizedActionException;
import com.ga.hotel_booking_app.model.*;
import com.ga.hotel_booking_app.repository.*;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class RoomService {
    @Autowired
    public HotelRepository hotelRepository;
    @Autowired
    public RoomRepository roomRepository;
    @Autowired
    public RoomTypeRepository roomTypeRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private BookingRepository bookingRepository;
    @Autowired
    private AuditLogService auditLogService;

    public User getCurrentLoggedInUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        return userRepository.findUserByEmail(email);
    }

    public ResponseEntity<?> createRoom(Long id, @Valid CreateRoomRequest request) {

        Hotel hotel = findHotel(id);
        // check if hotel is active
        if (!hotel.getStatus().equals(Hotel.Status.ACTIVE)) {
            throw new InvalidInformationException("You can't add room to a hotel that is not active");
        }
        User user = getCurrentLoggedInUser();
        if (user.getRole().getName().equals(Role.RoleName.HOTEL_MANAGER)) {
            // check if manager is assigned to this hotel
            boolean isThisHotelManager = hotel.getManagers().stream()
                    .anyMatch(m -> m.getId().equals(user.getId()));
            if (!isThisHotelManager) {
                throw new UnauthorizedActionException("You are not authorized to add room to this hotel");
            }
        }
        Room room = new Room();
        RoomType type = roomTypeRepository.findById(request.getRoomTypeId())
                .orElseThrow(() -> new InformationNotFoundException("Room Type with id " + request.getRoomTypeId() + " not found"));
        if (!type.getStatus().equals(RoomType.Status.ACTIVE)) {
            throw new InvalidInformationException("You can't use room type that is not active, please try again with active room type");
        }
        fillRoom(room, request, findActiveRoomType(request.getRoomTypeId()));
        room.setStatus(Room.Status.ACTIVE);
        room.setHotel(hotel);
        roomRepository.save(room);
        auditLogService.log(user, "ROOM_CREATED", "ROOM", room.getId(),
                "User " + user.getId() + " added room " + room.getRoomNumber() + " to hotel " + hotel.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(new MessageResponse("Room added successfully"));
    }

    private Hotel findHotel(Long id) {
        return hotelRepository.findById(id)
                .orElseThrow(() -> new InformationNotFoundException("Hotel with id " + id + " not found"));
    }

    private Room findRoomOfHotel(Long hotelId, Long roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new InformationNotFoundException("Room with id " + roomId + " not found"));
        if (!room.getHotel().getId().equals(hotelId)) {
            throw new InformationNotFoundException("Room with id " + roomId + " not found in hotel " + hotelId);
        }
        return room;
    }

    private void checkCanManage(Hotel hotel, User user, String message) {
        if (user.getRole().getName().equals(Role.RoleName.HOTEL_MANAGER)) {
            boolean isThisHotelManager = hotel.getManagers().stream()
                    .anyMatch(m -> m.getId().equals(user.getId()));
            if (!isThisHotelManager) {
                throw new UnauthorizedActionException(message);
            }
        }
    }

    private RoomType findActiveRoomType(Long roomTypeId) {
        RoomType type = roomTypeRepository.findById(roomTypeId)
                .orElseThrow(() -> new InformationNotFoundException("Room Type with id " + roomTypeId + " not found"));
        if (!type.getStatus().equals(RoomType.Status.ACTIVE)) {
            throw new InvalidInformationException("You can't use room type that is not active, please try again with active room type");
        }
        return type;
    }

    private void fillRoom(Room room, CreateRoomRequest request, RoomType type) {
        room.setRoomNumber(request.getRoomNumber());
        room.setFloorNumber(request.getFloorNumber());
        room.setMaxAdults(request.getMaxAdults());
        room.setMaxChildren(request.getMaxChildren());
        room.setMaxOccupancy(request.getMaxOccupancy());
        room.setPricePerNight(request.getPricePerNight());
        room.setRoomType(type);
    }
    public ResponseEntity<?> getRoom(Long hotelId, Long roomId) {
        Hotel hotel = findHotel(hotelId);
        User user = getCurrentLoggedInUser();
        checkCanManage(hotel, user, "You can't view rooms of a hotel you don't manage");
        return ResponseEntity.ok(toResponse(findRoomOfHotel(hotelId, roomId)));
    }

    private RoomResponse toResponse(Room room) {
        RoomResponse response = new RoomResponse();
        response.setId(room.getId());
        response.setRoomNumber(room.getRoomNumber());
        response.setFloorNumber(room.getFloorNumber());
        response.setRoomTypeId(room.getRoomType().getId());
        response.setRoomTypeName(room.getRoomType().getName());
        response.setMaxAdults(room.getMaxAdults());
        response.setMaxChildren(room.getMaxChildren());
        response.setMaxOccupancy(room.getMaxOccupancy());
        response.setPricePerNight(room.getPricePerNight());
        response.setStatus(room.getStatus().name());
        response.setHotelId(room.getHotel().getId());
        return response;
    }

    public ResponseEntity<?> getHotelRooms(Long id, Pageable pageable) {
        Hotel hotel = hotelRepository.findById(id)
                .orElseThrow(() -> new InformationNotFoundException("Hotel with id " + id + " not found"));
        // check if hotel is active
        if (!hotel.getStatus().equals(Hotel.Status.ACTIVE)) {
            throw new InvalidInformationException("You can't view rooms of a hotel that is not active");
        }
        Page<Room> roomsList;
        User user = getCurrentLoggedInUser();
        if (user.getRole().getName().equals(Role.RoleName.HOTEL_MANAGER)) {
            // check if manager is assigned to this hotel
            boolean isThisHotelManager = hotel.getManagers().stream()
                    .anyMatch(m -> m.getId().equals(user.getId()));
            if (!isThisHotelManager) { // if manger not assigned should not view rooms
                throw new UnauthorizedActionException("You can't view rooms of a hotel that is not active");
            }
            roomsList = roomRepository.findByHotelId(hotel.getId(), pageable);
        } else if (user.getRole().getName().equals(Role.RoleName.CUSTOMER)) { // customer can view only active room
            roomsList = roomRepository.findByHotelIdAndStatus(hotel.getId(), Room.Status.ACTIVE, pageable);
        } else { // admin
            roomsList = roomRepository.findByHotelId(id, pageable);
        }
        List<RoomResponse> responseList = new ArrayList<>();

        for (Room room : roomsList) {
            RoomResponse response =toResponse(room);
            responseList.add(response);
        }
        return ResponseEntity.ok(responseList);
    }
    public ResponseEntity<?> updateRoom(Long hotelId, Long roomId, CreateRoomRequest request) {
        Hotel hotel = findHotel(hotelId);
        User user = getCurrentLoggedInUser();
        checkCanManage(hotel, user, "You are not authorized to update rooms of this hotel");
        Room room = findRoomOfHotel(hotelId, roomId);

        if (!room.getRoomNumber().equals(request.getRoomNumber())
                && roomRepository.existsByHotelIdAndRoomNumber(hotelId, request.getRoomNumber())) {
            throw new InformationExistException("Room number " + request.getRoomNumber() + " already exists in this hotel");
        }
        fillRoom(room, request, findActiveRoomType(request.getRoomTypeId()));
        roomRepository.save(room);

        auditLogService.log(user, "ROOM_UPDATED", "ROOM", room.getId(),
                "User " + user.getId() + " updated room " + room.getId() + " of hotel " + hotelId);
        return ResponseEntity.ok(toResponse(room));
    }
    @Transactional
    public ResponseEntity<?> createRoomsBulk(Long id, BulkCreateRoomsRequest request) {
        Hotel hotel = findHotel(id);
        if (!hotel.getStatus().equals(Hotel.Status.ACTIVE)) {
            throw new InvalidInformationException("You can't add room to a hotel that is not active");
        }
        User user = getCurrentLoggedInUser();
        checkCanManage(hotel, user, "You are not authorized to add room to this hotel");

        Set<String> seenNumbers = new HashSet<>();
        List<Room> toSave = new ArrayList<>();
        for (CreateRoomRequest item : request.getRooms()) {
            if (!seenNumbers.add(item.getRoomNumber())) {
                throw new InvalidInformationException("Room number " + item.getRoomNumber() + " is repeated in your list");
            }
            if (roomRepository.existsByHotelIdAndRoomNumber(hotel.getId(), item.getRoomNumber())) {
                throw new InformationExistException("Room number " + item.getRoomNumber() + " already exists in this hotel");
            }
            Room room = new Room();
            fillRoom(room, item, findActiveRoomType(item.getRoomTypeId()));
            room.setStatus(Room.Status.ACTIVE);
            room.setHotel(hotel);
            toSave.add(room);
        }
        roomRepository.saveAll(toSave);
        auditLogService.log(user, "ROOMS_BULK_CREATED", "HOTEL", hotel.getId(),
                "User " + user.getId() + " added " + toSave.size() + " rooms to hotel " + hotel.getId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new MessageResponse(toSave.size() + " rooms added successfully"));
    }
    public ResponseEntity<?> updateRoomStatus(Long hotelId, Long roomId, UpdateRoomStatusRequest request) {
        Hotel hotel = findHotel(hotelId);
        User user = getCurrentLoggedInUser();
        checkCanManage(hotel, user, "You are not authorized to update rooms of this hotel");
        Room room = findRoomOfHotel(hotelId, roomId);

        if (room.getStatus() == request.getStatus()) {
            throw new InvalidInformationException("Room is already " + request.getStatus());
        }
        if (request.getStatus() != Room.Status.ACTIVE
                && bookingRepository.existsUpcomingBookingForRoom(roomId, LocalDate.now())) {
            throw new InvalidInformationException(
                    "This room has upcoming bookings. Cancel or finish them before changing its status");
        }
        room.setStatus(request.getStatus());
        roomRepository.save(room);

        auditLogService.log(user, "ROOM_" + request.getStatus(), "ROOM", room.getId(),
                "User " + user.getId() + " set room " + room.getId() + " to " + request.getStatus());
        return ResponseEntity.ok(toResponse(room));
    }
}
