package com.ThanhND05.url_shortener.analytics.kafka;

import com.ThanhND05.url_shortener.analytics.dto.ClickEventMessage;
import com.ThanhND05.url_shortener.common.config.KafkaConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Kafka Producer cho click events — bắn message vào topic "click-events".
 *
 * === SO SÁNH TRƯỚC/SAU ===
 *
 * TRƯỚC (ApplicationEventPublisher):
 * RedirectService → publishEvent(LinkClickedEvent)
 * → @Async ClickIngestionService.handleClickEvent()
 * → clickEventRepository.save(clickEvent) ← 1 INSERT/click
 * → linkCounterRepository.incrementClickCount() ← 1 UPSERT/click
 *
 * Vấn đề:
 * - 10.000 click/s → 10.000 async thread → cạn kiệt thread pool
 * - Server crash → event trong RAM mất trắng (fire-and-forget)
 * - Mỗi click = 2 DB operations → 20.000 DB ops/s → bottleneck
 *
 * SAU (Kafka):
 * RedirectService → clickEventProducer.send(message)
 * → KafkaTemplate.send() (non-blocking, < 1ms, message lưu trên Kafka disk)
 * → ClickEventConsumer gom 500 messages → batch INSERT DB 1 lần
 *
 * Ưu điểm:
 * - Redirect response < 5ms (không chờ DB)
 * - Server crash → message vẫn nằm trong Kafka → consumer đọc lại
 * - 500 click = 1 batch INSERT → giảm 500x DB round-trip
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ClickEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Gửi click event message tới Kafka topic.
     *
     * @param message click event data đã serialize-ready
     *
     *                Key = linkId.toString() → đảm bảo tất cả click của cùng 1 link
     *                rơi vào cùng 1 partition → giữ thứ tự cho analytics
     *                aggregation.
     */
    public void send(ClickEventMessage message) {
        String key = message.getLinkId() != null ? message.getLinkId().toString() : "unknown";
        kafkaTemplate.send(KafkaConfig.TOPIC_CLICK_EVENTS, key, message);
        log.debug("Sent click event to Kafka: linkId={}, shortCode={}",
                message.getLinkId(), message.getShortCode());
    }
}
