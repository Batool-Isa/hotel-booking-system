package com.ga.hotel_booking_app.service;


import com.ga.hotel_booking_app.dto.reponse.HotelImageResponse;
import com.ga.hotel_booking_app.dto.reponse.MessageResponse;
import com.ga.hotel_booking_app.exception.custom.InformationNotFoundException;
import com.ga.hotel_booking_app.exception.custom.InvalidInformationException;
import com.ga.hotel_booking_app.exception.custom.UnauthorizedActionException;
import com.ga.hotel_booking_app.model.*;
import com.ga.hotel_booking_app.repository.HotelImageRepository;
import com.ga.hotel_booking_app.repository.HotelRepository;
import com.ga.hotel_booking_app.repository.UserRepository;
import lombok.experimental.NonFinal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.management.relation.Relation;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class HotelImageService {
    @Autowired
    private HotelRepository hotelRepository;

    @Autowired
    private HotelImageRepository hotelImageRepository;
    @Autowired
    private UserRepository userRepository;

    public User getCurrentLoggedInUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        return userRepository.findUserByEmail(email);
    }


    public ResponseEntity<?> getImages(Long id) {
        List<HotelImage> images = hotelImageRepository.findByHotelId(id);
        List<HotelImageResponse> responsesList = new ArrayList<>();

        for (HotelImage image : images) {
            HotelImageResponse response = new HotelImageResponse();
            response.setId(image.getId());
            response.setImageUrl(image.getImageURL());
            response.setAltText(image.getAltText());
            responsesList.add(response);
        }
        return ResponseEntity.ok(responsesList);
    }

    private final String UPLOAD_DIR = "uploads/hotels";

    public ResponseEntity<?> uploadImages(Long id, List<MultipartFile> images, List<String> altTexts) {
        Hotel hotel = hotelRepository.findById(id)
                .orElseThrow(() -> new InformationNotFoundException("Hotel with id " + id + " not found"));
        User currentUser = getCurrentLoggedInUser();
        if (currentUser.getRole().getName() == Role.RoleName.HOTEL_MANAGER) {
            boolean isThisHotelManager = hotel.getManagers().stream()
                    .anyMatch(m -> m.equals(getCurrentLoggedInUser()));
            if (!isThisHotelManager) {
                throw new UnauthorizedActionException("You are not authorized to update this hotel");
            }
        }
        if (images.size() != altTexts.size()) {
            throw new InvalidInformationException(
                    "Each image must have an alt text"
            );
        }
        try {
            // check if hotel folder exists
            Path uploadPath = Paths.get(UPLOAD_DIR + "/" + hotel.getId());
            //if not , create one
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            String fileExtenstion;
            for (int i = 0; i < images.size(); i++) {
                String type = images.get(i).getContentType();

                if (type.equals("image/png")){
                    fileExtenstion=".png";
                }else if (type.equals("image/webp")) {
                    fileExtenstion=".webp";
                }else{
                    fileExtenstion=".jpg";
                }

                MultipartFile image = images.get(i);
                String altText = altTexts.get(i);
                //create images naming
                String uniqueId = UUID.randomUUID().toString().substring(0, 5);
                String imageName = uniqueId + fileExtenstion;

                //upload images
                Path imageFilePath = uploadPath.resolve(imageName);
                image.transferTo(imageFilePath);
                String imageURL = imageFilePath.toString();
                HotelImage hotelImage = new HotelImage();
                hotelImage.setImageURL(imageURL);
                hotelImage.setAltText(altText);
                hotelImage.setHotel(hotel);
                hotelImageRepository.save(hotelImage);
            }
            return ResponseEntity.ok(new MessageResponse("Images uploaded successfully"));

        } catch (Exception e) {
            throw new RuntimeException("Failed to upload images", e);
        }
    }

    public ResponseEntity<?> deleteImage(Long hotelId, Long imageId) throws IOException {
        Hotel hotel = hotelRepository.findById(hotelId)
                .orElseThrow(() -> new InformationNotFoundException("Hotel with id " + hotelId + " not found"));
        User currentUser = getCurrentLoggedInUser();
        if (currentUser.getRole().getName() == Role.RoleName.HOTEL_MANAGER) {
            boolean isThisHotelManager = hotel.getManagers().stream()
                    .anyMatch(m -> m.equals(getCurrentLoggedInUser()));
            if (!isThisHotelManager) {
                throw new UnauthorizedActionException("You are not authorized to update this hotel");
            }
        }
        HotelImage image = hotelImageRepository.findById(imageId)
                .orElseThrow(() -> new InformationNotFoundException("Hotel Image with id " + imageId + " not found"));
        if(!image.getHotel().equals(hotel)){
            throw new UnauthorizedActionException("This image does not belong to this hotel");
        }
       try{
           Path filePaths = Paths.get(image.getImageURL());
           Files.deleteIfExists(filePaths);
       }catch (IOException e){
           throw new IOException("Unable to delete image "+e);
       }
        hotelImageRepository.delete(image);
       return ResponseEntity.ok(new MessageResponse("Hotel Image deleted successfully"));
    }
}
