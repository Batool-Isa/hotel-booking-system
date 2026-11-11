package com.ga.billingservice.service;
import com.ga.billingservice.client.BookingServiceClient;
import com.ga.billingservice.client.BookingSummary;
import com.ga.billingservice.dto.response.MessageResponse;
import com.ga.billingservice.exception.custom.InformationNotFoundException;
import com.ga.billingservice.exception.custom.InvalidInformationException;
import com.ga.billingservice.exception.custom.ServiceUnavailableException;
import com.ga.billingservice.exception.custom.UnauthorizedActionException;
import com.ga.billingservice.model.Payment;
import com.ga.billingservice.repository.PaymentRepository;
import com.ga.billingservice.security.AuthUser;
import com.ga.billingservice.security.CurrentUser;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.checkout.SessionCreateParams;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class PaymentService {
    @Autowired
    private BookingServiceClient bookingServiceClient;
    @Autowired
    private CurrentUser currentUser;
    @Autowired
    private PaymentRepository paymentRepository;
    @Autowired
    private StripeGateway stripeGateway;
    @Value("${stripe.webhook.secret}")
    private String endpointSecret;
    @Value("${frontend.url}")
    private String frontendUrl;

    private static final Logger logger = LoggerFactory.getLogger(PaymentService.class);


    public ResponseEntity<?> initiatePayment(Long id, String idempotencyKey) {
        AuthUser user = currentUser.get();
        if (user == null) {
            throw new UnauthorizedActionException("Please sign in to pay for a booking");
        }
        BookingSummary booking = bookingServiceClient.getBooking(id, currentUser.token())
                .orElseThrow(() -> new InformationNotFoundException("Booking with id " + id + " not found"));

        if (!user.getId().equals(booking.getUserId())) {
            throw new UnauthorizedActionException("Customer " + user.getId() + " not authorized to pay for this booking");
        }

        if (!"PENDING".equals(booking.getStatus()) ||
                booking.getHoldExpiresAt() == null ||
                booking.getHoldExpiresAt().isBefore(LocalDateTime.now())) {
            throw new InvalidInformationException("This booking can no longer be paid. Please book again.");
        }

        Optional<Payment> paymentExists = paymentRepository.findByIdempotencyKey(idempotencyKey);
        if (paymentExists.isPresent()) {
            Payment existing = paymentExists.get();
            if (!existing.getBookingId().equals(booking.getId())) {
                throw new InvalidInformationException("This idempotency key was already used for another booking");
            }
            logger.info("Payment session for booking {} already exists", booking.getId());
            return ResponseEntity.ok(existing.getSessionURL());
        }

        try {
            long amountInCent = booking.getTotalAmount().multiply(new BigDecimal("100")).longValue();
            SessionCreateParams params = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    .setCustomerEmail(user.getEmail())
                    .setSuccessUrl(frontendUrl + "/payment/success?bookingId=" + booking.getId())
                    .setCancelUrl(frontendUrl + "/payment/cancel?bookingId=" + booking.getId())
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
            logger.error("Stripe could not create a session for booking {}: {}", booking.getId(), e.getMessage());
            throw new ServiceUnavailableException("Payment provider is not available right now. Please try again.");
        }
    }

    public void savePayment(Session session, BookingSummary booking, String idempotencyKey) {
        Payment payment = new Payment();
        payment.setBookingId(booking.getId());
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
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid signature");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid payload");
        }
        EventDataObjectDeserializer objectDeserializer = event.getDataObjectDeserializer();

        if ("checkout.session.completed".equals(event.getType())) {
            Session session = null;
            if (objectDeserializer.getObject().isPresent()) {
                session = (Session) objectDeserializer.getObject().get();
            } else {
                try {
                    session = (Session) objectDeserializer.deserializeUnsafe();
                } catch (Exception e) {
                    logger.error("Could not read Stripe session from event {}", event.getId());
                }
            }
            if (session != null) {
                confirmPayment(session);
            }
        }

        return ResponseEntity.ok(new MessageResponse("Success"));
    }

    private void confirmPayment(Session session) {
        Payment payment = paymentRepository.findByProviderReference(session.getId());
        if (payment == null) {
            logger.warn("Payment does not exists with session {}", session.getId());
            return;
        }
        if (payment.getStatus() == Payment.STATUS.SUCCEED) {
            logger.info("Payment {} is already confirmed, ignoring duplicate webhook", payment.getId());
            return;
        }
        if (!"paid".equals(session.getPaymentStatus())) {
            logger.warn("Booking {} is not paid", payment.getBookingId());
            return;
        }
        BookingSummary booking = bookingServiceClient.confirmPayment(payment.getBookingId())
                .orElseThrow(() -> new InformationNotFoundException(
                        "Booking for payment " + payment.getId() + " not found"));

        payment.setStatus(Payment.STATUS.SUCCEED);
        payment.setPaymentIntent(session.getPaymentIntent());
        paymentRepository.save(payment);

        if ("CONFIRMED".equals(booking.getStatus())) {
            logger.info("Booking {} is confirmed after payment paid", booking.getId());
        } else {
            logger.warn("Payment {} succeeded but booking {} is {}",
                    payment.getId(), booking.getBookingReference(), booking.getStatus());
        }
    }
}
