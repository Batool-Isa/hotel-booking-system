package com.ga.bookingservice.service;

import com.ga.bookingservice.client.*;
import com.ga.bookingservice.dto.response.BookingGuestResponse;
import com.ga.bookingservice.dto.response.BookingResponse;
import com.ga.bookingservice.dto.response.BookingRoomResponse;
import com.ga.bookingservice.dto.response.MessageResponse;
import com.ga.bookingservice.dto.request.BookingGuestRequest;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import com.ga.bookingservice.dto.request.BookingRequest;
import com.ga.bookingservice.dto.request.BookingRoomRequest;
import com.ga.bookingservice.dto.request.BookingStatusRequest;
import com.ga.bookingservice.exception.custom.InformationNotFoundException;
import com.ga.bookingservice.exception.custom.InvalidInformationException;
import com.ga.bookingservice.exception.custom.RoomUnavailableException;
import com.ga.bookingservice.exception.custom.UnauthorizedActionException;
import com.ga.bookingservice.event.BookingChanged;
import org.springframework.context.ApplicationEventPublisher;
import com.ga.bookingservice.model.*;
import com.ga.bookingservice.repository.BookingRepository;
import com.ga.bookingservice.security.AuthUser;
import com.ga.bookingservice.security.CurrentUser;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Predicate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.*;

@Service
public class BookingService {
    @Autowired
    private BookingRepository bookingRepository;
    @Autowired
    private CurrentUser currentUser;
    @Autowired
    private HotelServiceClient hotelServiceClient;
    @Autowired
    private UserServiceClient userServiceClient;
    @Autowired
    private EmailService emailService;
    // in-memory events: BookingEventRelay sends them to Kafka after the transaction commits
    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;
    //    @Autowired
//    private NotificationService notificationService;
    @Autowired
    private AuditLogService auditLogService;
    private static final Logger logger =
            LoggerFactory.getLogger(BookingService.class);

    public UserSummary getUserDetails(Long id){
      return userServiceClient.findUser(id, currentUser.token())
                .orElseThrow(() -> new InformationNotFoundException("User "+id+" not found"));
    }
    // everything validateBookingRequest fetched from hotel-service, so createNewBooking can reuse it
    // instead of calling hotel-service a second time
    private record ValidatedBooking(HotelSummary hotel, ChildPolicySummary childPolicy,
                                    Map<Long, RoomSummary> rooms) {}

