package com.cjlogistics.mini.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtTokenService {
    private final SecretKey key;
    private final long expirationSeconds;
    public JwtTokenService(@Value("${app.security.jwt.secret}") String secret,
                           @Value("${app.security.jwt.expiration-seconds:3600}") long expirationSeconds) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)); this.expirationSeconds = expirationSeconds;
    }
    public String create(String email, String role, Long profileId) {
        Instant now = Instant.now();
        return Jwts.builder().subject(email).claim("role", role).claim("profileId", profileId)
                .issuedAt(Date.from(now)).expiration(Date.from(now.plusSeconds(expirationSeconds))).signWith(key).compact();
    }
    public AuthenticatedMember parse(String token) {
        var claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        return new AuthenticatedMember(claims.get("role", String.class), claims.get("profileId", Long.class), claims.getSubject());
    }
    public long expirationSeconds() { return expirationSeconds; }
}
