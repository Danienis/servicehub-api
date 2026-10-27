package com.servicehub.controller;

import com.servicehub.dto.AuthResponse;
import com.servicehub.dto.LoginRequest;
import com.servicehub.dto.RefreshRequest;
import com.servicehub.dto.RegisterRequest;
import com.servicehub.dto.UserResponse;
import com.servicehub.exception.ErrorResponse;
import com.servicehub.model.User;
import com.servicehub.service.AuthService;
import com.servicehub.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

/** Endpoints públicos de cadastro, login e renovação de token (/api/auth/**). */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticação", description = "Cadastro, login e renovação de tokens JWT (rotas públicas)")
@SecurityRequirements // remove o cadeado do Swagger: estas rotas não exigem token
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    public AuthController(AuthService authService, UserService userService) {
        this.authService = authService;
        this.userService = userService;
    }

    @PostMapping("/register")
    @Operation(summary = "Cadastrar-se",
            description = "Auto-cadastro como CLIENT (padrão) ou PROVIDER. O papel ADMIN não pode ser auto-atribuído.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Usuário criado",
                    content = @Content(schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "400", description = "Dados inválidos ou papel não permitido",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "E-mail já cadastrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        User created = userService.register(request);
        return ResponseEntity.created(URI.create("/api/v1/users/" + created.getId()))
                .body(UserResponse.fromEntity(created));
    }

    @PostMapping("/login")
    @Operation(summary = "Login",
            description = "Valida e-mail e senha e devolve um access token (24h) e um refresh token (7 dias), "
                    + "com as roles no payload do JWT.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Autenticado",
                    content = @Content(schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "400", description = "Dados inválidos",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Credenciais inválidas ou usuário inativo",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Renovar tokens",
            description = "Troca um refresh token válido por um novo par de tokens, sem pedir a senha novamente.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tokens renovados",
                    content = @Content(schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "401", description = "Refresh token inválido, expirado ou usuário inativo",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ResponseEntity.ok(authService.refresh(request));
    }
}
