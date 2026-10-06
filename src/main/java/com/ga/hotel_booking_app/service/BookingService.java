package com.ga.hotel_booking_app.service;

import com.ga.hotel_booking_app.dto.reponse.BookingGuestResponse;
import com.ga.hotel_booking_app.dto.reponse.BookingResponse;
import com.ga.hotel_booking_app.dto.reponse.BookingRoomResponse;
import com.ga.hotel_booking_app.dto.reponse.MessageResponse;
import com.ga.hotel_booking_app.dto.request.BookingGuestRequest;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import com.ga.hotel_booking_app.dto.request.BookingRequest;
import com.ga.hotel_booking_app.dto.request.BookingRoomRequest;
import com.ga.hotel_booking_app.dto.request.BookingStatusRequest;
import com.ga.hotel_booking_app.exception.custom.InformationNotFoundException;
import com.ga.hotel_booking_app.exception.custom.InvalidInformationException;
import com.ga.hotel_booking_app.exception.custom.UnauthorizedActionException;
import com.ga.hotel_booking_app.model.*;
import com.ga.hotel_booking_app.repository.BookingRepository;
import com.ga.hotel_booking_app.repository.HotelRepository;
import com.ga.hotel_booking_app.repository.RoomRepository;
import com.ga.hotel_booking_app.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

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
                if (!overlappingBookings.isEmpty()) {
                    throw new InvalidInformationException("Booking room at these days is overlapped");
                }
            }
        }

        // insert into booking table
        Booking booking = new Booking();
        User customer = getCurrentLoggedInUser();
        if (!customer.getStatus().equals(User.Status.ACTIVE)) {
            throw new InvalidInformationException("Customer with no active satate can'et book");
        }
        String bookingReference = hotel.getName().substring(0, 2).toUpperCase() +
                UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        booking.setBookingReference(bookingReference);
        booking.setHotel(hotel);
        booking.setCheckIn(request.getCheckIn());
        booking.setCheckOut(request.getCheckOut());
        booking.setSpecialRequest(request.getSpecialRequest());
        booking.setStatus(Booking.Status.CONFIRMED);
        booking.setUser(customer);
        Set<BookingRoom> bookingRooms = new HashSet<>();
        int totalAdults = 0;
        int totalChildren = 0;

        // calculate total amount
        int daysNum = (int) ChronoUnit.DAYS.between(request.getCheckIn(), request.getCheckOut());
        BigDecimal totalAmount = BigDecimal.valueOf(0.0);

        for (BookingRoomRequest roomRequest : bookingRoomRequests) {

            Room room = roomRepository.findById(roomRequest.getRoomId())
                    .orElseThrow(() -> new InformationNotFoundException("Room with id " + roomRequest.getRoomId() + " not found"));

            BookingRoom bookingRoom = new BookingRoom();
            bookingRoom.setBooking(booking);
            bookingRoom.setRoom(room);
            bookingRoom.setPricePerNight(room.getPricePerNight());

            // add room cost
            totalAmount = totalAmount.add(BigDecimal.valueOf(daysNum).multiply(room.getPricePerNight()));
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
                    totalChildren++;
                } else {
                    guest.setGuestType(BookingGuest.GuestType.ADULT);
                    totalAdults++;
                }

                bookingGuests.add(guest);
            }

            bookingRoom.setGuests(bookingGuests);
            bookingRooms.add(bookingRoom);
        }
        booking.setTotalAmount(totalAmount);
        booking.setBookingRooms(bookingRooms);
        booking.setAdults(totalAdults);
        booking.setChildren(totalChildren);
        bookingRepository.save(booking);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new MessageResponse("Booking created successfully"));

    }

    public ResponseEntity<?> getMyBookings(Pageable pageable) {
        User user = getCurrentLoggedInUser();
        Page<Booking> bookingList = bookingRepository.findByUserId(user.getId(), pageable);
        List<BookingResponse> responses = bookingList.stream()
                .map(b -> mapToBookingResponse((b))).toList();
        return ResponseEntity.ok(responses);
    }

    private BookingResponse mapToBookingResponse(Booking booking) {
        BookingResponse response = new BookingResponse();
        response.setId(booking.getId());
        response.setBookingReference(booking.getBookingReference());
        response.setHotelId(booking.getHotel().getId());
        response.setHotelName(booking.getHotel().getName());
        response.setCheckIn(booking.getCheckIn());
        response.setCheckOut(booking.getCheckOut());
        response.setStatus(booking.getStatus().name());
        response.setAdults(booking.getAdults());
        response.setChildren(booking.getChildren());
        response.setTotalAmount(booking.getTotalAmount());
        response.setSpecialRequest(booking.getSpecialRequest());
        List<BookingRoomResponse> rooms = booking.getBookingRooms()
                .stream()
                .map(this::mapToBookingRoomResponse)
                .toList();
        response.setRooms(rooms);
        return response;
    }

    private BookingRoomResponse mapToBookingRoomResponse(BookingRoom bookingRoom) {
        BookingRoomResponse response = new BookingRoomResponse();
        response.setRoomId(bookingRoom.getRoom().getId());
        response.setRoomNumber(bookingRoom.getRoom().getRoomNumber());
        response.setRoomTypeName(
                bookingRoom.getRoom().getRoomType().getName()
        );
        response.setPricePerNight(bookingRoom.getPricePerNight());
        List<BookingGuestResponse> guests = bookingRoom.getGuests()
                .stream()
                .map(guest -> {
                    BookingGuestResponse guestResponse = new BookingGuestResponse();
                    guestResponse.setId(guest.getId());
                    guestResponse.setName(guest.getName());
                    guestResponse.setAge(guest.getAge());
                    guestResponse.setGuestType(guest.getGuestType().name());
                    return guestResponse;
                })
                .toList();

        response.setGuests(guests);

        return response;
    }

    public ResponseEntity<?> cancelBooking(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(()-> new InformationNotFoundException("Booking with id "+bookingId+ " not found"));
        User user = getCurrentLoggedInUser();
        if (user.getRole().getName().equals(Role.RoleName.CUSTOMER) && !booking.getUser().getId().equals(user.getId())){
            throw new UnauthorizedActionException("You are not authorized to cancel this booking");
        }
        if(user.getRole().getName().equals(Role.RoleName.HOTEL_MANAGER)){
            boolean isHotelManger = booking.getHotel().getManagers().stream()
                    .anyMatch(m->m.getId().equals(user.getId()));
            if(!isHotelManger){
                throw new UnauthorizedActionException("You are not authorized to cancel this booking");
            }
        }
        if (booking.getStatus().equals(Booking.Status.CANCELLED)) {
            throw new InvalidInformationException("This booking is already cancelled");
        }

        if (booking.getStatus().equals(Booking.Status.COMPLETED)) {
            throw new InvalidInformationException("A completed booking can't be cancelled");
        }

        // only confirmed booking can be canceled
        if(!booking.getStatus().equals(Booking.Status.CONFIRMED)){
            throw new InvalidInformationException("This booking can't be canceled");
        }
        booking.setStatus(Booking.Status.CANCELLED);
        bookingRepository.save(booking);
        return ResponseEntity.ok(new MessageResponse("Booking canceled successfully"));
    }

    public ResponseEntity<?> getBookingById(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new InformationNotFoundException("Booking with id " + bookingId + " not found"));
        User user = getCurrentLoggedInUser();
        Role.RoleName role = user.getRole().getName();
        if (role.equals(Role.RoleName.CUSTOMER)
                && !booking.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedActionException("You are not authorized to view this booking");
        }
        if (role.equals(Role.RoleName.HOTEL_MANAGER)) {
                boolean isHotelManager = booking.getHotel()
                        .getManagers().stream().anyMatch(manager -> manager.getId().equals(user.getId()));
                if (!isHotelManager) {
                    throw new UnauthorizedActionException("You are not authorized to view this booking");
                }
            }
            BookingResponse response = mapToBookingResponse(booking);
            return ResponseEntity.ok(response);

    }

        public ResponseEntity<?> updateBookingStatus(Long bookingId, BookingStatusRequest request) {
            Booking booking = bookingRepository.findById(bookingId).orElseThrow(() -> new InformationNotFoundException("Booking with id " + bookingId + " not found"));
            User user = getCurrentLoggedInUser();
            Role.RoleName role = user.getRole().getName();
            if (role.equals(Role.RoleName.HOTEL_MANAGER)) {
                boolean isHotelManager = booking.getHotel()
                        .getManagers().stream().anyMatch(manager -> manager.getId().equals(user.getId()));
                if (!isHotelManager) {
                    throw new UnauthorizedActionException("You are not authorized to update this booking");
                }
            } else if (!role.equals(Role.RoleName.ADMIN)) {
                throw new UnauthorizedActionException("You are not authorized to update booking status");
            }

            Booking.Status currentStatus = booking.getStatus();
            Booking.Status newStatus = request.getStatus();
            if (currentStatus.equals(newStatus)) {
                throw new InvalidInformationException(
                        "Booking is already " + currentStatus);
            }
            if (!currentStatus.equals(Booking.Status.CONFIRMED)) {
                throw new InvalidInformationException("Booking status cannot be changed from " + currentStatus);
            }
            if (!newStatus.equals(Booking.Status.COMPLETED)) {
                throw new InvalidInformationException("The booking can only be marked as COMPLETED");
            }
            booking.setStatus(Booking.Status.COMPLETED);
            bookingRepository.save(booking);
            return ResponseEntity.ok(new MessageResponse("Booking status updated successfully"));
        }
}
