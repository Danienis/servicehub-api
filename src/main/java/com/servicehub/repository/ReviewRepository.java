package com.servicehub.repository;

import com.servicehub.model.Review;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    @EntityGraph(attributePaths = {"serviceOrder", "reviewer"})
    Optional<Review> findByServiceOrderId(Long orderId);

    boolean existsByServiceOrderId(Long orderId);

    long countByServiceOrderServiceId(Long serviceId);

    /** Média por serviço (equivalente à consulta de apoio do schema). Null se não houver avaliações. */
    @Query("select avg(r.rating) from Review r where r.serviceOrder.service.id = :serviceId")
    Double averageRatingByService(@Param("serviceId") Long serviceId);

    @Query("select avg(r.rating) from Review r where r.serviceOrder.service.provider.id = :providerId")
    Double averageRatingByProvider(@Param("providerId") Long providerId);
}
