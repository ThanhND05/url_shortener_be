package com.ThanhND05.url_shortener.link.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ThanhND05.url_shortener.TestcontainersConfiguration;
import com.ThanhND05.url_shortener.billing.entity.Subscription;
import com.ThanhND05.url_shortener.billing.enums.SubscriptionPlan;
import com.ThanhND05.url_shortener.billing.enums.SubscriptionStatus;
import com.ThanhND05.url_shortener.billing.repository.SubscriptionRepository;
import com.ThanhND05.url_shortener.link.dto.request.CreateLinkRequest;
import com.ThanhND05.url_shortener.link.repository.LinkRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class LinkCreationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private LinkRepository linkRepository;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    private UUID userId;

    @BeforeEach
    void setUp() {
        linkRepository.deleteAll();
        subscriptionRepository.deleteAll();
        userId = UUID.randomUUID();
        
        // Setup Free Subscription Quota
        Subscription sub = Subscription.builder()
                .userId(userId)
                .plan(SubscriptionPlan.FREE)
                .status(SubscriptionStatus.ACTIVE)
                .linksUsed(0)
                .linksResetAt(Instant.now())
                .build();
        subscriptionRepository.save(sub);
    }

    @AfterEach
    void tearDown() {
        linkRepository.deleteAll();
        subscriptionRepository.deleteAll();
    }

    @Test
    void testCreateLink_Success() throws Exception {
        // Arrange
        CreateLinkRequest request = new CreateLinkRequest(
                "https://example.com/very/long/url",
                null,
                null, // default domain
                "Test Link",
                "Description test",
                null,
                (short) 302,
                null,
                null,
                null,
                null
        );

        // Act & Assert
        mockMvc.perform(post("/api/v1/links")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(jwt().jwt(builder -> builder.subject(userId.toString()))
                                .authorities(new SimpleGrantedAuthority("link:create"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.originalUrl").value("https://example.com/very/long/url"))
                .andExpect(jsonPath("$.data.shortCode").isNotEmpty());
                
        // Verify it was actually saved in Postgres
        assertTrue(linkRepository.count() > 0);
    }
}
