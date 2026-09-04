package com.cjlogistics.mini.security;

import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtTokenServiceTest {
    private final JwtTokenService tokenService = new JwtTokenService(
            "test-signing-secret-must-be-at-least-thirty-two-bytes", 3600);

    @Test
    void token_preserves_role_profile_and_email_claims() {
        String token = tokenService.create("cj@example.com", "SHIPPER", 10L);

        AuthenticatedMember member = tokenService.parse(token);

        assertThat(member.email()).isEqualTo("cj@example.com");
        assertThat(member.role()).isEqualTo("SHIPPER");
        assertThat(member.profileId()).isEqualTo(10L);
    }

    @Test
    void altered_token_is_rejected() {
        String token = tokenService.create("cj@example.com", "SHIPPER", 10L);

        assertThatThrownBy(() -> tokenService.parse(token + "tampered"))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void expired_token_is_rejected() {
        JwtTokenService expiredService = new JwtTokenService(
                "test-signing-secret-must-be-at-least-thirty-two-bytes", -1);

        assertThatThrownBy(() -> expiredService.parse(expiredService.create("cj@example.com", "SHIPPER", 10L)))
                .isInstanceOf(JwtException.class);
    }
}
