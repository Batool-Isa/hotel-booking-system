package com.ga.bookingservice.cucumber;

import com.ga.bookingservice.client.HotelServiceClient;
import com.ga.bookingservice.client.HotelSummary;
import com.ga.bookingservice.model.Booking;
import com.ga.bookingservice.repository.BookingRepository;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class BookingSteps {

    private final BookingRepository bookingRepository;
    private final MockMvc mockMvc;
    private final HotelServiceClient hotelServiceClient;
    private final String jwtSecret;

    private ResultActions result;
    private Long bookingId;
    private String token;

    public BookingSteps(BookingRepository bookingRepository, MockMvc mockMvc,
                        HotelServiceClient hotelServiceClient,
                        @Value("${jwt-secret}") String jwtSecret) {
        this.bookingRepository = bookingRepository;
        this.mockMvc = mockMvc;
        this.hotelServiceClient = hotelServiceClient;
        this.jwtSecret = jwtSecret;
    }

    @Before
    public void cleanDb() {

        bookingRepository.deleteAll();
    }

    @Then("the application has started with an empty booking table")
    public void emptyBookingTable() {
        assertThat(bookingRepository.count()).isZero();
    }

    @When("I request booking with id {long} without a token")
    public void requestBookingWithoutToken(Long id) throws Exception {
        result = mockMvc.perform(get("/api/bookings/{id}", id));
    }

    @Given("a booking exists that belongs to user {long}")
    public void bookingExistsForUser(Long ownerId) {
        Booking booking = new Booking();
        booking.setBookingReference("REF-TEST-1");
        booking.setCheckIn(LocalDate.now().plusDays(5));
        booking.setCheckOut(LocalDate.now().plusDays(7));
        booking.setAdults(2);
        booking.setChildren(0);
        booking.setTotalAmount(new BigDecimal("200.00"));
        booking.setHotelId(1L);
        booking.setUserId(ownerId);
        bookingId = bookingRepository.save(booking).getId();
        HotelSummary hotel = new HotelSummary();
        hotel.setId(1L);
        hotel.setName("Test Hotel");
        when(hotelServiceClient.getHotelSummary(1L)).thenReturn(hotel);
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

    @When("I request that booking")
    public void requestThatBooking() throws Exception {
        result = mockMvc.perform(get("/api/bookings/{id}", bookingId)
                .header("Authorization", "Bearer " + token));
    }
    @Then("the response status should be {int}")
    public void checkStatus(int expected) throws Exception {
        result.andExpect(status().is(expected));
    }
}
