package com.servicehub.dto;

import com.servicehub.model.ServiceCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
    private String title;

    @Schema(description = "Descrição detalhada do serviço", example = "Instalação completa de ar-condicionado split, incluindo materiais básicos.")
    private String description;

    @Schema(description = "Preço do serviço em reais", example = "250.00")
    @NotNull(message = "O preço é obrigatório")
    @DecimalMin(value = "0.0", inclusive = false, message = "O preço deve ser maior que zero")
    private BigDecimal price;

    @Schema(description = "Categoria do serviço", example = "MANUTENCAO",
            allowableValues = {"LIMPEZA", "MANUTENCAO", "TECNOLOGIA", "BELEZA", "CONSULTORIA", "EDUCACAO", "OUTROS"})
    @NotNull(message = "A categoria é obrigatória")
    private ServiceCategory category;

    @Schema(description = "ID do usuário prestador responsável pelo serviço", example = "1")
    @NotNull(message = "O ID do prestador é obrigatório")
    private Long providerId;
}
