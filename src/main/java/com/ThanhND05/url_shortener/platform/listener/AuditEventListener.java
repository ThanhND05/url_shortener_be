package com.ThanhND05.url_shortener.platform.listener;

import com.ThanhND05.url_shortener.iam.event.AccountLockedEvent;
import com.ThanhND05.url_shortener.iam.event.PasswordChangedEvent;
import com.ThanhND05.url_shortener.iam.event.UserCreatedEvent;
import com.ThanhND05.url_shortener.link.event.LinkCreatedEvent;
import com.ThanhND05.url_shortener.platform.dto.AuditLogMessage;
import com.ThanhND05.url_shortener.platform.kafka.AuditLogProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Catch-all event listener — lắng nghe domain events → bắn vào Kafka.
 *
 * === THAY ĐỔI QUAN TRỌNG ===
 * TRƯỚC: Nhận event → auditService.logAction() → INSERT DB 1-by-1 (trực tiếp).
 * SAU:   Nhận event → auditLogProducer.send() → Kafka topic "audit-events"
 *          → AuditLogConsumer gom batch → batch INSERT DB.
 *
 * Lý do thay đổi:
 * - Event trong RAM → server crash = mất audit log (vi phạm compliance).
 * - Kafka lưu message bền vững → không bao giờ mất (ngay cả khi server sập).
 * - Batch insert giảm DB contention khi có hàng nghìn event cùng lúc.
 *
 * Lưu ý: @Async vẫn giữ để listener KHÔNG block business logic gốc.
 * Tuy nhiên send() đã non-blocking (~1ms), nên @Async chỉ là layer phòng thủ thêm.
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

    private final AuditLogProducer auditLogProducer;

    @Async
    @EventListener
    public void onUserCreated(UserCreatedEvent event) {
        auditLogProducer.send(AuditLogMessage.builder()
                .actorId(event.userId().toString())
                .action("USER_CREATED")
                .resourceType("User")
                .resourceId(event.userId().toString())
                .metadata(String.format("{\"email\":\"%s\"}", event.email()))
                .timestamp(System.currentTimeMillis())
                .build());
    }

    @Async
    @EventListener
    public void onPasswordChanged(PasswordChangedEvent event) {
        auditLogProducer.send(AuditLogMessage.builder()
                .actorId(event.userId().toString())
                .action("PASSWORD_CHANGED")
                .resourceType("User")
                .resourceId(event.userId().toString())
                .metadata("{}")
                .timestamp(System.currentTimeMillis())
                .build());
    }

    @Async
    @EventListener
    public void onAccountLocked(AccountLockedEvent event) {
        auditLogProducer.send(AuditLogMessage.builder()
                .actorId(event.userId().toString())
                .action("ACCOUNT_LOCKED")
                .resourceType("User")
                .resourceId(event.userId().toString())
                .metadata("{}")
                .timestamp(System.currentTimeMillis())
                .build());
    }

    @Async
    @EventListener
    public void onLinkCreated(LinkCreatedEvent event) {
        auditLogProducer.send(AuditLogMessage.builder()
                .actorId(event.ownerId().toString())
                .action("LINK_CREATED")
                .resourceType("Link")
                .resourceId(event.publicId().toString())
                .metadata(String.format("{\"shortCode\":\"%s\"}", event.shortCode()))
                .timestamp(System.currentTimeMillis())
                .build());
    }
}
