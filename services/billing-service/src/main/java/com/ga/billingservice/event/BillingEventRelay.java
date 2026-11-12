package com.ga.billingservice.event;

import com.ga.billingservice.kafka.BillingEventProducer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class BillingEventRelay {
    private final BillingEventProducer producer;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onBillingChanged(BillingChanged changed) {
        producer.publish(changed.payment(), changed.booking(), changed.type(), changed.customerEmail());
    }
}
