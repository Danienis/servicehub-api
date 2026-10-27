package com.servicehub.dto;

import com.servicehub.model.ServiceCategory;
import com.servicehub.model.ServiceOffering;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "Representação de um serviço retornada pela API")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ServiceResponse {

    @Schema(description = "Identificador único do serviço", example = "1")
    private Long id;

    @Schema(description = "Título do serviço", example = "Instalação de Ar-Condicionado")
    private String title;

    @Schema(description = "Descrição detalhada do serviço", example = "Instalação completa de ar-condicionado split.")
    private String description;

    @Schema(description = "Preço do serviço em reais", example = "250.00")
    private BigDecimal price;

    @Schema(description = "Categoria do serviço", example = "AR_CONDICIONADO")
    private ServiceCategory category;

    @Schema(description = "ID do prestador responsável pelo serviço", example = "1")
    private Long providerId;

    @Schema(description = "Nome do prestador responsável pelo serviço", example = "Maria Silva")
    private String providerName;

    @Schema(description = "Indica se o serviço está ativo", example = "true")
    private boolean active;

    @Schema(description = "Data de criação do registro")
    private LocalDateTime createdAt;

    @Schema(description = "Data da última atualização do registro")
    private LocalDateTime updatedAt;

    public static ServiceResponse fromEntity(ServiceOffering service) {
        return new ServiceResponse(
                service.getId(),
                service.getTitle(),
                service.getDescription(),
                service.getPrice(),
                service.getCategory(),
                service.getProvider() != null ? service.getProvider().getId() : null,
                service.getProvider() != null ? service.getProvider().getFullName() : null,
                service.isActive(),
                service.getCreatedAt(),
                service.getUpdatedAt()
        );
    }
}
