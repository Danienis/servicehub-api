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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ordens de serviço (tabela service_requests): contratação, listagem por perfil e máquina de estados.
 * Quem pode o quê:
 *  - criar: CLIENT (o cliente é o usuário autenticado; não pode contratar o próprio serviço);
 *  - ACCEPTED / IN_PROGRESS / COMPLETED: o prestador do serviço (ou ADMIN);
 *  - CANCELLED: cliente da ordem, prestador do serviço ou ADMIN.
 */
@Service
@Transactional
public class ServiceOrderService {

    private final ServiceOrderRepository orderRepository;
    private final ServiceOfferingRepository serviceRepository;
    private final UserRepository userRepository;
    private final InputSanitizer sanitizer;

    public ServiceOrderService(ServiceOrderRepository orderRepository,
                               ServiceOfferingRepository serviceRepository,
                               UserRepository userRepository,
                               InputSanitizer sanitizer) {
        this.orderRepository = orderRepository;
        this.serviceRepository = serviceRepository;
        this.userRepository = userRepository;
        this.sanitizer = sanitizer;
    }

    public ServiceOrder create(OrderRequest request, AuthUser actor) {
        ServiceOffering service = serviceRepository.findById(request.getServiceId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Serviço não encontrado com o ID: " + request.getServiceId()));
        if (!service.isActive() || !service.getProvider().isActive()) {
            throw new BusinessException("O serviço não está disponível para contratação");
        }
        if (service.getProvider().getId().equals(actor.getId())) {
            throw new BusinessException("O prestador não pode contratar o próprio serviço");
        }
        User client = userRepository.findById(actor.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado com o ID: " + actor.getId()));

        ServiceOrder order = new ServiceOrder();
        order.setService(service);
        order.setClient(client);
        order.setStatus(OrderStatus.PENDING);
        order.setScheduledAt(request.getScheduledAt());
        order.setNotes(sanitizer.clean(request.getNotes()));
        order.setTotalPrice(service.getPrice()); // snapshot do preço
        return orderRepository.save(order);
    }

    /** ADMIN vê todas; PROVIDER vê as dos seus serviços; CLIENT vê as suas. */
    @Transactional(readOnly = true)
    public Page<ServiceOrder> findAll(AuthUser actor, Pageable pageable) {
        if (actor.isAdmin()) {
            return orderRepository.findAll(pageable);
        }
        if (actor.getRole() == Role.PROVIDER) {
            return orderRepository.findByServiceProviderId(actor.getId(), pageable);
        }
        return orderRepository.findByClientId(actor.getId(), pageable);
    }

    @Transactional(readOnly = true)
    public ServiceOrder findById(Long id, AuthUser actor) {
        ServiceOrder order = findById(id);
        if (!isInvolved(order, actor)) {
            throw new AccessDeniedException("Acesso negado: você não participa desta ordem de serviço");
        }
        return order;
    }

    /** Busca interna (sem checagem de dono). */
    @Transactional(readOnly = true)
    public ServiceOrder findById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ordem de serviço não encontrada com o ID: " + id));
    }

    public ServiceOrder updateStatus(Long id, OrderStatus newStatus, AuthUser actor) {
        ServiceOrder order = findById(id, actor);

        boolean isProvider = order.getService().getProvider().getId().equals(actor.getId());
        boolean isClient = order.getClient().getId().equals(actor.getId());

        if (newStatus == OrderStatus.CANCELLED) {
            if (!(actor.isAdmin() || isProvider || isClient)) {
                throw new AccessDeniedException("Acesso negado para cancelar esta ordem");
            }
        } else if (!(actor.isAdmin() || isProvider)) {
            throw new AccessDeniedException("Acesso negado: apenas o prestador do serviço altera este status");
        }

        if (!order.getStatus().canTransitionTo(newStatus)) {
            throw new BusinessException("Transição de status inválida: " + order.getStatus() + " -> " + newStatus);
        }
        order.setStatus(newStatus);
        return orderRepository.save(order);
    }

    static boolean isInvolved(ServiceOrder order, AuthUser actor) {
        return actor.isAdmin()
                || order.getClient().getId().equals(actor.getId())
                || order.getService().getProvider().getId().equals(actor.getId());
    }
}
