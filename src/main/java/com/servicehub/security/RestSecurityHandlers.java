package com.servicehub.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.servicehub.exception.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Respostas 401 e 403 geradas na cadeia de filtros (antes de chegar ao controller),
 * no mesmo formato ErrorResponse do restante da API.
 */
@Component
public class RestSecurityHandlers implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public RestSecurityHandlers(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException ex) throws IOException {
        write(response, request, HttpStatus.UNAUTHORIZED,
                "Autenticação necessária: envie um token JWT válido em Authorization: Bearer <token>");
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException ex) throws IOException {
        write(response, request, HttpStatus.FORBIDDEN, "Acesso negado: seu perfil não tem permissão para este recurso");
    }

    private void write(HttpServletResponse response, HttpServletRequest request,
                       HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        ErrorResponse body = new ErrorResponse(status.value(), status.getReasonPhrase(), message, request.getRequestURI());
        objectMapper.writeValue(response.getWriter(), body);
    }
}
