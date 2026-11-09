package com.nhom7.truyenapi.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class JavaMailEmailService implements EmailService {
    private static final Logger logger = LoggerFactory.getLogger(JavaMailEmailService.class);

    private final ObjectProvider<JavaMailSender> mailSenders;
    private final String mailHost;
    private final String fromAddress;

    public JavaMailEmailService(
            ObjectProvider<JavaMailSender> mailSenders,
            @Value("${spring.mail.host:}") String mailHost,
            @Value("${app.mail.from:}") String fromAddress) {
        this.mailSenders = mailSenders;
        this.mailHost = mailHost;
        this.fromAddress = fromAddress;
    }

    @Override
    public void sendPasswordResetEmail(String recipient, String token, long expiryMinutes) {
        JavaMailSender mailSender = mailSenders.getIfAvailable();
        if (mailHost.isBlank() || fromAddress.isBlank() || mailSender == null) {
            logger.warn("Chưa cấu hình email gửi mã đặt lại mật khẩu");
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(recipient);
        message.setSubject("Hướng dẫn đặt lại mật khẩu");
        message.setText("Mã đặt lại mật khẩu của bạn là:\n\n"
                + token
                + "\n\nMã có hiệu lực trong "
                + expiryMinutes
                + " phút. Nếu bạn không yêu cầu đặt lại mật khẩu, hãy bỏ qua email này.");
        mailSender.send(message);
    }
}
