package com.example.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Logger;
import java.util.logging.Level;

@Service
public class EmailService {

    private static final Logger logger = Logger.getLogger(EmailService.class.getName());

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${spring.mail.username:noreply@aistudio.com}")
    private String fromEmail;

    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    @Value("${app.name:AI Studio}")
    private String appName;

    @Value("${app.email.enabled:true}")
    private boolean emailEnabled;

    @Autowired
    public EmailService(JavaMailSender mailSender, TemplateEngine templateEngine) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
    }

    @Async("emailExecutor")
    public void sendRegistrationEmail(String toEmail, String fullName) {
        if (!emailEnabled) {
            logger.warning("[Email] Email service is disabled. Skipping registration email to: " + toEmail);
            return;
        }

        try {
            logger.info("[Email] Attempting to send registration email to: " + toEmail);
            
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Welcome to " + appName + "! 🎉");

            Context context = new Context();
            context.setVariable("fullName", fullName != null ? fullName : "User");
            context.setVariable("appName", appName);
            context.setVariable("frontendUrl", frontendUrl);
            context.setVariable("currentYear", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy")));

            String htmlContent = templateEngine.process("emails/registration", context);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            logger.info("[Email] Registration email sent successfully to: " + toEmail);
        } catch (MessagingException e) {
            logger.log(Level.SEVERE, "[Email] Failed to send registration email to " + toEmail, e);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "[Email] Unexpected error sending registration email to " + toEmail, e);
        }
    }

    @Async("emailExecutor")
    public void sendLoginNotification(String toEmail, String fullName, String loginTime) {
        if (!emailEnabled) {
            logger.warning("[Email] Email service is disabled. Skipping login notification to: " + toEmail);
            return;
        }

        try {
            logger.info("[Email] Attempting to send login notification to: " + toEmail);
            
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("New Login to " + appName);

            Context context = new Context();
            context.setVariable("fullName", fullName != null ? fullName : "User");
            context.setVariable("appName", appName);
            context.setVariable("loginTime", loginTime);
            context.setVariable("frontendUrl", frontendUrl);
            context.setVariable("currentYear", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy")));

            String htmlContent = templateEngine.process("emails/login-notification", context);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            logger.info("[Email] Login notification sent successfully to: " + toEmail);
        } catch (MessagingException e) {
            logger.log(Level.SEVERE, "[Email] Failed to send login notification to " + toEmail, e);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "[Email] Unexpected error sending login notification to " + toEmail, e);
        }
    }

    public void sendSimpleEmail(String toEmail, String subject, String text) {
        if (!emailEnabled) {
            logger.warning("[Email] Email service is disabled. Skipping simple email to: " + toEmail);
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject(subject);
            message.setText(text);
            mailSender.send(message);
            logger.info("[Email] Simple email sent successfully to: " + toEmail);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "[Email] Failed to send simple email to " + toEmail, e);
        }
    }

    @Async("emailExecutor")
    public void sendPasswordResetEmail(String toEmail, String fullName, String resetUrl) {
        if (!emailEnabled) {
            logger.warning("[Email] Password reset requested while email delivery is disabled.");
            return;
        }
        String greeting = fullName == null || fullName.isBlank() ? "Hello" : "Hello " + fullName;
        String body = greeting + ",\n\n"
                + "Use the secure link below to reset your " + appName + " password. "
                + "The link expires in one hour and can be used only once.\n\n"
                + resetUrl + "\n\n"
                + "If you did not request this change, ignore this message.";
        sendSimpleEmail(toEmail, "Reset your " + appName + " password", body);
    }

    public boolean isConfigured() {
        return emailEnabled && fromEmail != null && !fromEmail.equals("noreply@aistudio.com");
    }
}
