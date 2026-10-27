package com.servicehub.dto;

import com.servicehub.model.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "Novo papel do usuário (somente ADMIN)")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoleUpdateRequest {

    @Schema(description = "Papel", example = "PROVIDER")
    @NotNull(message = "O papel (role) é obrigatório")
    private Role role;
}
