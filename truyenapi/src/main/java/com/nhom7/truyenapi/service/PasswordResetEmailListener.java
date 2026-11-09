package com.nhom7.truyenapi.service;

import com.nhom7.truyenapi.event.PasswordResetRequestedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class PasswordResetEmailListener {
    private static final Logger logger = LoggerFactory.getLogger(PasswordResetEmailListener.class);

    private final EmailService emailService;

    public PasswordResetEmailListener(EmailService emailService) {
        this.emailService = emailService;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void sendPasswordResetEmail(PasswordResetRequestedEvent event) {
        try {
            emailService.sendPasswordResetEmail(
                    event.email(), event.rawToken(), event.expiryMinutes());
        } catch (RuntimeException exception) {
            logger.error("Không gửi được email đặt lại mật khẩu ({})",
                    exception.getClass().getSimpleName());
        }
    }
}
