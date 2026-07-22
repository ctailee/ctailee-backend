package com.ctailee.backend.project.controller;

import com.ctailee.backend.project.service.TextCipherService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.blankOrNullString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProjectController.class)
public class ProjectControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    public void textCipherEncrypt_returnsEncryptedText() throws Exception {
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
        String encryptedText = new TextCipherService().encrypt(
                "test-password",
                "Hello, TextCipher!"
        );

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
        String encryptedText = new TextCipherService().encrypt(
                "correct-password",
                "Hello, TextCipher!"
        );

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
}
