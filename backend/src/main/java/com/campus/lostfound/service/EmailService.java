package com.campus.lostfound.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    @Value("${app.mail.from:lostandfound@campus.edu}")
    private String mailFrom;

    @Value("${app.auth.dev-otp-enabled:true}")
    private boolean devOtpEnabled;

    @Value("${app.mail.enabled:${MAIL_ENABLED:false}}")
    private boolean mailEnabled;

    private final JavaMailSender mailSender;

    public EmailService(@Autowired(required = false) JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public boolean isDevOtpEnabled() {
        return devOtpEnabled;
    }

    public boolean isMailEnabled() {
        return mailEnabled;
    }

    public boolean sendVerificationCode(String toEmail, String otpCode) {
        boolean emailDispatched = false;

        // In development mode: log OTP to server logs for fast debugging
        if (devOtpEnabled) {
            logger.info("================================================================================");
            logger.info("[AUTH DEV OTP] One-Time Verification Code for <{}>: [{}] (Valid for 10 minutes)", toEmail, otpCode);
            logger.info("================================================================================");
        } else {
            // In production mode: NEVER log the OTP code
            logger.info("Dispatching verification email to <{}>", toEmail);
        }

        // Only attempt real SMTP delivery if explicitly enabled and mailSender is present
        if (mailEnabled && mailSender != null) {
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setFrom(mailFrom);
                message.setTo(toEmail);
                message.setSubject("Your Campus Lost & Found Verification Code: " + otpCode);
                message.setText(String.format("""
                    Hello,

                    Your one-time verification code for Campus Lost & Found AI is:

                    %s

                    This code is valid for 10 minutes. Please do not share this code with anyone.

                    Best regards,
                    Campus Lost & Found System
                    """, otpCode));

                mailSender.send(message);
                emailDispatched = true;
                logger.info("Verification code email successfully dispatched to {}", toEmail);
            } catch (Exception e) {
                logger.warn("Could not send SMTP email to {}: {}. Dev OTP enabled: {}", toEmail, e.getMessage(), devOtpEnabled);
                if (!devOtpEnabled) {
                    throw new RuntimeException("Email delivery service is currently unavailable. Please contact system support.");
                }
            }
        } else if (!devOtpEnabled) {
            logger.error("Production mode active but mail delivery is disabled (app.mail.enabled=false).");
            throw new RuntimeException("Email delivery is not configured on this server.");
        } else {
            logger.info("[AUTH DEV OTP] Real SMTP delivery is disabled (MAIL_ENABLED=false). Development mode code issued.");
        }

        return emailDispatched;
    }
}
