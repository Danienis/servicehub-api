package com.servicehub.repository;

import com.servicehub.model.ServiceCategory;
import com.servicehub.model.ServiceOffering;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Filtros dinâmicos da listagem de serviços. Usa Criteria API: todos os valores vão como
 * parâmetros (nada é concatenado em SQL), o que evita SQL injection.
 */
public final class ServiceOfferingSpecs {

    private ServiceOfferingSpecs() {
    }

    public static Specification<ServiceOffering> filter(ServiceCategory category,
                                                        BigDecimal minPrice,
                                                        BigDecimal maxPrice,
                                                        String titleContains,
                                                        Long providerId) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isTrue(root.get("active")));

            if (category != null) {
                predicates.add(cb.equal(root.get("category"), category));
            }
            if (minPrice != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.<BigDecimal>get("price"), minPrice));
            }
            if (maxPrice != null) {
                predicates.add(cb.lessThanOrEqualTo(root.<BigDecimal>get("price"), maxPrice));
            }
            if (providerId != null) {
                predicates.add(cb.equal(root.get("provider").get("id"), providerId));
            }
            if (titleContains != null && !titleContains.isBlank()) {
                String escaped = titleContains.trim().toLowerCase(Locale.ROOT)
                        .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
                predicates.add(cb.like(cb.lower(root.get("title")), "%" + escaped + "%", '\\'));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
