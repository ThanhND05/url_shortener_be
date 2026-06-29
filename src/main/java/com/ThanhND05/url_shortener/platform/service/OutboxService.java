package com.ThanhND05.url_shortener.platform.service;

import com.ThanhND05.url_shortener.platform.entity.OutboxEvent;
import com.ThanhND05.url_shortener.platform.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Service quản lý Transactional Outbox Pattern.
 *
 * === FLOW ===
 * 1. Business service gọi addEvent() TRONG CÙNG transaction:
 *    VD: linkService.createLink() {
 *          linkRepository.save(link);
 *          outboxService.addEvent("Link", link.getPublicId(), "LinkCreated", payloadJson);
 *        } // → cả link + outbox event commit cùng lúc
 *
 * 2. Poller (mỗi 5 giây) đọc events chưa publish → "gửi" (hiện chỉ log).
 *    Trong production: gửi tới Kafka/RabbitMQ/webhook.
 *
 * 3. Nếu gửi thành công → set published_at.
 *    Nếu fail → increment retry_count + lưu last_error.
 *    Sau N retries → bỏ qua (dead letter).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxService {

    private final OutboxEventRepository outboxEventRepository;
    private static final int MAX_RETRIES = 5;

    /**
     * Thêm event vào outbox — GỌI TRONG CÙNG TRANSACTION với business logic.
     */
    @Transactional
    public void addEvent(String aggregateType, String aggregateId,
                         String eventType, String payload) {
        OutboxEvent event = OutboxEvent.builder()
                .aggregateType(aggregateType)
                .aggregateId(aggregateId)
                .eventType(eventType)
                .payload(payload)
                .build();
        outboxEventRepository.save(event);
    }

    /**
     * Poller — chạy mỗi 5 giây, đọc events chưa publish → xử lý.
     * Hiện tại chỉ log (MVP). Production: gửi tới message broker.
     */
    @Scheduled(fixedRate = 5000)
    @Transactional
    public void pollAndPublish() {
        List<OutboxEvent> events = outboxEventRepository
                .findByPublishedAtIsNullOrderByCreatedAtAsc();

        for (OutboxEvent event : events) {
            if (event.getRetryCount() >= MAX_RETRIES) {
                log.warn("Outbox event {} exceeded max retries, skipping", event.getId());
                continue;
            }
            try {
                // === MVP: Chỉ log. Production: gửi tới Kafka/RabbitMQ/webhook ===
                log.info("Publishing outbox event: type={} aggregate={}:{}",
                        event.getEventType(), event.getAggregateType(), event.getAggregateId());

                event.setPublishedAt(Instant.now());
                outboxEventRepository.save(event);

            } catch (Exception e) {
                event.setRetryCount(event.getRetryCount() + 1);
                event.setLastError(e.getMessage());
                outboxEventRepository.save(event);
                log.error("Failed to publish outbox event {}: {}", event.getId(), e.getMessage());
            }
        }
    }
}
