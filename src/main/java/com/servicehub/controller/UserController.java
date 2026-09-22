package com.servicehub.controller;

import com.servicehub.dto.UserRequest;
import com.servicehub.dto.UserResponse;
import com.servicehub.exception.ErrorResponse;
import com.servicehub.model.User;
import com.servicehub.service.UserService;
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

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Usuários", description = "Gerenciamento de usuários (prestadores e clientes) da plataforma ServiceHub")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    @Operation(
            summary = "Criar novo usuário",
            description = "Cadastra um novo usuário na plataforma ServiceHub. O e-mail deve ser único."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Usuário criado com sucesso",
                    content = @Content(schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "400", description = "Dados de entrada inválidos",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "E-mail já cadastrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<UserResponse> create(@Valid @RequestBody UserRequest request) {
        User created = userService.create(request);
        UserResponse response = UserResponse.fromEntity(created);
        return ResponseEntity.created(URI.create("/api/v1/users/" + created.getId())).body(response);
    }

    @GetMapping
    @Operation(
            summary = "Listar todos os usuários",
            description = "Retorna a lista completa de usuários cadastrados na plataforma ServiceHub."
    )
    @ApiResponse(responseCode = "200", description = "Lista de usuários retornada com sucesso")
    public ResponseEntity<List<UserResponse>> findAll() {
        List<UserResponse> users = userService.findAll().stream()
                .map(UserResponse::fromEntity)
                .toList();
        return ResponseEntity.ok(users);
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Buscar usuário por ID",
            description = "Retorna os dados completos de um usuário a partir do seu identificador único."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuário encontrado",
                    content = @Content(schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<UserResponse> findById(
            @Parameter(description = "ID único do usuário", example = "1", required = true)
            @PathVariable Long id) {
        return ResponseEntity.ok(UserResponse.fromEntity(userService.findById(id)));
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Atualizar usuário",
            description = "Substitui integralmente os dados de um usuário existente."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuário atualizado com sucesso",
                    content = @Content(schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "400", description = "Dados de entrada inválidos",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "E-mail já cadastrado por outro usuário",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<UserResponse> update(
            @Parameter(description = "ID único do usuário", example = "1", required = true)
            @PathVariable Long id,
            @Valid @RequestBody UserRequest request) {
        User updated = userService.update(id, request);
        return ResponseEntity.ok(UserResponse.fromEntity(updated));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
            summary = "Excluir usuário",
            description = "Remove permanentemente um usuário da plataforma ServiceHub."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Usuário excluído com sucesso"),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public void delete(
            @Parameter(description = "ID único do usuário", example = "1", required = true)
            @PathVariable Long id) {
        userService.delete(id);
    }
}
