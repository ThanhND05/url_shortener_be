package com.ThanhND05.url_shortener.link.controller;

import com.ThanhND05.url_shortener.TestcontainersConfiguration;
import com.ThanhND05.url_shortener.analytics.dto.ClickEventMessage;
import com.ThanhND05.url_shortener.analytics.kafka.ClickEventProducer;
import com.ThanhND05.url_shortener.link.entity.RedirectLookup;
import com.ThanhND05.url_shortener.link.repository.RedirectLookupRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class RedirectIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RedirectLookupRepository redirectLookupRepository;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private ClickEventProducer clickEventProducer;

    @BeforeEach
    void setUp() {
        redirectLookupRepository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        redirectLookupRepository.deleteAll();
    }

    @Test
    void testRedirect_Success_Returns302AndSendsKafkaMessage() throws Exception {
        // Arrange
        String shortCode = "test1234";
        String originalUrl = "https://google.com";

        RedirectLookup lookup = RedirectLookup.builder()
                .domainId(1L)
                .shortCode(shortCode)
                .linkId(100L)
                .linkPublicId(UUID.randomUUID())
                .originalUrl(originalUrl)
                .status("ACTIVE")
                .redirectType((short) 302)
                .clickCount(0L)
                .passwordRequired(false)
                .updatedAt(Instant.now())
                .build();
        redirectLookupRepository.save(lookup);

        // Act & Assert
        mockMvc.perform(get("/r/" + shortCode)
                        .header("User-Agent", "Test-Agent")
                        .header("X-Forwarded-For", "192.168.1.1"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", originalUrl));

        // Verify Kafka message was queued up
        verify(clickEventProducer).send(any(ClickEventMessage.class));
    }
}
