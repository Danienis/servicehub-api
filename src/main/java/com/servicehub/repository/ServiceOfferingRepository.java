package com.servicehub.repository;

import com.servicehub.model.ServiceCategory;
import com.servicehub.model.ServiceOffering;
import com.servicehub.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * O provider é LAZY; o @EntityGraph o carrega junto (JOIN FETCH) para os DTOs poderem
 * ler providerName fora da transação (spring.jpa.open-in-view=false).
 */
public interface ServiceOfferingRepository
        extends JpaRepository<ServiceOffering, Long>, JpaSpecificationExecutor<ServiceOffering> {

    @Override
    @EntityGraph(attributePaths = "provider")
    Page<ServiceOffering> findAll(Specification<ServiceOffering> spec, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = "provider")
    Optional<ServiceOffering> findById(Long id);

    List<ServiceOffering> findByCategory(ServiceCategory category);

    List<ServiceOffering> findByProviderId(Long providerId);

    @EntityGraph(attributePaths = "provider")
    List<ServiceOffering> findByProviderIdAndActiveTrue(Long providerId);

    List<ServiceOffering> findByActiveTrue();

    /** Prestadores que têm ao menos um serviço ativo na categoria (usado pelo GraphQL). */
    @Query("select distinct s.provider from ServiceOffering s where s.active = true and s.category = :category")
    List<User> findProvidersByCategory(@Param("category") ServiceCategory category);
}
