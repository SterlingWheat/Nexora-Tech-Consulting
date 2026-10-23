package com.example.backend;

import com.example.backend.security.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity // Permite usar @PreAuthorize en los controladores si se requiere mayor granularidad
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationFilter jwtAuthenticationFilter) throws Exception {

        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/error").permitAll()

                        .requestMatchers("/openapi.yaml", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        
                        // Perfil
                        .requestMatchers("/api/perfil/me").authenticated()
                        
                        // Gestión de Usuarios
                        .requestMatchers("/api/usuarios/**").hasRole("ADMINISTRADOR")
                        
                        // Gestión de Tareas
                        .requestMatchers(HttpMethod.GET, "/api/tareas/**")
                            .hasAnyRole("ADMINISTRADOR", "TI", "GESTOR_TAREAS", "VOLUNTARIO")
                        .requestMatchers(HttpMethod.POST, "/api/tareas/**")
                            .hasAnyRole("ADMINISTRADOR", "GESTOR_TAREAS")
                        .requestMatchers(HttpMethod.PUT, "/api/tareas/**")
                            .hasAnyRole("ADMINISTRADOR", "GESTOR_TAREAS")
                        .requestMatchers(HttpMethod.PATCH, "/api/tareas/**")
                            .hasAnyRole("ADMINISTRADOR", "GESTOR_TAREAS")
                        .requestMatchers("/api/tareas/**").denyAll()
                        
                        // Gestión de Turnos (Solución Error 1 - Restricción de creación/modificación)
                        .requestMatchers(HttpMethod.POST, "/api/turnos/**").hasAnyRole("ADMINISTRADOR", "GESTOR_TAREAS")
                        .requestMatchers(HttpMethod.PUT, "/api/turnos/**").hasAnyRole("ADMINISTRADOR", "GESTOR_TAREAS")
                        .requestMatchers(HttpMethod.DELETE, "/api/turnos/**").hasAnyRole("ADMINISTRADOR", "GESTOR_TAREAS")
                        .requestMatchers(HttpMethod.GET, "/api/turnos/**").authenticated()
                        
                        // Gestión de Voluntarios (Solución Error 1 - Restricción general por roles)
                        .requestMatchers(HttpMethod.POST, "/api/voluntarios/*/habilitar-acceso").hasAnyRole("ADMINISTRADOR", "TI")
                        .requestMatchers(HttpMethod.POST, "/api/voluntarios").hasAnyRole("ADMINISTRADOR", "TI", "GESTOR_TAREAS")
                        .requestMatchers("/api/voluntarios/**").authenticated()
                        
                        .anyRequest().authenticated())
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(401);
                            response.setContentType("application/json");
                            response.setCharacterEncoding("UTF-8");
                            response.getWriter().write("{\"message\":\"No autorizado\"}");
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(403);
                            response.setContentType("application/json");
                            response.setCharacterEncoding("UTF-8");
                            response.getWriter().write("{\"message\":\"Acceso denegado\"}");
                        }))
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${app.frontend-origin:http://localhost:5173}") String frontendOrigin) {
        
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of(
                frontendOrigin,
                "http://localhost:*",
                "http://127.0.0.1:*"
        ));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of("Authorization"));
        configuration.setAllowCredentials(true);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}