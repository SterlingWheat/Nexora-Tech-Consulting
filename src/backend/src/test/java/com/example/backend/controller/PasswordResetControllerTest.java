package com.example.backend.controller;

import com.example.backend.dto.PasswordResetResponse;
import com.example.backend.dto.PasswordResetValidationResponse;
import com.example.backend.exception.BusinessValidationException;
import com.example.backend.exception.GlobalExceptionHandler;
import com.example.backend.service.PasswordResetService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class PasswordResetControllerTest {

    private PasswordResetService service;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        service = mock(PasswordResetService.class);
        mockMvc = standaloneSetup(new PasswordResetController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void requestReturnsTheGenericResponse() throws Exception {
        when(service.solicitar("persona@example.com"))
                .thenReturn(new PasswordResetResponse(PasswordResetService.GENERIC_REQUEST_MESSAGE));

        mockMvc.perform(post("/api/auth/password-reset/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"persona@example.com\"}"))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"message":"Si el correo electrónico está registrado, recibirás un enlace para restablecer tu contraseña."}
                        """));
    }

    @Test
    void invalidTokenReturnsAClientErrorWithoutDetails() throws Exception {
        when(service.validar("token-invalido"))
                .thenThrow(new BusinessValidationException("El enlace de recuperación no es válido o expiró."));

        mockMvc.perform(post("/api/auth/password-reset/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"token-invalido\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("El enlace de recuperación no es válido o expiró."));
    }

    @Test
    void validTokenHasAnExplicitPositiveResponse() throws Exception {
        when(service.validar("token-valido")).thenReturn(new PasswordResetValidationResponse(true));

        mockMvc.perform(post("/api/auth/password-reset/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"token-valido\"}"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"valid\":true}"));
    }
}
