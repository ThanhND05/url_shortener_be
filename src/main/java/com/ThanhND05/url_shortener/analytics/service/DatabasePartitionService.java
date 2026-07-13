package com.ThanhND05.url_shortener.analytics.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DatabasePartitionService {

    private final JdbcTemplate jdbcTemplate;

    // Danh sách các bảng cần tự động chia partition theo tháng
    private final List<String> partitionedTables = List.of(
            "analytics.click_events",
            "analytics.click_agg_minute" // Đã thêm bảng này vào danh sách
    );

    /**
     * Chạy ngay khi ứng dụng vừa khởi động xong (@PostConstruct)
     * VÀ chạy định kỳ vào lúc 02:00 sáng mỗi ngày (@Scheduled)
     */
    @PostConstruct
    @Scheduled(cron = "0 0 2 * * ?")
    public void autoCreateMonthlyPartitions() {
        log.info("Bắt đầu kiểm tra và tạo Partition cho các bảng Analytics...");

        for (String baseTableName : partitionedTables) {
            // Tạo cho tháng hiện tại
            createPartitionForMonth(baseTableName, YearMonth.now());
            // Tạo sẵn cho tháng sau
            createPartitionForMonth(baseTableName, YearMonth.now().plusMonths(1));
        }
    }

    private void createPartitionForMonth(String baseTableName, YearMonth targetMonth) {
        // Tách schema và tên bảng (VD: "analytics.click_events" -> schema:
        // "analytics.", table: "click_events")
        String[] parts = baseTableName.split("\\.");
        String schemaName = parts.length > 1 ? parts[0] + "." : "";
        String pureTableName = parts.length > 1 ? parts[1] : parts[0];

        // Tên partition sẽ là: analytics.click_events_2026_07
        String partitionName = schemaName + pureTableName + "_"
                + targetMonth.format(DateTimeFormatter.ofPattern("yyyy_MM"));

        // Cộng thêm '00:00:00+00' để PostgreSQL hiểu đúng định dạng Timestamp
        String startDate = targetMonth.atDay(1).toString() + " 00:00:00+00";
        String endDate = targetMonth.plusMonths(1).atDay(1).toString() + " 00:00:00+00";

        String sql = String.format(
                "CREATE TABLE IF NOT EXISTS %s PARTITION OF %s " +
                        "FOR VALUES FROM ('%s') TO ('%s');",
                partitionName, baseTableName, startDate, endDate);

        try {
            jdbcTemplate.execute(sql);
            log.info("✅ Đã đảm bảo Partition tồn tại: {} (từ {} đến {})", partitionName, startDate, endDate);
        } catch (Exception e) {
            log.error("❌ Lỗi khi tạo partition {}: {}", partitionName, e.getMessage());
        }
    }
}