package com.ga.hotel_booking_app.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

        @Component
        public class RateLimitFilter extends OncePerRequestFilter {

            private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

            private Bucket createBucket(int capacity, Duration refillDuration) {

                Refill refill = Refill.intervally(capacity, refillDuration);

                Bandwidth limit = Bandwidth.classic(capacity, refill);

                return Bucket.builder().addLimit(limit).build();
            }

            private Bucket getBucket(String key, int capacity, Duration duration) {
                return buckets.computeIfAbsent(
                        key, k -> createBucket(capacity, duration)
                );
            }

            @Override
            protected void doFilterInternal(
                    HttpServletRequest request,
                    HttpServletResponse response,
                    FilterChain filterChain)
                    throws ServletException, IOException {

                String path = request.getRequestURI();

                String ipAddress = request.getRemoteAddr();

                Bucket bucket = null;

                if (path.equals("/auth/users/login") && request.getMethod().equals("POST")) {

                    bucket = getBucket(
                            "login:" + ipAddress,
                            5,
                            Duration.ofMinutes(1)
                    );

                } else if (path.equals("/auth/users/register")
                        && request.getMethod().equals("POST")) {

                    bucket = getBucket("register:" + ipAddress,
                            5, Duration.ofMinutes(1)
                    );

                } else if (path.equals("/auth/users/forgot-password")
                        && request.getMethod().equals("POST")) {

                    bucket = getBucket(
                            "forgot-password:" + ipAddress, 3, Duration.ofMinutes(1)
                    );
                }

                if (bucket != null && !bucket.tryConsume(1)) {
                    response.setStatus(429);
                    response.setContentType("application/json");
                    response.getWriter().write("""
                    { "message": "Too many requests. Please try again later. }
                    """);
                    return;
                }
                filterChain.doFilter(request, response);
            }
        }