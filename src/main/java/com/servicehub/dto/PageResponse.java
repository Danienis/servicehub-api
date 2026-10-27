package com.servicehub.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/** Envelope de paginação estável (não expõe o PageImpl do Spring Data). */
@Schema(description = "Página de resultados")
public record PageResponse<T>(
        @Schema(description = "Itens da página") List<T> content,
        @Schema(description = "Número da página (começa em 0)", example = "0") int page,
        @Schema(description = "Tamanho da página", example = "20") int size,
        @Schema(description = "Total de elementos", example = "135") long totalElements,
        @Schema(description = "Total de páginas", example = "7") int totalPages,
        @Schema(description = "Indica se é a última página") boolean last) {

    public static <E, T> PageResponse<T> of(Page<E> page, Function<E, T> mapper) {
        return new PageResponse<>(
                page.getContent().stream().map(mapper).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast());
    }
}
