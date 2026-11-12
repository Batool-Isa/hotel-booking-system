package com.ga.notificationservice.service;

import com.ga.notificationservice.dto.EmailDetails;
import com.ga.notificationservice.event.BookingEvent;
import com.ga.notificationservice.event.UserEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;


@Service
public class EmailService {
    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    @Autowired
    MailSenderService mailSenderService;
    @Autowired
    HotelMapService hotelMapService;

    public String buildActionEmailBody(
            String title,
            String message,
            String buttonText,
            String link,
            String expiryMessage
    ) {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>VibeStay - {{TITLE}}</title>
                </head>
                
                <body style="margin: 0; padding: 0; background-color: #f2f4f8; font-family: Arial, Helvetica, sans-serif; color: #2d2d3a;">
                
                    <div style="width: 100%; padding: 35px 0;">
                
                        <div style="max-width: 620px; margin: 0 auto; background-color: #ffffff; border-radius: 10px; overflow: hidden;">
                
                            <div style="background-color: #172554; padding: 28px 30px;">
                                <div style="font-size: 25px; font-weight: bold; color: #ffffff;">
                                    VibeStay
                                </div>
                                <div style="margin-top: 5px; font-size: 13px; color: #cbd5e1;">
                                    Your stay starts here
                                </div>
                            </div>
                
                            <div style="padding: 35px 32px;">
                
                                <h1 style="margin: 0 0 12px 0; font-size: 28px; color: #172554;">
                                    {{TITLE}}
                                </h1>
                
                                <div style="height: 1px; background-color: #e5e7eb; margin-bottom: 24px;"></div>
                
                                <p style="margin: 0 0 8px 0; font-size: 15px; color: #333333;">
                                    Hello,
                                </p>
                
                                <p style="margin: 0 0 25px 0; font-size: 14px; line-height: 1.6; color: #666666;">
                                    {{MESSAGE}}
                                </p>
                
                                <div style="text-align: center; margin: 28px 0;">
                                    <a href="{{LINK}}"
                                       style="display: inline-block; padding: 12px 25px; background-color: #172554; color: #ffffff; text-decoration: none; border-radius: 6px; font-size: 14px; font-weight: bold;">
                                        {{BUTTON}}
                                    </a>
                                </div>
                
                                <div style="background-color: #f7f8fc; padding: 20px; border-radius: 8px; margin-bottom: 28px;">
                                    <div style="font-size: 13px; font-weight: bold; color: #172554; margin-bottom: 10px;">
                                        IMPORTANT
                                    </div>
                                    <div style="font-size: 14px; line-height: 1.6; color: #666666;">
                                        {{EXPIRY}}
                                    </div>
                                </div>
                
                                <div style="font-size: 12px; line-height: 1.6; color: #999999; margin-bottom: 8px;">
                                    If the button does not work, copy and paste this link into your browser:
                                </div>
                                <div style="font-size: 12px; line-height: 1.6; color: #172554; word-break: break-all;">
                                    {{LINK}}
                                </div>
                
                                <div style="margin-top: 30px; padding-top: 24px; border-top: 1px solid #e5e7eb;">
                                    <div style="font-size: 14px; font-weight: bold; color: #333333; margin-bottom: 8px;">
                                        Didn't request this?
                                    </div>
                                    <div style="font-size: 13px; line-height: 1.6; color: #777777;">
                                        If you did not request this, you can safely ignore this email.
                                    </div>
                                </div>
                
                            </div>
                
                            <div style="background-color: #f2f4f8; padding: 25px; text-align: center;">
                                <div style="font-size: 17px; font-weight: bold; color: #172554;">
                                    VibeStay
                                </div>
                                <div style="margin-top: 6px; font-size: 12px; color: #999999;">
                                    Your stay starts here.
                                </div>
                                <div style="margin-top: 12px; font-size: 11px; color: #aaaaaa;">
                                    © 2026 VibeStay. All rights reserved.
                                </div>
                            </div>
                
                        </div>
                
                    </div>
                
                </body>
                </html>
                """
                .replace("{{TITLE}}", HtmlUtils.htmlEscape(title))
                .replace("{{MESSAGE}}", HtmlUtils.htmlEscape(message).replace("\n", "<br>"))
                .replace("{{BUTTON}}", HtmlUtils.htmlEscape(buttonText))
                .replace("{{LINK}}", HtmlUtils.htmlEscape(link))
                .replace("{{EXPIRY}}", HtmlUtils.htmlEscape(expiryMessage));
    }

    public void sendResetLink(UserEvent event) {
        String body = buildActionEmailBody(
                "Reset Your VibeStay Password",
                " We received a request to reset the password for your VibeStay account." +
                        "\nClick the button below to create a new password\n" +
                        "and regain access to your account",
                "Reset Password",
                event.link(),
                "This reset link will expire in 15 minutes."
        );

        EmailDetails emailDetails = new EmailDetails();
        emailDetails.setSubject("Reset your VibeStay password");
        emailDetails.setMsgBody(body);
        emailDetails.setRecipient(event.email());
        mailSenderService.sendSimpleMail(emailDetails);
    }

    public void sendVerificationEmail(UserEvent event) {
        String emailBody = buildActionEmailBody(
                "Verify your email",
                "Thank you for creating your VibeStay account. " +
                        "Please verify your email address to activate your account.",
                "Verify My Email",
                event.link(),
                "This verification link will expire in 24 hours."
        );

        EmailDetails emailDetails = new EmailDetails();
        emailDetails.setSubject("Verify your VibeStay Account");
        emailDetails.setMsgBody(emailBody);
        emailDetails.setRecipient(event.email());
        mailSenderService.sendSimpleMail(emailDetails);
    }

    private String formatMoney(BigDecimal amount) {

        if (amount == null) {
            return "0.00";
        }

        return String.format("%.2f", amount);
    }

    private String buildMapBlock(String directionsUrl) {
        return "<a href=\"" + directionsUrl + "\" style=\"display: block; text-decoration: none;\">"
                + "<img src=\"cid:hotelmap\" alt=\"Map showing hotel location\" width=\"556\" "
                + "style=\"display: block; width: 100%; max-width: 556px; height: auto; "
                + "border-radius: 8px; border: 1px solid #e5e7eb;\">"
                + "</a>";
    }

    private static final String ROOM_TEMPLATE = """
                    <div style="
                        border: 1px solid #e1e4ea;
                        border-radius: 8px;
                        padding: 17px;
                        margin-bottom: 12px;
                    ">

                        <table width="100%%"
                               cellpadding="0"
                               cellspacing="0">

                            <tr>

                                <td style="
                                    vertical-align: top;
                                ">

                                    <div style="
                                        font-size: 16px;
                                        font-weight: bold;
                                        color: #222222;
                                    ">
                                        %s
                                    </div>

                                    <div style="
                                        margin-top: 5px;
                                        font-size: 13px;
                                        color: #777777;
                                    ">
                                        Room %s
                                    </div>

                                    <div style="
                                        margin-top: 10px;
                                        font-size: 12px;
                                        color: #777777;
                                    ">
                                        Guests: %s
                                    </div>

                                </td>

                                <td style="
                                    vertical-align: top;
                                    text-align: right;
                                ">

                                    <div style="
                                        font-size: 14px;
                                        color: #777777;
                                    ">
                                        BHD %s / night
                                    </div>

                                    <div style="
                                        margin-top: 7px;
                                        font-size: 15px;
                                        font-weight: bold;
                                        color: #222222;
                                    ">
                                        BHD %s
                                    </div>

                                </td>

                            </tr>

                        </table>

                    </div>
                    """;

    public void sendBookingConfirmationEmail(BookingEvent event) {
        sendBookingEmail(event, "Booking Confirmed",
                "Your VibeStay booking has been successfully confirmed.",
                "VibeStay Booking Confirmed - ");
    }

    public void sendBookingCancellationEmail(BookingEvent event) {
        sendBookingEmail(event, "Booking Cancelled",
                "Your VibeStay booking has been cancelled.",
                "VibeStay Booking Cancelled - ");
    }

    private void sendBookingEmail(BookingEvent event, String title, String message, String subjectPrefix) {
        if (event.guestEmail() == null || event.guestEmail().isBlank()) {
            log.warn("No guest email for booking {}, skipping {} email", event.bookingReference(), title);
            return;
        }
        byte[] map = hotelMapService.loadHotelMap(event.hotelLatitude(), event.hotelLongitude());
        String body = buildBookingEmailBody(event, title, message, map != null);
        EmailDetails emailDetails = new EmailDetails();
        emailDetails.setSubject(subjectPrefix + event.bookingReference());
        emailDetails.setMsgBody(body);
        emailDetails.setRecipient(event.guestEmail());
        mailSenderService.sendMailWithInlineImage(emailDetails, "hotelmap", map);
    }

    public String buildBookingEmailBody(BookingEvent event, String title, String message, boolean hasMap) {
        String customerName = event.guestName() == null ? "Guest" : event.guestName();
        String hotelName = event.hotelName() == null ? "" : event.hotelName();
        String fullAddress = joinAddress(event.hotelAddress(), event.hotelCity(), event.hotelCountry());
        String directionsUrl = buildDirectionsUrl(event.hotelLatitude(), event.hotelLongitude());
        long nights = event.checkIn() == null || event.checkOut() == null ? 0
                : ChronoUnit.DAYS.between(event.checkIn(), event.checkOut());
        String roomsHtml = buildBookingRoomsHtml(event, nights);

        String specialRequestHtml = "";
        if (event.specialRequest() != null && !event.specialRequest().isBlank()) {
            specialRequestHtml = """
                    <div style="
                        margin-top: 24px;
                        padding: 16px;
                        background-color: #f7f8fc;
                        border-radius: 8px;
                    ">
                        <div style="
                            font-size: 13px;
                            font-weight: bold;
                            color: #333333;
                            margin-bottom: 6px;
                        ">
                            Special request
                        </div>

                        <div style="
                            font-size: 14px;
                            color: #666666;
                            line-height: 1.5;
                        ">
                            {{SPECIAL_REQUEST_TEXT}}
                        </div>
                    </div>
                    """;
            specialRequestHtml = specialRequestHtml.replace(
                    "{{SPECIAL_REQUEST_TEXT}}",
                    HtmlUtils.htmlEscape(event.specialRequest())
            );
        }

        String guestSummary = buildGuestSummary(event.adults(), event.children());

        String html = """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport"
                          content="width=device-width, initial-scale=1.0">

                    <title>VibeStay - {{TITLE}}</title>
                </head>

                <body style="
                    margin: 0;
                    padding: 0;
                    background-color: #f2f4f8;
                    font-family: Arial, Helvetica, sans-serif;
                    color: #2d2d3a;
                ">

                    <div style="
                        width: 100%;
                        padding: 35px 0;
                    ">

                        <div style="
                            max-width: 620px;
                            margin: 0 auto;
                            background-color: #ffffff;
                            border-radius: 10px;
                            overflow: hidden;
                        ">

                            <!-- HEADER -->
                            <div style="
                                background-color: #172554;
                                padding: 28px 30px;
                            ">

                                <div style="
                                    font-size: 25px;
                                    font-weight: bold;
                                    color: #ffffff;
                                ">
                                    VibeStay
                                </div>

                                <div style="
                                    margin-top: 5px;
                                    font-size: 13px;
                                    color: #cbd5e1;
                                ">
                                    Your stay starts here
                                </div>

                            </div>


                            <!-- MAIN CONTENT -->
                            <div style="
                                padding: 35px 32px;
                            ">

                                <!-- TITLE -->
                                <h1 style="
                                    margin: 0 0 12px 0;
                                    font-size: 28px;
                                    color: #172554;
                                ">
                                    {{TITLE}}
                                </h1>

                                <div style="
                                    height: 1px;
                                    background-color: #e5e7eb;
                                    margin-bottom: 24px;
                                "></div>


                                <!-- GREETING -->
                                <p style="
                                    margin: 0 0 8px 0;
                                    font-size: 15px;
                                    color: #333333;
                                ">
                                    Hello {{CUSTOMER_NAME}},
                                </p>

                                <p style="
                                    margin: 0 0 25px 0;
                                    font-size: 14px;
                                    line-height: 1.6;
                                    color: #666666;
                                ">
                                    {{MESSAGE}}
                                </p>


                                <!-- BOOKING DETAILS -->
                                <div style="
                                    background-color: #f7f8fc;
                                    padding: 20px;
                                    border-radius: 8px;
                                    margin-bottom: 28px;
                                ">

                                    <div style="
                                        font-size: 13px;
                                        font-weight: bold;
                                        color: #172554;
                                        margin-bottom: 15px;
                                    ">
                                        BOOKING DETAILS
                                    </div>

                                    <table width="100%"
                                           cellpadding="0"
                                           cellspacing="0"
                                           style="font-size: 14px;">

                                        <tr>
                                            <td style="
                                                padding: 6px 0;
                                                color: #777777;
                                            ">
                                                Booking reference
                                            </td>

                                            <td style="
                                                padding: 6px 0;
                                                text-align: right;
                                                font-weight: bold;
                                                color: #222222;
                                            ">
                                                {{BOOKING_REFERENCE}}
                                            </td>
                                        </tr>

                                        <tr>
                                            <td style="
                                                padding: 6px 0;
                                                color: #777777;
                                            ">
                                                Check-in
                                            </td>

                                            <td style="
                                                padding: 6px 0;
                                                text-align: right;
                                                font-weight: bold;
                                            ">
                                                {{CHECK_IN}}
                                            </td>
                                        </tr>

                                        <tr>
                                            <td style="
                                                padding: 6px 0;
                                                color: #777777;
                                            ">
                                                Check-out
                                            </td>

                                            <td style="
                                                padding: 6px 0;
                                                text-align: right;
                                                font-weight: bold;
                                            ">
                                                {{CHECK_OUT}}
                                            </td>
                                        </tr>

                                        <tr>
                                            <td style="
                                                padding: 6px 0;
                                                color: #777777;
                                            ">
                                                Guests
                                            </td>

                                            <td style="
                                                padding: 6px 0;
                                                text-align: right;
                                                font-weight: bold;
                                            ">
                                                {{GUESTS}}
                                            </td>
                                        </tr>

                                        <tr>
                                            <td style="
                                                padding: 6px 0;
                                                color: #777777;
                                            ">
                                                Status
                                            </td>

                                            <td style="
                                                padding: 6px 0;
                                                text-align: right;
                                                font-weight: bold;
                                                color: #16a34a;
                                            ">
                                                {{STATUS}}
                                            </td>
                                        </tr>

                                    </table>

                                </div>


                                <!-- HOTEL -->
                                <div style="
                                    font-size: 13px;
                                    font-weight: bold;
                                    color: #172554;
                                    margin-bottom: 14px;
                                ">
                                    HOTEL
                                </div>

                                <div style="
                                    margin-bottom: 15px;
                                ">

                                    <div style="
                                        font-size: 20px;
                                        font-weight: bold;
                                        color: #222222;
                                        margin-bottom: 6px;
                                    ">
                                        {{HOTEL_NAME}}
                                    </div>

                                    <div style="
                                        font-size: 14px;
                                        color: #777777;
                                        line-height: 1.5;
                                    ">
                                        {{HOTEL_ADDRESS}}
                                    </div>

                                </div>


                                {{MAP_BLOCK}}


                                <!-- DIRECTIONS BUTTON -->
                                <div style="
                                    text-align: center;
                                    margin: 18px 0 30px 0;
                                ">

                                    <a href="{{DIRECTIONS_URL}}"
                                       style="
                                           display: inline-block;
                                           padding: 12px 25px;
                                           background-color: #172554;
                                           color: #ffffff;
                                           text-decoration: none;
                                           border-radius: 6px;
                                           font-size: 14px;
                                           font-weight: bold;
                                       ">
                                        Get directions
                                    </a>

                                </div>


                                <!-- ROOMS -->
                                <div style="
                                    font-size: 13px;
                                    font-weight: bold;
                                    color: #172554;
                                    margin-bottom: 12px;
                                ">
                                    ROOM DETAILS
                                </div>

                                {{ROOMS}}


                                {{SPECIAL_REQUEST}}


                                <!-- TOTAL -->
                                <div style="
                                    margin-top: 25px;
                                    border: 1px solid #dfe3eb;
                                    border-radius: 8px;
                                    overflow: hidden;
                                ">

                                    <div style="
                                        padding: 16px 18px;
                                        background-color: #f7f8fc;
                                    ">

                                        <table width="100%"
                                               cellpadding="0"
                                               cellspacing="0">

                                            <tr>
                                                <td style="
                                                    font-size: 14px;
                                                    color: #555555;
                                                ">
                                                    {{NIGHTS}} {{NIGHT_LABEL}}
                                                </td>

                                                <td style="
                                                    text-align: right;
                                                    font-size: 14px;
                                                    color: #555555;
                                                ">
                                                    BHD {{TOTAL}}
                                                </td>
                                            </tr>

                                        </table>

                                    </div>


                                    <div style="
                                        padding: 18px;
                                        border-top: 1px solid #e1e4ea;
                                    ">

                                        <table width="100%"
                                               cellpadding="0"
                                               cellspacing="0">

                                            <tr>
                                                <td style="
                                                    font-size: 18px;
                                                    font-weight: bold;
                                                    color: #172554;
                                                ">
                                                    Total
                                                </td>

                                                <td style="
                                                    text-align: right;
                                                    font-size: 20px;
                                                    font-weight: bold;
                                                    color: #172554;
                                                ">
                                                    BHD {{TOTAL}}
                                                </td>
                                            </tr>

                                        </table>

                                    </div>

                                </div>


                                <!-- FOOTER MESSAGE -->
                                <div style="
                                    margin-top: 30px;
                                    padding-top: 24px;
                                    border-top: 1px solid #e5e7eb;
                                ">

                                    <div style="
                                        font-size: 14px;
                                        font-weight: bold;
                                        color: #333333;
                                        margin-bottom: 8px;
                                    ">
                                        Questions?
                                    </div>

                                    <div style="
                                        font-size: 13px;
                                        line-height: 1.6;
                                        color: #777777;
                                    ">
                                        If you have any questions about your
                                        reservation, please contact VibeStay support.
                                    </div>

                                </div>

                            </div>


                            <!-- FOOTER -->
                            <div style="
                                background-color: #f2f4f8;
                                padding: 25px;
                                text-align: center;
                            ">

                                <div style="
                                    font-size: 17px;
                                    font-weight: bold;
                                    color: #172554;
                                ">
                                    VibeStay
                                </div>

                                <div style="
                                    margin-top: 6px;
                                    font-size: 12px;
                                    color: #999999;
                                ">
                                    Your stay starts here.
                                </div>

                                <div style="
                                    margin-top: 12px;
                                    font-size: 11px;
                                    color: #aaaaaa;
                                ">
                                    © 2026 VibeStay. All rights reserved.
                                </div>

                            </div>

                        </div>

                    </div>

                </body>
                </html>
                """;

        return html
                .replace("{{TITLE}}", HtmlUtils.htmlEscape(title))
                .replace("{{CUSTOMER_NAME}}", HtmlUtils.htmlEscape(customerName))
                .replace("{{MESSAGE}}", HtmlUtils.htmlEscape(message))
                .replace("{{BOOKING_REFERENCE}}", HtmlUtils.htmlEscape(nullToEmpty(event.bookingReference())))
                .replace("{{CHECK_IN}}", event.checkIn() == null ? "" : event.checkIn().toString())
                .replace("{{CHECK_OUT}}", event.checkOut() == null ? "" : event.checkOut().toString())
                .replace("{{GUESTS}}", HtmlUtils.htmlEscape(guestSummary))
                .replace("{{STATUS}}", HtmlUtils.htmlEscape(nullToEmpty(event.status())))
                .replace("{{HOTEL_NAME}}", HtmlUtils.htmlEscape(hotelName))
                .replace("{{HOTEL_ADDRESS}}", HtmlUtils.htmlEscape(fullAddress))
                .replace("{{MAP_BLOCK}}", hasMap ? buildMapBlock(directionsUrl) : "")
                .replace("{{DIRECTIONS_URL}}", directionsUrl)
                .replace("{{ROOMS}}", roomsHtml)
                .replace("{{SPECIAL_REQUEST}}", specialRequestHtml)
                .replace("{{NIGHTS}}", String.valueOf(nights))
                .replace("{{NIGHT_LABEL}}", nights == 1 ? "night" : "nights")
                .replace("{{TOTAL}}", formatMoney(event.totalAmount()));
    }

    private String buildBookingRoomsHtml(BookingEvent event, long nights) {
        StringBuilder html = new StringBuilder();
        if (event.rooms() == null) {
            return "";
        }
        for (BookingEvent.Room room : event.rooms()) {
            BigDecimal price = room.pricePerNight() == null ? BigDecimal.ZERO : room.pricePerNight();
            BigDecimal roomTotal = price.multiply(BigDecimal.valueOf(nights));
            html.append(ROOM_TEMPLATE.formatted(
                    HtmlUtils.htmlEscape(nullToEmpty(room.roomTypeName())),
                    HtmlUtils.htmlEscape(nullToEmpty(room.roomNumber())),
                    buildRoomGuestNames(room),
                    formatMoney(price),
                    formatMoney(roomTotal)
            ));
        }
        return html.toString();
    }

    private String buildRoomGuestNames(BookingEvent.Room room) {
        StringBuilder guests = new StringBuilder();
        if (room.guests() == null) {
            return "";
        }
        for (BookingEvent.Guest guest : room.guests()) {
            if (!guests.isEmpty()) {
                guests.append(", ");
            }
            guests.append(HtmlUtils.htmlEscape(nullToEmpty(guest.name())));
            if (guest.guestType() != null) {
                guests.append(" (").append(guest.guestType().toLowerCase()).append(")");
            }
        }
        return guests.toString();
    }

    private String buildGuestSummary(int adults, int children) {
        StringBuilder summary = new StringBuilder();
        if (adults > 0) {
            summary.append(adults).append(adults == 1 ? " adult" : " adults");
        }
        if (children > 0) {
            if (!summary.isEmpty()) {
                summary.append(" · ");
            }
            summary.append(children).append(children == 1 ? " child" : " children");
        }
        return summary.toString();
    }

    private String buildDirectionsUrl(BigDecimal latitude, BigDecimal longitude) {
        if (latitude == null || longitude == null) {
            return "https://www.google.com/maps";
        }
        return "https://www.google.com/maps/dir/?api=1"
                + "&destination=" + latitude + "," + longitude
                + "&travelmode=driving";
    }

    private String joinAddress(String... parts) {
        StringBuilder out = new StringBuilder();
        for (String part : parts) {
            if (part != null && !part.isBlank()) {
                if (!out.isEmpty()) {
                    out.append(", ");
                }
                out.append(part);
            }
        }
        return out.toString();
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
