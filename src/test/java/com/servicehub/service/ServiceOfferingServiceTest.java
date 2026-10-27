package com.servicehub.service;

import com.servicehub.dto.ServiceRequest;
import com.servicehub.exception.BusinessException;
import com.servicehub.exception.ResourceNotFoundException;
import com.servicehub.model.OrderStatus;
import com.servicehub.model.Role;
import com.servicehub.model.ServiceCategory;
import com.servicehub.model.ServiceOffering;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServiceOfferingServiceTest {

    @Mock ServiceOfferingRepository serviceRepository;
    @Mock UserRepository userRepository;
    @Mock ServiceOrderRepository orderRepository;
    @Spy InputSanitizer sanitizer = new InputSanitizer();

    ServiceOfferingService service;

    @BeforeEach
    void setUp() {
        service = new ServiceOfferingService(serviceRepository, userRepository, orderRepository, sanitizer);
    }

    private static User user(long id, Role role) {
        User u = new User();
        u.setId(id);
        u.setFullName("Usuário " + id);
        u.setRole(role);
        return u;
    }

    private static AuthUser actor(long id, Role role) {
        return new AuthUser(id, "u" + id + "@hub.com", "hash", role, true);
    }

    private static ServiceRequest request(Long providerId) {
        return new ServiceRequest("Limpeza <b>pesada</b>", "Descrição", new BigDecimal("150.00"),
                ServiceCategory.LIMPEZA, providerId);
    }

    private static ServiceOffering offering(long id, User provider) {
        ServiceOffering s = new ServiceOffering();
        s.setId(id);
        s.setProvider(provider);
        s.setActive(true);
        s.setPrice(BigDecimal.TEN);
        return s;
    }

    @Test
    @DisplayName("PROVIDER cria serviço para si mesmo, com título sanitizado")
    void deveCriarParaOProprioPrestador() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(user(2, Role.PROVIDER)));
        when(serviceRepository.save(any(ServiceOffering.class))).thenAnswer(i -> i.getArgument(0));

        ServiceOffering created = service.create(request(null), actor(2, Role.PROVIDER));

        assertThat(created.getProvider().getId()).isEqualTo(2L);
        assertThat(created.getTitle()).doesNotContain("<b>");
        assertThat(created.isActive()).isTrue();
    }

    @Test
    @DisplayName("PROVIDER não pode criar serviço em nome de outro prestador")
    void deveNegarCriacaoEmNomeDeOutro() {
        assertThatThrownBy(() -> service.create(request(3L), actor(2, Role.PROVIDER)))
                .isInstanceOf(AccessDeniedException.class);
        verify(serviceRepository, never()).save(any());
    }

    @Test
    @DisplayName("ADMIN precisa informar um prestador válido (role PROVIDER)")
    void deveExigirPrestadorValidoParaAdmin() {
        assertThatThrownBy(() -> service.create(request(null), actor(1, Role.ADMIN)))
                .isInstanceOf(IllegalArgumentException.class);

        when(userRepository.findById(5L)).thenReturn(Optional.of(user(5, Role.CLIENT)));
        assertThatThrownBy(() -> service.create(request(5L), actor(1, Role.ADMIN)))
                .isInstanceOf(BusinessException.class);

        when(userRepository.findById(6L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.create(request(6L), actor(1, Role.ADMIN)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("somente o dono (ou ADMIN) altera o serviço")
    void deveNegarAtualizacaoDeOutroPrestador() {
        when(serviceRepository.findById(10L)).thenReturn(Optional.of(offering(10, user(2, Role.PROVIDER))));

        assertThatThrownBy(() -> service.update(10L, request(null), actor(3, Role.PROVIDER)))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("serviço inativo é tratado como não encontrado")
    void deveTratarInativoComoNaoEncontrado() {
        ServiceOffering inactive = offering(10, user(2, Role.PROVIDER));
        inactive.setActive(false);
        when(serviceRepository.findById(10L)).thenReturn(Optional.of(inactive));

        assertThatThrownBy(() -> service.findById(10L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("exclusão é lógica e bloqueada com ordens ativas")
    void deveExcluirLogicamenteOuBloquear() {
        ServiceOffering s = offering(10, user(2, Role.PROVIDER));
        when(serviceRepository.findById(10L)).thenReturn(Optional.of(s));

        when(orderRepository.existsByServiceIdAndStatusIn(10L, OrderStatus.ACTIVE)).thenReturn(true);
        assertThatThrownBy(() -> service.delete(10L, actor(2, Role.PROVIDER))).isInstanceOf(BusinessException.class);
        assertThat(s.isActive()).isTrue();

        when(orderRepository.existsByServiceIdAndStatusIn(10L, OrderStatus.ACTIVE)).thenReturn(false);
        service.delete(10L, actor(2, Role.PROVIDER));
        assertThat(s.isActive()).isFalse();
        verify(serviceRepository, never()).delete(any(ServiceOffering.class));
    }

    @Test
    @DisplayName("deve rejeitar faixa de preço invertida")
    void deveRejeitarFaixaInvertida() {
        assertThatThrownBy(() -> service.search(null, new BigDecimal("500"), new BigDecimal("100"), null, null,
                org.springframework.data.domain.Pageable.unpaged()))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
