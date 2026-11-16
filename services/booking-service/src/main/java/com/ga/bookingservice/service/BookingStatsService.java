package com.ga.bookingservice.service;

import com.ga.bookingservice.client.HotelServiceClient;
import com.ga.bookingservice.dto.response.BookingStatsResponse;
import com.ga.bookingservice.model.Booking;
import com.ga.bookingservice.repository.BookingRepository;
import com.ga.bookingservice.repository.ReviewRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class BookingStatsService {
    private static final List<Booking.Status> PAID = List.of(Booking.Status.CONFIRMED, Booking.Status.COMPLETED);
    private static final int DAYS = 30;

    @Autowired
    private BookingRepository bookingRepository;
    @Autowired
    private ReviewRepository reviewRepository;
    @Autowired
    private HotelServiceClient hotelServiceClient;

    @Transactional(readOnly = true)
    public BookingStatsResponse getStats() {
        LocalDate today = LocalDate.now();
        LocalDateTime thisMonth = today.withDayOfMonth(1).atStartOfDay();
        LocalDateTime nextMonth = thisMonth.plusMonths(1);
        LocalDateTime lastMonth = thisMonth.minusMonths(1);
        LocalDateTime lastMonthSamePoint = lastMonth.plus(Duration.between(thisMonth, LocalDateTime.now()));
        if (lastMonthSamePoint.isAfter(thisMonth)) {
            lastMonthSamePoint = thisMonth;
        }

        BookingStatsResponse r = new BookingStatsResponse();

        Map<String, Long> bookingsByStatus = new LinkedHashMap<>();
        for (Booking.Status s : Booking.Status.values()) {
            bookingsByStatus.put(s.name(), 0L);
        }
        for (Object[] row : bookingRepository.countGroupByStatus()) {
            bookingsByStatus.put(((Booking.Status) row[0]).name(), (Long) row[1]);
        }
        long bookingsTotal = bookingsByStatus.values().stream().mapToLong(Long::longValue).sum();
        long paidCount = bookingsByStatus.get(Booking.Status.CONFIRMED.name())
                + bookingsByStatus.get(Booking.Status.COMPLETED.name());
        BigDecimal revenueTotal = bookingRepository.sumRevenue(PAID);

        r.setBookingsByStatus(bookingsByStatus);
        r.setBookingsTotal(bookingsTotal);
        r.setBookingsThisMonth(bookingRepository.countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(thisMonth, nextMonth));
        r.setBookingsLastMonth(bookingRepository.countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(lastMonth, lastMonthSamePoint));
        r.setCancellationRate(bookingsTotal == 0 ? 0
                : Math.round(bookingsByStatus.get(Booking.Status.CANCELLED.name()) * 1000.0 / bookingsTotal) / 10.0);
        r.setRevenueTotal(revenueTotal);
        r.setRevenueThisMonth(bookingRepository.sumRevenueBetween(PAID, thisMonth, nextMonth));
        r.setRevenueLastMonth(bookingRepository.sumRevenueBetween(PAID, lastMonth, lastMonthSamePoint));
        r.setAverageBookingValue(paidCount == 0 ? BigDecimal.ZERO
                : revenueTotal.divide(BigDecimal.valueOf(paidCount), 2, RoundingMode.HALF_UP));

        Double avg = reviewRepository.averageRating();
        r.setReviewsTotal(reviewRepository.count());
        r.setAverageRating(avg == null ? 0 : Math.round(avg * 10) / 10.0);

        LocalDate from = today.minusDays(DAYS - 1);
        Map<LocalDate, Object[]> byDay = new HashMap<>();
        for (Object[] row : bookingRepository.dailyTotalsSince(PAID, from.atStartOfDay())) {
            byDay.put((LocalDate) row[0], row);
        }
        List<BookingStatsResponse.DailyStat> daily = new ArrayList<>();
        for (int i = 0; i < DAYS; i++) {
            LocalDate d = from.plusDays(i);
            Object[] row = byDay.get(d);
            daily.add(row == null
                    ? new BookingStatsResponse.DailyStat(d, 0, BigDecimal.ZERO)
                    : new BookingStatsResponse.DailyStat(d, (Long) row[1], (BigDecimal) row[2]));
        }
        r.setDaily(daily);

        List<BookingStatsResponse.TopHotel> top = new ArrayList<>();
        for (Object[] row : bookingRepository.topHotelsByRevenue(PAID, PageRequest.of(0, 5))) {
            Long hotelId = (Long) row[0];
            top.add(new BookingStatsResponse.TopHotel(hotelId, hotelName(hotelId), (Long) row[1], (BigDecimal) row[2]));
        }
        r.setTopHotels(top);
        return r;
    }

    private String hotelName(Long hotelId) {
        try {
            return hotelServiceClient.getHotelSummary(hotelId).getName();
        } catch (RuntimeException e) {
            return "Hotel #" + hotelId;
        }
    }
}
