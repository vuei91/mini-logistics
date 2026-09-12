package com.cjlogistics.mini.security;

import com.cjlogistics.mini.common.ErrorCode;
import com.cjlogistics.mini.common.ErrorResponse;
import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

import java.util.List;

@Configuration @RequiredArgsConstructor
public class SecurityConfig {
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ObjectMapper objectMapper;

    @Value("${app.cors.allowed-origins:http://localhost:3000}")
    private String allowedOrigins;

    @Bean SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http.csrf(csrf -> csrf.disable()).sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authorizeHttpRequests(auth -> auth.requestMatchers("/api/auth/**", "/swagger-ui/**", "/v3/api-docs", "/v3/api-docs/**", "/h2-console/**").permitAll()
                        .requestMatchers("/api/notifications/stream").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/shipment-requests").hasRole("SHIPPER")
                        .requestMatchers(HttpMethod.GET, "/api/shipment-requests").hasRole("SHIPPER")
                        .requestMatchers(HttpMethod.POST, "/api/shipment-requests/*/cancel").hasRole("SHIPPER")
                        .requestMatchers(HttpMethod.POST, "/api/shipment-requests/*/dispatch").hasRole("SHIPPER")
                        .requestMatchers(HttpMethod.GET, "/api/dispatches").hasRole("DRIVER")
                        .requestMatchers(HttpMethod.POST, "/api/dispatches/*/accept", "/api/dispatches/*/reject").hasRole("DRIVER")
                        .requestMatchers(HttpMethod.PATCH, "/api/dispatches/*/status").hasRole("DRIVER")
                        .requestMatchers("/api/notifications/**").hasAnyRole("SHIPPER", "DRIVER")
                        .anyRequest().authenticated())
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, exception) -> writeSecurityError(
                                request, response, HttpStatus.UNAUTHORIZED, ErrorCode.AUTHENTICATION_REQUIRED,
                                "로그인이 필요하거나 인증이 만료되었습니다."))
                        .accessDeniedHandler((request, response, exception) -> writeSecurityError(
                                request, response, HttpStatus.FORBIDDEN, ErrorCode.ACCESS_DENIED,
                                "요청을 수행할 권한이 없습니다.")))
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class).build();
    }

    private void writeSecurityError(HttpServletRequest request, HttpServletResponse response,
                                    HttpStatus status, ErrorCode code, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), ErrorResponse.of(status, code, message, request.getRequestURI()));
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(allowedOrigins.split(",")));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
