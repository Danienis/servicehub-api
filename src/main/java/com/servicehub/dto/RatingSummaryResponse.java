package com.servicehub.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Média de avaliações de um serviço")
public record RatingSummaryResponse(
        @Schema(description = "ID do serviço", example = "1") Long serviceId,
        @Schema(description = "Média (1 casa decimal); nula se não houver avaliações", example = "4.7") BigDecimal average,
        @Schema(description = "Quantidade de avaliações", example = "12") long totalReviews) {
}