    private ValidatedBooking validateBookingRequest(BookingRequest request){
        // validate dates
        boolean validDate = !request.getCheckIn().isBefore(LocalDate.now())
                && request.getCheckOut().isAfter(request.getCheckIn());
        if (!validDate) {
            throw new InvalidInformationException("Invalid booking dates");
        }

        // get rooms that customer wants to book
        List<BookingRoomRequest> bookingRoomRequests = request.getRooms();
        // get hotel
        HotelSummary hotel = hotelServiceClient.getHotelSummary(request.getHotelId());
        if (!hotel.getStatus().equals("ACTIVE")) {
            throw new InvalidInformationException("This hotel is not available for booking");
        }
        // get hotel's child policy
        ChildPolicySummary childPolicy = hotelServiceClient.getChildPolicySummary(hotel.getId());
        if (childPolicy == null) {
            throw new InvalidInformationException(
                    "This hotel has not set its child policy yet, so it can't accept bookings right now");
        }


        Set<Long> roomIds = new TreeSet<>();
        for (BookingRoomRequest r : bookingRoomRequests) {
            if (!roomIds.add(r.getRoomId())) {
                throw new InvalidInformationException("Room with id " + r.getRoomId() + " was selected twice");
            }
        }


        // rooms fetched from hotel-service, by room id
        Map<Long, RoomSummary> rooms = new HashMap<>();

        //check capacity of each room (no lock yet: this is hotel-service data, not bookings)
        for (BookingRoomRequest r : bookingRoomRequests) {
            // variable to holds count of guests age
            int adultNum = 0;
            int childrenNum = 0;
            int infantNum = 0;

            RoomSummary room = hotelServiceClient.getRoomSummary(hotel.getId(), r.getRoomId(), currentUser.token());
            rooms.put(room.getId(), room);
            if (!room.getHotelId().equals(hotel.getId())) {
                throw new InvalidInformationException("Room does not belong to the selected hotel");
            }
            // validate room status --> only active room can be booked
            if (!room.getStatus().equals("ACTIVE")) {
                throw new InvalidInformationException("Room with id " + room.getId() + " is not active for booking for now");
            }
            List<BookingGuestRequest> guestRequests = r.getGuests();

            if (guestRequests == null || guestRequests.isEmpty()) {
                throw new InvalidInformationException("Room " + r.getRoomId() + " needs at least one guest");
            }

            for (BookingGuestRequest guestRequest : guestRequests) {
                if (guestRequest.getAge() == null) {
                    throw new InvalidInformationException("Age is required");
                }
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
            }
        }

        // lock all rooms AFTER the hotel-service calls, so the lock is held as short as possible.
        // roomIds is a TreeSet (sorted) -> every request locks in the same order -> no deadlock.
        // the lock is released when the transaction of createNewBooking commits.
        for (Long roomId : roomIds) {
            logger.info("[{}] waiting for the lock on room {}", Thread.currentThread().getName(), roomId);
            bookingRepository.lockRoom(roomId);
            logger.info("[{}] got the lock on room {}", Thread.currentThread().getName(), roomId);
        }

        // check bookings that might overlap -> must be inside the lock
        for (Long roomId : roomIds) {
            List<Booking> overlappingBookings = bookingRepository.findOverlappingBookings(roomId,
                    request.getCheckIn(), request.getCheckOut(), LocalDateTime.now());
            if (!overlappingBookings.isEmpty()) {
                throw new RoomUnavailableException("Room " + roomId + " is not available for these dates");
            }
        }
        return new ValidatedBooking(hotel, childPolicy, rooms);
    }
    @Transactional
    public ResponseEntity<?> createNewBooking(BookingRequest request) {
        // check user status
        AuthUser customer = currentUser.get();
        if (!"ACTIVE".equals(customer.getStatus())) {
            throw new InvalidInformationException("Only active customer can book hotels");
        }

        // validate user request; reuse what it fetched (no second call to hotel-service)
        ValidatedBooking validated = validateBookingRequest(request);
        HotelSummary hotel = validated.hotel();
        ChildPolicySummary childPolicy = validated.childPolicy();
        Booking booking = new Booking();
        String referencePrefix = hotel.getName().replaceAll("[^A-Za-z]","").toUpperCase();
        if(referencePrefix.length() >= 4){
            referencePrefix = referencePrefix.substring(0,4);
        }else{
            referencePrefix = (referencePrefix+"XXXX").substring(0,4);
        }
        String referenceSuffix = UUID.randomUUID().toString().substring(0, 5).toUpperCase();
        String bookingReference = referencePrefix + referenceSuffix;
        booking.setBookingReference(bookingReference);
        booking.setHotelId(hotel.getId());
        booking.setCheckIn(request.getCheckIn());
        booking.setCheckOut(request.getCheckOut());
        booking.setSpecialRequest(request.getSpecialRequest());
        booking.setStatus(Booking.Status.PENDING);
        booking.setHoldExpiresAt(LocalDateTime.now().plusMinutes(15));
        booking.setUserId(customer.getId());
        Set<BookingRoom> bookingRooms = new HashSet<>();
        int totalAdults = 0;
        int totalChildren = 0;

        // calculate total amount
        int daysNum = (int) ChronoUnit.DAYS.between(request.getCheckIn(), request.getCheckOut());
        BigDecimal totalAmount = BigDecimal.valueOf(0.0);

        // get rooms that customer wants to book
        List<BookingRoomRequest> bookingRoomRequests = request.getRooms();

        for (BookingRoomRequest roomRequest : bookingRoomRequests) {
            RoomSummary room = validated.rooms().get(roomRequest.getRoomId());
            BookingRoom bookingRoom = new BookingRoom();
            bookingRoom.setBooking(booking);
            bookingRoom.setRoomId(room.getId());
            bookingRoom.setRoomNumber(room.getRoomNumber());
            bookingRoom.setRoomTypeName(room.getRoomTypeName());
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
        BookingResponse bookingResponse = mapToBookingResponse(booking, hotel);
        UserSummary customerSummary = userServiceClient.findUser(customer.getId(), currentUser.token())
                .orElseThrow(() -> new InformationNotFoundException("User "+customer.getId()+" not found"));
        auditLogService.log(
                customer.getId(),
                "BOOKING_CREATED",
                "BOOKING",
                booking.getId(),
                "Customer " + customerSummary.getFullName() + " reserve booking " + booking.getBookingReference()
        );
        applicationEventPublisher.publishEvent(new BookingChanged(booking, "BOOKING_CREATED"));
        logger.info(
                "[{}] Booking created: reference={}, userId={}, hotelId={} (room lock is released when this transaction commits)",
                Thread.currentThread().getName(),
                booking.getBookingReference(),
                customer.getId(),
                hotel.getId()
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(bookingResponse);

    }

    public ResponseEntity<?> getMyBookings(Pageable pageable) {
        AuthUser user = currentUser.get();
        Page<Booking> bookingList = bookingRepository.findByUserId(user.getId(), pageable);
        List<BookingResponse> responses = bookingList.stream()
                .map(b -> mapToBookingResponse(b, hotelServiceClient.getHotelSummary(b.getHotelId()))).toList();
        return ResponseEntity.ok(responses);
    }

    private BookingResponse mapToBookingResponse(Booking booking, HotelSummary hotel) {
        BookingResponse response = new BookingResponse();
        response.setUserId(currentUser.get().getId());
        response.setId(booking.getId());
        response.setBookingReference(booking.getBookingReference());
        response.setHotelId(booking.getHotelId());
        response.setHotelName(hotel.getName());
        setGuestDetails(response, booking.getUserId());
        response.setCreatedAt(booking.getCreatedAt());
        response.setCheckIn(booking.getCheckIn());
        response.setCheckOut(booking.getCheckOut());
        response.setStatus(booking.getStatus().name());
        response.setHoldExpiresAt(booking.getHoldExpiresAt());
        response.setAdults(booking.getAdults());
        response.setChildren(booking.getChildren());
        response.setTotalAmount(booking.getTotalAmount());
        response.setSpecialRequest(booking.getSpecialRequest());
//        response.setReviewed(booking.getReview() != null);
        List<BookingRoomResponse> rooms = booking.getBookingRooms()
                .stream()
                .map(this::mapToBookingRoomResponse)
                .toList();
        response.setRooms(rooms);
        return response;
    }

    private void setGuestDetails(BookingResponse response, Long guestId) {
        AuthUser viewer = currentUser.get();

        if (viewer != null && viewer.getId().equals(guestId) && !viewer.isAdmin() && !viewer.isManager()) {
            response.setGuestEmail(viewer.getEmail());
            return;
        }
        UserSummary guest = userServiceClient.findUser(guestId, currentUser.token())
                .orElseThrow(() -> new InformationNotFoundException("User with id " + guestId + " not found"));
        response.setGuestName(guest.getFullName());
        response.setGuestEmail(guest.getEmail());
        response.setGuestPhone(guest.getPhone());
    }

    private BookingRoomResponse mapToBookingRoomResponse(BookingRoom bookingRoom) {
        BookingRoomResponse response = new BookingRoomResponse();
        response.setRoomId(bookingRoom.getRoomId());
        if (bookingRoom.getRoomNumber() != null) {
            // saved at booking time -> no call to hotel-service
            response.setRoomNumber(bookingRoom.getRoomNumber());
            response.setRoomTypeName(bookingRoom.getRoomTypeName());
        } else {
            // older booking made before the copy existed -> ask hotel-service
            RoomSummary room = hotelServiceClient.getRoomSummary(
                    bookingRoom.getBooking().getHotelId(), bookingRoom.getRoomId(), currentUser.token());
            if (room != null) {
                response.setRoomNumber(room.getRoomNumber());
                response.setRoomTypeName(room.getRoomTypeName());
            }
        }
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

    @Transactional
    public ResponseEntity<?> cancelBooking(Long bookingId) {
        Booking booking = bookingRepository.findByIdForUpdate(bookingId)
                .orElseThrow(() -> new InformationNotFoundException("Booking with id " + bookingId + " not found"));
        AuthUser user = currentUser.get();
        if ("CUSTOMER".equals(user.getRole()) && !booking.getUserId().equals(user.getId())) {
            throw new UnauthorizedActionException("You are not authorized to cancel this booking");
        }
        if ("HOTEL_MANAGER".equals(user.getRole())) {
            List<HotelSummary> hotels = hotelServiceClient.getManagersHotel(currentUser.token());
            boolean isHotelManger = hotels.stream()
                    .anyMatch(m -> m.getId().equals(booking.getHotelId()));
            if (!isHotelManger) {
                throw new UnauthorizedActionException("You are not authorized to cancel this booking");
            }
        }
        if (booking.getStatus().equals(Booking.Status.CANCELLED)) {
            throw new InvalidInformationException("This booking is already cancelled");
        }

        if (booking.getStatus().equals(Booking.Status.COMPLETED)) {
            throw new InvalidInformationException("A completed booking can't be cancelled");
        }


        booking.setStatus(Booking.Status.CANCELLED);
        bookingRepository.save(booking);
//        notificationService.sendBookingNotification(booking);
//        emailService.sendBookingCancellationEmail(booking);
        applicationEventPublisher.publishEvent(new BookingChanged(booking, "BOOKING_CANCELLED"));
        String logDescription;
       UserSummary userDetails = getUserDetails(user.getId());
        if ("HOTEL_MANAGER".equals(user.getRole())) {
            logDescription = "Manager " + userDetails.getFullName() + " cancelled booking "
                    + booking.getBookingReference() + " for user " + booking.getUserId();
        } else if ("ADMIN".equals(user.getRole())) {
            logDescription = "Admin " + userDetails.getFullName() + " cancelled booking "
                    + booking.getBookingReference() + " for user " + booking.getUserId();
        } else {
            logDescription = "Customer " + userDetails.getFullName() + " cancelled booking " + booking.getBookingReference();
        }
        auditLogService.log(
                user.getId(),
                "BOOKING_CANCELLED",
                "BOOKING",
                booking.getId(),
                logDescription
        );
        logger.info(
                "Booking cancelled: reference={}, userId={}",
                booking.getBookingReference(),
                user.getId()
        );
        return ResponseEntity.ok(new MessageResponse("Booking canceled successfully"));
    }

    public ResponseEntity<?> getBookingById(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new InformationNotFoundException("Booking with id " + bookingId + " not found"));
        AuthUser user = currentUser.get();
        if ("CUSTOMER".equals(user.getRole())
                && !booking.getUserId().equals(user.getId())) {
            throw new UnauthorizedActionException("You are not authorized to view this booking");
        }
        if ("HOTEL_MANAGER".equals(user.getRole())) {
            checkManagerAuthority(booking.getHotelId());
        }
        HotelSummary hotelSummary = hotelServiceClient.getHotelSummary(booking.getHotelId());
        BookingResponse response = mapToBookingResponse(booking, hotelSummary);
        return ResponseEntity.ok(response);

    }

    public ResponseEntity<?> updateBookingStatus(Long bookingId, BookingStatusRequest request) {
        Booking booking = bookingRepository.findById(bookingId).orElseThrow(() -> new InformationNotFoundException("Booking with id " + bookingId + " not found"));
        AuthUser user = currentUser.get();
        if ("HOTEL_MANAGER".equals(user.getRole())) {
            checkManagerAuthority(booking.getHotelId());
        } else if (!"ADMIN".equals(user.getRole())) {
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
//        notificationService.sendBookingNotification(booking);
        applicationEventPublisher.publishEvent(new BookingChanged(booking, "BOOKING_COMPLETED"));

        String logDescription;
        UserSummary userDetails = getUserDetails(user.getId());
        if ("HOTEL_MANAGER".equals(user.getRole())) {
            logDescription = "Manager " + userDetails.getFullName() + " update booking "
                    + booking.getBookingReference() + " for user " + booking.getUserId();
        } else if ("ADMIN".equals(user.getRole())) {
            logDescription = "Admin " + userDetails.getFullName() + " update booking "
                    + booking.getBookingReference() + " for user " + booking.getUserId();
        } else {
            logDescription = "Customer " + userDetails.getFullName() + " update booking " + booking.getBookingReference();
        }
        auditLogService.log(
                user.getId(),
                "BOOKING_UPDATE",
                "BOOKING",
                booking.getId(),
                logDescription
        );
        logger.info(
                "Booking status changed: reference={}, oldStatus={}, newStatus={}, userId={}",
                booking.getBookingReference(),
                currentStatus,
                booking.getStatus(),
                user.getId()
        );
        return ResponseEntity.ok(new MessageResponse("Booking status updated successfully"));
    }

    // called by billing-service (service-to-service, no user token) after Stripe says the booking is paid.
    // locks the booking row so it can't race with the hold-expiry job or a cancel.
    // returns the booking's status after the call, so billing knows if it was confirmed.
    @Transactional
    public Map<String, Object> confirmPaidBooking(Long bookingId) {
        Booking booking = bookingRepository.findByIdForUpdate(bookingId)
                .orElseThrow(() -> new InformationNotFoundException("Booking with id " + bookingId + " not found"));

        if (booking.getStatus() == Booking.Status.PENDING
                && booking.getHoldExpiresAt() != null
                && booking.getHoldExpiresAt().isAfter(LocalDateTime.now())) {
            booking.setStatus(Booking.Status.CONFIRMED);
            bookingRepository.save(booking);
            applicationEventPublisher.publishEvent(new BookingChanged(booking, "BOOKING_CONFIRMED"));

            auditLogService.log(
                    booking.getUserId(),
                    "BOOKING_PAID",
                    "BOOKING",
                    booking.getId(),
                    "Booking " + booking.getBookingReference() + " confirmed after payment"
            );
            logger.info("Booking {} is confirmed after payment paid", booking.getBookingReference());
        } else if (booking.getStatus() != Booking.Status.CONFIRMED) {
            // paid too late (hold expired / cancelled): leave it, billing logs a warning
            logger.warn("Payment arrived for booking {} but it is {}",
                    booking.getBookingReference(), booking.getStatus());
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("id", booking.getId());
        body.put("bookingReference", booking.getBookingReference());
        body.put("userId", booking.getUserId());
        body.put("hotelId", booking.getHotelId());
        body.put("status", booking.getStatus().name());
        body.put("holdExpiresAt", booking.getHoldExpiresAt());
        body.put("totalAmount", booking.getTotalAmount());
        return body;
    }

    public void checkManagerAuthority(Long hotelId){
        List<HotelSummary> hotelSummaries =  hotelServiceClient.getManagersHotel(currentUser.token());
        boolean isHotelManager = hotelSummaries.stream()
                .anyMatch(h ->
                        h.getId().equals(hotelId));
        if (!isHotelManager) {
            throw new UnauthorizedActionException(
                    "You are not authorized to manage bookings of this hotel");
        }
    }

    public ResponseEntity<?> getManagerBookings(Long hotelId, Booking.Status status, String when,
                                                String q, Pageable pageable) {
        List<HotelSummary> hotels = hotelServiceClient.getManagersHotel(currentUser.token());
        List<Long> hotelIds = hotels.stream().map(HotelSummary::getId).toList();

        if (hotelId != null && !hotelIds.contains(hotelId)) {
            throw new UnauthorizedActionException("You are not authorized to view this hotel's bookings");
        }
        if (hotelIds.isEmpty()) {
            return ResponseEntity.ok(pageBody(Page.empty(pageable)));
        }
        return ResponseEntity.ok(pageBody(bookingRepository.findAll(
                bookingFilter(hotelIds, hotelId, status, when, q), pageable)));
    }

    public ResponseEntity<?> getAllAppBookings(Long hotelId, Booking.Status status, String when,
                                               String q, Pageable pageable) {
        return ResponseEntity.ok(pageBody(bookingRepository.findAll(
                bookingFilter(null, hotelId, status, when, q), pageable)));
    }



    private Specification<Booking> bookingFilter(List<Long> allowedHotelIds, Long hotelId, Booking.Status status,
                                                 String when, String q) {
        LocalDate today = LocalDate.now();
        return (root, query, cb) -> {
            List<Predicate> filters = new ArrayList<>();
            if (hotelId != null) {
                filters.add(cb.equal(root.get("hotelId"), hotelId));
            } else if (allowedHotelIds != null) {
                filters.add(root.get("hotelId").in(allowedHotelIds));
            }
            if (status != null) {
                filters.add(cb.equal(root.get("status"), status));
            }
            if ("upcoming".equalsIgnoreCase(when)) {
                filters.add(cb.greaterThanOrEqualTo(root.get("checkIn"), today));
            } else if ("current".equalsIgnoreCase(when)) {
                filters.add(cb.lessThanOrEqualTo(root.get("checkIn"), today));
                filters.add(cb.greaterThan(root.get("checkOut"), today));
            } else if ("past".equalsIgnoreCase(when)) {
                filters.add(cb.lessThanOrEqualTo(root.get("checkOut"), today));
            }
            if (q != null && !q.isBlank()) {
                    String like = "%" + q.trim().toLowerCase() + "%";
                    filters.add(cb.like(cb.lower(root.get("bookingReference")), like));
            }
            return cb.and(filters.toArray(new Predicate[0]));
        };
    }

    private Map<String, Object> pageBody(Page<Booking> page) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("content", page.getContent().stream().map(b -> mapToBookingResponse(b, hotelServiceClient.getHotelSummary(b.getHotelId()))).toList());
        body.put("page", page.getNumber());
        body.put("size", page.getSize());
        body.put("totalElements", page.getTotalElements());
        body.put("totalPages", page.getTotalPages());
        return body;
    }

}
