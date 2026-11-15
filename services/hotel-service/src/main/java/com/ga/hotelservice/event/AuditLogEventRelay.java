package com.ga.hotelservice.event;

import com.ga.hotelservice.kafka.AuditLogEventProducer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class AuditLogEventRelay {
    private final AuditLogEventProducer producer;
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onAuditLogEvent(AuditLogEvent event) {
        producer.publish(event);
    }


}
