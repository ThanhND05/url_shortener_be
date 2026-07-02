package com.ThanhND05.url_shortener.analytics.service;

import com.ThanhND05.url_shortener.analytics.entity.ClickEvent;
import com.ThanhND05.url_shortener.analytics.repository.ClickEventRepository;
import com.ThanhND05.url_shortener.analytics.repository.LinkCounterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service xử lý click ingestion — CHỈ CÒN LÀ REFERENCE.
 *
 * === TRƯỚC (Legacy — @Async + ApplicationEventPublisher) ===
 * RedirectService publish LinkClickedEvent → @EventListener ở đây nhận event
 *   → INSERT 1 ClickEvent vào DB per click → UPSERT LinkCounter per click.
 * Vấn đề: 10.000 click/s = 10.000 async thread = 20.000 DB ops = sập server.
 *
 * === SAU (Kafka — Hiện tại) ===
 * RedirectService → ClickEventProducer.send() → Kafka topic "click-events"
 *   → ClickEventConsumer gom 500 messages → batch INSERT DB 1 lần.
 *
 * Class này KHÔNG CÒN @EventListener nữa.
 * Logic parse User-Agent + insert ClickEvent đã được chuyển sang ClickEventConsumer.
 *
 * @see com.ThanhND05.url_shortener.analytics.kafka.ClickEventConsumer
 * @see com.ThanhND05.url_shortener.analytics.kafka.ClickEventProducer
 * @deprecated Replaced by Kafka-based ClickEventProducer + ClickEventConsumer.
 *             Giữ lại class này để tham chiếu logic cũ. Có thể xóa khi đã ổn định.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Deprecated
public class ClickIngestionService {

    private final ClickEventRepository clickEventRepository;
    private final LinkCounterRepository linkCounterRepository;

    // Logic xử lý đã được chuyển sang ClickEventConsumer (Kafka batch consumer).
    // Xem: analytics/kafka/ClickEventConsumer.java
}
