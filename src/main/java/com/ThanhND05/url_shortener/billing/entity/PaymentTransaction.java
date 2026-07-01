package com.ThanhND05.url_shortener.billing.entity;

import com.ThanhND05.url_shortener.billing.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

/**
 * Entity đại diện cho bảng billing.payment_transactions — lịch sử giao dịch VNPay.
 *
 * Khi user bấm "Nâng cấp Pro":
 *   1. Tạo bản ghi PENDING với txnRef unique.
 *   2. Redirect user tới VNPay payment URL.
 *   3. VNPay IPN callback → cập nhật SUCCESS/FAILED.
 *
 * txn_ref: mã đơn hàng unique gửi cho VNPay (format: URLSHORT_{userId}_{timestamp}).
 * vnp_txn_no: mã giao dịch phía VNPay trả về.
 */
@Entity
@Table(name = "payment_transactions", schema = "billing")
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /** Mã đơn hàng unique gửi cho VNPay. */
    @Column(name = "txn_ref", nullable = false, unique = true, length = 100)
    private String txnRef;

    /** Số tiền (đơn vị VND, KHÔNG nhân 100 — VnPayService sẽ nhân khi build URL). */
    @Column(nullable = false)
    private long amount;

    /** Mô tả đơn hàng. */
    @Column(name = "order_info")
    private String orderInfo;

    /** Mã giao dịch phía VNPay (nhận từ IPN). */
    @Column(name = "vnp_txn_no", length = 100)
    private String vnpTxnNo;

    /** Response code từ VNPay (00 = thành công). */
    @Column(name = "vnp_response_code", length = 10)
    private String vnpResponseCode;

    /** Mã ngân hàng thanh toán. */
    @Column(name = "vnp_bank_code", length = 30)
    private String vnpBankCode;

    /** Thời gian thanh toán phía VNPay. */
    @Column(name = "vnp_pay_date", length = 30)
    private String vnpPayDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private PaymentStatus status = PaymentStatus.PENDING;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
