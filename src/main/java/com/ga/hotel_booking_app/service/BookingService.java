package com.ga.hotel_booking_app.service;

import com.ga.hotel_booking_app.dto.reponse.MessageResponse;
import com.ga.hotel_booking_app.dto.request.BookingGuestRequest;
import com.ga.hotel_booking_app.dto.request.BookingRequest;
import com.ga.hotel_booking_app.dto.request.BookingRoomRequest;
import com.ga.hotel_booking_app.exception.custom.InformationNotFoundException;
import com.ga.hotel_booking_app.exception.custom.InvalidInformationException;
import com.ga.hotel_booking_app.model.*;
import com.ga.hotel_booking_app.repository.BookingRepository;
import com.ga.hotel_booking_app.repository.HotelRepository;
import com.ga.hotel_booking_app.repository.RoomRepository;
import com.ga.hotel_booking_app.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class BookingService {
    @Autowired
    private BookingRepository bookingRepository;
    @Autowired
    private RoomRepository roomRepository;
    @Autowired
    private HotelRepository hotelRepository;
    @Autowired
    private UserRepository userRepository;

    public User getCurrentLoggedInUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        return userRepository.findUserByEmail(email);
    }
    @Transactional
    public ResponseEntity<?> createNewBooking(BookingRequest request) {
        // validate dates
        boolean validDate = request.getCheckIn().isAfter(LocalDate.now())
                && request.getCheckOut().isAfter(LocalDate.now())
                && request.getCheckOut().isAfter(request.getCheckIn());
        if (!validDate) {
            throw new InvalidInformationException("Invalid booking dates");
        }

        // get rooms that customer wants to book
        List<BookingRoomRequest> bookingRoomRequests = request.getRooms();
        // get hotel
        Hotel hotel = hotelRepository.findById(request.getHotelId())
                .orElseThrow(() -> new InformationNotFoundException("Hotel with id " + request.getHotelId() + " not found"));

        // get hotel's child policy
        ChildPolicy childPolicy = hotel.getChildPolicy();


        //check capacity of each room
        for (BookingRoomRequest r : bookingRoomRequests) {
            // variable to holds count of guests age
            int adultNum = 0;
            int childrenNum = 0;
            int infantNum = 0;

            Room room = roomRepository.findById(r.getRoomId())
                    .orElseThrow(() -> new InformationNotFoundException("Room with id" + r.getRoomId() + " not found"));
            if (!room.getHotel().getId().equals(hotel.getId())) {
                throw new InvalidInformationException("Room does not belong to the selected hotel");
            }
            // validate room status --> only active room can be booked
            if (!room.getStatus().equals(Room.Status.ACTIVE)) {
                throw new InvalidInformationException("Room with id " + room.getId() + " is not active for booking for now");
            }
            List<BookingGuestRequest> guestRequests = r.getGuests();

            for (BookingGuestRequest guestRequest : guestRequests) {
                // check guests ages
                if (guestRequest.getAge() <= childPolicy.getInfantMaxAge()) {
                    infantNum++;
                } else if (guestRequest.getAge() <= childPolicy.getChildMaxAge()) {
                    childrenNum++;
                } else {
                    adultNum++;
                }

                // validate number of guests
                int total = childrenNum + adultNum;

                if (childPolicy.isInfantsCountTowardOccupancy()) {
                    total += infantNum;
                }
                if (adultNum > room.getMaxAdults()
                || childrenNum > room.getMaxChildren()
                || total > room.getMaxOccupancy()) {
                    throw new InvalidInformationException("Invalid number of guests, room can't accommodate this number of guests");
                }

                // check booking that might overlap
                List<Booking> overlappingBookings = bookingRepository.findOverlappingBookings(r.getRoomId()
                        , Booking.Status.CONFIRMED, request.getCheckIn(), request.getCheckOut());
                if (overlappingBookings.size() > 0) {
                    throw new InvalidInformationException("Booking room at these days is overlapped");
                }

            }


        }

        // insert into booking table
        Booking booking = new Booking();
        User customer = getCurrentLoggedInUser();
        if(!customer.getStatus().equals(User.Status.ACTIVE)){
            throw  new InvalidInformationException("Customer with no active satate can'et book");
        }
        booking.setHotel(hotel);
        booking.setCheckIn(request.getCheckIn());
        booking.setCheckOut(request.getCheckOut());
        booking.setSpecialRequest(request.getSpecialRequest());
        booking.setStatus(Booking.Status.CONFIRMED);
        booking.setUser(customer);
        Set<BookingRoom> bookingRooms = new HashSet<>();

        for (BookingRoomRequest roomRequest : bookingRoomRequests) {

            Room room = roomRepository.findById(roomRequest.getRoomId())
                    .orElseThrow(() -> new InformationNotFoundException("Room with id " + roomRequest.getRoomId() + " not found"));

            BookingRoom bookingRoom = new BookingRoom();
            bookingRoom.setBooking(booking);
            bookingRoom.setRoom(room);
            bookingRoom.setPricePerNight(room.getPricePerNight());

            List<BookingGuest> bookingGuests = new ArrayList<>();

            for (BookingGuestRequest guestRequest : roomRequest.getGuests()) {

                BookingGuest guest = new BookingGuest();

                guest.setBookingRoom(bookingRoom);
                guest.setName(guestRequest.getName());
                guest.setAge(guestRequest.getAge());

                // determine guest type
                if (guestRequest.getAge() <= childPolicy.getInfantMaxAge()) {
                    guest.setGuestType(BookingGuest.GuestType.INFANT);
                } else if (guestRequest.getAge() <= childPolicy.getChildMaxAge()) {
                    guest.setGuestType(BookingGuest.GuestType.CHILD);
                } else {
                    guest.setGuestType(BookingGuest.GuestType.ADULT);
                }

                bookingGuests.add(guest);
            }

            bookingRoom.setGuests(bookingGuests);
            bookingRooms.add(bookingRoom);
        }

        booking.setBookingRooms(bookingRooms);

        bookingRepository.save(booking);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new MessageResponse("Booking created successfully"));

    }
}
