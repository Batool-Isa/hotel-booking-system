package com.ga.bookingservice.controller;

import com.ga.bookingservice.dto.response.BookingStatsResponse;
import com.ga.bookingservice.exception.custom.UnauthorizedActionException;
import com.ga.bookingservice.service.BookingStatsService;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Hidden
@RestController
@RequestMapping("/internal/stats")
public class InternalStatsController {
    @Autowired
    private BookingStatsService bookingStatsService;

    @Value("${internal.api-key}")
    private String internalApiKey;

    @GetMapping
    public BookingStatsResponse getStats(@RequestHeader(value = "X-Internal-Key", required = false) String key) {
        if (key == null || !MessageDigest.isEqual(
                key.getBytes(StandardCharsets.UTF_8), internalApiKey.getBytes(StandardCharsets.UTF_8))) {
            throw new UnauthorizedActionException("Only internal services can call this endpoint");
        }
        return bookingStatsService.getStats();
    }
}
