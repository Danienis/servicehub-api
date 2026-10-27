package com.servicehub.service;

import com.servicehub.dto.RegisterRequest;
import com.servicehub.dto.UserUpdateRequest;
import com.servicehub.exception.BusinessException;
import com.servicehub.exception.ResourceNotFoundException;
import com.servicehub.model.OrderStatus;
import com.servicehub.model.Role;
import com.servicehub.model.User;
import com.servicehub.repository.ServiceOfferingRepository;
import com.servicehub.repository.ServiceOrderRepository;
import com.servicehub.repository.UserRepository;
import com.servicehub.security.AuthUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock UserRepository userRepository;
    @Mock ServiceOfferingRepository serviceRepository;
    @Mock ServiceOrderRepository orderRepository;
    @Spy PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    @Spy InputSanitizer sanitizer = new InputSanitizer();

    UserService service;

    @BeforeEach
    void setUp() {
        service = new UserService(userRepository, serviceRepository, orderRepository, passwordEncoder, sanitizer);
    }

    private static RegisterRequest register(Role role) {
        return new RegisterRequest("João Silva", "  Joao@Hub.COM ", "Senha123", role, "(62) 99999-0000");
    }

    private static User user(long id, Role role) {
        User u = new User();
        u.setId(id);
        u.setEmail("u" + id + "@hub.com");
        u.setFullName("Usuário " + id);
        u.setRole(role);
        u.setPasswordHash("hash");
        return u;
    }

    private static AuthUser actor(long id, Role role) {
        return new AuthUser(id, "u" + id + "@hub.com", "hash", role, true);
    }

    @Test
    @DisplayName("deve cadastrar com e-mail em minúsculas e senha com BCrypt")
    void deveCadastrarComHash() {
        when(userRepository.existsByEmail("joao@hub.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        User saved = service.register(register(null));

        assertThat(saved.getEmail()).isEqualTo("joao@hub.com");
        assertThat(saved.getRole()).isEqualTo(Role.CLIENT);
        assertThat(saved.getPasswordHash()).isNotEqualTo("Senha123");
        assertThat(new BCryptPasswordEncoder().matches("Senha123", saved.getPasswordHash())).isTrue();
    }

    @Test
    @DisplayName("deve lançar BusinessException quando o e-mail já existe")
    void deveRejeitarEmailDuplicado() {
        when(userRepository.existsByEmail("joao@hub.com")).thenReturn(true);

        assertThatThrownBy(() -> service.register(register(Role.PROVIDER))).isInstanceOf(BusinessException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("deve rejeitar auto-cadastro como ADMIN")
    void deveRejeitarAdmin() {
        assertThatThrownBy(() -> service.register(register(Role.ADMIN))).isInstanceOf(IllegalArgumentException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("deve lançar 404 quando o usuário não existe")
    void deveLancarNaoEncontrado() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(99L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("deve negar acesso aos dados de outro usuário (BOLA) e permitir ao ADMIN")
    void deveProtegerContraBola() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(user(2, Role.CLIENT)));

        assertThatThrownBy(() -> service.findById(2L, actor(1, Role.CLIENT))).isInstanceOf(AccessDeniedException.class);
        assertThat(service.findById(2L, actor(2, Role.CLIENT))).isNotNull();
        assertThat(service.findById(2L, actor(9, Role.ADMIN))).isNotNull();
    }

    @Test
    @DisplayName("deve atualizar sem alterar papel nem senha")
    void deveAtualizarSemTocarPapel() {
        User existing = user(1, Role.CLIENT);
        when(userRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        User updated = service.update(1L,
                new UserUpdateRequest("Novo Nome", "u1@hub.com", null, "bio <b>x</b>", null), actor(1, Role.CLIENT));

        assertThat(updated.getFullName()).isEqualTo("Novo Nome");
        assertThat(updated.getRole()).isEqualTo(Role.CLIENT);
        assertThat(updated.getPasswordHash()).isEqualTo("hash");
        assertThat(updated.getBio()).doesNotContain("<b>");
    }

    @Test
    @DisplayName("deve bloquear exclusão de usuário com ordens ativas")
    void deveBloquearExclusaoComOrdensAtivas() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1, Role.PROVIDER)));
        when(orderRepository.existsByClientIdAndStatusIn(1L, OrderStatus.ACTIVE)).thenReturn(false);
        when(orderRepository.existsByServiceProviderIdAndStatusIn(1L, OrderStatus.ACTIVE)).thenReturn(true);

        assertThatThrownBy(() -> service.delete(1L, actor(1, Role.PROVIDER))).isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("deve excluir logicamente (is_active = false) e desativar os serviços do prestador")
    void deveExcluirLogicamente() {
        User u = user(1, Role.PROVIDER);
        when(userRepository.findById(1L)).thenReturn(Optional.of(u));
        when(serviceRepository.findByProviderId(1L)).thenReturn(List.of());

        service.delete(1L, actor(1, Role.PROVIDER));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().isActive()).isFalse();
        verify(userRepository, never()).delete(any(User.class));
    }
}
