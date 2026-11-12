package com.ga.userservice.event;

import com.ga.userservice.model.User;
import com.ga.userservice.model.UserProfile;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class UserEvent {
    private String eventId;
    private String type;
    private Long userId;
    private String email;
    private String name;
    private String link;
    private LocalDateTime expiresAt;
    private LocalDateTime occurredAt;

    public static UserEvent from(User user, String type, String link, LocalDateTime expiresAt) {
        UserEvent e = new UserEvent();
        e.setEventId(UUID.randomUUID().toString());
        e.setType(type);
        e.setUserId(user.getId());
        e.setEmail(user.getEmail());
        e.setName(displayName(user));
        e.setLink(link);
        e.setExpiresAt(expiresAt);
        e.setOccurredAt(LocalDateTime.now());
        return e;
    }

    private static String displayName(User user) {
        UserProfile profile = user.getUserProfile();
        if (profile != null) {
            String first = profile.getFirstName() == null ? "" : profile.getFirstName();
            String last = profile.getLastName() == null ? "" : profile.getLastName();
            String name = (first + " " + last).trim();
            if (!name.isEmpty()) {
                return name;
            }
        }
        return user.getUsername();
    }
}
