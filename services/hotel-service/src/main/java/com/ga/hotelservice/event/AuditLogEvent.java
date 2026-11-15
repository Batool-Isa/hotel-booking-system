package com.ga.hotelservice.event;

import lombok.*;

import java.time.LocalDateTime;


@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogEvent {
    private String eventId;
    // source service who have the event like hotel service ... etc
    private String sourceService;
    private Long userId;
    private String action;
    private String entityType;
    private Long entityId;
    private String description;
    private LocalDateTime occurredAt;
}
