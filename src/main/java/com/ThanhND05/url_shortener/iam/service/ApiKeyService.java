package com.ThanhND05.url_shortener.iam.service;

import com.ThanhND05.url_shortener.common.exception.ResourceNotFoundException;
import com.ThanhND05.url_shortener.common.util.HashUtil;
import com.ThanhND05.url_shortener.iam.dto.request.*;
import com.ThanhND05.url_shortener.iam.dto.response.*;
import com.ThanhND05.url_shortener.iam.entity.ApiKey;
import com.ThanhND05.url_shortener.iam.enums.ApiKeyStatus;
import com.ThanhND05.url_shortener.iam.repository.ApiKeyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Service quản lý API Keys — cho phép user truy cập API bằng key thay vì JWT.
 *
 * === FLOW TẠO API KEY ===
 * 1. Sinh raw key: "sk_live_" + UUID (32 hex chars).
 *    VD: "sk_live_a1b2c3d4e5f67890a1b2c3d4e5f67890"
 * 2. Lưu vào DB:
 *    - key_prefix = "sk_live_a1b2c3d4" (8 ký tự đầu sau prefix, hiển thị UI).
 *    - key_hash = SHA-256(raw key) — dùng để lookup khi authenticate.
 * 3. Trả raw key cho client MỘT LẦN DUY NHẤT — sau đó không thể xem lại.
 *
 * === FLOW AUTHENTICATE ===
 * (Sẽ implement ở ApiKeyAuthFilter trong phase sau)
 * 1. Client gửi header: X-API-Key: sk_live_a1b2c3d4e5f67890...
 * 2. Server hash → tìm trong DB → check status ACTIVE + chưa hết hạn.
 * 3. Kiểm tra scopes xem key có quyền gọi endpoint không.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApiKeyService {

    private final ApiKeyRepository apiKeyRepository;

    private static final String KEY_PREFIX_LITERAL = "sk_live_";

    // ── TẠO API KEY ───────────────────────────────────────

    @Transactional
    public ApiKeyResponse createApiKey(UUID ownerId, CreateApiKeyRequest request) {
        // 1. Sinh raw key
        String rawUuid = UUID.randomUUID().toString().replace("-", "");
        String rawKey = KEY_PREFIX_LITERAL + rawUuid;

        // 2. Lấy prefix hiển thị (8 ký tự đầu của UUID phần)
        String displayPrefix = KEY_PREFIX_LITERAL + rawUuid.substring(0, 8) + "...";

        // 3. Hash toàn bộ raw key
        String keyHash = HashUtil.sha256Hex(rawKey);

        // 4. Lưu DB
        ApiKey apiKey = ApiKey.builder()
                .ownerId(ownerId)
                .name(request.name())
                .keyPrefix(displayPrefix)
                .keyHash(keyHash)
                .scopes(request.scopes() != null ? request.scopes() : new String[]{})
                .expiresAt(request.expiresAt())
                .build();
        apiKey = apiKeyRepository.save(apiKey);

        log.info("API key created: {} for user {}", displayPrefix, ownerId);

        // 5. Trả về kèm raw key (lần duy nhất)
        return ApiKeyResponse.from(apiKey, rawKey);
    }

    // ── LIỆT KÊ API KEY ──────────────────────────────────

    @Transactional(readOnly = true)
    public Page<ApiKeyResponse> listApiKeys(UUID ownerId, Pageable pageable) {
        return apiKeyRepository.findByOwnerId(ownerId, pageable)
                .map(ApiKeyResponse::from);  // không trả rawKey
    }

    // ── THU HỒI API KEY ──────────────────────────────────

    @Transactional
    public void revokeApiKey(UUID keyId, UUID ownerId) {
        ApiKey apiKey = apiKeyRepository.findById(keyId)
                .orElseThrow(() -> new ResourceNotFoundException("ApiKey", "id", keyId));

        // Chỉ chủ sở hữu mới được revoke
        if (!apiKey.getOwnerId().equals(ownerId)) {
            throw new ResourceNotFoundException("ApiKey", "id", keyId);
        }

        apiKey.setStatus(ApiKeyStatus.REVOKED);
        apiKey.setRevokeReason("Revoked by owner");
        apiKeyRepository.save(apiKey);
        log.info("API key revoked: {}", keyId);
    }
}
