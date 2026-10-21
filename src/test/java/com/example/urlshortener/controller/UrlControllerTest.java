package com.example.urlshortener.controller;

import com.example.urlshortener.exception.InvalidUrlException;
import com.example.urlshortener.exception.UrlNotFoundException;
import com.example.urlshortener.model.UrlEntity;
import com.example.urlshortener.service.UrlService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UrlController.class)
class UrlControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UrlService service;

    @Test
    void shortenReturns201WithCode() throws Exception {
        when(service.shorten("https://example.com"))
                .thenReturn(new UrlEntity("abc1234", "https://example.com"));

        mockMvc.perform(post("/api/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"https://example.com\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("abc1234"))
                .andExpect(jsonPath("$.shortUrl").value("http://localhost/abc1234"))
                .andExpect(jsonPath("$.originalUrl").value("https://example.com"));
    }

    @Test
    void shortenReturns400ForBlankUrl() throws Exception {
        mockMvc.perform(post("/api/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void shortenReturns400ForInvalidUrl() throws Exception {
        when(service.shorten("ftp://bad"))
                .thenThrow(new InvalidUrlException("URL must start with http:// or https://"));

        mockMvc.perform(post("/api/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"ftp://bad\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("URL must start with http:// or https://"));
    }

    @Test
    void redirectReturns302WithLocationHeader() throws Exception {
        when(service.resolve("abc1234")).thenReturn("https://example.com/target");

        mockMvc.perform(get("/abc1234"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://example.com/target"));
    }

    @Test
    void redirectReturns404ForUnknownCode() throws Exception {
        when(service.resolve("unknown1")).thenThrow(new UrlNotFoundException("unknown1"));

        mockMvc.perform(get("/unknown1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void statsReturnsClickCount() throws Exception {
        UrlEntity entity = new UrlEntity("abc1234", "https://example.com");
        entity.incrementClicks();
        entity.incrementClicks();
        when(service.getStats("abc1234")).thenReturn(entity);

        mockMvc.perform(get("/api/stats/abc1234"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clickCount").value(2))
                .andExpect(jsonPath("$.code").value("abc1234"));
    }
}
