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

@Service
public class AvailabilityService {
    @Autowired
    private HotelRepository hotelRepository;

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private RoomRepository roomRepository;
    @Autowired
    private ChildPolicyRepository childPolicyRepository;

    public ResponseEntity<?> searchAvailability(AvailabilityRequest request) {
        // validate user request
        if (request.getCheckOut().isBefore(request.getCheckIn())) {
            throw new InvalidInformationException("Invalid checkout date, checkout should be after checkin date");
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
                .filter(r -> r.getStatus().equals(Hotel.Status.ACTIVE)).toList();

        // get only active booking
        List<Booking> bookingList = new ArrayList<>();
        for (Hotel h : hotelList) {
             bookingList.addAll(bookingRepository.findByHotelIdAndStatus(h.getId(), Booking.Status.CONFIRMED));
        }
        // gets booking that overlap
        List<Booking> overLappedBookings =  bookingList.stream()
                .filter(b ->
                        b.getCheckOut().isAfter(request.getCheckIn())
                                && b.getCheckIn().isBefore(request.getCheckOut())).toList();
        // get rooms that overlapped and can't booked for this user
        List<Room> rooms = overLappedBookings.stream()
                .flatMap(b -> b.getBookingRooms().stream())
                .map(br -> br.getRoom()).toList();


        List<AvailableHotelResponse> hotelResponseList = new ArrayList<>();
        List<RoomResponse> roomResponseList = new ArrayList<>();
        for (Hotel hotel : hotelList) {
            AvailableHotelResponse response = new AvailableHotelResponse();
            response.setHotelId(hotel.getId());
            response.setAddress(hotel.getAddress());
            response.setCity(hotel.getCity());
            response.setCountry(hotel.getCountry());
            response.setHotelName(hotel.getName());
            List<Room> hotelActiveRooms = hotel.getRooms().stream().filter(r->r.getStatus().equals(Room.Status.ACTIVE)).toList();
            for (Room r : hotelActiveRooms) {
                //check if room can't be booked
                if (rooms.contains(r)){
                continue;
                }
                RoomResponse roomResponse = new RoomResponse();
                roomResponse.setId(r.getId());
                roomResponse.setRoomTypeName(r.getRoomType().getName());
                roomResponse.setPricePerNight(r.getPricePerNight());
                roomResponse.setCapacity(r.getCapacity());
                roomResponse.setRoomNumber(r.getRoomNumber());
                roomResponse.setFloorNumber(r.getFloorNumber());
                roomResponseList.add(roomResponse);
            }
            response.getRooms().addAll(roomResponseList);
            hotelResponseList.add(response);
        }

        return ResponseEntity.ok(hotelResponseList);
    }
}
