package com.ga.hotel_booking_app.service;

import com.ga.hotel_booking_app.dto.reponse.MessageResponse;
import com.ga.hotel_booking_app.dto.reponse.RoomResponse;
import com.ga.hotel_booking_app.dto.request.CreateRoomRequest;
import com.ga.hotel_booking_app.exception.custom.InformationNotFoundException;
import com.ga.hotel_booking_app.exception.custom.InvalidInformationException;
import com.ga.hotel_booking_app.exception.custom.UnauthorizedActionException;
import com.ga.hotel_booking_app.model.*;
import com.ga.hotel_booking_app.repository.HotelRepository;
import com.ga.hotel_booking_app.repository.RoomRepository;
import com.ga.hotel_booking_app.repository.RoomTypeRepository;
import com.ga.hotel_booking_app.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

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

    public User getCurrentLoggedInUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        return userRepository.findUserByEmail(email);
    }

    public ResponseEntity<?> createRoom(Long id, @Valid CreateRoomRequest request) {

        Hotel hotel = hotelRepository.findById(id)
                .orElseThrow(() -> new InformationNotFoundException("Hotel with id " + id + " not found"));
       // check if hotel is active
        if (!hotel.getStatus().equals(Hotel.Status.ACTIVE)) {
            throw new InvalidInformationException("You can't add room to a hotel that is not active");
        }
        User user = getCurrentLoggedInUser();
        if(user.getRole().equals(Role.RoleName.HOTEL_MANAGER)){
            // check if manager is assigned to this hotel
            boolean isThisHotelManager = hotel.getManagers().stream()
                    .anyMatch(m -> m.getId().equals(user.getId()));
            if(!isThisHotelManager){
                throw new UnauthorizedActionException("You are not authorized to add room to this hotel");
            }
        }
        Room room = new Room();
        RoomType type = roomTypeRepository.findById(request.getRoomTypeId())
                .orElseThrow(() -> new InformationNotFoundException("Room Type with id " + request.getRoomTypeId() + " not found"));
        if (!type.getStatus().equals(RoomType.Status.ACTIVE)) {
            throw new InvalidInformationException("You can't use room type that is not active, please try again with active room type");
        }
        room.setRoomNumber(request.getRoomNumber());
        room.setFloorNumber(request.getFloorNumber());
        room.setMaxAdults(request.getMaxAdults());
        room.setMaxChildren(request.getMaxChildren());
        room.setPricePerNight(request.getPricePerNight());
        room.setMaxAdults(request.getMaxAdults());
        room.setMaxChildren(request.getMaxChildren());
        room.setMaxOccupancy(request.getMaxOccupancy());

        room.setStatus(Room.Status.ACTIVE);
        room.setHotel(hotel);
        room.setRoomType(type);
        roomRepository.save(room);
        return ResponseEntity.status(HttpStatus.CREATED).body(new MessageResponse("Room added successfully"));
    }

    public ResponseEntity<?> getHotelRooms(Long id) {
        Hotel hotel = hotelRepository.findById(id)
                .orElseThrow(() -> new InformationNotFoundException("Hotel with id " + id + " not found"));
        // check if hotel is active
        if (!hotel.getStatus().equals(Hotel.Status.ACTIVE)) {
            throw new InvalidInformationException("You can't view rooms of a hotel that is not active");
        }
        List<Room> roomsList = roomRepository.findByHotelId(id);
        User user = getCurrentLoggedInUser();
        if(user.getRole().equals(Role.RoleName.HOTEL_MANAGER)){
            // check if manager is assigned to this hotel
            boolean isThisHotelManager = hotel.getManagers().stream()
                    .anyMatch(m -> m.getId().equals(user.getId()));
            if(!isThisHotelManager){ // if manger not assigned should not view rooms
                throw new UnauthorizedActionException( "You can't view rooms of a hotel that is not active");
            }
        }else if(user.getRole().equals(Role.RoleName.CUSTOMER)){ // customer can view only active room
            roomsList = roomsList.stream().filter(r -> r.getStatus().equals(Room.Status.ACTIVE)).toList();
        }
        List<RoomResponse> responseList = new ArrayList<>();

        for (Room room: roomsList){
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
        responseList.add(response);
        }
return ResponseEntity.ok(responseList);
    }
}
