package com.servicehub.security;

import com.servicehub.model.Role;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRET = "unit-test-secret-unit-test-secret-1234";
    private JwtService jwtService;
    private AuthUser user;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET, 60_000, 120_000);
        user = new AuthUser(7L, "joao@hub.com", "hash", Role.PROVIDER, true);
    }

    @Test
    @DisplayName("deve gerar token de acesso válido com roles e uid")
    void deveGerarTokenValido() {
        String token = jwtService.generateAccessToken(user);

        assertThat(jwtService.isTokenValid(token, user)).isTrue();
        assertThat(jwtService.extractUsername(token)).isEqualTo("joao@hub.com");
        assertThat(jwtService.parse(token).get("roles")).asList().containsExactly("ROLE_PROVIDER");
        assertThat(((Number) jwtService.parse(token).get("uid")).longValue()).isEqualTo(7L);
    }

    @Test
    @DisplayName("deve rejeitar token adulterado")
    void deveRejeitarTokenAdulterado() {
        String token = jwtService.generateAccessToken(user);
        int i = token.length() - 10;
        char flipped = token.charAt(i) == 'A' ? 'B' : 'A';
        String tampered = token.substring(0, i) + flipped + token.substring(i + 1);

        assertThat(jwtService.isTokenValid(tampered, user)).isFalse();
    }

    @Test
    @DisplayName("deve rejeitar token expirado")
    void deveRejeitarTokenExpirado() {
        JwtService expired = new JwtService(SECRET, -1_000, -1_000);

        assertThat(jwtService.isTokenValid(expired.generateAccessToken(user), user)).isFalse();
    }

    @Test
    @DisplayName("deve rejeitar token assinado com outro segredo")
    void deveRejeitarOutroSegredo() {
        JwtService other = new JwtService("another-secret-another-secret-123456", 60_000, 60_000);

        assertThat(jwtService.isTokenValid(other.generateAccessToken(user), user)).isFalse();
    }

    @Test
    @DisplayName("refresh token não vale como access token e vice-versa")
    void deveSepararTiposDeToken() {
        String refresh = jwtService.generateRefreshToken(user);
        String access = jwtService.generateAccessToken(user);

        assertThat(jwtService.isTokenValid(refresh, user)).isFalse();
        assertThat(jwtService.extractUsernameFromRefreshToken(refresh)).isEqualTo("joao@hub.com");
        assertThatThrownBy(() -> jwtService.extractUsernameFromRefreshToken(access)).isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("deve exigir segredo com ao menos 32 caracteres")
    void deveExigirSegredoForte() {
        assertThatThrownBy(() -> new JwtService("curto", 1, 1)).isInstanceOf(IllegalStateException.class);
    }
}
