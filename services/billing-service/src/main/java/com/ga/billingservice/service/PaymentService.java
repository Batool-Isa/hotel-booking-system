package com.ga.billingservice.service;

import com.ga.bookingservice.dto.response.MessageResponse;
import com.ga.bookingservice.exception.custom.InformationNotFoundException;
import com.ga.bookingservice.exception.custom.InvalidInformationException;
import com.ga.bookingservice.exception.custom.UnauthorizedActionException;
import com.ga.bookingservice.model.Booking;
import com.ga.bookingservice.model.Payment;
import com.ga.bookingservice.model.User;
import com.ga.bookingservice.repository.BookingRepository;
import com.ga.bookingservice.repository.PaymentRepository;
import com.ga.bookingservice.repository.UserRepository;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.net.Webhook;

import jakarta.transaction.Transactional;

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
import java.time.LocalDateTime;
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
    @Autowired
    private EmailService emailService;
    @Autowired
    private NotificationService notificationService;

    @Autowired
    private StripeGateway stripeGateway;


    private static final Logger logger =
            LoggerFactory.getLogger(PaymentService.class);

    public User getCurrentLoggedInUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        return userRepository.findUserByEmail(email);
    }

    public ResponseEntity<?> initiatePayment(Long id, String idempotencyKey) {
        Booking booking = bookingRepository.findById(id).orElseThrow(() -> new InformationNotFoundException("Booking with id " + id + " not found"));
        User user = getCurrentLoggedInUser();
        // only customer who owned booking should be able to pay it
        if (!booking.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedActionException("Customer " + user.getId() + " not authorized to pay for this booking");
        }

        // validate booking status as only pending and in hold time can be paid
        if (booking.getStatus() != (Booking.Status.PENDING) ||
                booking.getHoldExpiresAt() == null ||
                booking.getHoldExpiresAt().isBefore(LocalDateTime.now())) {
            throw new InvalidInformationException("This booking can no longer be paid. Please book again.");
        }
        try {
            Optional<Payment> paymentExists = paymentRepository.findByIdempotencyKey(idempotencyKey);
            if (paymentExists.isPresent()) {
                Payment existing = paymentExists.get();
                if (!existing.getBooking().getId().equals(booking.getId())) {
                    throw new InvalidInformationException("This idempotency key was already used for another booking");
                }
                logger.info("Payment session for booking {} already exists", booking.getId());
                return ResponseEntity.ok(existing.getSessionURL());
            }

            // convert amount from big decimal to long
            Long amountInCent = booking.getTotalAmount().multiply(new BigDecimal("100")).longValue();
            SessionCreateParams params = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    .setCustomerEmail(user.getEmail())
                    .setSuccessUrl("http://localhost:5173/payment/success?bookingId=" + booking.getId())
                    .setCancelUrl("http://localhost:5173/payment/cancel?bookingId=" + booking.getId())
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

            Session session = stripeGateway.createCheckoutSession(params);

            savePayment(session, booking, idempotencyKey);
            logger.info("Payment session for booking {} has been created", booking.getId());
            return ResponseEntity.ok(session.getUrl());
        } catch (StripeException e) {
            throw new RuntimeException(e);
        }


    }

    public void savePayment(Session session, Booking booking, String idempotencyKey) {
        Payment payment = new Payment();
        payment.setBooking(booking);
        payment.setStatus(Payment.STATUS.PENDING);
        payment.setAmount(booking.getTotalAmount());
        payment.setMethod(null);
        payment.setIdempotencyKey(idempotencyKey);
        payment.setProviderReference(session.getId());
        payment.setSessionURL(session.getUrl());
        paymentRepository.save(payment);
    }


    @Transactional
    public ResponseEntity<?> handleStripeWebhook(String payload, String sigHeader) {

        Event event;
        try {
            event = Webhook.constructEvent(payload, sigHeader, endpointSecret);
        } catch (SignatureVerificationException e) {
            // Invalid signature
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid signature");
        } catch (Exception e) {
            // global parsing issue
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid payload");
        }
        EventDataObjectDeserializer ObjectDeserializer = event.getDataObjectDeserializer();


        if (event.getType().equals("checkout.session.completed")) {
            Session session = null;
            if (ObjectDeserializer.getObject().isPresent()) {
                session = (Session) ObjectDeserializer.getObject().get();
            } else {
                try {
                    session = (Session) ObjectDeserializer.deserializeUnsafe();
                } catch (Exception e) {
                    logger.error("Could not read Stripe session from event {}", event.getId());
                }
            }
            if (session != null) {
                confirmPayment(session);
            }
        }

        return ResponseEntity.ok(new MessageResponse("Suceess"));
    }

    private void confirmPayment(Session session) {
        Payment payment = paymentRepository.findByProviderReference(session.getId());
        if (payment == null) {
            logger.warn("Payment does not exists with session {}", session.getId());
            return;
        }

        if (!"paid".equals(session.getPaymentStatus())) {
            logger.warn("Booking is not paid");
            return;
        }
        Booking booking = bookingRepository.findByIdForUpdate(payment.getBooking().getId())
                .orElseThrow(() -> new InformationNotFoundException("Booking for payment " + payment.getId() + " not found"));
        if (booking.getStatus() == Booking.Status.CONFIRMED) {
            logger.info("Booking {} is already confirmed, ignoring duplicate webhook", booking.getId());
            return;
        }
        payment.setStatus(Payment.STATUS.SUCCEED);
        payment.setPaymentIntent(session.getPaymentIntent());
        paymentRepository.save(payment);
        if (booking.getStatus() == Booking.Status.PENDING
                && booking.getHoldExpiresAt() != null
                && booking.getHoldExpiresAt().isAfter(LocalDateTime.now())) {
            booking.setStatus(Booking.Status.CONFIRMED);
            bookingRepository.save(booking);
            logger.info("Booking {} is confirmed after payment paid", booking.getId());
            notificationService.sendBookingNotification(booking);
            emailService.sendBookingConfirmationEmail(booking);
        } else {
            logger.warn("Payment {} succeeded but booking {} is {}",
                    payment.getId(), booking.getBookingReference(), booking.getStatus());

        }

    }
}
