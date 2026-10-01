package com.ga.hotel_booking_app.service;

import com.ga.hotel_booking_app.dto.EmailDetails;
import com.ga.hotel_booking_app.model.EmailVerificationToken;
import com.ga.hotel_booking_app.model.PasswordResetToken;
import com.ga.hotel_booking_app.model.User;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMailMessage;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.File;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender javaMailSender;
    @Value("${spring.mail.username}")
    private String sender;

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
                    <title>{{TITLE}}</title>
                </head>
                
                <body style="
                    margin: 0;
                    padding: 0;
                    background-color: #f5f7fb;
                    font-family: Arial, Helvetica, sans-serif;
                    color: #333333;
                ">
                
                    <div style="
                        width: 100%%;
                        padding: 40px 0;
                    ">
                
                        <div style="
                            max-width: 600px;
                            margin: 0 auto;
                            background-color: #ffffff;
                            border-radius: 12px;
                            overflow: hidden;
                            box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
                        ">
                
                            <!-- Header -->
                            <div style="
                                background-color: #6c63ff;
                                padding: 32px 20px;
                                text-align: center;
                                color: #ffffff;
                            ">
                                <h1 style="margin: 0;">
                                    VibeStay
                                </h1>
                
                                <p style="
                                    margin: 8px 0 0;
                                    font-size: 14px;
                                ">
                                    Your stay starts here
                                </p>
                            </div>
                
                            <!-- Content -->
                            <div style="padding: 40px 35px;">
                
                                <h2 style="
                                    margin: 0 0 20px;
                                    font-size: 24px;
                                    color: #222222;
                                ">
                                    {{TITLE}}
                                </h2>
                
                                <p style="
                                    margin: 0 0 20px;
                                    font-size: 15px;
                                    line-height: 1.7;
                                    color: #555555;
                                ">
                                    Hello,
                                </p>
                
                                <p style="
                                    margin: 0 0 30px;
                                    font-size: 15px;
                                    line-height: 1.7;
                                    color: #555555;
                                ">
                                    {{MESSAGE}}
                                </p>
                
                                <!-- Button -->
                                <div style="
                                    text-align: center;
                                    margin: 30px 0;
                                ">
                                    <a href="{{LINK}}"
                                       style="
                                           display: inline-block;
                                           padding: 14px 32px;
                                           background-color: #6c63ff;
                                           color: #ffffff;
                                           text-decoration: none;
                                           border-radius: 8px;
                                           font-size: 15px;
                                           font-weight: bold;
                                       ">
                                        {{BUTTON}}
                                    </a>
                                </div>
                
                                <!-- Expiry -->
                                <div style="
                                    margin: 30px 0;
                                    padding: 16px 18px;
                                    background-color: #f4f3ff;
                                    border-left: 4px solid #6c63ff;
                                    border-radius: 6px;
                                ">
                                    <p style="
                                        margin: 0;
                                        font-size: 14px;
                                        line-height: 1.6;
                                        color: #555555;
                                    ">
                                        <strong style="color: #333333;">
                                            Important:
                                        </strong>
                                        {{EXPIRY}}
                                    </p>
                                </div>
                
                                <!-- Security -->
                                <p style="
                                    margin: 25px 0 0;
                                    font-size: 13px;
                                    line-height: 1.7;
                                    color: #777777;
                                ">
                                    If you did not request this, you can safely
                                    ignore this email.
                                </p>
                
                            </div>
                
                            <!-- Footer -->
                            <div style="
                                background-color: #f5f7fb;
                                padding: 20px;
                                text-align: center;
                            ">
                                <p style="
                                    margin: 0;
                                    font-size: 12px;
                                    color: #888888;
                                ">
                                    © 2026 VibeStay. All rights reserved.
                                </p>
                
                                <p style="
                                    margin: 6px 0 0;
                                    font-size: 12px;
                                    color: #aaaaaa;
                                ">
                                    Your stay starts here.
                                </p>
                            </div>
                
                        </div>
                    </div>
                
                </body>
                </html>
                """
                .replace("{{TITLE}}", title)
                .replace("{{MESSAGE}}", message)
                .replace("{{BUTTON}}", buttonText)
                .replace("{{LINK}}", link)
                .replace("{{EXPIRY}}", expiryMessage);
    }

    public void sendResetLink(PasswordResetToken token, User user) {
        String link = "http://localhost:8000/auth/users/reset-link?token=" + token.getToken();


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
        emailDetails.setSubject("Reset");
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
}
