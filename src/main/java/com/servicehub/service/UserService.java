package com.servicehub.service;

import com.servicehub.dto.RegisterRequest;
import com.servicehub.dto.UserRequest;
import com.servicehub.dto.UserUpdateRequest;
import com.servicehub.exception.BusinessException;
import com.servicehub.exception.ResourceNotFoundException;
import com.servicehub.model.OrderStatus;
import com.servicehub.model.Role;
import com.servicehub.model.ServiceOffering;
import com.servicehub.model.User;
import com.servicehub.repository.ServiceOfferingRepository;
import com.servicehub.repository.ServiceOrderRepository;
import com.servicehub.repository.UserRepository;
import com.servicehub.security.AuthUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Regras de negócio de usuários. O Controller nunca acessa o Repository diretamente.
 * Senhas são sempre salvas com BCrypt; e-mails normalizados em minúsculas; exclusão é lógica.
 */
@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final ServiceOfferingRepository serviceRepository;
    private final ServiceOrderRepository orderRepository;
    private final PasswordEncoder passwordEncoder;
    private final InputSanitizer sanitizer;

    public UserService(UserRepository userRepository,
                       ServiceOfferingRepository serviceRepository,
                       ServiceOrderRepository orderRepository,
                       PasswordEncoder passwordEncoder,
                       InputSanitizer sanitizer) {
        this.userRepository = userRepository;
        this.serviceRepository = serviceRepository;
        this.orderRepository = orderRepository;
        this.passwordEncoder = passwordEncoder;
        this.sanitizer = sanitizer;
    }

    /** Auto-cadastro público: apenas CLIENT (padrão) ou PROVIDER. ADMIN nunca é aceito (mass assignment). */
    public User register(RegisterRequest request) {
        Role role = request.getRole() == null ? Role.CLIENT : request.getRole();
        if (role == Role.ADMIN) {
            throw new IllegalArgumentException("Papel inválido para auto-cadastro: use CLIENT ou PROVIDER");
        }
        return persistNew(request.getFullName(), request.getEmail(), request.getPassword(), role,
                request.getPhone(), null, null);
    }

    /** Criação por um administrador (pode definir qualquer papel). */
    public User create(UserRequest request) {
        return persistNew(request.getFullName(), request.getEmail(), request.getPassword(), request.getRole(),
                request.getPhone(), request.getBio(), request.getAvatarUrl());
    }

    private User persistNew(String fullName, String rawEmail, String rawPassword, Role role,
                            String phone, String bio, String avatarUrl) {
        String email = sanitizer.normalizeEmail(rawEmail);
        if (userRepository.existsByEmail(email)) {
            throw new BusinessException("Já existe um usuário cadastrado com este e-mail: " + email);
        }
        User user = new User();
        user.setFullName(sanitizer.clean(fullName, 100));
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setRole(role);
        user.setPhone(sanitizer.clean(phone, 20));
        user.setBio(sanitizer.clean(bio));
        user.setAvatarUrl(avatarUrl);
        user.setActive(true);
        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public Page<User> findAll(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    /** Busca interna (sem checagem de dono). */
    @Transactional(readOnly = true)
    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado com o ID: " + id));
    }

    /** Busca para a API: somente o próprio usuário ou um ADMIN (proteção contra BOLA). */
    @Transactional(readOnly = true)
    public User findById(Long id, AuthUser actor) {
        User user = findById(id);
        assertSelfOrAdmin(id, actor);
        return user;
    }

    public User update(Long id, UserUpdateRequest request, AuthUser actor) {
        User user = findById(id);
        assertSelfOrAdmin(id, actor);

        String email = sanitizer.normalizeEmail(request.getEmail());
        if (!user.getEmail().equals(email) && userRepository.existsByEmail(email)) {
            throw new BusinessException("Já existe um usuário cadastrado com este e-mail: " + email);
        }

        user.setFullName(sanitizer.clean(request.getFullName(), 100));
        user.setEmail(email);
        user.setPhone(sanitizer.clean(request.getPhone(), 20));
        user.setBio(sanitizer.clean(request.getBio()));
        user.setAvatarUrl(request.getAvatarUrl());
        return userRepository.save(user);
    }

    /** Somente ADMIN (garantido por @PreAuthorize no controller). */
    public User changeRole(Long id, Role role) {
        User user = findById(id);
        user.setRole(role);
        return userRepository.save(user);
    }

    /**
     * Exclusão lógica (is_active = false): o usuário deixa de logar e o JWT dele deixa de valer.
     * Bloqueada (409) se houver ordens ativas como cliente ou prestador. Os serviços do prestador são desativados.
     */
    public void delete(Long id, AuthUser actor) {
        User user = findById(id);
        assertSelfOrAdmin(id, actor);

        if (orderRepository.existsByClientIdAndStatusIn(id, OrderStatus.ACTIVE)
                || orderRepository.existsByServiceProviderIdAndStatusIn(id, OrderStatus.ACTIVE)) {
            throw new BusinessException("Usuário possui ordens de serviço ativas (PENDING, ACCEPTED ou IN_PROGRESS) "
                    + "e não pode ser excluído");
        }

        for (ServiceOffering service : serviceRepository.findByProviderId(id)) {
            service.setActive(false);
        }
        user.setActive(false);
        userRepository.save(user);
    }

    private void assertSelfOrAdmin(Long id, AuthUser actor) {
        if (!actor.isAdmin() && !actor.getId().equals(id)) {
            throw new AccessDeniedException("Acesso negado: você só pode acessar o seu próprio usuário");
        }
    }
}
