package com.ThanhND05.url_shortener.billing.service;

import com.ThanhND05.url_shortener.billing.dto.response.AdminBillingOverviewResponse;
import com.ThanhND05.url_shortener.billing.dto.response.AdminPaymentTransactionResponse;
import com.ThanhND05.url_shortener.billing.dto.response.DailyRevenueResponse;
import com.ThanhND05.url_shortener.billing.entity.PaymentTransaction;
import com.ThanhND05.url_shortener.billing.enums.PaymentStatus;
import com.ThanhND05.url_shortener.billing.repository.PaymentTransactionRepository;
import com.ThanhND05.url_shortener.billing.repository.SubscriptionRepository;
import com.ThanhND05.url_shortener.iam.entity.User;
import com.ThanhND05.url_shortener.iam.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service admin billing — cung cấp dữ liệu cho admin dashboard billing.
 *
 * Chức năng:
 * - Tổng quan doanh thu (overview): tổng revenue, phân theo period, subscription stats.
 * - Danh sách giao dịch (transactions): filter theo status/userId, phân trang.
 * - Revenue timeseries: doanh thu theo ngày cho chart.
 *
 * Tối ưu hóa:
 * - Cache overview với TTL ngắn (5 phút) để giảm query aggregate nặng.
 * - Batch lookup user emails qua IN query thay vì N+1.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminBillingService {

    private final PaymentTransactionRepository paymentTransactionRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;

    // ── OVERVIEW ──────────────────────────────────────────

    /**
     * Tổng quan doanh thu toàn hệ thống.
     * Cache 5 phút để tránh query SUM liên tục.
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "admin:billing:overview", unless = "#result == null")
    public AdminBillingOverviewResponse getOverview() {
        Instant now = Instant.now();
        Instant startOfToday = LocalDate.now(ZoneOffset.UTC).atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant sevenDaysAgo = now.minus(7, ChronoUnit.DAYS);
        Instant thirtyDaysAgo = now.minus(30, ChronoUnit.DAYS);

        long totalRevenue = paymentTransactionRepository.sumTotalRevenue();
        long revenueToday = paymentTransactionRepository.sumRevenueAfter(startOfToday);
        long revenue7Days = paymentTransactionRepository.sumRevenueAfter(sevenDaysAgo);
        long revenue30Days = paymentTransactionRepository.sumRevenueAfter(thirtyDaysAgo);

        long successCount = paymentTransactionRepository.countByStatus(PaymentStatus.SUCCESS);
        long failedCount = paymentTransactionRepository.countByStatus(PaymentStatus.FAILED);
        long pendingCount = paymentTransactionRepository.countByStatus(PaymentStatus.PENDING);

        long proUsers = subscriptionRepository.countActiveProUsers(now);
        long freeUsers = subscriptionRepository.countFreeUsers();

        return AdminBillingOverviewResponse.builder()
                .totalRevenue(totalRevenue)
                .totalSuccessTransactions(successCount)
                .totalFailedTransactions(failedCount)
                .totalPendingTransactions(pendingCount)
                .revenueToday(revenueToday)
                .revenue7Days(revenue7Days)
                .revenue30Days(revenue30Days)
                .totalProUsers(proUsers)
                .totalFreeUsers(freeUsers)
                .build();
    }

    // ── TRANSACTIONS ─────────────────────────────────────

    /**
     * Danh sách giao dịch toàn hệ thống (phân trang).
     * Có thể filter theo status hoặc userId.
     * Kết quả bao gồm email user.
     */
    @Transactional(readOnly = true)
    public Page<AdminPaymentTransactionResponse> listTransactions(
            PaymentStatus status, UUID userId, Pageable pageable) {

        Page<PaymentTransaction> page;
        if (userId != null) {
            page = paymentTransactionRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        } else if (status != null) {
            page = paymentTransactionRepository.findByStatusOrderByCreatedAtDesc(status, pageable);
        } else {
            page = paymentTransactionRepository.findAllByOrderByCreatedAtDesc(pageable);
        }

        // Batch lookup user emails (tránh N+1)
        Set<UUID> userIds = page.getContent().stream()
                .map(PaymentTransaction::getUserId)
                .collect(Collectors.toSet());
        Map<UUID, String> emailMap = resolveUserEmails(userIds);

        return page.map(tx -> toAdminResponse(tx, emailMap));
    }

    // ── REVENUE TIMESERIES ───────────────────────────────

    /**
     * Doanh thu theo ngày trong khoảng thời gian.
     * Dùng cho line/bar chart trên admin billing dashboard.
     *
     * @param days số ngày lấy dữ liệu (mặc định 30, tối đa 365).
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "admin:billing:timeseries", key = "#days", unless = "#result == null")
    public List<DailyRevenueResponse> getRevenueTimeseries(int days) {
        Instant now = Instant.now();
        Instant from = now.minus(days, ChronoUnit.DAYS);

        List<PaymentTransaction> transactions =
                paymentTransactionRepository.findSuccessfulTransactionsBetween(from, now);

        // Group by date, tính tổng revenue và count
        Map<LocalDate, long[]> dailyMap = new LinkedHashMap<>();

        // Khởi tạo tất cả ngày với giá trị 0 (để chart không bị thiếu ngày)
        LocalDate startDate = LocalDate.now(ZoneOffset.UTC).minusDays(days);
        LocalDate endDate = LocalDate.now(ZoneOffset.UTC);
        for (LocalDate d = startDate; !d.isAfter(endDate); d = d.plusDays(1)) {
            dailyMap.put(d, new long[]{0, 0}); // [revenue, count]
        }

        // Aggregate transactions
        for (PaymentTransaction tx : transactions) {
            LocalDate date = tx.getCreatedAt().atZone(ZoneOffset.UTC).toLocalDate();
            long[] values = dailyMap.computeIfAbsent(date, k -> new long[]{0, 0});
            values[0] += tx.getAmount();
            values[1] += 1;
        }

        return dailyMap.entrySet().stream()
                .map(e -> DailyRevenueResponse.builder()
                        .date(e.getKey())
                        .revenue(e.getValue()[0])
                        .transactionCount(e.getValue()[1])
                        .build())
                .toList();
    }

    // ── HELPERS ──────────────────────────────────────────

    /**
     * Batch resolve user IDs → emails.
     * Sử dụng findAllById (1 query IN) thay vì N queries.
     */
    private Map<UUID, String> resolveUserEmails(Set<UUID> userIds) {
        if (userIds.isEmpty()) return Map.of();

        List<User> users = userRepository.findAllById(userIds);
        return users.stream()
                .collect(Collectors.toMap(User::getId, User::getEmail));
    }

    private AdminPaymentTransactionResponse toAdminResponse(
            PaymentTransaction tx, Map<UUID, String> emailMap) {
        return AdminPaymentTransactionResponse.builder()
                .id(tx.getId())
                .userId(tx.getUserId())
                .userEmail(emailMap.getOrDefault(tx.getUserId(), "N/A"))
                .txnRef(tx.getTxnRef())
                .amount(tx.getAmount())
                .orderInfo(tx.getOrderInfo())
                .status(tx.getStatus().name())
                .vnpResponseCode(tx.getVnpResponseCode())
                .vnpBankCode(tx.getVnpBankCode())
                .vnpPayDate(tx.getVnpPayDate())
                .createdAt(tx.getCreatedAt())
                .updatedAt(tx.getUpdatedAt())
                .build();
    }
}
