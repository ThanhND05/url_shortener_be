package com.ThanhND05.url_shortener.platform.service;

import com.ThanhND05.url_shortener.common.exception.ResourceNotFoundException;
import com.ThanhND05.url_shortener.platform.dto.request.UpdateSystemConfigRequest;
import com.ThanhND05.url_shortener.platform.dto.response.SystemConfigResponse;
import com.ThanhND05.url_shortener.platform.entity.SystemConfig;
import com.ThanhND05.url_shortener.platform.repository.SystemConfigRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service quản lý cấu hình hệ thống — key-value config lưu trong DB.
 *
 * Tối ưu hóa:
 * - Cache toàn bộ config trong ConcurrentHashMap tại memory.
 * - Reload cache khi admin update config.
 * - Cung cấp typed getters (getString, getInt, getLong) cho các module khác.
 *
 * Cross-module interface:
 * - Link module gọi getInt("slug.min_length") khi validate custom slug.
 * - Billing module gọi getInt("user.max_links_free") cho quota enforcement.
 * - Rate limiter gọi getInt("rate_limit.links_per_minute") cho throttling.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SystemConfigService {

    private final SystemConfigRepository systemConfigRepository;

    /** In-memory cache: key → value. */
    private final Map<String, String> configCache = new ConcurrentHashMap<>();

    @PostConstruct
    void initCache() {
        reloadCache();
        log.info("Loaded {} system configs into cache.", configCache.size());
    }

    private void reloadCache() {
        configCache.clear();
        systemConfigRepository.findAll()
                .forEach(c -> configCache.put(c.getConfigKey(), c.getValue()));
    }

    // ── Typed Getters (Cross-module) ─────────────────────

    /**
     * Lấy config value dưới dạng String.
     *
     * @param key         config key.
     * @param defaultVal  giá trị mặc định nếu key không tồn tại.
     * @return config value hoặc defaultVal.
     */
    public String getString(String key, String defaultVal) {
        return configCache.getOrDefault(key, defaultVal);
    }

    /**
     * Lấy config value dưới dạng int.
     *
     * @param key         config key.
     * @param defaultVal  giá trị mặc định nếu key không tồn tại hoặc parse lỗi.
     * @return config value hoặc defaultVal.
     */
    public int getInt(String key, int defaultVal) {
        String val = configCache.get(key);
        if (val == null) return defaultVal;
        try {
            return Integer.parseInt(val.trim());
        } catch (NumberFormatException e) {
            log.warn("Invalid int config for key '{}': '{}'. Using default: {}", key, val, defaultVal);
            return defaultVal;
        }
    }

    /**
     * Lấy config value dưới dạng long.
     *
     * @param key         config key.
     * @param defaultVal  giá trị mặc định.
     * @return config value hoặc defaultVal.
     */
    public long getLong(String key, long defaultVal) {
        String val = configCache.get(key);
        if (val == null) return defaultVal;
        try {
            return Long.parseLong(val.trim());
        } catch (NumberFormatException e) {
            log.warn("Invalid long config for key '{}': '{}'. Using default: {}", key, val, defaultVal);
            return defaultVal;
        }
    }

    /**
     * Lấy config value dưới dạng boolean.
     * "true", "1", "yes" → true. Còn lại → false.
     */
    public boolean getBoolean(String key, boolean defaultVal) {
        String val = configCache.get(key);
        if (val == null) return defaultVal;
        val = val.trim().toLowerCase();
        return "true".equals(val) || "1".equals(val) || "yes".equals(val);
    }

    // ── Admin API ────────────────────────────────────────

    /**
     * Liệt kê tất cả config (cho admin UI).
     */
    @Transactional(readOnly = true)
    public List<SystemConfigResponse> listAll() {
        return systemConfigRepository.findAllByOrderByConfigKeyAsc()
                .stream()
                .map(SystemConfigResponse::from)
                .toList();
    }

    /**
     * Lấy chi tiết 1 config.
     */
    @Transactional(readOnly = true)
    public SystemConfigResponse getByKey(String key) {
        SystemConfig config = systemConfigRepository.findById(key)
                .orElseThrow(() -> new ResourceNotFoundException("SystemConfig", "key", key));
        return SystemConfigResponse.from(config);
    }

    /**
     * Cập nhật giá trị config.
     * Chỉ admin mới gọi được (bảo vệ bởi @PreAuthorize ở controller).
     */
    @Transactional
    public SystemConfigResponse updateConfig(String key, UpdateSystemConfigRequest request, String actorEmail) {
        SystemConfig config = systemConfigRepository.findById(key)
                .orElseThrow(() -> new ResourceNotFoundException("SystemConfig", "key", key));

        String oldValue = config.getValue();
        config.setValue(request.value());
        config.setUpdatedAt(Instant.now());
        config.setUpdatedBy(actorEmail);
        config = systemConfigRepository.save(config);

        // Update cache
        configCache.put(key, request.value());

        log.info("System config '{}' updated: '{}' → '{}' by {}", key, oldValue, request.value(), actorEmail);
        return SystemConfigResponse.from(config);
    }

    /**
     * Tạo config mới (nếu chưa tồn tại).
     * Chủ yếu dùng cho việc thêm config mới trong runtime.
     */
    @Transactional
    public SystemConfigResponse createConfig(String key, String value, String description, String actorEmail) {
        if (systemConfigRepository.existsById(key)) {
            throw new com.ThanhND05.url_shortener.common.exception.DuplicateResourceException(
                    "SystemConfig", "key", key);
        }

        SystemConfig config = SystemConfig.builder()
                .configKey(key)
                .value(value)
                .description(description)
                .updatedBy(actorEmail)
                .build();
        config = systemConfigRepository.save(config);

        // Update cache
        configCache.put(key, value);

        log.info("System config '{}' created with value '{}' by {}", key, value, actorEmail);
        return SystemConfigResponse.from(config);
    }

    /**
     * Xóa config.
     */
    @Transactional
    public void deleteConfig(String key) {
        SystemConfig config = systemConfigRepository.findById(key)
                .orElseThrow(() -> new ResourceNotFoundException("SystemConfig", "key", key));
        systemConfigRepository.delete(config);

        // Update cache
        configCache.remove(key);

        log.info("System config '{}' deleted.", key);
    }
}
