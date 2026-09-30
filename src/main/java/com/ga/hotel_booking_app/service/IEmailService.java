package com.ga.hotel_booking_app.service;


import com.ga.hotel_booking_app.dto.EmailDetails;

public interface IEmailService {
    // Method to send simple email
    String sendSimpleMail(EmailDetails details);

    // Method to send email with attachment
    String sendMailWithAttachment(EmailDetails details);
}
