package com.ThanhND05.url_shortener.billing.listener;

import com.ThanhND05.url_shortener.billing.service.BillingService;
import com.ThanhND05.url_shortener.iam.event.UserCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Lắng nghe UserCreatedEvent → tự động tạo subscription FREE cho user mới.
 *
 * Khi user đăng ký tài khoản:
 *   AuthService publish UserCreatedEvent
 *   → BillingEventListener nhận event
 *   → Tạo bản ghi billing.subscriptions với plan = FREE.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BillingEventListener {

    private final BillingService billingService;

    @Async
    @EventListener
    public void onUserCreated(UserCreatedEvent event) {
        billingService.getOrCreateSubscription(event.userId());
        log.info("Created FREE subscription for new user {}", event.userId());
    }
}
