package com.servicehub.model;

import java.util.List;

/**
 * Máquina de estados de uma ordem de serviço (service_requests.status).
 * PENDING -> ACCEPTED -> IN_PROGRESS -> COMPLETED, com CANCELLED a partir de qualquer estado ativo.
 */
public enum OrderStatus {
    PENDING,
    ACCEPTED,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED;

    /** Estados "ativos": bloqueiam a exclusão do serviço/usuário. */
    public static final List<OrderStatus> ACTIVE = List.of(PENDING, ACCEPTED, IN_PROGRESS);

    public boolean isActive() {
        return ACTIVE.contains(this);
    }

    public boolean canTransitionTo(OrderStatus target) {
        return switch (this) {
            case PENDING -> target == ACCEPTED || target == CANCELLED;
            case ACCEPTED -> target == IN_PROGRESS || target == CANCELLED;
            case IN_PROGRESS -> target == COMPLETED || target == CANCELLED;
            case COMPLETED, CANCELLED -> false;
        };
    }
}
