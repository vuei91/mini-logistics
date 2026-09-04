package com.cjlogistics.mini.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpMethod;

@Configuration @RequiredArgsConstructor
public class SecurityConfig {
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    @Bean SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http.csrf(csrf -> csrf.disable()).sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.requestMatchers("/auth/**", "/swagger-ui/**", "/v3/api-docs/**", "/h2-console/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/shipment-requests").hasRole("SHIPPER")
                        .requestMatchers(HttpMethod.POST, "/shipment-requests/*/cancel").hasRole("SHIPPER")
                        .requestMatchers(HttpMethod.POST, "/dispatches/*/accept", "/dispatches/*/reject").hasRole("DRIVER")
                        .requestMatchers(HttpMethod.PATCH, "/dispatches/*/status").hasRole("DRIVER")
                        .anyRequest().authenticated())
                .exceptionHandling(exceptions -> exceptions.authenticationEntryPoint((request, response, exception) -> response.setStatus(HttpStatus.UNAUTHORIZED.value())))
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class).build();
    }
}
