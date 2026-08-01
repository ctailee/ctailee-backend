package com.ctailee.backend.project.controller;

import com.ctailee.backend.project.entity.ShortUrlEntity;
import com.ctailee.backend.project.service.ShortUrlService;
import com.ctailee.backend.project.service.TextCipherService;
import com.ctailee.backend.project.textcipher.EncryptionException;
import com.ctailee.backend.project.urlshortener.ShortCodeNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.blankOrNullString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProjectController.class)
public class ProjectControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    ShortUrlService shortUrlService;

    @MockitoBean
    TextCipherService textCipherService;

    @Test
    public void textCipherEncrypt_returnsEncryptedText() throws Exception {
        when(textCipherService.encrypt(
                "test-password",
                "Hello, TextCipher!"
        )).thenReturn("encrypted-token");

        mockMvc.perform(
                post("/project/textcipher/encrypt")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "password": "test-password",
                                  "plainText": "Hello, TextCipher!"
                                }
                                """)
        )
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.encryptedText").value(not(blankOrNullString())));
    }

    @Test
    public void textCipherDecrypt_returnsOriginalPlainText() throws Exception {
        String encryptedText = "encrypted-token";
        when(textCipherService.decrypt("test-password", encryptedText))
                .thenReturn("Hello, TextCipher!");

        mockMvc.perform(
                post("/project/textcipher/decrypt")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "password": "test-password",
                                  "encryptedText": "%s"
                                }
                                """.formatted(encryptedText))
        )
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.plainText").value("Hello, TextCipher!"));
    }

    @Test
    public void textCipherEncrypt_returnsBadRequest_whenPasswordAndPlainTextAreBlank() throws Exception {
        mockMvc.perform(
                post("/project/textcipher/encrypt")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "password": " ",
                                  "plainText": ""
                                }
                                """)
        )
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Request validation failed"))
                .andExpect(jsonPath("$.errors.password").value("password must not be blank"))
                .andExpect(jsonPath("$.errors.plainText").value("plain text must not be blank"));
    }

    @Test
    public void textCipherDecrypt_returnsBadRequest_whenRequestFieldsAreBlank() throws Exception {
        mockMvc.perform(
                post("/project/textcipher/decrypt")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "password": "",
                                  "encryptedText": " "
                                }
                                """)
        )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.password").value("password must not be blank"))
                .andExpect(jsonPath("$.errors.encryptedText").value("encrypted text must not be blank"));
    }

    @Test
    public void textCipherDecrypt_returnsBadRequest_whenPasswordIsIncorrect() throws Exception {
        String encryptedText = "encrypted-token";
        when(textCipherService.decrypt("wrong-password", encryptedText))
                .thenThrow(new EncryptionException("Unable to decrypt"));

        mockMvc.perform(
                post("/project/textcipher/decrypt")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "password": "wrong-password",
                                  "encryptedText": "%s"
                                }
                                """.formatted(encryptedText))
        )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Unable to process the encrypted text"));
    }

    @Test
    public void shortenUrl_returnsShortCode_whenGivenOriginalUrl() throws Exception {
        ShortUrlEntity entity = new ShortUrlEntity(
                "https://ctailee.com/",
                "Ab12Cd3",
                LocalDateTime.now().plusDays(3)
        );
        when(shortUrlService.create("https://ctailee.com/"))
                .thenReturn(entity);

        mockMvc.perform(
                post("/project/shorturl/shortenurl")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "originalUrl": "https://ctailee.com/"
                                }
                                """)
                        .accept(MediaType.APPLICATION_JSON)
        )
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.shortCode").value("Ab12Cd3"));

        verify(shortUrlService).create("https://ctailee.com/");
    }

    @Test
    public void shortenUrl_returnsBadRequest_whenOriginalUrlIsBlank() throws Exception {
        mockMvc.perform(
                post("/project/shorturl/shortenurl")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "originalUrl": " "
                                }
                                """)
        )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Request validation failed"))
                .andExpect(jsonPath("$.errors.originalUrl")
                        .value("Original URL can't be empty"));
    }

    @Test
    public void getOriginalUrl_returnsOriginalUrl_whenShortCodeExists() throws Exception {
        when(shortUrlService.getOriginalUrl("Ab12Cd3"))
                .thenReturn("https://ctailee.com/");

        mockMvc.perform(
                post("/project/shorturl/getOriginalUrl")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "shortCode": "Ab12Cd3"
                                }
                                """)
        )
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.originalUrl").value("https://ctailee.com/"));

        verify(shortUrlService).getOriginalUrl("Ab12Cd3");
    }

    @Test
    public void getOriginalUrl_returnsNotFound_whenShortCodeDoesNotExist() throws Exception {
        when(shortUrlService.getOriginalUrl("missing"))
                .thenThrow(new ShortCodeNotFoundException("missing"));

        mockMvc.perform(
                post("/project/shorturl/getOriginalUrl")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "shortCode": "missing"
                                }
                                """)
        )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message")
                        .value("Short code not found: missing"));
    }

    @Test
    public void getOriginalUrl_returnsBadRequest_whenShortCodeIsBlank() throws Exception {
        mockMvc.perform(
                post("/project/shorturl/getOriginalUrl")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "shortCode": ""
                                }
                                """)
        )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Request validation failed"))
                .andExpect(jsonPath("$.errors.shortCode")
                        .value("Short code can't be empty"));
    }
}
