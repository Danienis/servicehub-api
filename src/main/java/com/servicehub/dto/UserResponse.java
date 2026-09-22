package com.servicehub.dto;

import com.servicehub.model.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Schema(description = "Representação de um usuário retornada pela API")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

    @Schema(description = "Identificador único do usuário", example = "1")
    private Long id;

    @Schema(description = "Nome completo do usuário", example = "Maria Silva")
    private String fullName;

    @Schema(description = "E-mail do usuário", example = "maria@servicehub.com")
    private String email;

    @Schema(description = "Telefone de contato", example = "(62) 99999-0000")
    private String phone;

    @Schema(description = "Indica se o usuário está ativo na plataforma", example = "true")
    private boolean active;

    @Schema(description = "Data de criação do registro")
    private LocalDateTime createdAt;

    @Schema(description = "Data da última atualização do registro")
    private LocalDateTime updatedAt;

    public static UserResponse fromEntity(User user) {
        return new UserResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getPhone(),
                user.isActive(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
