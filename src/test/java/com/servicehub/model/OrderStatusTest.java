package com.servicehub.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class OrderStatusTest {

    @ParameterizedTest(name = "{0} -> {1} = {2}")
    @DisplayName("máquina de estados da ordem de serviço")
    @CsvSource({
            "PENDING,ACCEPTED,true", "PENDING,CANCELLED,true", "PENDING,IN_PROGRESS,false", "PENDING,COMPLETED,false",
            "ACCEPTED,IN_PROGRESS,true", "ACCEPTED,CANCELLED,true", "ACCEPTED,COMPLETED,false",
            "IN_PROGRESS,COMPLETED,true", "IN_PROGRESS,CANCELLED,true", "IN_PROGRESS,PENDING,false",
            "COMPLETED,CANCELLED,false", "CANCELLED,PENDING,false"
    })
    void deveValidarTransicoes(OrderStatus from, OrderStatus to, boolean expected) {
        assertThat(from.canTransitionTo(to)).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({"PENDING,true", "ACCEPTED,true", "IN_PROGRESS,true", "COMPLETED,false", "CANCELLED,false"})
    void deveIdentificarEstadosAtivos(OrderStatus status, boolean active) {
        assertThat(status.isActive()).isEqualTo(active);
    }
}
