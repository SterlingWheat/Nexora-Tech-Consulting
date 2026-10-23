package com.example.backend.security;

import com.example.backend.service.JwtService;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtAuthenticationFilterTest {

    private static final String SECRET = "test-secret-with-at-least-32-characters-long";

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void rechazaTokenAlteradoYNoContinuaLaPeticion() throws Exception {
        JwtService jwtService = new JwtService(SECRET, Duration.ofMinutes(5));
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService);
        String token = jwtService.generarToken("persona@example.com", "VOLUNTARIO");
        String alteredToken = token.substring(0, token.length() - 1)
                + (token.endsWith("x") ? "y" : "x");
        MockHttpServletResponse response = ejecutarFiltro(
                filter, alteredToken, new AtomicBoolean());

        assertEquals(401, response.getStatus());
        assertEquals("{\"message\":\"No autorizado\"}", response.getContentAsString());
    }

    @Test
    void rechazaTokenExpiradoYNoContinuaLaPeticion() throws Exception {
        JwtService jwtService = new JwtService(SECRET, Duration.ofMillis(1));
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService);
        String token = jwtService.generarToken("persona@example.com", "VOLUNTARIO");
        Thread.sleep(20);
        AtomicBoolean continued = new AtomicBoolean();

        MockHttpServletResponse response = ejecutarFiltro(filter, token, continued);

        assertEquals(401, response.getStatus());
        assertFalse(continued.get());
    }

    @Test
    void autenticaConUnTokenValidoYContinuaLaPeticion() throws Exception {
        JwtService jwtService = new JwtService(SECRET, Duration.ofMinutes(5));
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService);
        String token = jwtService.generarToken("persona@example.com", "VOLUNTARIO");
        AtomicBoolean continued = new AtomicBoolean();

        MockHttpServletResponse response = ejecutarFiltro(filter, token, continued);

        assertEquals(200, response.getStatus());
        assertTrue(continued.get());
        assertEquals("persona@example.com",
                SecurityContextHolder.getContext().getAuthentication().getName());
        assertTrue(SecurityContextHolder.getContext().getAuthentication()
                .getAuthorities().stream().anyMatch(authority -> authority.getAuthority().equals("ROLE_VOLUNTARIO")));
    }

    private MockHttpServletResponse ejecutarFiltro(
            JwtAuthenticationFilter filter,
            String token,
            AtomicBoolean continued) throws IOException, jakarta.servlet.ServletException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (servletRequest, servletResponse) -> continued.set(true);

        filter.doFilter(request, response, chain);
        return response;
    }
}
