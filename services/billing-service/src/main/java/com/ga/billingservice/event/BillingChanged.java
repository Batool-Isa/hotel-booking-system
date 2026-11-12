package com.ga.billingservice.event;

import com.ga.billingservice.client.BookingSummary;
import com.ga.billingservice.model.Payment;

public record BillingChanged(Payment payment, BookingSummary booking, String type, String customerEmail) {
}
