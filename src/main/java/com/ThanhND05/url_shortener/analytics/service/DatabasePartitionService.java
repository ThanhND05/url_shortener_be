package com.ThanhND05.url_shortener.analytics.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class DatabasePartitionService {

    private final JdbcTemplate jdbcTemplate;

    /**
     * Chạy tự động vào lúc 02:00 sáng mỗi ngày.
     * Hàm này sẽ kiểm tra và tạo sẵn Partition cho tháng HIỆN TẠI và tháng TIẾP
     * THEO.
     * Dùng IF NOT EXISTS nên chạy bao nhiêu lần cũng không bị lỗi.
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void autoCreateMonthlyPartitions() {
        log.info("Bắt đầu kiểm tra và tạo Partition cho bảng click_events...");

        // Tạo cho tháng hiện tại (phòng trường hợp DB bị xóa build lại)
        createPartitionForMonth(YearMonth.now());

        // Tạo sẵn cho tháng sau (chuẩn bị trước ổ đĩa)
        createPartitionForMonth(YearMonth.now().plusMonths(1));
    }

    private void createPartitionForMonth(YearMonth targetMonth) {
        // Ví dụ targetMonth là Tháng 8/2026
        // Tên bảng sẽ là: analytics.click_events_2026_08
        String tableName = "analytics.click_events_" + targetMonth.format(DateTimeFormatter.ofPattern("yyyy_MM"));

        // Ngày bắt đầu: '2026-08-01'
        String startDate = targetMonth.atDay(1).toString();

        // Ngày kết thúc (sang tháng kế tiếp): '2026-09-01'
        String endDate = targetMonth.plusMonths(1).atDay(1).toString();

        String sql = String.format(
                "CREATE TABLE IF NOT EXISTS %s PARTITION OF analytics.click_events " +
                        "FOR VALUES FROM ('%s') TO ('%s');",
                tableName, startDate, endDate);

        try {
            jdbcTemplate.execute(sql);
            log.info("✅ Đã đảm bảo Partition tồn tại: {} (từ {} đến {})", tableName, startDate, endDate);
        } catch (Exception e) {
            log.error("❌ Lỗi khi tạo partition {}: {}", tableName, e.getMessage());
        }
    }
}