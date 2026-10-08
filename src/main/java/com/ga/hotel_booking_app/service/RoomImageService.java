package com.ga.hotel_booking_app.service;

import com.ga.hotel_booking_app.dto.reponse.MessageResponse;
import com.ga.hotel_booking_app.dto.reponse.RoomImageResponse;
import com.ga.hotel_booking_app.exception.custom.InformationNotFoundException;
import com.ga.hotel_booking_app.exception.custom.InvalidInformationException;
import com.ga.hotel_booking_app.exception.custom.UnauthorizedActionException;
import com.ga.hotel_booking_app.model.*;
import com.ga.hotel_booking_app.repository.HotelRepository;
import com.ga.hotel_booking_app.repository.RoomImageRepository;
import com.ga.hotel_booking_app.repository.RoomRepository;
import com.ga.hotel_booking_app.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class RoomImageService {
    private static final String UPLOAD_DIR = "uploads/rooms";
    private static final int MAX_IMAGES_PER_ROOM = 20;

    @Autowired
    private HotelRepository hotelRepository;
    @Autowired
    private RoomRepository roomRepository;
    @Autowired
    private RoomImageRepository roomImageRepository;
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuditLogService auditLogService;

    private User getCurrentLoggedInUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return userRepository.findUserByEmail(authentication.getName());
    }

    /** The room must exist and belong to the hotel in the URL. */
    private Room findRoomOfHotel(Long hotelId, Long roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new InformationNotFoundException("Room with id " + roomId + " not found"));
        if (!room.getHotel().getId().equals(hotelId)) {
            throw new InformationNotFoundException("Room with id " + roomId + " not found in hotel " + hotelId);
        }
        return room;
    }

    private void checkCanManage(Room room, User user) {
        if (user.getRole().getName() == Role.RoleName.HOTEL_MANAGER) {
            boolean isThisHotelManager = room.getHotel().getManagers().stream()
                    .anyMatch(m -> m.getId().equals(user.getId()));
            if (!isThisHotelManager) {
                throw new UnauthorizedActionException("You are not authorized to change images of this room");
            }
        }
    }

    public ResponseEntity<?> getImages(Long hotelId, Long roomId) {
        findRoomOfHotel(hotelId, roomId);
        List<RoomImageResponse> list = new ArrayList<>();
        for (RoomImage image : roomImageRepository.findByRoomId(roomId)) {
            RoomImageResponse response = new RoomImageResponse();
            response.setId(image.getId());
            response.setImageUrl(image.getImageURL());
            list.add(response);
        }
        return ResponseEntity.ok(list);
    }
    public ResponseEntity<?> uploadImages(Long hotelId, Long roomId, List<MultipartFile> images) {
        Room room = findRoomOfHotel(hotelId, roomId);
        User user = getCurrentLoggedInUser();
        checkCanManage(room, user);

        if (images == null || images.isEmpty()) {
            throw new InvalidInformationException("Please choose at least one image");
        }
        if (roomImageRepository.countByRoomId(roomId) + images.size() > MAX_IMAGES_PER_ROOM) {
            throw new InvalidInformationException("A room can have at most " + MAX_IMAGES_PER_ROOM + " images");
        }
        try {
            // room folder: uploads/rooms/{hotelId}/{roomId}, created if it does not exist
            Path uploadPath = Paths.get(UPLOAD_DIR + "/" + hotelId + "/" + roomId);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            for (MultipartFile image : images) {
                if (image.isEmpty()) {
                    throw new InvalidInformationException("One of the files is empty");
                }
                String uniqueId = UUID.randomUUID().toString().substring(0, 5);
                String imageName = uniqueId + "-" + Paths.get(image.getOriginalFilename()).getFileName();

                Path imageFilePath = uploadPath.resolve(imageName);
                image.transferTo(imageFilePath);

                RoomImage roomImage = new RoomImage();
                roomImage.setImageURL(imageFilePath.toString());
                roomImage.setRoom(room);
                roomImageRepository.save(roomImage);
            }
        } catch (InvalidInformationException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload images", e);
        }
        auditLogService.log(user, "ROOM_IMAGES_UPLOADED", "ROOM", roomId,
                "User " + user.getId() + " uploaded " + images.size() + " image(s) to room " + roomId);
        return ResponseEntity.ok(new MessageResponse("Images uploaded successfully"));
    }

    public ResponseEntity<?> deleteImage(Long hotelId, Long roomId, Long imageId) throws IOException {
        Room room = findRoomOfHotel(hotelId, roomId);
        User user = getCurrentLoggedInUser();
        checkCanManage(room, user);

        RoomImage image = roomImageRepository.findById(imageId)
                .orElseThrow(() -> new InformationNotFoundException("Room image with id " + imageId + " not found"));
        if (!image.getRoom().getId().equals(roomId)) {
            throw new InformationNotFoundException("This image does not belong to this room");
        }
        Files.deleteIfExists(Paths.get(image.getImageURL()));
        roomImageRepository.delete(image);

        auditLogService.log(user, "ROOM_IMAGE_DELETED", "ROOM", roomId,
                "User " + user.getId() + " deleted image " + imageId + " of room " + roomId);
        return ResponseEntity.ok(new MessageResponse("Room image deleted successfully"));
    }
}
