package com.TrucVanban.shared.config;

import com.TrucVanban.exchange.service.AuditLogService;
import com.TrucVanban.registry.service.RegistryService;
import com.TrucVanban.shared.security.hmac.HmacAuthenticationFilter;
import com.TrucVanban.shared.security.hmac.HmacAuthenticationService;
import com.TrucVanban.shared.security.hmac.HmacProperties;
import com.TrucVanban.shared.utils.CanonicalStringBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    @Bean
    public SignatureVerificationFilter signatureVerificationFilter(
            RegistryService registryService,
            AuditLogService auditLogService,
            ObjectMapper objectMapper,
            CanonicalStringBuilder canonicalStringBuilder) {

        return new SignatureVerificationFilter(registryService, auditLogService, objectMapper, canonicalStringBuilder);
    }

    @Bean
    public HmacAuthenticationFilter hmacAuthenticationFilter(
            HmacAuthenticationService hmacAuthenticationService,
            HmacProperties hmacProperties,
            AuditLogService auditLogService) {
        return new HmacAuthenticationFilter(hmacAuthenticationService, hmacProperties, auditLogService);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   SignatureVerificationFilter signatureVerificationFilter,
                                                   HmacAuthenticationFilter hmacAuthenticationFilter) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs/**"
                        ).permitAll()
                        .anyRequest().permitAll()
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterAfter(signatureVerificationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(hmacAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(false);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
