package com.ThanhND05.url_shortener.billing.service;

import com.ThanhND05.url_shortener.billing.dto.response.PaymentTransactionResponse;
import com.ThanhND05.url_shortener.billing.dto.response.PaymentUrlResponse;
import com.ThanhND05.url_shortener.billing.dto.response.SubscriptionResponse;
import com.ThanhND05.url_shortener.billing.entity.PaymentTransaction;
import com.ThanhND05.url_shortener.billing.entity.Subscription;
import com.ThanhND05.url_shortener.billing.enums.PaymentStatus;
import com.ThanhND05.url_shortener.billing.enums.SubscriptionPlan;
import com.ThanhND05.url_shortener.billing.enums.SubscriptionStatus;
import com.ThanhND05.url_shortener.billing.repository.PaymentTransactionRepository;
import com.ThanhND05.url_shortener.billing.repository.SubscriptionRepository;
import com.ThanhND05.url_shortener.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service xử lý business logic cho Billing:
 * - Xem gói hiện tại
 * - Tạo payment → lấy URL VNPay
 * - Xử lý IPN callback → upgrade subscription
 * - Kiểm tra quota link (enforced khi tạo link)
 * - Scheduled jobs: expire subscriptions, reset monthly counters
 *
 * === GÓI DỊCH VỤ ===
 * FREE: 50 links/tháng, analytics 7 ngày, không có Link Rules/Custom Domain.
 * PRO (50.000đ/tháng): Unlimited links, analytics vĩnh viễn, full features.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BillingService {

    private static final long PRO_PRICE_VND = 50_000L; // 50.000 VND/tháng
    private static final int FREE_LINKS_PER_MONTH = 50;

    private final SubscriptionRepository subscriptionRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final VnPayService vnPayService;

    // ── SUBSCRIPTION ──────────────────────────────────────

    /**
     * Lấy hoặc tạo subscription cho user (lazy initialization).
     * Nếu user chưa có subscription → tạo mới với plan FREE.
     */
    @Transactional
    public Subscription getOrCreateSubscription(UUID userId) {
        return subscriptionRepository.findById(userId)
                .orElseGet(() -> {
                    Subscription sub = Subscription.builder()
                            .userId(userId)
                            .plan(SubscriptionPlan.FREE)
                            .status(SubscriptionStatus.ACTIVE)
                            .linksUsed(0)
                            .linksResetAt(Instant.now())
                            .build();
                    return subscriptionRepository.save(sub);
                });
    }

    /**
     * Xem thông tin gói hiện tại của user.
     */
    @Transactional
    public SubscriptionResponse getCurrentSubscription(UUID userId) {
        Subscription sub = getOrCreateSubscription(userId);
        return SubscriptionResponse.from(sub);
    }

    // ── PAYMENT ───────────────────────────────────────────

    /**
     * Tạo payment URL VNPay để nâng cấp lên Pro.
     *
     * @param userId    ID user
     * @param ipAddress IP của client
     * @return URL để redirect user tới VNPay
     */
    @Transactional
    public PaymentUrlResponse createProUpgradePayment(UUID userId, String ipAddress) {
        // Check xem user đã là Pro chưa
        Subscription sub = getOrCreateSubscription(userId);
        if (sub.isPro()) {
            throw new BusinessException("Bạn đang sử dụng gói Pro. Gói sẽ được gia hạn tự động khi thanh toán.");
        }

        // Tạo transaction PENDING
        String txnRef = vnPayService.generateTxnRef();
        PaymentTransaction transaction = PaymentTransaction.builder()
                .userId(userId)
                .txnRef(txnRef)
                .amount(PRO_PRICE_VND)
                .orderInfo("Nang cap goi Pro - URL Shortener")
                .status(PaymentStatus.PENDING)
                .build();
        paymentTransactionRepository.save(transaction);

        // Tạo payment URL
        String paymentUrl = vnPayService.createPaymentUrl(
                txnRef, PRO_PRICE_VND,
                "Nang cap goi Pro - URL Shortener",
                ipAddress);

        log.info("Created payment for user {} with txnRef={}", userId, txnRef);

        return PaymentUrlResponse.builder()
                .paymentUrl(paymentUrl)
                .txnRef(txnRef)
                .build();
    }

    /**
     * Xử lý IPN callback từ VNPay (Server-to-Server).
     *
     * @param params tất cả params VNPay gửi về
     * @return response code: "00" = OK, "97" = checksum fail, "02" = not found,
     *         "01" = đã xử lý
     */
    @Transactional
    public String processIpnCallback(Map<String, String> params) {
        // 1. Verify checksum
        if (!vnPayService.verifyChecksum(params)) {
            log.warn("VNPay IPN checksum verification failed!");
            return "97"; // Checksum invalid
        }

        String txnRef = params.get("vnp_TxnRef");
        String responseCode = params.get("vnp_ResponseCode");
        String vnpTxnNo = params.get("vnp_TransactionNo");
        String bankCode = params.get("vnp_BankCode");
        String payDate = params.get("vnp_PayDate");

        // 2. Tìm transaction theo txnRef
        PaymentTransaction transaction = paymentTransactionRepository.findByTxnRef(txnRef)
                .orElse(null);
        if (transaction == null) {
            log.warn("VNPay IPN: txnRef {} not found", txnRef);
            return "02"; // Order not found
        }

        // 3. Kiểm tra đã xử lý chưa (idempotent)
        if (transaction.getStatus() != PaymentStatus.PENDING) {
            log.info("VNPay IPN: txnRef {} already processed (status={})", txnRef, transaction.getStatus());
            return "01"; // Already processed
        }

        // 4. Cập nhật thông tin từ VNPay
        transaction.setVnpResponseCode(responseCode);
        transaction.setVnpTxnNo(vnpTxnNo);
        transaction.setVnpBankCode(bankCode);
        transaction.setVnpPayDate(payDate);

        // 5. Xử lý kết quả
        if ("00".equals(responseCode)) {
            // Thanh toán thành công → Upgrade
            transaction.setStatus(PaymentStatus.SUCCESS);
            paymentTransactionRepository.save(transaction);

            // Auto upgrade subscription
            Subscription sub = getOrCreateSubscription(transaction.getUserId());
            sub.upgradeToPro();
            subscriptionRepository.save(sub);

            log.info("✅ Payment SUCCESS for user {}. Upgraded to PRO. txnRef={}",
                    transaction.getUserId(), txnRef);
        } else {
            // Thanh toán thất bại
            transaction.setStatus(PaymentStatus.FAILED);
            paymentTransactionRepository.save(transaction);

            log.warn("❌ Payment FAILED for txnRef={}. Response code: {}", txnRef, responseCode);
        }

        return "00"; // Xác nhận đã xử lý
    }

    /**
     * Xử lý Return URL (VNPay redirect user về sau thanh toán).
     * Chỉ dùng để hiển thị kết quả, KHÔNG update trạng thái (IPN đã xử lý).
     *
     * @param params params từ VNPay
     * @return true nếu thanh toán thành công
     */
    public boolean processReturnUrl(Map<String, String> params) {
        if (!vnPayService.verifyChecksum(params)) {
            return false;
        }
        String responseCode = params.get("vnp_ResponseCode");
        return "00".equals(responseCode);
    }

    // ── QUOTA ENFORCEMENT ─────────────────────────────────

    /**
     * Kiểm tra user có được tạo thêm link không.
     * Gọi hàm này từ LinkService trước khi tạo link.
     *
     * @throws BusinessException nếu đã đạt giới hạn
     */
    @Transactional
    public void enforceCreateLinkQuota(UUID userId) {
        Subscription sub = getOrCreateSubscription(userId);

        // Pro → unlimited
        if (sub.isPro()) {
            return;
        }

        // Free → check 50 links/tháng
        // Reset counter nếu sang tháng mới
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        ZonedDateTime resetAt = sub.getLinksResetAt().atZone(ZoneOffset.UTC);
        if (now.getMonthValue() != resetAt.getMonthValue() || now.getYear() != resetAt.getYear()) {
            sub.setLinksUsed(0);
            sub.setLinksResetAt(Instant.now());
            subscriptionRepository.save(sub);
        }

        if (sub.getLinksUsed() >= FREE_LINKS_PER_MONTH) {
            throw new BusinessException(
                    "Bạn đã đạt giới hạn " + FREE_LINKS_PER_MONTH
                            + " links/tháng của gói Free. Vui lòng nâng cấp lên Pro để tạo không giới hạn.");
        }
    }

    /**
     * Tăng counter links_used sau khi tạo link thành công.
     * Gọi hàm này từ LinkService sau khi tạo link.
     */
    @Transactional
    public void incrementLinkUsage(UUID userId) {
        Subscription sub = getOrCreateSubscription(userId);
        sub.setLinksUsed(sub.getLinksUsed() + 1);
        subscriptionRepository.save(sub);
    }

    /**
     * Kiểm tra user có quyền dùng feature chỉ dành cho Pro không.
     * Dùng cho: Link Rules, A/B Testing, Custom Domain.
     *
     * @throws BusinessException nếu không phải Pro
     */
    @Transactional(readOnly = true)
    public void enforceProFeature(UUID userId, String featureName) {
        Subscription sub = getOrCreateSubscription(userId);
        if (!sub.isPro()) {
            throw new BusinessException(
                    "Tính năng \"" + featureName + "\" chỉ dành cho gói Pro. "
                            + "Vui lòng nâng cấp để sử dụng.");
        }
    }

    // ── PAYMENT HISTORY ───────────────────────────────────

    /**
     * Lấy lịch sử giao dịch của user.
     */
    @Transactional(readOnly = true)
    public List<PaymentTransactionResponse> getPaymentHistory(UUID userId) {
        return paymentTransactionRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(PaymentTransactionResponse::from)
                .toList();
    }

    // ── SCHEDULED JOBS ────────────────────────────────────

    /**
     * Job chạy mỗi giờ: kiểm tra và expire các subscription Pro đã hết hạn.
     */
    @Scheduled(fixedRate = 3600_000) // 1 giờ
    @Transactional
    public void expireSubscriptions() {
        List<Subscription> expired = subscriptionRepository.findExpiredSubscriptions(
                SubscriptionPlan.PRO, SubscriptionStatus.ACTIVE, Instant.now());

        for (Subscription sub : expired) {
            sub.expire();
            subscriptionRepository.save(sub);
            log.info("Subscription expired for user {}", sub.getUserId());
        }

        if (!expired.isEmpty()) {
            log.info("Expired {} Pro subscriptions.", expired.size());
        }
    }
}
