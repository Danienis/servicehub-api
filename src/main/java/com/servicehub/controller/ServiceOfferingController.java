package com.servicehub.controller;

import com.servicehub.dto.PageResponse;
import com.servicehub.dto.RatingSummaryResponse;
import com.servicehub.dto.ServiceRequest;
import com.servicehub.dto.ServiceResponse;
import com.servicehub.exception.ErrorResponse;
import com.servicehub.model.ServiceCategory;
import com.servicehub.model.ServiceOffering;
import com.servicehub.security.AuthUser;
import com.servicehub.service.ReviewService;
import com.servicehub.service.ServiceOfferingService;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.net.URI;

/**
 * Serviços oferecidos. Leitura: qualquer usuário autenticado. Escrita: PROVIDER (dono) ou ADMIN.
 */
@RestController
@RequestMapping("/api/v1/services")
@Tag(name = "Serviços", description = "Operações de gerenciamento de serviços oferecidos na plataforma ServiceHub")
public class ServiceOfferingController {

    private final ServiceOfferingService serviceOfferingService;
    private final ReviewService reviewService;

    public ServiceOfferingController(ServiceOfferingService serviceOfferingService, ReviewService reviewService) {
        this.serviceOfferingService = serviceOfferingService;
        this.reviewService = reviewService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('PROVIDER','ADMIN')")
    @Operation(
            summary = "Criar novo serviço",
            description = "Cadastra um serviço. PROVIDER cria para si mesmo; ADMIN informa o providerId."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Serviço criado com sucesso",
                    content = @Content(schema = @Schema(implementation = ServiceResponse.class))),
            @ApiResponse(responseCode = "400", description = "Dados de entrada inválidos",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Não autenticado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Perfil sem permissão (exige PROVIDER ou ADMIN)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Prestador informado não encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Usuário informado não é um prestador ativo",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<ServiceResponse> create(@Valid @RequestBody ServiceRequest request,
                                                  @Parameter(hidden = true) @AuthenticationPrincipal AuthUser actor) {
        ServiceOffering created = serviceOfferingService.create(request, actor);
        return ResponseEntity.created(URI.create("/api/v1/services/" + created.getId()))
                .body(ServiceResponse.fromEntity(created));
    }

    @GetMapping
    @Operation(
            summary = "Listar serviços (paginado, com filtros)",
            description = "Lista os serviços ativos. Filtros opcionais: category, minPrice, maxPrice, q (parte do título) "
                    + "e providerId. Paginação: page (0..), size (máx. 50) e sort (ex.: price,asc)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página de serviços"),
            @ApiResponse(responseCode = "400", description = "Filtro inválido (ex.: minPrice > maxPrice)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Não autenticado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<PageResponse<ServiceResponse>> search(
            @Parameter(description = "Categoria do serviço") @RequestParam(required = false) ServiceCategory category,
            @Parameter(description = "Preço mínimo", example = "50") @RequestParam(required = false) BigDecimal minPrice,
            @Parameter(description = "Preço máximo", example = "500") @RequestParam(required = false) BigDecimal maxPrice,
            @Parameter(description = "Parte do título", example = "limpeza") @RequestParam(required = false) String q,
            @Parameter(description = "ID do prestador") @RequestParam(required = false) Long providerId,
            @ParameterObject @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(PageResponse.of(
                serviceOfferingService.search(category, minPrice, maxPrice, q, providerId, pageable),
                ServiceResponse::fromEntity));
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Buscar serviço por ID",
            description = "Retorna os dados completos de um serviço ativo a partir do seu identificador único."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Serviço encontrado",
                    content = @Content(schema = @Schema(implementation = ServiceResponse.class))),
            @ApiResponse(responseCode = "404", description = "Serviço não encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<ServiceResponse> findById(
            @Parameter(description = "ID único do serviço", example = "1", required = true)
            @PathVariable Long id) {
        return ResponseEntity.ok(ServiceResponse.fromEntity(serviceOfferingService.findById(id)));
    }

    @GetMapping("/{id}/rating")
    @Operation(summary = "Média de avaliações do serviço",
            description = "Média (1 casa decimal) e quantidade de avaliações das ordens concluídas do serviço.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Resumo de avaliações",
                    content = @Content(schema = @Schema(implementation = RatingSummaryResponse.class))),
            @ApiResponse(responseCode = "404", description = "Serviço não encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<RatingSummaryResponse> rating(
            @Parameter(description = "ID único do serviço", example = "1", required = true)
            @PathVariable Long id) {
        return ResponseEntity.ok(reviewService.ratingOfService(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('PROVIDER','ADMIN')")
    @Operation(
            summary = "Atualizar serviço",
            description = "Substitui os dados de um serviço. Somente o prestador dono ou um ADMIN."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Serviço atualizado com sucesso",
                    content = @Content(schema = @Schema(implementation = ServiceResponse.class))),
            @ApiResponse(responseCode = "400", description = "Dados de entrada inválidos",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Não é o dono do serviço nem ADMIN",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Serviço ou prestador não encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<ServiceResponse> update(
            @Parameter(description = "ID único do serviço", example = "1", required = true)
            @PathVariable Long id,
            @Valid @RequestBody ServiceRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal AuthUser actor) {
        return ResponseEntity.ok(ServiceResponse.fromEntity(serviceOfferingService.update(id, request, actor)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('PROVIDER','ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
            summary = "Excluir serviço (exclusão lógica)",
            description = "Desativa o serviço. Somente o prestador dono ou um ADMIN. Bloqueado se houver ordens ativas."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Serviço excluído com sucesso"),
            @ApiResponse(responseCode = "403", description = "Não é o dono do serviço nem ADMIN",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Serviço não encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Serviço possui ordens ativas",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public void delete(
            @Parameter(description = "ID único do serviço", example = "1", required = true)
            @PathVariable Long id,
            @Parameter(hidden = true) @AuthenticationPrincipal AuthUser actor) {
        serviceOfferingService.delete(id, actor);
    }
}
