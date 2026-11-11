package com.ga.userservice.service;

import com.ga.userservice.dto.EmailDetails;
import com.ga.userservice.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.math.BigDecimal;


@Service
public class EmailService {
    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    @Autowired
    MailSenderService mailSenderService;

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
        mailSenderService.sendSimpleMail(emailDetails);
        System.out.println("sending reset password email");
    }

    public void sendVerificationEmail(EmailVerificationToken emailVerificationToken) {
        String verificationLink =
                "http://localhost:8081/auth/users/verify-email?token="
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






   }
