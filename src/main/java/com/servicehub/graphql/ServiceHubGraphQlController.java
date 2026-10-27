package com.servicehub.graphql;

import com.servicehub.dto.OrderRequest;
import com.servicehub.model.ServiceCategory;
import com.servicehub.model.ServiceOffering;
import com.servicehub.model.ServiceOrder;
import com.servicehub.model.User;
import com.servicehub.security.AuthUser;
import com.servicehub.service.ReviewService;
import com.servicehub.service.ServiceOfferingService;
import com.servicehub.service.ServiceOrderService;
import com.servicehub.service.UserService;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Resolvers GraphQL. Reutilizam os mesmos services do REST (nenhuma regra duplicada) e a mesma
 * autenticação JWT (o endpoint /graphql passa pelo JwtAuthFilter).
 */
@Controller
public class ServiceHubGraphQlController {

    private final ServiceOfferingService serviceOfferingService;
    private final ServiceOrderService orderService;
    private final UserService userService;
    private final ReviewService reviewService;

    public ServiceHubGraphQlController(ServiceOfferingService serviceOfferingService,
                                       ServiceOrderService orderService,
                                       UserService userService,
                                       ReviewService reviewService) {
        this.serviceOfferingService = serviceOfferingService;
        this.orderService = orderService;
        this.userService = userService;
        this.reviewService = reviewService;
    }

    // ---------------------------------------------------------------- queries

    @QueryMapping
    public User provider(@Argument Long id) {
        return userService.findById(id);
    }

    @QueryMapping
    public List<User> providers(@Argument String category, @Argument Boolean available) {
        ServiceCategory parsed = category == null || category.isBlank()
                ? null
                : ServiceCategory.valueOf(category.trim().toUpperCase());
        return serviceOfferingService.findProviders(parsed, available);
    }

    @QueryMapping
    public ServiceOffering service(@Argument Long id) {
        return serviceOfferingService.findById(id);
    }

    @QueryMapping
    public List<ServiceOffering> servicesByProvider(@Argument Long providerId) {
        return serviceOfferingService.findByProvider(providerId);
    }

    // -------------------------------------------------------------- mutation

    @MutationMapping
    @PreAuthorize("hasRole('CLIENT')")
    public ServiceOrder bookService(@Argument Long serviceId,
                                    @Argument String scheduledAt,
                                    @Argument String notes,
                                    @AuthenticationPrincipal AuthUser actor) {
        OrderRequest request = new OrderRequest(
                serviceId,
                scheduledAt == null || scheduledAt.isBlank() ? null : LocalDateTime.parse(scheduledAt),
                notes);
        return orderService.create(request, actor);
    }

    // ------------------------------------------------- campos calculados/mapeados

    @SchemaMapping(typeName = "Provider", field = "name")
    public String providerName(User provider) {
        return provider.getFullName();
    }

    @SchemaMapping(typeName = "Provider", field = "available")
    public boolean providerAvailable(User provider) {
        return provider.isActive();
    }

    @SchemaMapping(typeName = "Provider", field = "rating")
    public Double providerRating(User provider) {
        return reviewService.ratingOfProvider(provider.getId());
    }

    @SchemaMapping(typeName = "Provider", field = "services")
    public List<ServiceOffering> providerServices(User provider) {
        return serviceOfferingService.findByProvider(provider.getId());
    }
}
