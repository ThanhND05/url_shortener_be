package com.ThanhND05.url_shortener.platform.listener;

import com.ThanhND05.url_shortener.iam.event.AccountLockedEvent;
import com.ThanhND05.url_shortener.iam.event.PasswordChangedEvent;
import com.ThanhND05.url_shortener.iam.event.UserCreatedEvent;
import com.ThanhND05.url_shortener.link.event.LinkCreatedEvent;
import com.ThanhND05.url_shortener.platform.service.AuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Catch-all event listener — lắng nghe tất cả domain events → ghi audit log.
 *
 * Cơ chế:
 * - Các module (IAM, Link) publish events qua ApplicationEventPublisher.
 * - Listener này ở module Platform nhận TẤT CẢ events → ghi nhật ký.
 * - @Async: chạy trên thread riêng, KHÔNG block business logic.
 *
 * Events được lắng nghe:
 * - UserCreatedEvent   → ghi "USER_CREATED"
 * - PasswordChangedEvent → ghi "PASSWORD_CHANGED"
 * - AccountLockedEvent  → ghi "ACCOUNT_LOCKED"
 * - LinkCreatedEvent    → ghi "LINK_CREATED"
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuditEventListener {

    private final AuditService auditService;

    @Async
    @EventListener
    public void onUserCreated(UserCreatedEvent event) {
        auditService.logAction(
                event.userId(), "USER_CREATED",
                "User", event.userId().toString(),
                String.format("{\"email\":\"%s\"}", event.email()));
    }

    @Async
    @EventListener
    public void onPasswordChanged(PasswordChangedEvent event) {
        auditService.logAction(
                event.userId(), "PASSWORD_CHANGED",
                "User", event.userId().toString(),
                "{}");
    }

    @Async
    @EventListener
    public void onAccountLocked(AccountLockedEvent event) {
        auditService.logAction(
                event.userId(), "ACCOUNT_LOCKED",
                "User", event.userId().toString(),
                "{}");
    }

    @Async
    @EventListener
    public void onLinkCreated(LinkCreatedEvent event) {
        auditService.logAction(
                event.ownerId(), "LINK_CREATED",
                "Link", event.publicId().toString(),
                String.format("{\"shortCode\":\"%s\"}", event.shortCode()));
    }
}
