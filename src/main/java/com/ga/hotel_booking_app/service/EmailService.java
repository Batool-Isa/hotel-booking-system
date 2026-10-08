package com.ga.hotel_booking_app.service;

import com.ga.hotel_booking_app.dto.EmailDetails;
import com.ga.hotel_booking_app.model.Booking;
import com.ga.hotel_booking_app.model.EmailVerificationToken;
import com.ga.hotel_booking_app.model.PasswordResetToken;
import com.ga.hotel_booking_app.model.User;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import com.ga.hotel_booking_app.model.BookingGuest;
import com.ga.hotel_booking_app.model.BookingRoom;
import org.springframework.web.util.HtmlUtils;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.io.File;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender javaMailSender;
    @Value("${spring.mail.username}")
    private String sender;

    private static final java.util.Map<String, byte[]> MAP_CACHE = new java.util.concurrent.ConcurrentHashMap<>();

    public String sendSimpleMail(EmailDetails details) {
        try {
            MimeMessage mimeMessage = javaMailSender.createMimeMessage();

            MimeMessageHelper helper =
                    new MimeMessageHelper(mimeMessage, true);

            helper.setFrom(sender);
            helper.setTo(details.getRecipient());
            helper.setSubject(details.getSubject());
            helper.setText(details.getMsgBody(), true);

            javaMailSender.send(mimeMessage);
            return "Mail Sent Successfully";
        } catch (Exception e) {
            return "Error while sending mail";
        }
    }

    public String sendMailWithAttachment(EmailDetails details) {
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        MimeMessageHelper helper;
        try {
            helper = new MimeMessageHelper(mimeMessage, true);

            helper.setFrom(sender);
            helper.setTo(details.getRecipient());
            helper.setText(details.getMsgBody(), true);
            helper.setSubject(details.getSubject());
            FileSystemResource fileSystemResource = new FileSystemResource(new File(details.getAttachment()));

            helper.addAttachment(fileSystemResource.getFilename(), fileSystemResource);

            javaMailSender.send(mimeMessage);

            return "Mail Sent Successfully";

        } catch (MessagingException e) {

            return "Error while sending mail";
        }
    }
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

    public void sendResetLink(PasswordResetToken token, User user) {
        String link = "http://localhost:8000/#/reset-password?token=" + token.getToken();

        String body = buildActionEmailBody(
                "Reset Your VibeStay Password",
                " We received a request to reset the password for your VibeStay account." +
                        "\nClick the button below to create a new password\n" +
                        "and regain access to your account",
                "Reset Password",
                link,
                "This reset link will expire in 15 minutes."
        );


        EmailDetails emailDetails = new EmailDetails();
        emailDetails.setSubject("Reset your VibeStay password");
        emailDetails.setMsgBody(body);
        emailDetails.setRecipient(user.getEmail());
        sendSimpleMail(emailDetails);
        System.out.println("sending reset password email");
    }
    public void sendVerificationEmail(EmailVerificationToken emailVerificationToken) {
        String verificationLink =
                "http://localhost:8000/auth/users/verify-email?token="
                        + emailVerificationToken.getToken();
        String emailBody = buildActionEmailBody(
                "Verify your email",
                "Thank you for creating your VibeStay account. " +
                        "Please verify your email address to activate your account.",
                "Verify My Email",
                verificationLink,
                "This verification link will expire in 24 hours."
        );


        EmailDetails emailDetails = new EmailDetails();
        emailDetails.setSubject("Verify your VibeStay Account");
        emailDetails.setMsgBody(emailBody);
        emailDetails.setRecipient(emailVerificationToken.getUser().getEmail());
        System.out.println("sending verification email");
        sendSimpleMail(emailDetails);

    }

    public String buildBookingEmailBody(
            Booking booking,
            String title,
            String message) {
        return buildBookingEmailBody(booking, title, message, false);
    }

    public String buildBookingEmailBody(
            Booking booking,
            String title,
            String message,
            boolean hasMap) {

        String customerName = booking.getUser().getUsername();
        String hotelName = booking.getHotel().getName();

        String hotelAddress = booking.getHotel().getAddress();
        String hotelCity = booking.getHotel().getCity();
        String hotelCountry = booking.getHotel().getCountry();

        String fullAddress = String.join(", ",
                hotelAddress,
                hotelCity,
                hotelCountry
        );


        String directionsUrl = buildDirectionsUrl(booking);

        long nights = ChronoUnit.DAYS.between(
                booking.getCheckIn(),
                booking.getCheckOut()
        );

        String roomsHtml = buildBookingRoomsHtml(booking, nights);

        String specialRequestHtml = "";

        if (booking.getSpecialRequest() != null
                && !booking.getSpecialRequest().isBlank()) {

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
                    HtmlUtils.htmlEscape(booking.getSpecialRequest())
            );
        }

        String guestSummary = buildGuestSummary(booking);

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

        /*
         * Replace dynamic values.
         */

        html = html
                .replace(
                        "{{TITLE}}",
                        HtmlUtils.htmlEscape(title)
                )
                .replace(
                        "{{CUSTOMER_NAME}}",
                        HtmlUtils.htmlEscape(customerName)
                )
                .replace(
                        "{{MESSAGE}}",
                        HtmlUtils.htmlEscape(message)
                )
                .replace(
                        "{{BOOKING_REFERENCE}}",
                        HtmlUtils.htmlEscape(
                                booking.getBookingReference()
                        )
                )
                .replace(
                        "{{CHECK_IN}}",
                        booking.getCheckIn().toString()
                )
                .replace(
                        "{{CHECK_OUT}}",
                        booking.getCheckOut().toString()
                )
                .replace(
                        "{{GUESTS}}",
                        HtmlUtils.htmlEscape(guestSummary)
                )
                .replace(
                        "{{STATUS}}",
                        HtmlUtils.htmlEscape(
                                booking.getStatus().name()
                        )
                )
                .replace(
                        "{{HOTEL_NAME}}",
                        HtmlUtils.htmlEscape(hotelName)
                )
                .replace(
                        "{{HOTEL_ADDRESS}}",
                        HtmlUtils.htmlEscape(fullAddress)
                )
                .replace(
                        "{{MAP_BLOCK}}",
                        hasMap ? buildMapBlock(directionsUrl) : ""
                )
                .replace(
                        "{{DIRECTIONS_URL}}",
                        directionsUrl
                )
                .replace(
                        "{{ROOMS}}",
                        roomsHtml
                )
                .replace(
                        "{{SPECIAL_REQUEST}}",
                        specialRequestHtml
                )
                .replace(
                        "{{NIGHTS}}",
                        String.valueOf(nights)
                )
                .replace(
                        "{{NIGHT_LABEL}}",
                        nights == 1 ? "night" : "nights"
                )
                .replace(
                        "{{TOTAL}}",
                        formatMoney(booking.getTotalAmount())
                );

        return html;
    }

    private String buildBookingRoomsHtml(Booking booking, long nights) {

        StringBuilder html = new StringBuilder();

        for (BookingRoom bookingRoom : booking.getBookingRooms()) {

            String roomType = bookingRoom.getRoom()
                    .getRoomType()
                    .getName();

            String roomNumber = bookingRoom.getRoom()
                    .getRoomNumber();

            BigDecimal roomTotal = bookingRoom.getPricePerNight()
                    .multiply(BigDecimal.valueOf(nights));

            html.append("""
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
                """.formatted(
                    HtmlUtils.htmlEscape(roomType),
                    HtmlUtils.htmlEscape(roomNumber),
                    buildRoomGuestNames(bookingRoom),
                    formatMoney(bookingRoom.getPricePerNight()),
                    formatMoney(roomTotal)
            ));
        }

        return html.toString();
    }
    private String buildRoomGuestNames(BookingRoom bookingRoom) {

        StringBuilder guests = new StringBuilder();

        for (BookingGuest guest : bookingRoom.getGuests()) {

            if (!guests.isEmpty()) {
                guests.append(", ");
            }

            guests.append(
                    HtmlUtils.htmlEscape(guest.getName())
            );

            guests.append(" (")
                    .append(guest.getGuestType().name().toLowerCase())
                    .append(")");
        }

        return guests.toString();
    }
    private String buildGuestSummary(Booking booking) {

        int adults = booking.getAdults();
        int children = booking.getChildren();

        StringBuilder summary = new StringBuilder();

        if (adults > 0) {
            summary.append(adults)
                    .append(adults == 1 ? " adult" : " adults");
        }

        if (children > 0) {

            if (!summary.isEmpty()) {
                summary.append(" · ");
            }

            summary.append(children)
                    .append(children == 1 ? " child" : " children");
        }

        return summary.toString();
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

    private byte[] loadHotelMap(Booking booking) {
        BigDecimal latitude = booking.getHotel().getLatitude();
        BigDecimal longitude = booking.getHotel().getLongitude();
        if (latitude == null || longitude == null) {
            return null;
        }
        String key = latitude.toPlainString() + "," + longitude.toPlainString();
        byte[] cached = MAP_CACHE.get(key);
        if (cached != null) {
            return cached;
        }
        byte[] png = MapImageBuilder.build(latitude.doubleValue(), longitude.doubleValue());
        if (png != null) {
            MAP_CACHE.put(key, png);
        }
        return png;
    }

    private String sendMailWithInlineImage(EmailDetails details, String contentId, byte[] image) {
        try {
            MimeMessage mimeMessage = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);
            helper.setFrom(sender);
            helper.setTo(details.getRecipient());
            helper.setSubject(details.getSubject());
            helper.setText(details.getMsgBody(), true);
            if (image != null) {
                helper.addInline(contentId, new ByteArrayResource(image), "image/png");
            }
            javaMailSender.send(mimeMessage);
            return "Mail Sent Successfully";
        } catch (Exception e) {
            return "Error while sending mail";
        }
    }

    private String buildDirectionsUrl(Booking booking) {

        BigDecimal latitude = booking.getHotel().getLatitude();
        BigDecimal longitude = booking.getHotel().getLongitude();

        if (latitude == null || longitude == null) {
            return "https://www.google.com/maps";
        }

        return "https://www.google.com/maps/dir/?api=1"
                + "&destination="
                + latitude + "," + longitude
                + "&travelmode=driving";
    }
    public void sendBookingConfirmationEmail(Booking booking) {
        byte[] map = loadHotelMap(booking);
        String body = buildBookingEmailBody(booking, "Booking Confirmed", "Your VibeStay booking has been successfully confirmed.", map != null);
        EmailDetails emailDetails = new EmailDetails();
        emailDetails.setSubject("VibeStay Booking Confirmed - " + booking.getBookingReference());
        emailDetails.setMsgBody(body);
        emailDetails.setRecipient(booking.getUser().getEmail());
        sendMailWithInlineImage(emailDetails, "hotelmap", map);
    }
    public void sendBookingCancellationEmail(Booking booking) {
        byte[] map = loadHotelMap(booking);
        String body = buildBookingEmailBody(
                booking, "Booking Cancelled", "Your VibeStay booking has been cancelled.", map != null);

        EmailDetails emailDetails = new EmailDetails();
        emailDetails.setSubject("VibeStay Booking Cancelled - " + booking.getBookingReference());
        emailDetails.setMsgBody(body);
        emailDetails.setRecipient(booking.getUser().getEmail());

        sendMailWithInlineImage(emailDetails, "hotelmap", map);
    }

    private static class MapImageBuilder {

        interface TileLoader {
            BufferedImage load(int zoom, int x, int y);
        }

        private static final int TILE = 256;
        private static final int ZOOM = 15;
        private static final int WIDTH = 800;
        private static final int HEIGHT = 400;

        public static byte[] build(double lat, double lng) {
            return build(lat, lng, MapImageBuilder::downloadTile);
        }

        static byte[] build(double lat, double lng, TileLoader loader) {
            ExecutorService pool = Executors.newFixedThreadPool(6);
            try {
                int n = 1 << ZOOM;
                double latRad = Math.toRadians(lat);
                double worldX = (lng + 180.0) / 360.0 * n * TILE;
                double worldY = (1.0 - Math.log(Math.tan(latRad) + 1.0 / Math.cos(latRad)) / Math.PI) / 2.0 * n * TILE;

                double left = worldX - WIDTH / 2.0;
                double top = worldY - HEIGHT / 2.0;
                int tx0 = (int) Math.floor(left / TILE);
                int tx1 = (int) Math.floor((left + WIDTH - 1) / TILE);
                int ty0 = (int) Math.floor(top / TILE);
                int ty1 = (int) Math.floor((top + HEIGHT - 1) / TILE);

                List<int[]> coords = new ArrayList<>();
                List<Future<BufferedImage>> futures = new ArrayList<>();
                for (int ty = ty0; ty <= ty1; ty++) {
                    for (int tx = tx0; tx <= tx1; tx++) {
                        final int x = Math.floorMod(tx, n);
                        final int y = Math.max(0, Math.min(n - 1, ty));
                        coords.add(new int[]{tx, ty});
                        futures.add(pool.submit(() -> loader.load(ZOOM, x, y)));
                    }
                }

                BufferedImage canvas = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
                Graphics2D g = canvas.createGraphics();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(new Color(0xE5E7EB));
                g.fillRect(0, 0, WIDTH, HEIGHT);

                for (int i = 0; i < futures.size(); i++) {
                    BufferedImage tile = futures.get(i).get(4, TimeUnit.SECONDS);
                    if (tile == null) {
                        return null;
                    }
                    int px = (int) Math.round(coords.get(i)[0] * TILE - left);
                    int py = (int) Math.round(coords.get(i)[1] * TILE - top);
                    g.drawImage(tile, px, py, null);
                }

                drawMarker(g, WIDTH / 2, HEIGHT / 2);
                drawAttribution(g);
                g.dispose();

                ByteArrayOutputStream out = new ByteArrayOutputStream();
                ImageIO.write(canvas, "png", out);
                return out.toByteArray();
            } catch (Exception e) {
                return null;
            } finally {
                pool.shutdownNow();
            }
        }

        private static void drawMarker(Graphics2D g, int cx, int cy) {
            double r = 15;
            double headY = cy - 30;
            Path2D tail = new Path2D.Double();
            tail.moveTo(cx - r * 0.85, headY + r * 0.5);
            tail.lineTo(cx, cy);
            tail.lineTo(cx + r * 0.85, headY + r * 0.5);
            tail.closePath();
            Area pin = new Area(new Ellipse2D.Double(cx - r, headY - r, r * 2, r * 2));
            pin.add(new Area(tail));

            g.setColor(new Color(0, 0, 0, 50));
            g.fill(new Ellipse2D.Double(cx - 9, cy - 4, 18, 8));
            g.setColor(new Color(0xE11D48));
            g.fill(pin);
            g.setColor(Color.WHITE);
            g.setStroke(new BasicStroke(3f));
            g.draw(pin);
            g.setColor(Color.WHITE);
            g.fill(new Ellipse2D.Double(cx - 6, headY - 6, 12, 12));
        }

        private static void drawAttribution(Graphics2D g) {
            String text = "© OpenStreetMap contributors";
            g.setFont(new Font("SansSerif", Font.PLAIN, 12));
            FontMetrics fm = g.getFontMetrics();
            int w = fm.stringWidth(text) + 12;
            g.setColor(new Color(255, 255, 255, 200));
            g.fillRect(WIDTH - w, HEIGHT - 20, w, 20);
            g.setColor(new Color(0x333333));
            g.drawString(text, WIDTH - w + 6, HEIGHT - 6);
        }

        private static BufferedImage downloadTile(int zoom, int x, int y) {
            try {
                URL url = new URL("https://tile.openstreetmap.org/" + zoom + "/" + x + "/" + y + ".png");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(2500);
                conn.setReadTimeout(2500);
                conn.setRequestProperty("User-Agent", "VibeStay/1.0 (hotel booking student project)");
                if (conn.getResponseCode() != 200) {
                    return null;
                }
                try (var in = conn.getInputStream()) {
                    return ImageIO.read(in);
                }
            } catch (Exception e) {
                return null;
            }
        }
    }
}
