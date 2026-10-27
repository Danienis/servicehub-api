package com.servicehub.service;

import com.servicehub.dto.AuthResponse;
import com.servicehub.dto.LoginRequest;
import com.servicehub.dto.RefreshRequest;
import com.servicehub.dto.UserResponse;
import com.servicehub.model.User;
import com.servicehub.security.AuthUser;
import com.servicehub.security.CustomUserDetailsService;
import com.servicehub.security.JwtService;
import io.jsonwebtoken.JwtException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.Locale;

/** Login (AuthenticationManager + BCrypt) e renovação de tokens. */
@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;
    private final UserService userService;

    public AuthService(AuthenticationManager authenticationManager,
                       JwtService jwtService,
                       CustomUserDetailsService userDetailsService,
                       UserService userService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.userService = userService;
    }

    public AuthResponse login(LoginRequest request) {
        String email = request.getEmail().strip().toLowerCase(Locale.ROOT);
        // Lança BadCredentialsException/DisabledException (-> 401) se inválido ou usuário inativo.
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, request.getPassword()));
        return buildResponse((AuthUser) authentication.getPrincipal());
    }

    public AuthResponse refresh(RefreshRequest request) {
        String email;
        try {
            email = jwtService.extractUsernameFromRefreshToken(request.getRefreshToken());
        } catch (JwtException | IllegalArgumentException e) {
            throw new BadCredentialsException("Refresh token inválido ou expirado");
        }
        AuthUser user = (AuthUser) userDetailsService.loadUserByUsername(email);
        if (!user.isEnabled()) {
            throw new BadCredentialsException("Usuário inativo");
        }
        return buildResponse(user);
    }

    private AuthResponse buildResponse(AuthUser authUser) {
        User user = userService.findById(authUser.getId());
        return new AuthResponse(
                jwtService.generateAccessToken(authUser),
                jwtService.generateRefreshToken(authUser),
                jwtService.getAccessExpirationSeconds(),
                UserResponse.fromEntity(user));
    }
}
