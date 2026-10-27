package com.servicehub.dto;

import com.servicehub.model.OrderStatus;
import com.servicehub.model.ServiceOrder;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "Ordem de serviço retornada pela API")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {

    @Schema(description = "Identificador da ordem", example = "1")
    private Long id;
    @Schema(description = "ID do serviço contratado", example = "1")
    private Long serviceId;
    @Schema(description = "Título do serviço", example = "Instalação de Ar-Condicionado")
    private String serviceTitle;
    @Schema(description = "ID do prestador", example = "2")
    private Long providerId;
    @Schema(description = "Nome do prestador", example = "João Prestador")
    private String providerName;
    @Schema(description = "ID do cliente", example = "3")
    private Long clientId;
    @Schema(description = "Nome do cliente", example = "Ana Cliente")
    private String clientName;
    @Schema(description = "Status atual", example = "PENDING")
    private OrderStatus status;
    @Schema(description = "Data/hora agendada")
    private LocalDateTime scheduledAt;
    @Schema(description = "Observações")
    private String notes;
    @Schema(description = "Preço no momento da contratação (snapshot)", example = "250.00")
    private BigDecimal totalPrice;
    @Schema(description = "Data de criação")
    private LocalDateTime createdAt;
    @Schema(description = "Data da última atualização")
    private LocalDateTime updatedAt;

    public static OrderResponse fromEntity(ServiceOrder o) {
        return new OrderResponse(
                o.getId(),
                o.getService().getId(),
                o.getService().getTitle(),
                o.getService().getProvider().getId(),
                o.getService().getProvider().getFullName(),
                o.getClient().getId(),
                o.getClient().getFullName(),
                o.getStatus(),
                o.getScheduledAt(),
                o.getNotes(),
                o.getTotalPrice(),
                o.getCreatedAt(),
                o.getUpdatedAt());
    }
}
