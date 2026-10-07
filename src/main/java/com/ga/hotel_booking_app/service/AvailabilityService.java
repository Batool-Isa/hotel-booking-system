package com.ga.hotel_booking_app.service;

import com.ga.hotel_booking_app.dto.reponse.AvailableHotelResponse;
import com.ga.hotel_booking_app.dto.reponse.RoomResponse;
import com.ga.hotel_booking_app.dto.request.AvailabilityRequest;
import com.ga.hotel_booking_app.exception.custom.InvalidInformationException;
import com.ga.hotel_booking_app.model.Booking;
import com.ga.hotel_booking_app.model.Hotel;
import com.ga.hotel_booking_app.model.Room;
import com.ga.hotel_booking_app.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AvailabilityService {
    @Autowired
    private HotelRepository hotelRepository;
    @Autowired
    private BookingRepository bookingRepository;

    public ResponseEntity<?> searchAvailability(AvailabilityRequest request) {
        // validate user request
        if (!request.getCheckOut().isAfter(request.getCheckIn())) {
            throw new InvalidInformationException("Checkout date must be after check-in date");
        }
        // get available hotels with room

        //get active hotels
        List<Hotel> hotelList = hotelRepository.findByStatus(Hotel.Status.ACTIVE);
        // if city is provided
        if (request.getCity() != null) {
            hotelList = hotelList.stream().filter(h -> h.getCity().equalsIgnoreCase(request.getCity())).toList();
        }
        // get only active rooms
        List<Room> activeRoomsList = hotelList.stream()
                .flatMap(h -> h.getRooms().stream())
                .filter(r -> r.getStatus().equals(Room.Status.ACTIVE)).toList();

        // get only active booking
        List<Booking> bookingList = new ArrayList<>();
        for (Hotel h : hotelList) {
            bookingList.addAll(bookingRepository.findByHotelIdAndStatus(h.getId(), Booking.Status.CONFIRMED));
        }
        // gets booking that overlap
        List<Booking> overLappedBookings = bookingList.stream()
                .filter(b ->
                        b.getCheckOut().isAfter(request.getCheckIn())
                                && b.getCheckIn().isBefore(request.getCheckOut())).toList();
        // get rooms id of rooms that overlapped and can't booked for this user
        Set<Long> unavailableRooms = overLappedBookings.stream()
                .flatMap(b -> b.getBookingRooms().stream())
                .map(br -> br.getRoom().getId()).collect(Collectors.toSet());


        List<AvailableHotelResponse> hotelResponseList = new ArrayList<>();
        for (Hotel hotel : hotelList) {
            List<RoomResponse> roomResponseList = new ArrayList<>();
            AvailableHotelResponse response = new AvailableHotelResponse();
            response.setHotelId(hotel.getId());
            response.setAddress(hotel.getAddress());
            response.setCity(hotel.getCity());
            response.setCountry(hotel.getCountry());
            response.setHotelName(hotel.getName());
            List<Room> hotelActiveRooms = hotel.getRooms().stream().filter(r -> r.getStatus().equals(Room.Status.ACTIVE)).toList();
            for (Room r : hotelActiveRooms) {
                //check if room can't be booked
                if (unavailableRooms.contains(r.getId())) {
                    continue;
                }
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
