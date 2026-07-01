package com.ThanhND05.url_shortener.billing.repository;

import com.ThanhND05.url_shortener.billing.entity.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, UUID> {

    /** Tìm giao dịch theo txnRef (mã đơn hàng gửi cho VNPay). */
    Optional<PaymentTransaction> findByTxnRef(String txnRef);

    /** Lấy lịch sử giao dịch của user, sắp xếp mới nhất trước. */
    List<PaymentTransaction> findByUserIdOrderByCreatedAtDesc(UUID userId);
}
