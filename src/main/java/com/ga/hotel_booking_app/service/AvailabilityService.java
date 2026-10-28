package com.ga.hotel_booking_app.service;

import com.ga.hotel_booking_app.dto.response.AvailableHotelResponse;
import com.ga.hotel_booking_app.dto.response.RoomResponse;
import com.ga.hotel_booking_app.dto.request.AvailabilityRequest;
import com.ga.hotel_booking_app.exception.custom.InvalidInformationException;
import com.ga.hotel_booking_app.model.Booking;
import com.ga.hotel_booking_app.model.Hotel;
import com.ga.hotel_booking_app.model.Room;
import com.ga.hotel_booking_app.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AvailabilityService {
    @Autowired
    private HotelRepository hotelRepository;
    @Autowired
    private BookingRepository bookingRepository;
    @Autowired
    private RoomRepository roomRepository;
    public ResponseEntity<?> searchAvailability(AvailabilityRequest request, Pageable pageable) {
        // validate user request
        if (!request.getCheckOut().isAfter(request.getCheckIn())) {
            throw new InvalidInformationException("Checkout date must be after check-in date");
        }
        Integer adultNum = request.getAdults();
        // if adults num is null , use 1 b default
        int adults = adultNum == null ? 1 :adultNum;
        Integer childNum = request.getChildren();
        int children = childNum==null ? 0: childNum;
        String city = request.getCity();

        // get available hotels with room

        //get available hotels --> activeOne and has available slots in room in selected date and number of occupacy
        Page<Hotel> availableHotel = hotelRepository.findHotelsWithFreeRooms(city, adults, children,
                request.getCheckIn(), request.getCheckOut(), pageable);

        List<Long> hotelsIds = availableHotel.stream().map(h ->h.getId()).toList();
        List<Room> availableRoomsByHotel = roomRepository.findFreeRooms(hotelsIds, adults, children, request.getCheckIn(), request.getCheckOut(), LocalDateTime.now());


        List<AvailableHotelResponse> hotelResponseList = new ArrayList<>();
        for (Hotel hotel : availableHotel) {
            List<RoomResponse> roomResponseList = new ArrayList<>();
            AvailableHotelResponse response = new AvailableHotelResponse();
            response.setHotelId(hotel.getId());
            response.setAddress(hotel.getAddress());
            response.setCity(hotel.getCity());
            response.setCountry(hotel.getCountry());
            response.setHotelName(hotel.getName());
            response.setLatitude(hotel.getLatitude());
            response.setLongitude(hotel.getLongitude());
            List<Room> availableRoom = availableRoomsByHotel.stream().filter(r->r.getHotel().equals(hotel)).toList();
            for (Room r : availableRoom) {
                RoomResponse roomResponse = buildRoomResponse(r);
                roomResponseList.add(roomResponse);
            }
            response.getRooms().addAll(roomResponseList);
            // only add active hotel that has available rooms
            if (!roomResponseList.isEmpty()) {
                hotelResponseList.add(response);
            }
        }

        return ResponseEntity.ok(hotelResponseList);
    }

    public RoomResponse buildRoomResponse(Room room){
        RoomResponse roomResponse = new RoomResponse();
        roomResponse.setId(room.getId());
        roomResponse.setRoomTypeName(room.getRoomType().getName());
        roomResponse.setPricePerNight(room.getPricePerNight());
        roomResponse.setMaxAdults(room.getMaxAdults());
        roomResponse.setMaxChildren(room.getMaxChildren());
        roomResponse.setMaxOccupancy(room.getMaxOccupancy());
        roomResponse.setRoomNumber(room.getRoomNumber());
        roomResponse.setFloorNumber(room.getFloorNumber());
        return roomResponse;
    }
}
