package com.servicehub.dto;

import com.servicehub.model.Review;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Schema(description = "Avaliação retornada pela API")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReviewResponse {

    @Schema(description = "Identificador da avaliação", example = "1")
    private Long id;
    @Schema(description = "ID da ordem avaliada", example = "1")
    private Long orderId;
    @Schema(description = "ID de quem avaliou", example = "3")
    private Long reviewerId;
    @Schema(description = "Nome de quem avaliou", example = "Ana Cliente")
    private String reviewerName;
    @Schema(description = "Nota de 1 a 5", example = "5")
    private Integer rating;
    @Schema(description = "Comentário")
    private String comment;
    @Schema(description = "Data de criação")
    private LocalDateTime createdAt;

    public static ReviewResponse fromEntity(Review r) {
        return new ReviewResponse(
                r.getId(),
                r.getServiceOrder().getId(),
                r.getReviewer().getId(),
                r.getReviewer().getFullName(),
                r.getRating(),
                r.getComment(),
                r.getCreatedAt());
    }
}
