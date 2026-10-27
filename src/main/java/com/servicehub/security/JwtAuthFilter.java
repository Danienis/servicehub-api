package com.servicehub.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Intercepta cada requisição, lê "Authorization: Bearer ...", valida o token e popula o SecurityContext.
 * Token ausente/inválido/expirado, ou usuário inativo: segue sem autenticar (o entry point responde 401).
 *
 * NÃO é um @Component de propósito: como bean, o Spring Boot o registraria também como filtro
 * de servlet e ele rodaria fora da cadeia do Spring Security. A instância é criada no SecurityConfig.
 */
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;

    public JwtAuthFilter(JwtService jwtService, CustomUserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            String token = authHeader.substring(7);
            try {
                String username = jwtService.extractUsername(token);
                UserDetails user = userDetailsService.loadUserByUsername(username);
                if (user.isEnabled() && jwtService.isTokenValid(token, user)) {
                    UsernamePasswordAuthenticationToken auth =
                            new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
                    auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            } catch (JwtException | IllegalArgumentException | UsernameNotFoundException e) {
                logger.debug("Token JWT rejeitado: " + e.getMessage());
            }
        }
        filterChain.doFilter(request, response);
    }
}
