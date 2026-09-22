package com.servicehub.controller;

import com.servicehub.dto.ServiceRequest;
import com.servicehub.dto.ServiceResponse;
import com.servicehub.exception.ErrorResponse;
import com.servicehub.model.ServiceOffering;
import com.servicehub.service.ServiceOfferingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

/**
 * Controller REST responsável pelo recurso "services" — o CRUD completo
 * exigido pelo Checkpoint do Projeto ServiceHub API.
 */
@RestController
@RequestMapping("/api/v1/services")
@Tag(name = "Serviços", description = "Operações de gerenciamento de serviços oferecidos na plataforma ServiceHub")
public class ServiceOfferingController {

    private final ServiceOfferingService serviceOfferingService;

    public ServiceOfferingController(ServiceOfferingService serviceOfferingService) {
        this.serviceOfferingService = serviceOfferingService;
    }

    @PostMapping
    @Operation(
            summary = "Criar novo serviço",
            description = "Cadastra um novo serviço oferecido por um prestador (usuário) já existente na plataforma."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Serviço criado com sucesso",
                    content = @Content(schema = @Schema(implementation = ServiceResponse.class))),
            @ApiResponse(responseCode = "400", description = "Dados de entrada inválidos",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Prestador (usuário) informado não encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<ServiceResponse> create(@Valid @RequestBody ServiceRequest request) {
        ServiceOffering created = serviceOfferingService.create(request);
        ServiceResponse response = ServiceResponse.fromEntity(created);
        return ResponseEntity.created(URI.create("/api/v1/services/" + created.getId())).body(response);
    }

    @GetMapping
    @Operation(
            summary = "Listar todos os serviços",
            description = "Retorna a lista completa de serviços cadastrados na plataforma ServiceHub."
    )
    @ApiResponse(responseCode = "200", description = "Lista de serviços retornada com sucesso")
    public ResponseEntity<List<ServiceResponse>> findAll() {
        List<ServiceResponse> services = serviceOfferingService.findAll().stream()
                .map(ServiceResponse::fromEntity)
                .toList();
        return ResponseEntity.ok(services);
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Buscar serviço por ID",
            description = "Retorna os dados completos de um serviço cadastrado a partir do seu identificador único."
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

    @PutMapping("/{id}")
    @Operation(
            summary = "Atualizar serviço",
            description = "Substitui integralmente os dados de um serviço existente."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Serviço atualizado com sucesso",
                    content = @Content(schema = @Schema(implementation = ServiceResponse.class))),
            @ApiResponse(responseCode = "400", description = "Dados de entrada inválidos",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Serviço ou prestador não encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<ServiceResponse> update(
            @Parameter(description = "ID único do serviço", example = "1", required = true)
            @PathVariable Long id,
            @Valid @RequestBody ServiceRequest request) {
        ServiceOffering updated = serviceOfferingService.update(id, request);
        return ResponseEntity.ok(ServiceResponse.fromEntity(updated));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
            summary = "Excluir serviço",
            description = "Remove permanentemente um serviço da plataforma ServiceHub."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Serviço excluído com sucesso"),
            @ApiResponse(responseCode = "404", description = "Serviço não encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public void delete(
            @Parameter(description = "ID único do serviço", example = "1", required = true)
            @PathVariable Long id) {
        serviceOfferingService.delete(id);
    }
}
