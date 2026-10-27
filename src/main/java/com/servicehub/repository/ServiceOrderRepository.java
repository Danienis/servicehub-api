package com.servicehub.repository;

import com.servicehub.model.OrderStatus;
import com.servicehub.model.ServiceOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.Optional;

public interface ServiceOrderRepository extends JpaRepository<ServiceOrder, Long> {

    @Override
    @EntityGraph(attributePaths = {"service", "service.provider", "client"})
    Optional<ServiceOrder> findById(Long id);

    @Override
    @EntityGraph(attributePaths = {"service", "service.provider", "client"})
    Page<ServiceOrder> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"service", "service.provider", "client"})
    Page<ServiceOrder> findByClientId(Long clientId, Pageable pageable);

    @EntityGraph(attributePaths = {"service", "service.provider", "client"})
    Page<ServiceOrder> findByServiceProviderId(Long providerId, Pageable pageable);

    boolean existsByServiceIdAndStatusIn(Long serviceId, Collection<OrderStatus> statuses);

    boolean existsByServiceProviderIdAndStatusIn(Long providerId, Collection<OrderStatus> statuses);

    boolean existsByClientIdAndStatusIn(Long clientId, Collection<OrderStatus> statuses);
}
