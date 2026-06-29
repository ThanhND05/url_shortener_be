package com.ThanhND05.url_shortener.link.service;

import com.ThanhND05.url_shortener.common.util.Base62Encoder;
import com.ThanhND05.url_shortener.link.repository.LinkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Sinh short code cho link mới.
 *
 * Cơ chế:
 * 1. Gọi PostgreSQL sequence `link.short_code_seq` (bắt đầu từ 100000).
 * 2. Encode giá trị sequence sang Base62 → chuỗi ngắn URL-safe.
 *
 * VD: 100000 → "q0U", 100001 → "q0V", 999999 → "4c91"
 *
 * Ưu điểm:
 * - Đảm bảo unique (sequence DB = atomic).
 * - Không cần kiểm tra trùng (sequence luôn tăng).
 * - Short code ngắn gọn (3-5 ký tự cho triệu links đầu tiên).
 */
@Component
@RequiredArgsConstructor
public class ShortCodeGenerator {

    private final LinkRepository linkRepository;

    /**
     * Sinh short code tự động từ DB sequence + Base62.
     * @return short code duy nhất, VD: "q0U"
     */
    public String generate() {
        Long sequence = linkRepository.getNextShortCodeSequence();
        return Base62Encoder.encode(sequence);
    }
}
