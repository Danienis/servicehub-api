package com.servicehub.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "Avaliação de uma ordem concluída")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReviewRequest {

    @Schema(description = "Nota de 1 a 5", example = "5", minimum = "1", maximum = "5")
    @NotNull(message = "A nota é obrigatória")
    @Min(value = 1, message = "A nota mínima é 1")
    @Max(value = 5, message = "A nota máxima é 5")
    private Integer rating;

    @Schema(description = "Comentário opcional", example = "Serviço excelente e pontual!")
    @Size(max = 2000, message = "O comentário deve ter no máximo 2000 caracteres")
    private String comment;
}
