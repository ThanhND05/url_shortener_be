package com.ThanhND05.url_shortener.common.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Binds custom application properties under the "app" prefix.
 */
@Configuration
@ConfigurationProperties(prefix = "app")
@Getter
@Setter
public class AppProperties {

    private final Jwt jwt = new Jwt();
    private final Cors cors = new Cors();
    private final VnPay vnpay = new VnPay();

    @Getter
    @Setter
    public static class Jwt {
        private String secret;
        private long accessTokenExpirationMs = 900_000; // 15 min
        private long refreshTokenExpirationMs = 604_800_000; // 7 days
    }

    @Getter
    @Setter
    public static class Cors {
        private String allowedOrigins = "http://localhost:3000";
    }

    /**
     * Cấu hình VNPay.
     * - tmnCode & hashSecret: nhận từ VNPay Sandbox sau khi đăng ký.
     * - payUrl: URL cổng thanh toán VNPay (sandbox hoặc production).
     * - returnUrl: URL VNPay redirect user về sau thanh toán.
     */
    @Getter
    @Setter
    public static class VnPay {
        private String tmnCode = "NZCRQJ6U";
        private String hashSecret = "Q4K67KVGHMSZK5DKM3RHXPEM2U185SHD";
        private String payUrl = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html";
        private String returnUrl = "http://localhost:8080/api/v1/billing/vnpay-return";
    }
}
