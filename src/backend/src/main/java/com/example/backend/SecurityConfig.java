package com.example.backend;

import com.example.backend.security.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
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
                // Customizer.withDefaults() le indica a Spring Security que busque
                // e inyecte automáticamente el bean CorsConfigurationSource definido abajo
                .cors(Customizer.withDefaults())
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable())
                .authorizeHttpRequests(auth -> auth
                        // Permite todas las peticiones preflight OPTIONS sin autenticación[cite: 1]
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/error").permitAll()
                        
                        // Perfil[cite: 1]
                        .requestMatchers("/api/perfil/me").authenticated()
                        
                        // Gestión de Usuarios[cite: 1]
                        .requestMatchers("/api/usuarios/**").hasRole("ADMINISTRADOR")
                        
                        // Gestión de Tareas[cite: 1]
                        .requestMatchers(HttpMethod.GET, "/api/tareas/**")
                            .hasAnyRole("ADMINISTRADOR", "TI", "GESTOR_TAREAS", "VOLUNTARIO")
                        .requestMatchers(HttpMethod.POST, "/api/tareas/**")
                            .hasAnyRole("ADMINISTRADOR", "GESTOR_TAREAS")
                        .requestMatchers(HttpMethod.PUT, "/api/tareas/**")
                            .hasAnyRole("ADMINISTRADOR", "GESTOR_TAREAS")
                        .requestMatchers("/api/tareas/**").denyAll()
                        
                        // Gestión de Turnos y Voluntarios[cite: 1]
                        .requestMatchers("/api/turnos/**").authenticated()
                        .requestMatchers("/api/voluntarios/**").authenticated()
                        
                        .anyRequest().authenticated())
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(401);
                            response.setContentType("application/json");
                            response.setCharacterEncoding("UTF-8");
                            response.getWriter().write("{\"message\":\"No autorizado\"}");
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
        // Acepta el origen configurado y cualquier puerto de localhost de desarrollo[cite: 1]
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