package com.ga.hotel_booking_app.service;

import com.ga.hotel_booking_app.dto.response.MessageResponse;
import com.ga.hotel_booking_app.exception.custom.InformationNotFoundException;
import com.ga.hotel_booking_app.model.Booking;
import com.ga.hotel_booking_app.model.Payment;
import com.ga.hotel_booking_app.model.User;
import com.ga.hotel_booking_app.repository.BookingRepository;
import com.ga.hotel_booking_app.repository.PaymentRepository;
import com.ga.hotel_booking_app.repository.UserRepository;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.PaymentIntent;
import com.stripe.net.Webhook;
import com.stripe.param.CustomerCreateParams;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;

import java.math.BigDecimal;
import java.util.Optional;

@Service
public class PaymentService {
    @Autowired
    private BookingRepository bookingRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PaymentRepository paymentRepository;
    @Value("${stripe.webhook.secret}")
    private String endpointSecret;

    private static final Logger logger =
            LoggerFactory.getLogger(PaymentService.class);

    public User getCurrentLoggedInUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        return userRepository.findUserByEmail(email);
    }
    public ResponseEntity<?> initiatePayment(Long id, String idempotencyKey) {
        Booking booking = bookingRepository.findById(id).orElseThrow(() -> new InformationNotFoundException("Booking with id "+id+ " not found"));
    User user = getCurrentLoggedInUser();
    try {
        Optional<Payment> paymentExists = paymentRepository.findByIdempotencyKey(idempotencyKey);
        if(paymentExists.isPresent()){
            logger.info("Payment session for booking {} already exists", booking.getId());
            return ResponseEntity.ok(paymentExists.get().getSessionURL());
        }
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

        savePayment(session, booking, idempotencyKey);
        logger.info("Payment session for booking {} has been created", booking.getId());
        return ResponseEntity.ok(session.getUrl());
    } catch (StripeException e) {
        throw new RuntimeException(e);
    }


    }

    public Payment savePayment(Session session, Booking booking, String idempotencyKey){
        Payment payment = new Payment();
        payment.setBooking(booking);
        payment.setStatus(Payment.STATUS.PENDING);
        payment.setAmount(booking.getTotalAmount());
        payment.setMethod(null);
        payment.setIdempotencyKey(idempotencyKey);
        payment.setProviderReference(session.getId());
        payment.setSessionURL(session.getUrl());
        paymentRepository.save(payment);
        return payment;
    }


    public ResponseEntity<?> handleStripeWebhook(String payload, String sigHeader) {

        Event event;
        try{
        event = Webhook.constructEvent(payload, sigHeader, endpointSecret);
        }   catch (SignatureVerificationException e) {
        // Invalid signature
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid signature");
    } catch (Exception e) {
        // global parsing issue
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid payload");
    }
        EventDataObjectDeserializer ObjectDeserializer = event.getDataObjectDeserializer();


        switch (event.getType()){
            case "payment_intent.succeeded":
                if (ObjectDeserializer.getObject().isPresent()) {
                    PaymentIntent paymentIntent = (PaymentIntent) ObjectDeserializer.getObject().get();
                    System.out.println("Payment success for Intent: " + paymentIntent.getId());
                }
                break;

            case "payment_intent.payment_failed":
                if (ObjectDeserializer.getObject().isPresent()) {
                    PaymentIntent paymentIntent = (PaymentIntent) ObjectDeserializer.getObject().get();
                    System.out.println("Payment failed for Intent: " + paymentIntent.getId());
                }
                break;

            default:
                System.out.println("Unhandled event type: " + event.getType());
                break;
        }

        return ResponseEntity.ok(new MessageResponse("Suceess"));
    }
}
