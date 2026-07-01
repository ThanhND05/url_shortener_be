package com.ThanhND05.url_shortener.billing.repository;

import com.ThanhND05.url_shortener.billing.entity.Subscription;
import com.ThanhND05.url_shortener.billing.enums.SubscriptionPlan;
import com.ThanhND05.url_shortener.billing.enums.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {

    /**
     * Tìm tất cả subscription Pro đã hết hạn nhưng chưa bị đánh dấu EXPIRED.
     * Dùng cho scheduled job expire subscriptions.
     */
    @Query("""
        SELECT s FROM Subscription s
        WHERE s.plan = :plan
          AND s.status = :status
          AND s.expiresAt < :now
    """)
    List<Subscription> findExpiredSubscriptions(SubscriptionPlan plan,
                                                 SubscriptionStatus status,
                                                 Instant now);

    /**
     * Reset link counter cho tất cả subscriptions đã qua tháng mới.
     */
    @Modifying
    @Query("""
        UPDATE Subscription s
        SET s.linksUsed = 0, s.linksResetAt = :now
        WHERE s.linksResetAt < :startOfMonth
    """)
    int resetMonthlyLinkCounters(Instant now, Instant startOfMonth);
}
