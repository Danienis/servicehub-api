package com.servicehub.dto;

import com.servicehub.model.ServiceCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Schema(description = "Dados de entrada para criação ou atualização de um serviço")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ServiceRequest {

    @Schema(description = "Título do serviço", example = "Instalação de Ar-Condicionado", minLength = 3, maxLength = 150)
    @NotBlank(message = "O título é obrigatório")
    @Size(min = 3, max = 150, message = "O título deve ter entre 3 e 150 caracteres")
    private String title;

    @Schema(description = "Descrição detalhada do serviço", example = "Instalação completa de ar-condicionado split, incluindo materiais básicos.")
    @Size(max = 5000, message = "A descrição deve ter no máximo 5000 caracteres")
    private String description;

    @Schema(description = "Preço do serviço em reais (maior que zero)", example = "250.00")
    @NotNull(message = "O preço é obrigatório")
    @DecimalMin(value = "0.0", inclusive = false, message = "O preço deve ser maior que zero")
    @DecimalMax(value = "99999999.99", message = "O preço excede o valor máximo permitido")
    private BigDecimal price;

    @Schema(description = "Categoria do serviço", example = "AR_CONDICIONADO")
    @NotNull(message = "A categoria é obrigatória")
    private ServiceCategory category;

    @Schema(description = "ID do prestador. Opcional para PROVIDER (usa o usuário autenticado); "
            + "obrigatório quando um ADMIN cria/atualiza em nome de um prestador", example = "1")
    private Long providerId;
}
