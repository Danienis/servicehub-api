package com.servicehub.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Schema(description = "Resposta de autenticação: tokens JWT e dados do usuário")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    @Schema(description = "Token de acesso (enviar em Authorization: Bearer <token>)")
    @ToString.Exclude
    private String accessToken;

    @Schema(description = "Token de renovação (7 dias)")
    @ToString.Exclude
    private String refreshToken;

    @Schema(description = "Tipo do token", example = "Bearer")
    private String tokenType = "Bearer";

    @Schema(description = "Validade do access token em segundos", example = "86400")
    private long expiresIn;

    @Schema(description = "Usuário autenticado")
    private UserResponse user;

    public AuthResponse(String accessToken, String refreshToken, long expiresIn, UserResponse user) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.tokenType = "Bearer";
        this.expiresIn = expiresIn;
        this.user = user;
    }
}
