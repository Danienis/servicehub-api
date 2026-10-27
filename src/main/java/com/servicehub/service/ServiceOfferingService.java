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
import com.servicehub.repository.ServiceOfferingSpecs;
import com.servicehub.repository.ServiceOrderRepository;
import com.servicehub.repository.UserRepository;
import com.servicehub.security.AuthUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Regras de negócio dos serviços oferecidos. Só o prestador dono (ou um ADMIN) altera/exclui;
 * a exclusão é lógica e bloqueada enquanto houver ordens ativas.
 */
@Service
@Transactional
public class ServiceOfferingService {

    private final ServiceOfferingRepository serviceRepository;
    private final UserRepository userRepository;
    private final ServiceOrderRepository orderRepository;
    private final InputSanitizer sanitizer;

    public ServiceOfferingService(ServiceOfferingRepository serviceRepository,
                                  UserRepository userRepository,
                                  ServiceOrderRepository orderRepository,
                                  InputSanitizer sanitizer) {
        this.serviceRepository = serviceRepository;
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
        this.sanitizer = sanitizer;
    }

    public ServiceOffering create(ServiceRequest request, AuthUser actor) {
        User provider = resolveProvider(request, actor);

        ServiceOffering service = new ServiceOffering();
        apply(service, request);
        service.setProvider(provider);
        service.setActive(true);
        return serviceRepository.save(service);
    }

    @Transactional(readOnly = true)
    public Page<ServiceOffering> search(ServiceCategory category, BigDecimal minPrice, BigDecimal maxPrice,
                                        String titleContains, Long providerId, Pageable pageable) {
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            throw new IllegalArgumentException("minPrice não pode ser maior que maxPrice");
        }
        return serviceRepository.findAll(
                ServiceOfferingSpecs.filter(category, minPrice, maxPrice, titleContains, providerId), pageable);
    }

    /** Serviço ativo (inativo = excluído logicamente = 404). */
    @Transactional(readOnly = true)
    public ServiceOffering findById(Long id) {
        return serviceRepository.findById(id)
                .filter(ServiceOffering::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("Serviço não encontrado com o ID: " + id));
    }

    /** Prestadores (role PROVIDER), opcionalmente filtrados por categoria com serviço ativo e por disponibilidade. */
    @Transactional(readOnly = true)
    public List<User> findProviders(ServiceCategory category, Boolean available) {
        List<User> providers = category != null
                ? serviceRepository.findProvidersByCategory(category)
                : userRepository.findByRole(Role.PROVIDER);
        return providers.stream()
                .filter(p -> available == null || p.isActive() == available)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ServiceOffering> findByProvider(Long providerId) {
        return serviceRepository.findByProviderIdAndActiveTrue(providerId);
    }

    public ServiceOffering update(Long id, ServiceRequest request, AuthUser actor) {
        ServiceOffering service = findById(id);
        assertOwnerOrAdmin(service, actor);

        apply(service, request);
        if (actor.isAdmin() && request.getProviderId() != null) {
            service.setProvider(requireProvider(request.getProviderId()));
        }
        return serviceRepository.save(service);
    }

    public void delete(Long id, AuthUser actor) {
        ServiceOffering service = findById(id);
        assertOwnerOrAdmin(service, actor);

        if (orderRepository.existsByServiceIdAndStatusIn(id, OrderStatus.ACTIVE)) {
            throw new BusinessException("O serviço possui ordens ativas (PENDING, ACCEPTED ou IN_PROGRESS) "
                    + "e não pode ser excluído");
        }
        service.setActive(false);
        serviceRepository.save(service);
    }

    private void apply(ServiceOffering service, ServiceRequest request) {
        service.setTitle(sanitizer.clean(request.getTitle(), 150));
        service.setDescription(sanitizer.clean(request.getDescription()));
        service.setPrice(request.getPrice());
        service.setCategory(request.getCategory());
    }

    private User resolveProvider(ServiceRequest request, AuthUser actor) {
        if (actor.isAdmin()) {
            if (request.getProviderId() == null) {
                throw new IllegalArgumentException("providerId é obrigatório quando um ADMIN cria um serviço");
            }
            return requireProvider(request.getProviderId());
        }
        if (request.getProviderId() != null && !request.getProviderId().equals(actor.getId())) {
            throw new AccessDeniedException("Acesso negado: você não pode criar serviços em nome de outro prestador");
        }
        return requireProvider(actor.getId());
    }

    private User requireProvider(Long providerId) {
        User provider = userRepository.findById(providerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Prestador (usuário) não encontrado com o ID: " + providerId));
        if (provider.getRole() != Role.PROVIDER || !provider.isActive()) {
            throw new BusinessException("O usuário informado não é um prestador (PROVIDER) ativo");
        }
        return provider;
    }

    private void assertOwnerOrAdmin(ServiceOffering service, AuthUser actor) {
        if (!actor.isAdmin() && !service.getProvider().getId().equals(actor.getId())) {
            throw new AccessDeniedException("Acesso negado: apenas o prestador dono do serviço pode alterá-lo");
        }
    }
}
