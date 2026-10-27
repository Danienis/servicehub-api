package com.servicehub.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.servicehub.config.SecurityConfig;
import com.servicehub.dto.ServiceRequest;
import com.servicehub.exception.ResourceNotFoundException;
import com.servicehub.model.Role;
import com.servicehub.model.ServiceCategory;
import com.servicehub.model.ServiceOffering;
import com.servicehub.model.User;
import com.servicehub.security.AuthUser;
import com.servicehub.security.CustomUserDetailsService;
import com.servicehub.security.JwtService;
import com.servicehub.security.RestSecurityHandlers;
import com.servicehub.service.ReviewService;
import com.servicehub.service.ServiceOfferingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ServiceOfferingController.class)
@Import({SecurityConfig.class, RestSecurityHandlers.class})
class ServiceOfferingControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean ServiceOfferingService serviceOfferingService;
    @MockBean ReviewService reviewService;
    @MockBean JwtService jwtService;
    @MockBean CustomUserDetailsService customUserDetailsService;

    private static AuthUser authUser(long id, Role role) {
        return new AuthUser(id, "u" + id + "@hub.com", "hash", role, true);
    }

    private static ServiceOffering offering() {
        User provider = new User();
        provider.setId(2L);
        provider.setFullName("João Prestador");
        ServiceOffering s = new ServiceOffering();
        s.setId(10L);
        s.setTitle("Limpeza Residencial");
        s.setPrice(new BigDecimal("150.00"));
        s.setCategory(ServiceCategory.LIMPEZA);
        s.setProvider(provider);
        s.setActive(true);
        return s;
    }

    private String body(String title, BigDecimal price) throws Exception {
        return objectMapper.writeValueAsString(
                new ServiceRequest(title, "desc", price, ServiceCategory.LIMPEZA, null));
    }

    @Test
    @DisplayName("deve retornar 401 sem token")
    void deveRetornar401SemToken() throws Exception {
        mockMvc.perform(get("/api/v1/services"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("GET /services retorna 200 com página para usuário autenticado")
    void deveListarPaginado() throws Exception {
        when(serviceOfferingService.search(any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(offering())));

        mockMvc.perform(get("/api/v1/services").with(user(authUser(3, Role.CLIENT))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Limpeza Residencial"))
                .andExpect(jsonPath("$.content[0].providerName").value("João Prestador"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("POST /services como CLIENT deve retornar 403")
    void clienteNaoCriaServico() throws Exception {
        mockMvc.perform(post("/api/v1/services").with(user(authUser(3, Role.CLIENT)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("Limpeza", new BigDecimal("100"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("POST /services com dados inválidos deve retornar 400 com details")
    void deveRetornar400QuandoInvalido() throws Exception {
        mockMvc.perform(post("/api/v1/services").with(user(authUser(2, Role.PROVIDER)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("", new BigDecimal("-5"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details").isNotEmpty());
    }

    @Test
    @DisplayName("POST /services como PROVIDER deve retornar 201 com Location")
    void deveCriarERetornar201() throws Exception {
        when(serviceOfferingService.create(any(ServiceRequest.class), any(AuthUser.class))).thenReturn(offering());

        mockMvc.perform(post("/api/v1/services").with(user(authUser(2, Role.PROVIDER)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("Limpeza Residencial", new BigDecimal("150.00"))))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/services/10"))
                .andExpect(jsonPath("$.id").value(10));
    }

    @Test
    @DisplayName("GET /services/{id} inexistente deve retornar 404")
    void deveRetornar404() throws Exception {
        when(serviceOfferingService.findById(99L)).thenThrow(new ResourceNotFoundException("Serviço não encontrado"));

        mockMvc.perform(get("/api/v1/services/99").with(user(authUser(3, Role.CLIENT))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Serviço não encontrado"));
    }

    @Test
    @DisplayName("DELETE /services/{id} como PROVIDER dono deve retornar 204")
    void deveExcluirERetornar204() throws Exception {
        mockMvc.perform(delete("/api/v1/services/10").with(user(authUser(2, Role.PROVIDER))))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("AccessDenied do service (não é o dono) deve virar 403")
    void deveRetornar403QuandoNaoEDono() throws Exception {
        doThrow(new org.springframework.security.access.AccessDeniedException("negado"))
                .when(serviceOfferingService).delete(eq(10L), any(AuthUser.class));

        mockMvc.perform(delete("/api/v1/services/10").with(user(authUser(5, Role.PROVIDER))))
                .andExpect(status().isForbidden());
    }
}
