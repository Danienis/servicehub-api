package com.servicehub.controller;

import com.servicehub.dto.OrderRequest;
import com.servicehub.dto.OrderResponse;
import com.servicehub.dto.OrderStatusRequest;
import com.servicehub.dto.PageResponse;
import com.servicehub.dto.ReviewRequest;
import com.servicehub.dto.ReviewResponse;
import com.servicehub.exception.ErrorResponse;
import com.servicehub.model.Review;
import com.servicehub.model.ServiceOrder;
import com.servicehub.security.AuthUser;
import com.servicehub.service.ReviewService;
import com.servicehub.service.ServiceOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

/** Ordens de serviço (contratações) e suas avaliações. */
@RestController
@RequestMapping("/api/v1/orders")
@Tag(name = "Ordens de serviço", description = "Contratação de serviços, ciclo de vida (status) e avaliações")
public class ServiceOrderController {

    private final ServiceOrderService orderService;
    private final ReviewService reviewService;

    public ServiceOrderController(ServiceOrderService orderService, ReviewService reviewService) {
        this.orderService = orderService;
        this.reviewService = reviewService;
    }

    @PostMapping
    @PreAuthorize("hasRole('CLIENT')")
    @Operation(summary = "Contratar um serviço",
            description = "Cria uma ordem PENDING para o cliente autenticado. O preço do serviço é copiado "
                    + "para total_price (snapshot). O prestador não pode contratar o próprio serviço.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Ordem criada",
                    content = @Content(schema = @Schema(implementation = OrderResponse.class))),
            @ApiResponse(responseCode = "400", description = "Dados inválidos",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Apenas CLIENT pode contratar",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Serviço não encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Serviço indisponível ou contratação inválida",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<OrderResponse> create(@Valid @RequestBody OrderRequest request,
                                                @Parameter(hidden = true) @AuthenticationPrincipal AuthUser actor) {
        ServiceOrder created = orderService.create(request, actor);
        return ResponseEntity.created(URI.create("/api/v1/orders/" + created.getId()))
                .body(OrderResponse.fromEntity(created));
    }

    @GetMapping
    @Operation(summary = "Listar minhas ordens (paginado)",
            description = "ADMIN: todas. PROVIDER: ordens dos seus serviços. CLIENT: as suas contratações.")
    @ApiResponse(responseCode = "200", description = "Página de ordens")
    public ResponseEntity<PageResponse<OrderResponse>> findAll(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthUser actor,
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(PageResponse.of(orderService.findAll(actor, pageable), OrderResponse::fromEntity));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar ordem por ID",
            description = "Permitido ao cliente da ordem, ao prestador do serviço ou a um ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ordem encontrada",
                    content = @Content(schema = @Schema(implementation = OrderResponse.class))),
            @ApiResponse(responseCode = "403", description = "Usuário não participa da ordem",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Ordem não encontrada",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<OrderResponse> findById(
            @Parameter(description = "ID da ordem", example = "1", required = true) @PathVariable Long id,
            @Parameter(hidden = true) @AuthenticationPrincipal AuthUser actor) {
        return ResponseEntity.ok(OrderResponse.fromEntity(orderService.findById(id, actor)));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Alterar status da ordem",
            description = "Máquina de estados: PENDING→ACCEPTED→IN_PROGRESS→COMPLETED; CANCELLED a partir de qualquer "
                    + "estado ativo. ACCEPTED/IN_PROGRESS/COMPLETED: prestador (ou ADMIN). CANCELLED: cliente, prestador ou ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Status alterado",
                    content = @Content(schema = @Schema(implementation = OrderResponse.class))),
            @ApiResponse(responseCode = "403", description = "Perfil sem permissão para esta transição",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Ordem não encontrada",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Transição de status inválida",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<OrderResponse> updateStatus(
            @Parameter(description = "ID da ordem", example = "1", required = true) @PathVariable Long id,
            @Valid @RequestBody OrderStatusRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal AuthUser actor) {
        return ResponseEntity.ok(OrderResponse.fromEntity(orderService.updateStatus(id, request.getStatus(), actor)));
    }

    @PostMapping("/{id}/review")
    @PreAuthorize("hasRole('CLIENT')")
    @Operation(summary = "Avaliar ordem concluída",
            description = "O cliente da ordem avalia (nota 1 a 5) uma ordem COMPLETED. Uma avaliação por ordem.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Avaliação criada",
                    content = @Content(schema = @Schema(implementation = ReviewResponse.class))),
            @ApiResponse(responseCode = "400", description = "Nota inválida",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Apenas o cliente da ordem pode avaliar",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Ordem não encontrada",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Ordem não concluída ou já avaliada",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<ReviewResponse> review(
            @Parameter(description = "ID da ordem", example = "1", required = true) @PathVariable Long id,
            @Valid @RequestBody ReviewRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal AuthUser actor) {
        Review created = reviewService.create(id, request, actor);
        return ResponseEntity.created(URI.create("/api/v1/orders/" + id + "/review"))
                .body(ReviewResponse.fromEntity(created));
    }

    @GetMapping("/{id}/review")
    @Operation(summary = "Ver avaliação da ordem",
            description = "Permitido a quem participa da ordem (cliente, prestador) ou ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Avaliação encontrada",
                    content = @Content(schema = @Schema(implementation = ReviewResponse.class))),
            @ApiResponse(responseCode = "403", description = "Usuário não participa da ordem",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Ordem ou avaliação não encontrada",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<ReviewResponse> findReview(
            @Parameter(description = "ID da ordem", example = "1", required = true) @PathVariable Long id,
            @Parameter(hidden = true) @AuthenticationPrincipal AuthUser actor) {
        return ResponseEntity.ok(ReviewResponse.fromEntity(reviewService.findByOrder(id, actor)));
    }
}
