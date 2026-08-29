package com.ThanhND05.url_shortener.billing.service;

import com.ThanhND05.url_shortener.billing.entity.Subscription;
import com.ThanhND05.url_shortener.billing.enums.SubscriptionPlan;
import com.ThanhND05.url_shortener.billing.repository.SubscriptionRepository;
import com.ThanhND05.url_shortener.common.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BillingServiceTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @InjectMocks
    private BillingService billingService;

    @Test
    void testEnforceCreateLinkQuota_ProUser_Success() {
        // Arrange
        UUID userId = UUID.randomUUID();
        Subscription sub = Subscription.builder()
                .userId(userId)
                .plan(SubscriptionPlan.PRO)
                .status(com.ThanhND05.url_shortener.billing.enums.SubscriptionStatus.ACTIVE)
                .expiresAt(Instant.now().plus(30, ChronoUnit.DAYS))
                .linksUsed(100) // already over free limit
                .linksResetAt(Instant.now())
                .build();
        when(subscriptionRepository.findById(userId)).thenReturn(Optional.of(sub));

        // Act & Assert
        assertDoesNotThrow(() -> billingService.enforceCreateLinkQuota(userId));
    }

    @Test
    void testEnforceCreateLinkQuota_FreeUser_UnderLimit_Success() {
        // Arrange
        UUID userId = UUID.randomUUID();
        Subscription sub = Subscription.builder()
                .userId(userId)
                .plan(SubscriptionPlan.FREE)
                .linksUsed(49)
                .linksResetAt(Instant.now())
                .build();
        when(subscriptionRepository.findById(userId)).thenReturn(Optional.of(sub));

        // Act & Assert
        assertDoesNotThrow(() -> billingService.enforceCreateLinkQuota(userId));
    }

    @Test
    void testEnforceCreateLinkQuota_FreeUser_OverLimit_ThrowsException() {
        // Arrange
        UUID userId = UUID.randomUUID();
        Subscription sub = Subscription.builder()
                .userId(userId)
                .plan(SubscriptionPlan.FREE)
                .linksUsed(50)
                .linksResetAt(Instant.now()) // current month
                .build();
        when(subscriptionRepository.findById(userId)).thenReturn(Optional.of(sub));

        // Act & Assert
        assertThrows(BusinessException.class, () -> billingService.enforceCreateLinkQuota(userId));
    }

    @Test
    void testEnforceCreateLinkQuota_FreeUser_OverLimitButNewMonth_Success() {
        // Arrange
        UUID userId = UUID.randomUUID();
        // Set reset date to previous month
        Instant lastMonth = Instant.now().minus(35, ChronoUnit.DAYS);
        Subscription sub = Subscription.builder()
                .userId(userId)
                .plan(SubscriptionPlan.FREE)
                .linksUsed(50)
                .linksResetAt(lastMonth)
                .build();
        when(subscriptionRepository.findById(userId)).thenReturn(Optional.of(sub));

        // Act & Assert
        assertDoesNotThrow(() -> billingService.enforceCreateLinkQuota(userId));
    }
}
