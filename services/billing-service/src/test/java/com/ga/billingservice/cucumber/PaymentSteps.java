package com.ga.billingservice.cucumber;

import com.ga.billingservice.client.BookingServiceClient;
import com.ga.billingservice.client.BookingSummary;
import com.ga.billingservice.model.Payment;
import com.ga.billingservice.repository.PaymentRepository;
import com.ga.billingservice.service.StripeGateway;
import com.stripe.Stripe;
import com.stripe.model.checkout.Session;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class PaymentSteps {

    private static final String CHECKOUT_URL = "https://checkout.stripe.test/pay/cs_test_created";

    private final MockMvc mockMvc;
    private final PaymentRepository paymentRepository;
    private final BookingServiceClient bookingServiceClient;
    private final StripeGateway stripeGateway;
    private final String jwtSecret;
    private final String webhookSecret;

    private String token;
    private ResultActions result;

    public PaymentSteps(MockMvc mockMvc, PaymentRepository paymentRepository,
                        BookingServiceClient bookingServiceClient, StripeGateway stripeGateway,
                        @Value("${jwt-secret}") String jwtSecret,
                        @Value("${stripe.webhook.secret}") String webhookSecret) {
        this.mockMvc = mockMvc;
        this.paymentRepository = paymentRepository;
        this.bookingServiceClient = bookingServiceClient;
        this.stripeGateway = stripeGateway;
        this.jwtSecret = jwtSecret;
        this.webhookSecret = webhookSecret;
    }

    @Before
    public void reset() throws Exception {
        paymentRepository.deleteAll();
        Mockito.reset(bookingServiceClient, stripeGateway);
        token = null;
        Session session = new Session();
        session.setId("cs_test_created");
        session.setUrl(CHECKOUT_URL);
        when(stripeGateway.createCheckoutSession(any())).thenReturn(session);
    }

    @Given("a pending booking {long} that belongs to user {long}")
    public void pendingBooking(Long bookingId, Long ownerId) {
        mockBooking(bookingId, ownerId, "PENDING", LocalDateTime.now().plusMinutes(10));
    }

    @Given("I am logged in as a {word} with id {long}")
    public void loggedInAs(String role, Long userId) {
        token = Jwts.builder()
                .setSubject("user" + userId + "@test.com")
                .claim("userId", userId)
                .claim("role", role)
                .claim("status", "ACTIVE")
                .setExpiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(SignatureAlgorithm.HS256, jwtSecret)
                .compact();
    }

    @Given("a pending payment for booking {long} with session {string}")
    public void pendingPayment(Long bookingId, String sessionId) {
        Payment payment = new Payment();
        payment.setBookingId(bookingId);
        payment.setAmount(new BigDecimal("200.00"));
        payment.setStatus(Payment.STATUS.PENDING);
        payment.setProviderReference(sessionId);
        payment.setIdempotencyKey("key-for-" + sessionId);
        payment.setSessionURL("https://checkout.stripe.test/pay/" + sessionId);
        paymentRepository.save(payment);

        BookingSummary confirmed = new BookingSummary();
        confirmed.setId(bookingId);
        confirmed.setBookingReference("REF-" + bookingId);
        confirmed.setStatus("CONFIRMED");
        when(bookingServiceClient.confirmPayment(bookingId)).thenReturn(Optional.of(confirmed));
    }

    @When("I pay for booking {long} with idempotency key {string}")
    public void payForBooking(Long bookingId, String key) throws Exception {
        var request = post("/api/bookings/{id}/payment", bookingId).header("Idempotency-Key", key);
        if (token != null) {
            request.header("Authorization", "Bearer " + token);
        }
        result = mockMvc.perform(request);
    }

    @When("Stripe sends a completed payment with a wrong signature")
    public void wrongSignature() throws Exception {
        String payload = completedEvent("cs_test_unknown");
        result = mockMvc.perform(post("/webhook")
                .header("Stripe-Signature", "t=" + now() + ",v1=deadbeef")
                .contentType("application/json")
                .content(payload));
    }

    @When("Stripe sends a signed completed payment for session {string}")
    public void signedWebhook(String sessionId) throws Exception {
        String payload = completedEvent(sessionId);
        long timestamp = now();
        String signature = hmac(webhookSecret, timestamp + "." + payload);
        result = mockMvc.perform(post("/webhook")
                .header("Stripe-Signature", "t=" + timestamp + ",v1=" + signature)
                .contentType("application/json")
                .content(payload));
    }

    @Then("the response status should be {int}")
    public void checkStatus(int expected) throws Exception {
        result.andExpect(status().is(expected));
    }

    @Then("the response contains the Stripe checkout link")
    public void containsCheckoutLink() throws Exception {
        result.andExpect(content().string(CHECKOUT_URL));
    }

    @Then("{int} pending payment is saved for booking {long}")
    public void pendingPaymentSaved(int count, Long bookingId) {
        long found = paymentRepository.findAll().stream()
                .filter(p -> p.getBookingId().equals(bookingId) && p.getStatus() == Payment.STATUS.PENDING)
                .count();
        assertThat(found).isEqualTo(count);
    }

    @Then("no payment is saved")
    public void noPaymentSaved() {
        assertThat(paymentRepository.count()).isZero();
    }

    @Then("Stripe was asked to create only one checkout session")
    public void onlyOneCheckoutSession() throws Exception {
        verify(stripeGateway, times(1)).createCheckoutSession(any());
    }

    @Then("the payment for booking {long} is marked as succeeded")
    public void paymentSucceeded(Long bookingId) {
        boolean succeeded = paymentRepository.findAll().stream()
                .anyMatch(p -> p.getBookingId().equals(bookingId) && p.getStatus() == Payment.STATUS.SUCCEED);
        assertThat(succeeded).isTrue();
    }

    @Then("the booking service was asked to confirm booking {long} once")
    public void confirmedOnce(Long bookingId) {
        verify(bookingServiceClient, times(1)).confirmPayment(eq(bookingId));
    }

    private void mockBooking(Long bookingId, Long ownerId, String status, LocalDateTime holdExpiresAt) {
        BookingSummary booking = new BookingSummary();
        booking.setId(bookingId);
        booking.setBookingReference("REF-" + bookingId);
        booking.setUserId(ownerId);
        booking.setStatus(status);
        booking.setHoldExpiresAt(holdExpiresAt);
        booking.setTotalAmount(new BigDecimal("200.00"));
        when(bookingServiceClient.getBooking(eq(bookingId), any())).thenReturn(Optional.of(booking));
    }

    private static long now() {
        return System.currentTimeMillis() / 1000;
    }

    private static String completedEvent(String sessionId) {
        return "{\"id\":\"evt_test_1\",\"object\":\"event\",\"api_version\":\"" + Stripe.API_VERSION + "\","
                + "\"type\":\"checkout.session.completed\","
                + "\"data\":{\"object\":{\"id\":\"" + sessionId + "\",\"object\":\"checkout.session\","
                + "\"payment_status\":\"paid\",\"payment_intent\":\"pi_test_1\","
                + "\"customer_email\":\"customer@test.com\"}}}";
    }

    private static String hmac(String key, String message) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] raw = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
        StringBuilder hex = new StringBuilder();
        for (byte b : raw) {
            hex.append(String.format("%02x", b));
        }
        return hex.toString();
    }
}
