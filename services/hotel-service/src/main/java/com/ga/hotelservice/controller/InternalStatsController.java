package com.ga.hotelservice.controller;

import com.ga.hotelservice.dto.response.HotelStatsResponse;
import com.ga.hotelservice.exception.custom.UnauthorizedActionException;
import com.ga.hotelservice.model.Hotel;
import com.ga.hotelservice.repository.HotelRepository;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.LinkedHashMap;
import java.util.Map;

@Hidden
@RestController
@RequestMapping("/internal/stats")
public class InternalStatsController {
    @Autowired
    private HotelRepository hotelRepository;

    @Value("${internal.api-key}")
    private String internalApiKey;

    @GetMapping
    public HotelStatsResponse getStats(@RequestHeader(value = "X-Internal-Key", required = false) String key) {
        if (key == null || !MessageDigest.isEqual(
                key.getBytes(StandardCharsets.UTF_8), internalApiKey.getBytes(StandardCharsets.UTF_8))) {
            throw new UnauthorizedActionException("Only internal services can call this endpoint");
        }
        Map<String, Long> hotelsByStatus = new LinkedHashMap<>();
        for (Hotel.Status s : Hotel.Status.values()) {
            hotelsByStatus.put(s.name(), 0L);
        }
        for (Object[] row : hotelRepository.countGroupByStatus()) {
            hotelsByStatus.put(((Hotel.Status) row[0]).name(), (Long) row[1]);
        }
        long total = hotelsByStatus.values().stream().mapToLong(Long::longValue).sum();
        return new HotelStatsResponse(total, hotelsByStatus);
    }
}
