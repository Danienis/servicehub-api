package com.servicehub.service;

import com.servicehub.dto.ReviewRequest;
import com.servicehub.exception.BusinessException;
import com.servicehub.model.OrderStatus;
import com.servicehub.model.Review;
import com.servicehub.model.Role;
import com.servicehub.model.ServiceOrder;
import com.servicehub.model.User;
import com.servicehub.repository.ReviewRepository;
import com.servicehub.repository.ServiceOfferingRepository;
import com.servicehub.security.AuthUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock ReviewRepository reviewRepository;
    @Mock ServiceOrderService orderService;
    @Mock ServiceOfferingRepository serviceRepository;
    @Spy InputSanitizer sanitizer = new InputSanitizer();

    ReviewService service;
    ServiceOrder order;

    @BeforeEach
    void setUp() {
        service = new ReviewService(reviewRepository, orderService, serviceRepository, sanitizer);
        User client = new User();
        client.setId(3L);
        order = new ServiceOrder();
        order.setId(1L);
        order.setClient(client);
        order.setStatus(OrderStatus.COMPLETED);
    }

    private static AuthUser actor(long id) {
        return new AuthUser(id, "u" + id + "@hub.com", "hash", Role.CLIENT, true);
    }

    @Test
    @DisplayName("cliente avalia ordem concluída")
    void deveAvaliar() {
        when(orderService.findById(1L)).thenReturn(order);
        when(reviewRepository.existsByServiceOrderId(1L)).thenReturn(false);
        when(reviewRepository.save(any(Review.class))).thenAnswer(i -> i.getArgument(0));

        Review r = service.create(1L, new ReviewRequest(5, "<i>Ótimo</i>"), actor(3));

        assertThat(r.getRating()).isEqualTo(5);
        assertThat(r.getComment()).doesNotContain("<i>");
    }

    @Test
    @DisplayName("só o cliente da ordem avalia")
    void soOClienteAvalia() {
        when(orderService.findById(1L)).thenReturn(order);

        assertThatThrownBy(() -> service.create(1L, new ReviewRequest(5, null), actor(99)))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("não avalia ordem não concluída")
    void naoAvaliaSemConcluir() {
        order.setStatus(OrderStatus.IN_PROGRESS);
        when(orderService.findById(1L)).thenReturn(order);

        assertThatThrownBy(() -> service.create(1L, new ReviewRequest(5, null), actor(3)))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("uma avaliação por ordem")
    void umaPorOrdem() {
        when(orderService.findById(1L)).thenReturn(order);
        when(reviewRepository.existsByServiceOrderId(1L)).thenReturn(true);

        assertThatThrownBy(() -> service.create(1L, new ReviewRequest(4, null), actor(3)))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("média por serviço arredondada em 1 casa")
    void mediaArredondada() {
        when(serviceRepository.existsById(10L)).thenReturn(true);
        when(reviewRepository.averageRatingByService(10L)).thenReturn(4.666);
        when(reviewRepository.countByServiceOrderServiceId(10L)).thenReturn(3L);

        var summary = service.ratingOfService(10L);

        assertThat(summary.average()).isEqualByComparingTo("4.7");
        assertThat(summary.totalReviews()).isEqualTo(3L);
    }
}
