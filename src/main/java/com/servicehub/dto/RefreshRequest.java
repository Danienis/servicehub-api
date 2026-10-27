package com.servicehub.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Schema(description = "Refresh token para obter um novo par de tokens")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RefreshRequest {

    @Schema(description = "Refresh token recebido no login")
    @ToString.Exclude
    @NotBlank(message = "O refresh token é obrigatório")
    private String refreshToken;
}
