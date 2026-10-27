package com.servicehub.dto;

import com.servicehub.model.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Schema(description = "Dados para criação de um usuário por um administrador (pode definir o papel)")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserRequest {

    @Schema(description = "Nome completo do usuário", example = "Maria Silva", minLength = 3, maxLength = 100)
    @NotBlank(message = "O nome completo é obrigatório")
    @Size(min = 3, max = 100, message = "O nome completo deve ter entre 3 e 100 caracteres")
    private String fullName;

    @Schema(description = "E-mail único do usuário (armazenado em minúsculas)", example = "maria@servicehub.com")
    @NotBlank(message = "O e-mail é obrigatório")
    @Email(message = "E-mail inválido")
    @Size(max = 255, message = "O e-mail deve ter no máximo 255 caracteres")
    private String email;

    @Schema(description = "Senha: mínimo 8 caracteres, com ao menos uma letra maiúscula e um número",
            example = "Senha123", accessMode = Schema.AccessMode.WRITE_ONLY)
    @ToString.Exclude
    @NotBlank(message = "A senha é obrigatória")
    @Size(min = 8, max = 72, message = "A senha deve ter entre 8 e 72 caracteres")
    @Pattern(regexp = "^(?=.*[A-Z])(?=.*\\d).+$",
            message = "A senha deve conter ao menos uma letra maiúscula e um número")
    private String password;

    @Schema(description = "Papel do usuário", example = "PROVIDER")
    @NotNull(message = "O papel (role) é obrigatório")
    private Role role;

    @Schema(description = "Telefone de contato", example = "(62) 99999-0000")
    @Size(max = 20, message = "O telefone deve ter no máximo 20 caracteres")
    private String phone;

    @Schema(description = "Apresentação do usuário")
    @Size(max = 2000, message = "A bio deve ter no máximo 2000 caracteres")
    private String bio;

    @Schema(description = "URL do avatar (http/https)", example = "https://exemplo.com/avatar.png")
    @Size(max = 500, message = "A URL do avatar deve ter no máximo 500 caracteres")
    @Pattern(regexp = "^https?://.+", message = "A URL do avatar deve começar com http:// ou https://")
    private String avatarUrl;
}
