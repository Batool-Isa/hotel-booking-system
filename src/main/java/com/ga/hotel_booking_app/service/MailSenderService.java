package com.ga.hotel_booking_app.service;

import com.ga.hotel_booking_app.dto.EmailDetails;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.File;

@Service
@Async("emailThreadExecutor")
public class MailSenderService {
    @Autowired
    private JavaMailSender javaMailSender;
    @Value("${spring.mail.username}")
    private String sender;
    private static final Logger log = LoggerFactory.getLogger(MailSenderService.class);


    public void sendSimpleMail(EmailDetails details) {
        System.out.println("Email sent by thread "+ Thread.currentThread().getName());
        try {
            MimeMessage mimeMessage = javaMailSender.createMimeMessage();

            MimeMessageHelper helper =
                    new MimeMessageHelper(mimeMessage, true);

            helper.setFrom(sender);
            helper.setTo(details.getRecipient());
            helper.setSubject(details.getSubject());
            helper.setText(details.getMsgBody(), true);

            javaMailSender.send(mimeMessage);
            log.info("Mail sent Successfully");
        } catch (Exception e) {
            log.error("Error while sending mail");
        }
    }

    public void sendMailWithAttachment(EmailDetails details) {
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

            log.info("Mail sent Successfully");


        } catch (MessagingException e) {

            log.error("Error while sending mail");

        }
    }

    public void sendMailWithInlineImage(EmailDetails details, String contentId, byte[] image) {
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
            log.info("Mail Sent Successfully");
        } catch (Exception e) {
            log.error("Error while sending mail");
        }
    }
}
