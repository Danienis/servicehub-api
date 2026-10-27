package com.servicehub.controller;

import com.servicehub.dto.PageResponse;
import com.servicehub.dto.RoleUpdateRequest;
import com.servicehub.dto.UserRequest;
import com.servicehub.dto.UserResponse;
import com.servicehub.dto.UserUpdateRequest;
import com.servicehub.exception.ErrorResponse;
import com.servicehub.model.User;
import com.servicehub.security.AuthUser;
import com.servicehub.service.UserService;
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

import java.net.URI;

/**
 * Usuários. Perfis: ADMIN gerencia todos; os demais só enxergam/alteram a si mesmos
 * (checagem de dono feita no service — proteção contra BOLA).
 */
@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Usuários", description = "Gerenciamento de usuários da plataforma ServiceHub")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Criar novo usuário (ADMIN)",
            description = "Cadastra um usuário definindo o papel. Somente ADMIN. O e-mail deve ser único."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Usuário criado com sucesso",
                    content = @Content(schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "400", description = "Dados de entrada inválidos",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Não autenticado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Sem permissão (exige ADMIN)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "E-mail já cadastrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<UserResponse> create(@Valid @RequestBody UserRequest request) {
        User created = userService.create(request);
        return ResponseEntity.created(URI.create("/api/v1/users/" + created.getId()))
                .body(UserResponse.fromEntity(created));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Listar usuários (ADMIN)",
            description = "Lista paginada de usuários. Parâmetros: page (0..), size (máx. 50) e sort (ex.: fullName,asc)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página de usuários"),
            @ApiResponse(responseCode = "401", description = "Não autenticado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Sem permissão (exige ADMIN)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<PageResponse<UserResponse>> findAll(
            @ParameterObject @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(PageResponse.of(userService.findAll(pageable), UserResponse::fromEntity));
    }

    @GetMapping("/me")
    @Operation(summary = "Meu perfil", description = "Retorna os dados do usuário autenticado.")
    @ApiResponse(responseCode = "200", description = "Dados do usuário autenticado")
    public ResponseEntity<UserResponse> me(@Parameter(hidden = true) @AuthenticationPrincipal AuthUser actor) {
        return ResponseEntity.ok(UserResponse.fromEntity(userService.findById(actor.getId())));
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Buscar usuário por ID",
            description = "Retorna os dados de um usuário. Permitido ao próprio usuário ou a um ADMIN."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuário encontrado",
                    content = @Content(schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "403", description = "Acesso a dados de outro usuário",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<UserResponse> findById(
            @Parameter(description = "ID único do usuário", example = "1", required = true)
            @PathVariable Long id,
            @Parameter(hidden = true) @AuthenticationPrincipal AuthUser actor) {
        return ResponseEntity.ok(UserResponse.fromEntity(userService.findById(id, actor)));
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Atualizar usuário",
            description = "Atualiza nome, e-mail, telefone, bio e avatar. Permitido ao próprio usuário ou a um ADMIN. "
                    + "Papel e senha não são alterados por aqui."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuário atualizado com sucesso",
                    content = @Content(schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "400", description = "Dados de entrada inválidos",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Acesso a dados de outro usuário",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "E-mail já cadastrado por outro usuário",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<UserResponse> update(
            @Parameter(description = "ID único do usuário", example = "1", required = true)
            @PathVariable Long id,
            @Valid @RequestBody UserUpdateRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal AuthUser actor) {
        return ResponseEntity.ok(UserResponse.fromEntity(userService.update(id, request, actor)));
    }

    @PutMapping("/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Alterar papel (ADMIN)", description = "Define o papel (ADMIN, PROVIDER ou CLIENT) de um usuário.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Papel alterado",
                    content = @Content(schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "403", description = "Sem permissão (exige ADMIN)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<UserResponse> changeRole(
            @Parameter(description = "ID único do usuário", example = "1", required = true)
            @PathVariable Long id,
            @Valid @RequestBody RoleUpdateRequest request) {
        return ResponseEntity.ok(UserResponse.fromEntity(userService.changeRole(id, request.getRole())));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
            summary = "Excluir usuário (exclusão lógica)",
            description = "Marca o usuário como inativo: ele deixa de logar e o JWT deixa de valer. "
                    + "Permitido ao próprio usuário ou a um ADMIN. Bloqueado se houver ordens ativas."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Usuário excluído com sucesso"),
            @ApiResponse(responseCode = "403", description = "Acesso a dados de outro usuário",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Usuário possui ordens ativas",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public void delete(
            @Parameter(description = "ID único do usuário", example = "1", required = true)
            @PathVariable Long id,
            @Parameter(hidden = true) @AuthenticationPrincipal AuthUser actor) {
        userService.delete(id, actor);
    }
}
