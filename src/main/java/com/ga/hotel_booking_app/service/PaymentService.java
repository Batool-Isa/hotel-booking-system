package com.ga.hotel_booking_app.service;

import com.ga.hotel_booking_app.exception.custom.InformationNotFoundException;
import com.ga.hotel_booking_app.model.Booking;
import com.ga.hotel_booking_app.model.User;
import com.ga.hotel_booking_app.repository.BookingRepository;
import com.ga.hotel_booking_app.repository.UserRepository;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.param.CustomerCreateParams;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;

import java.math.BigDecimal;

@Service
public class PaymentService {
    @Autowired
    private BookingRepository bookingRepository;
    @Autowired

    private UserRepository userRepository;

    private static final Logger logger =
            LoggerFactory.getLogger(BookingService.class);

    public User getCurrentLoggedInUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        return userRepository.findUserByEmail(email);
    }
    public ResponseEntity<?> initiatePayment(Long id) {
        Booking booking = bookingRepository.findById(id).orElseThrow(() -> new InformationNotFoundException("Booking with id "+id+ " not found"));
    User user = getCurrentLoggedInUser();
    try {
        CustomerCreateParams customerCreateParams = new CustomerCreateParams.Builder().setName(user.getUsername()).setEmail(user.getEmail()).build();
        Customer customer = Customer.create(customerCreateParams);
        // convert amount from big decimal to long
        Long amountInCent = booking.getTotalAmount().multiply(new BigDecimal("100")).longValue();
        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setCancelUrl(customer.getEmail())
                .setSuccessUrl("http://localhost:8000/success.html")
                .setCancelUrl("http://localhost:8000/cancel.html")
                .addLineItem(SessionCreateParams.LineItem.builder().setQuantity(1L)
                        .setPriceData(
                                SessionCreateParams.LineItem.PriceData.builder()
                                        .setCurrency("usd")
                                        .setUnitAmount(amountInCent)
                                        .setProductData(
                                                SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                        .setName(booking.getBookingReference())
                                                        .build()
                                        )
                                        .build()
                        )
                        .build()
                )
                .build();
        com.stripe.model.checkout.Session session = Session.create(params);
        logger.info("Payment session for booking {} has been created", booking.getId());
        return ResponseEntity.ok(session.getUrl());
    } catch (StripeException e) {
        throw new RuntimeException(e);
    }


    }
}
