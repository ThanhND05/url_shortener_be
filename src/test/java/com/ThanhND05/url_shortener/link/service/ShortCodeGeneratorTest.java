package com.ThanhND05.url_shortener.link.service;

import com.ThanhND05.url_shortener.link.repository.LinkRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class ShortCodeGeneratorTest {

    @Mock
    private LinkRepository linkRepository;

    @InjectMocks
    private ShortCodeGenerator shortCodeGenerator;

    @Test
    void testGenerate_ReturnsCorrectBase62String() {
        // Arrange
        when(linkRepository.getNextShortCodeSequence()).thenReturn(100000L);

        // Act
        String result = shortCodeGenerator.generate();

        // Assert
        assertEquals("Q0u", result);
        verify(linkRepository, times(1)).getNextShortCodeSequence();
    }
}
