package com.ThanhND05.url_shortener.common.config;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

/**
 * Kafka configuration — tạo topics và cấu hình batch consumer.
 *
 * === VÌ SAO DÙNG KAFKA? ===
 * Trước đây: RedirectService → @Async → ClickIngestionService → INSERT DB
 * 1-by-1.
 * Vấn đề:
 * - 10.000 click/s → 10.000 thread → cạn kiệt thread pool → server sập.
 * - Server crash → tất cả event trong RAM bị mất trắng.
 *
 * Sau khi dùng Kafka:
 * RedirectService → KafkaProducer.send() (non-blocking, < 1ms)
 * → Kafka broker lưu message bền vững (disk, replicated)
 * → Consumer gom 500 messages → Batch INSERT DB 1 lần
 *
 * Kết quả:
 * - Redirect response KHÔNG bị chậm bởi DB insert.
 * - Server crash → messages vẫn nằm trong Kafka → consumer đọc lại khi start.
 * - Batch insert 500 rows/lần → giảm 500x số lượng DB round-trip.
 *
 * === TOPICS ===
 * - click-events: Lượt click link → ClickEventConsumer batch insert.
 * - audit-events: Audit log actions → AuditLogConsumer batch insert.
 */
import org.springframework.beans.factory.annotation.Value;

@Configuration
@EnableKafka
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    // Topic names — dùng constant để đảm bảo nhất quán giữa Producer/Consumer
    public static final String TOPIC_CLICK_EVENTS = "click-events";
    public static final String TOPIC_AUDIT_EVENTS = "audit-events";

    /**
     * Topic click-events: 3 partitions để consumer có thể scale horizontally.
     * Retention mặc định 7 ngày (Kafka broker config).
     */
    @Bean
    public NewTopic clickEventsTopic() {
        return TopicBuilder.name(TOPIC_CLICK_EVENTS)
                .partitions(3)
                .replicas(1) // Dev: 1 replica. Production nên >= 2.
                .build();
    }

    /**
     * Topic audit-events: 2 partitions (volume thấp hơn click).
     */
    @Bean
    public NewTopic auditEventsTopic() {
        return TopicBuilder.name(TOPIC_AUDIT_EVENTS)
                .partitions(2)
                .replicas(1)
                .build();
    }

    /**
     * Batch consumer factory — Consumer nhận List<message> thay vì từng message.
     *
     * Flow:
     * Kafka poll() → gom tối đa 500 records (max.poll.records) → gửi
     * cho @KafkaListener.
     * → Consumer batch insert toàn bộ → commit offset.
     *
     * Nếu batch insert fail → offset KHÔNG commit → Kafka resend → at-least-once
     * delivery.
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> batchFactory(
            ConsumerFactory<String, Object> consumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setBatchListener(true); // ← QUAN TRỌNG: bật batch mode
        return factory;
    }

    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        // Địa chỉ máy chủ Kafka
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);

        // Cấu hình Serializer: Key là String, Value (Event) sẽ được parse ra JSON
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    @Bean
    public ConsumerFactory<String, Object> consumerFactory() {
        Map<String, Object> configProps = new HashMap<>();

        // Địa chỉ Kafka Broker
        configProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);

        // Cấu hình Group ID mặc định (hoặc có thể điều chỉnh lại theo dự án)
        configProps.put(ConsumerConfig.GROUP_ID_CONFIG, "url-shortener-group");

        // Cấu hình giải mã dữ liệu (Deserializer): Key là String
        configProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        // Khởi tạo JsonDeserializer cho Value và cấu hình tin tưởng tất cả các package
        // (*)
        // Điều này rất quan trọng để tránh lỗi bảo mật khi nhận các Event Object từ
        // Kafka chuyển về Java Object
        JsonDeserializer<Object> jsonDeserializer = new JsonDeserializer<>();
        jsonDeserializer.addTrustedPackages("*");

        return new DefaultKafkaConsumerFactory<>(
                configProps,
                new StringDeserializer(),
                jsonDeserializer);
    }
}
