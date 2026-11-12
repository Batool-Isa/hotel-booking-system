package com.ga.userservice.controller;

import com.ga.userservice.exception.custom.UnauthorizedActionException;
import com.ga.userservice.service.UserService;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Hidden
@RestController
@RequestMapping("/internal/users")
public class InternalUserController {
    @Autowired
    private UserService userService;

    @Value("${internal.api-key}")
    private String internalApiKey;

    @GetMapping("/{id}")
    public ResponseEntity<?> getUserSummary(@PathVariable("id") Long id,
                                            @RequestHeader(value = "X-Internal-Key", required = false) String key) {
        if (key == null || !MessageDigest.isEqual(
                key.getBytes(StandardCharsets.UTF_8), internalApiKey.getBytes(StandardCharsets.UTF_8))) {
            throw new UnauthorizedActionException("Only internal services can call this endpoint");
        }
        return userService.getUserSummary(id);
    }
}
