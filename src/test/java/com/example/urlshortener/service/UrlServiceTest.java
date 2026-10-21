package com.example.urlshortener.service;

import com.example.urlshortener.exception.InvalidUrlException;
import com.example.urlshortener.exception.UrlNotFoundException;
import com.example.urlshortener.model.UrlEntity;
import com.example.urlshortener.repository.UrlRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UrlServiceTest {

    @Mock
    private UrlRepository repository;

    @InjectMocks
    private UrlService service;

    @Test
    void shortenSavesEntityWithGeneratedCode() {
        when(repository.existsByCode(anyString())).thenReturn(false);
        when(repository.save(any(UrlEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        UrlEntity result = service.shorten("https://example.com/page");

        assertEquals("https://example.com/page", result.getOriginalUrl());
        assertEquals(UrlService.CODE_LENGTH, result.getCode().length());
        verify(repository).save(any(UrlEntity.class));
    }

    @Test
    void shortenRetriesWhenCodeAlreadyExists() {
        when(repository.existsByCode(anyString())).thenReturn(true, false);
        when(repository.save(any(UrlEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        UrlEntity result = service.shorten("https://example.com");

        assertNotNull(result.getCode());
        verify(repository, times(2)).existsByCode(anyString());
    }

    @Test
    void shortenFailsWhenNoUniqueCodeCanBeFound() {
        when(repository.existsByCode(anyString())).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> service.shorten("https://example.com"));
        verify(repository, never()).save(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "not a url", "ftp://example.com", "javascript:alert(1)", "http://"})
    void shortenRejectsInvalidUrls(String bad) {
        assertThrows(InvalidUrlException.class, () -> service.shorten(bad));
        verifyNoInteractions(repository);
    }

    @Test
    void shortenRejectsNull() {
        assertThrows(InvalidUrlException.class, () -> service.shorten(null));
    }

    @Test
    void resolveReturnsOriginalUrlAndCountsClick() {
        UrlEntity entity = new UrlEntity("abc1234", "https://example.com");
        when(repository.findByCode("abc1234")).thenReturn(Optional.of(entity));

        String result = service.resolve("abc1234");

        assertEquals("https://example.com", result);
        assertEquals(1, entity.getClickCount());
        verify(repository).save(entity);
    }

    @Test
    void resolveThrowsWhenCodeUnknown() {
        when(repository.findByCode("nope")).thenReturn(Optional.empty());

        assertThrows(UrlNotFoundException.class, () -> service.resolve("nope"));
    }

    @Test
    void generatedCodesUseOnlyAlphabetCharacters() {
        for (int i = 0; i < 200; i++) {
            String code = service.generateCode();
            assertEquals(UrlService.CODE_LENGTH, code.length());
            assertTrue(code.chars().allMatch(c -> UrlService.ALPHABET.indexOf(c) >= 0));
        }
    }
}
