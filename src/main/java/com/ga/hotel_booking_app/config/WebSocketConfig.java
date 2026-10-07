package com.ga.hotel_booking_app.config;

import com.ga.hotel_booking_app.model.Hotel;
import com.ga.hotel_booking_app.model.Role;
import com.ga.hotel_booking_app.model.User;
import com.ga.hotel_booking_app.repository.HotelRepository;
import com.ga.hotel_booking_app.security.JwtUtils;
import com.ga.hotel_booking_app.security.MyUserDetails;
import com.ga.hotel_booking_app.security.MyUserDetailsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.security.Principal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    private static final Logger log = LoggerFactory.getLogger(WebSocketConfig.class);
    private static final Pattern HOTEL_TOPIC = Pattern.compile("^/topic/hotels/(\\d+)/bookings$");
    @Autowired
    @Lazy
    private JwtUtils jwtUtils;
    @Autowired
    @Lazy
    private MyUserDetailsService myUserDetailsService;
    @Autowired
    @Lazy
    private HotelRepository hotelRepository;
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic");
        config.setApplicationDestinationPrefixes("/app");
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*");
    }
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                if (accessor == null || accessor.getCommand() == null) {
                    return message;
                }
                if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                    accessor.setUser(authenticate(accessor.getFirstNativeHeader("Authorization")));
                } else if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
                    checkSubscription(accessor.getUser(), accessor.getDestination());
                } else if (StompCommand.SEND.equals(accessor.getCommand())) {
                    // clients never publish: the server is the only sender
                    throw new MessagingException("Sending messages is not allowed");
                }
                return message;
            }
        });
    }

    private Principal authenticate(String header) {
        if (header == null || !header.startsWith("Bearer ")) {
            throw new MessagingException("Missing token");
        }
        try {
            String token = header.substring(7);
            if (!jwtUtils.validateJwtToken(token)) {
                throw new MessagingException("Invalid token");
            }
            String email = jwtUtils.getUserEmailFromJwtToken(token);
            MyUserDetails details = (MyUserDetails) myUserDetailsService.loadUserByUsername(email);
            if (details.getUser().getStatus() != User.Status.ACTIVE) {
                throw new MessagingException("Account is not active");
            }
            return new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities());
        } catch (MessagingException e) {
            throw e;
        } catch (Exception e) {
            log.warn("WebSocket connection refused: {}", e.getMessage());
            throw new MessagingException("Invalid token");
        }
    }

    private void checkSubscription(Principal principal, String destination) {
        if (!(principal instanceof UsernamePasswordAuthenticationToken auth)
                || !(auth.getPrincipal() instanceof MyUserDetails details) || destination == null) {
            throw new MessagingException("Not authenticated");
        }
        User user = details.getUser();
        Role.RoleName role = user.getRole().getName();

        if (destination.equals("/user/queue/bookings")) {
            return; // always the caller's own queue
        }
        if (destination.equals("/topic/admin/bookings") && role == Role.RoleName.ADMIN) {
            return;
        }
        Matcher m = HOTEL_TOPIC.matcher(destination);
        if (m.matches()) {
            if (role == Role.RoleName.ADMIN) {
                return;
            }
            if (role == Role.RoleName.HOTEL_MANAGER) {
                Long hotelId = Long.valueOf(m.group(1));
                boolean manages = hotelRepository.findByManagers_Id(user.getId()).stream()
                        .map(Hotel::getId).anyMatch(hotelId::equals);
                if (manages) {
                    return;
                }
            }
        }
        throw new MessagingException("You are not allowed to subscribe to " + destination);
    }

}