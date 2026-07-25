package com.ThanhND05.url_shortener.billing.repository;

import com.ThanhND05.url_shortener.billing.entity.PaymentTransaction;
import com.ThanhND05.url_shortener.billing.enums.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, UUID> {

    /** Tìm giao dịch theo txnRef (mã đơn hàng gửi cho VNPay). */
    Optional<PaymentTransaction> findByTxnRef(String txnRef);

    /** Lấy lịch sử giao dịch của user, sắp xếp mới nhất trước. */
    List<PaymentTransaction> findByUserIdOrderByCreatedAtDesc(UUID userId);

    // ── ADMIN QUERIES ────────────────────────────────────

    /** Đếm giao dịch theo status. */
    long countByStatus(PaymentStatus status);

    /** Tổng doanh thu (SUM amount) cho giao dịch thành công sau mốc thời gian. */
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM PaymentTransaction t WHERE t.status = 'SUCCESS' AND t.createdAt >= :from")
    long sumRevenueAfter(Instant from);

    /** Tổng doanh thu tất cả thời gian (giao dịch SUCCESS). */
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM PaymentTransaction t WHERE t.status = 'SUCCESS'")
    long sumTotalRevenue();

    /** List tất cả giao dịch, sắp xếp mới nhất (cho admin). */
    Page<PaymentTransaction> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /** List giao dịch filter theo status (cho admin). */
    Page<PaymentTransaction> findByStatusOrderByCreatedAtDesc(PaymentStatus status, Pageable pageable);

    /** List giao dịch filter theo userId (cho admin xem giao dịch của 1 user). */
    Page<PaymentTransaction> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    /** List giao dịch SUCCESS trong khoảng thời gian (cho revenue timeseries). */
    @Query("SELECT t FROM PaymentTransaction t WHERE t.status = 'SUCCESS' AND t.createdAt >= :from AND t.createdAt <= :to ORDER BY t.createdAt ASC")
    List<PaymentTransaction> findSuccessfulTransactionsBetween(Instant from, Instant to);
}
