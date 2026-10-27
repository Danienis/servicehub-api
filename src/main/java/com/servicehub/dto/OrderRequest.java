package com.servicehub.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Schema(description = "Contratação de um serviço (cria uma ordem de serviço). O cliente é o usuário autenticado")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderRequest {

    @Schema(description = "ID do serviço a contratar", example = "1")
    @NotNull(message = "O ID do serviço é obrigatório")
    private Long serviceId;

    @Schema(description = "Data/hora agendada (futura)", example = "2030-01-15T14:00:00")
    @Future(message = "A data agendada deve estar no futuro")
    private LocalDateTime scheduledAt;

    @Schema(description = "Observações para o prestador", example = "Apartamento 302, portaria 24h")
    @Size(max = 2000, message = "As observações devem ter no máximo 2000 caracteres")
    private String notes;
}
