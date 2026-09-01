package com.technicalblog.service;

import com.technicalblog.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Sends the account emails. When no SMTP server is configured the link is written to the
 * log instead, so the whole flow can be exercised locally without a mail account.
 */
@Service
public class MailService {

    private static final Logger log = LoggerFactory.getLogger(MailService.class);

    private final ObjectProvider<JavaMailSender> mailSender;
    private final String fromAddress;
    private final String frontendUrl;

    public MailService(ObjectProvider<JavaMailSender> mailSender,
                       @Value("${app.mail.from}") String fromAddress,
                       @Value("${app.frontend-url}") String frontendUrl) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
        this.frontendUrl = frontendUrl;
    }

    public void sendVerification(User user, String token) {
        String link = frontendUrl + "/verify-email?token=" + token;
        deliver(user.getEmail(),
                "Confirm your email address",
                "Hi " + user.getUsername() + ",\n\n"
                        + "Confirm your email address to finish creating your account:\n\n"
                        + link + "\n\n"
                        + "The link is valid for 24 hours. If you did not sign up, ignore this message.",
                link);
    }

    public void sendPasswordReset(User user, String token) {
        String link = frontendUrl + "/reset-password?token=" + token;
        deliver(user.getEmail(),
                "Reset your password",
                "Hi " + user.getUsername() + ",\n\n"
                        + "Use this link to choose a new password:\n\n"
                        + link + "\n\n"
                        + "The link is valid for 1 hour. If you did not ask for this, ignore this message "
                        + "and your password stays unchanged.",
                link);
    }

    private void deliver(String to, String subject, String body, String link) {
        JavaMailSender sender = mailSender.getIfAvailable();

        if (sender == null) {
            // No SMTP configured: print the link so local development still works.
            log.warn("Mail is not configured. Link for {} ({}): {}", to, subject, link);
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            sender.send(message);
            log.info("Sent \"{}\" to {}", subject, to);
        } catch (Exception ex) {
            // A failed send must not reveal anything to the caller or break the request.
            log.error("Could not send \"{}\" to {}", subject, to, ex);
        }
    }
}
