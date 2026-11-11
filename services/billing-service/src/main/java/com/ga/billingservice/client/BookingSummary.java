package com.ga.billingservice.client;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BookingSummary {
    private Long id;
    private String bookingReference;
    private Long hotelId;
    private Long userId;
    private String hotelName;
    private String status;
    private LocalDateTime holdExpiresAt;
    private BigDecimal totalAmount;
}
