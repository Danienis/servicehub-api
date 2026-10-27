package com.servicehub.service;

import com.servicehub.dto.OrderRequest;
import com.servicehub.exception.BusinessException;
import com.servicehub.exception.ResourceNotFoundException;
import com.servicehub.model.OrderStatus;
import com.servicehub.model.Role;
import com.servicehub.model.ServiceOffering;
import com.servicehub.model.ServiceOrder;
import com.servicehub.model.User;
import com.servicehub.repository.ServiceOfferingRepository;
import com.servicehub.repository.ServiceOrderRepository;
import com.servicehub.repository.UserRepository;
import com.servicehub.security.AuthUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServiceOrderServiceTest {

    @Mock ServiceOrderRepository orderRepository;
    @Mock ServiceOfferingRepository serviceRepository;
    @Mock UserRepository userRepository;
    @Spy InputSanitizer sanitizer = new InputSanitizer();

    ServiceOrderService service;
    User provider;
    User client;
    ServiceOffering offering;

    @BeforeEach
    void setUp() {
        service = new ServiceOrderService(orderRepository, serviceRepository, userRepository, sanitizer);
        provider = user(2, Role.PROVIDER);
        client = user(3, Role.CLIENT);
        offering = new ServiceOffering();
        offering.setId(10L);
        offering.setProvider(provider);
        offering.setActive(true);
        offering.setPrice(new BigDecimal("250.00"));
    }

    private static User user(long id, Role role) {
        User u = new User();
        u.setId(id);
        u.setFullName("Usuário " + id);
        u.setRole(role);
        u.setActive(true);
        return u;
    }

    private static AuthUser actor(long id, Role role) {
        return new AuthUser(id, "u" + id + "@hub.com", "hash", role, true);
    }

    private ServiceOrder order(OrderStatus status) {
        ServiceOrder o = new ServiceOrder();
        o.setId(1L);
        o.setService(offering);
        o.setClient(client);
        o.setStatus(status);
        o.setTotalPrice(offering.getPrice());
        return o;
    }

    @Test
    @DisplayName("deve criar ordem PENDING copiando o preço do serviço (snapshot)")
    void deveCriarComSnapshotDePreco() {
        when(serviceRepository.findById(10L)).thenReturn(Optional.of(offering));
        when(userRepository.findById(3L)).thenReturn(Optional.of(client));
        when(orderRepository.save(any(ServiceOrder.class))).thenAnswer(i -> i.getArgument(0));

        ServiceOrder created = service.create(new OrderRequest(10L, null, "obs"), actor(3, Role.CLIENT));

        assertThat(created.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(created.getTotalPrice()).isEqualByComparingTo("250.00");
        assertThat(created.getClient().getId()).isEqualTo(3L);
    }

    @Test
    @DisplayName("deve lançar 404 quando o serviço não existe")
    void deveLancarNaoEncontrado() {
        when(serviceRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(new OrderRequest(99L, null, null), actor(3, Role.CLIENT)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("deve impedir contratar serviço inativo ou o próprio serviço")
    void deveImpedirContratacoesInvalidas() {
        when(serviceRepository.findById(10L)).thenReturn(Optional.of(offering));

        assertThatThrownBy(() -> service.create(new OrderRequest(10L, null, null), actor(2, Role.PROVIDER)))
                .isInstanceOf(BusinessException.class);

        offering.setActive(false);
        assertThatThrownBy(() -> service.create(new OrderRequest(10L, null, null), actor(3, Role.CLIENT)))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("prestador aceita a ordem")
    void prestadorAceita() {
        ServiceOrder o = order(OrderStatus.PENDING);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(o));
        when(orderRepository.save(any(ServiceOrder.class))).thenAnswer(i -> i.getArgument(0));

        assertThat(service.updateStatus(1L, OrderStatus.ACCEPTED, actor(2, Role.PROVIDER)).getStatus())
                .isEqualTo(OrderStatus.ACCEPTED);
    }

    @Test
    @DisplayName("cliente não pode aceitar, mas pode cancelar")
    void clienteSoCancela() {
        ServiceOrder o = order(OrderStatus.PENDING);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(o));
        when(orderRepository.save(any(ServiceOrder.class))).thenAnswer(i -> i.getArgument(0));

        assertThatThrownBy(() -> service.updateStatus(1L, OrderStatus.ACCEPTED, actor(3, Role.CLIENT)))
                .isInstanceOf(AccessDeniedException.class);
        assertThat(service.updateStatus(1L, OrderStatus.CANCELLED, actor(3, Role.CLIENT)).getStatus())
                .isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    @DisplayName("transição inválida gera 409")
    void transicaoInvalida() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order(OrderStatus.PENDING)));

        assertThatThrownBy(() -> service.updateStatus(1L, OrderStatus.COMPLETED, actor(2, Role.PROVIDER)))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("terceiro não enxerga a ordem (BOLA)")
    void terceiroNaoEnxerga() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order(OrderStatus.PENDING)));

        assertThatThrownBy(() -> service.findById(1L, actor(77, Role.CLIENT)))
                .isInstanceOf(AccessDeniedException.class);
        assertThat(service.findById(1L, actor(77, Role.ADMIN))).isNotNull();
    }
}
