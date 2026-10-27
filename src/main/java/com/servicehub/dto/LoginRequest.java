package com.servicehub.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Schema(description = "Credenciais de login")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {

    @Schema(description = "E-mail do usuário (também aceito como \"username\")", example = "joao@hub.com")
    @JsonAlias("username")
    @NotBlank(message = "O e-mail é obrigatório")
    private String email;

    @Schema(description = "Senha", example = "Senha123", accessMode = Schema.AccessMode.WRITE_ONLY)
    @ToString.Exclude
    @NotBlank(message = "A senha é obrigatória")
    private String password;
}
