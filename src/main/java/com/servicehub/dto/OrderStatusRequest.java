package com.servicehub.dto;

import com.servicehub.model.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "Novo status da ordem de serviço")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatusRequest {

    @Schema(description = "Status de destino", example = "ACCEPTED")
    @NotNull(message = "O status é obrigatório")
    private OrderStatus status;
}
