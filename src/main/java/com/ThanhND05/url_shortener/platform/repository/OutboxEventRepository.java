package com.ThanhND05.url_shortener.platform.repository;

import com.ThanhND05.url_shortener.platform.entity.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    /** Lấy events chưa publish, sắp theo thời gian tạo (FIFO). */
    List<OutboxEvent> findByPublishedAtIsNullOrderByCreatedAtAsc();

    /** Lấy events đã fail quá N lần retry (dead letter). */
    List<OutboxEvent> findByPublishedAtIsNullAndRetryCountGreaterThanEqual(int maxRetries);
}
