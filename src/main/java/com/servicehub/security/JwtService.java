package com.servicehub.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Criação e validação de tokens JWT (HS256, biblioteca jjwt).
 * Dois tipos de token, diferenciados pelo claim "type": "access" (24h) e "refresh" (7 dias).
 */
@Service
public class JwtService {

    public static final String TYPE_ACCESS = "access";
    public static final String TYPE_REFRESH = "refresh";

    private final SecretKey key;
    private final long accessExpirationMs;
    private final long refreshExpirationMs;

    public JwtService(@Value("${jwt.secret}") String secret,
                      @Value("${jwt.expiration}") long accessExpirationMs,
                      @Value("${jwt.refresh-expiration}") long refreshExpirationMs) {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("jwt.secret precisa ter ao menos 32 caracteres (256 bits) para HS256");
        }
        this.key = Keys.hmacShaKeyFor(bytes);
        this.accessExpirationMs = accessExpirationMs;
        this.refreshExpirationMs = refreshExpirationMs;
    }

    public String generateAccessToken(AuthUser user) {
        return build(user, accessExpirationMs, TYPE_ACCESS);
    }

    public String generateRefreshToken(AuthUser user) {
        return build(user, refreshExpirationMs, TYPE_REFRESH);
    }

    private String build(AuthUser user, long expirationMs, String type) {
        Date now = new Date();
        return Jwts.builder()
                .subject(user.getUsername())
                .claim("uid", user.getId())
                .claim("roles", user.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList())
                .claim("type", type)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expirationMs))
                .signWith(key)
                .compact();
    }

    /** Valida assinatura e expiração; lança JwtException se inválido. */
    public Claims parse(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }

    public String extractUsername(String token) {
        return parse(token).getSubject();
    }

    /** Token de acesso válido (assinatura, expiração, tipo "access") e pertencente ao usuário. */
    public boolean isTokenValid(String token, UserDetails user) {
        try {
            Claims claims = parse(token);
            return user.getUsername().equals(claims.getSubject())
                    && TYPE_ACCESS.equals(claims.get("type", String.class));
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /** Refresh token válido: devolve o e-mail (subject) ou lança JwtException. */
    public String extractUsernameFromRefreshToken(String token) {
        Claims claims = parse(token);
        if (!TYPE_REFRESH.equals(claims.get("type", String.class))) {
            throw new JwtException("Token informado não é um refresh token");
        }
        return claims.getSubject();
    }

    public long getAccessExpirationSeconds() {
        return accessExpirationMs / 1000;
    }
}
