package com.servicehub.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "Dados de entrada para criação ou atualização de um usuário")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserRequest {

    @Schema(description = "Nome completo do usuário", example = "Maria Silva", minLength = 3, maxLength = 100)
    @NotBlank(message = "O nome completo é obrigatório")
    @Size(min = 3, max = 100, message = "O nome completo deve ter entre 3 e 100 caracteres")
    private String fullName;

    @Schema(description = "E-mail único do usuário", example = "maria@servicehub.com")
    @NotBlank(message = "O e-mail é obrigatório")
    @Email(message = "E-mail inválido")
    private String email;

    @Schema(description = "Telefone de contato", example = "(62) 99999-0000")
    private String phone;
}
