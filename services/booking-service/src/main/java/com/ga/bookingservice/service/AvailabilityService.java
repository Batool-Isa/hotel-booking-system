package com.ga.bookingservice.service;

import com.ga.bookingservice.client.AvailableHotel;
import com.ga.bookingservice.client.AvailableRoom;
import com.ga.bookingservice.client.HotelServiceClient;
import com.ga.bookingservice.dto.response.AvailableHotelResponse;
import com.ga.bookingservice.dto.response.RoomResponse;
import com.ga.bookingservice.dto.request.AvailabilityRequest;
import com.ga.bookingservice.exception.custom.InvalidInformationException;
import com.ga.bookingservice.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;

@Service
public class AvailabilityService {
    @Autowired
    private HotelServiceClient hotelServiceClient;

    public ResponseEntity<?> searchAvailability(AvailabilityRequest request) {
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
        List<AvailableHotel> availableHotel = hotelServiceClient.getAvailableRooms(city, request.getAdults(), request.getChildren());

        List<Long> hotelsIds = availableHotel.stream().map(h ->h.getHotelId()).toList();
//        List<AvailableRoom> availableRoomsByHotel = roomRepository.findFreeRooms(hotelsIds, adults, children, request.getCheckIn(), request.getCheckOut(), LocalDateTime.now());
//

        List<AvailableHotelResponse> hotelResponseList = new ArrayList<>();
        for (AvailableHotel hotel : availableHotel) {
            List<RoomResponse> roomResponseList = new ArrayList<>();
            AvailableHotelResponse response = new AvailableHotelResponse();
            response.setHotelId(hotel.getHotelId());
            response.setAddress(hotel.getAddress());
            response.setCity(hotel.getCity());
            response.setCountry(hotel.getCountry());
            response.setHotelName(hotel.getHotelName());
            response.setLatitude(hotel.getLatitude());
            response.setLongitude(hotel.getLongitude());
            List<AvailableRoom> availableRoom = hotel.getRooms();
            for (AvailableRoom r : availableRoom) {
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
    public RoomResponse buildRoomResponse(AvailableRoom room) {
        RoomResponse roomResponse = new RoomResponse();
        roomResponse.setId(room.getId());
        roomResponse.setRoomTypeName(room.getRoomTypeName());
        roomResponse.setPricePerNight(room.getPricePerNight());
        roomResponse.setMaxAdults(room.getMaxAdults());
        roomResponse.setMaxChildren(room.getMaxChildren());
        roomResponse.setMaxOccupancy(room.getMaxOccupancy());
        roomResponse.setRoomNumber(room.getRoomNumber());
        roomResponse.setFloorNumber(room.getFloorNumber());
        List<String> imageUrls = room.getImageUrls();
        roomResponse.setImageUrls(imageUrls);
        roomResponse.setImageUrl(imageUrls.isEmpty() ? null : imageUrls.get(0));
        roomResponse.setRoomTypeDescription(room.getRoomTypeDescription());
        return roomResponse;
    }

}
