package com.servicehub.service;

import com.servicehub.dto.RatingSummaryResponse;
import com.servicehub.dto.ReviewRequest;
import com.servicehub.exception.BusinessException;
import com.servicehub.exception.ResourceNotFoundException;
import com.servicehub.model.OrderStatus;
import com.servicehub.model.Review;
import com.servicehub.model.ServiceOrder;
import com.servicehub.repository.ReviewRepository;
import com.servicehub.repository.ServiceOfferingRepository;
import com.servicehub.security.AuthUser;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Avaliações: no máximo uma por ordem, somente do cliente da ordem e somente após COMPLETED.
 */
@Service
@Transactional
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ServiceOrderService orderService;
    private final ServiceOfferingRepository serviceRepository;
    private final InputSanitizer sanitizer;

    public ReviewService(ReviewRepository reviewRepository,
                         ServiceOrderService orderService,
                         ServiceOfferingRepository serviceRepository,
                         InputSanitizer sanitizer) {
        this.reviewRepository = reviewRepository;
        this.orderService = orderService;
        this.serviceRepository = serviceRepository;
        this.sanitizer = sanitizer;
    }

    public Review create(Long orderId, ReviewRequest request, AuthUser actor) {
        ServiceOrder order = orderService.findById(orderId);

        if (!order.getClient().getId().equals(actor.getId())) {
            throw new AccessDeniedException("Acesso negado: apenas o cliente da ordem pode avaliá-la");
        }
        if (order.getStatus() != OrderStatus.COMPLETED) {
            throw new BusinessException("Só é possível avaliar ordens com status COMPLETED");
        }
        if (reviewRepository.existsByServiceOrderId(orderId)) {
            throw new BusinessException("Esta ordem já foi avaliada");
        }

        Review review = new Review();
        review.setServiceOrder(order);
        review.setReviewer(order.getClient());
        review.setRating(request.getRating());
        review.setComment(sanitizer.clean(request.getComment()));
        return reviewRepository.save(review);
    }

    @Transactional(readOnly = true)
    public Review findByOrder(Long orderId, AuthUser actor) {
        orderService.findById(orderId, actor); // 404 se não existe, 403 se não participa
        return reviewRepository.findByServiceOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Ordem " + orderId + " ainda não foi avaliada"));
    }

    @Transactional(readOnly = true)
    public RatingSummaryResponse ratingOfService(Long serviceId) {
        if (!serviceRepository.existsById(serviceId)) {
            throw new ResourceNotFoundException("Serviço não encontrado com o ID: " + serviceId);
        }
        Double avg = reviewRepository.averageRatingByService(serviceId);
        long total = reviewRepository.countByServiceOrderServiceId(serviceId);
        BigDecimal average = avg == null ? null : BigDecimal.valueOf(avg).setScale(1, RoundingMode.HALF_UP);
        return new RatingSummaryResponse(serviceId, average, total);
    }

    /** Média do prestador (todas as avaliações dos seus serviços); null se não houver. */
    @Transactional(readOnly = true)
    public Double ratingOfProvider(Long providerId) {
        Double avg = reviewRepository.averageRatingByProvider(providerId);
        return avg == null ? null : BigDecimal.valueOf(avg).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }
}
