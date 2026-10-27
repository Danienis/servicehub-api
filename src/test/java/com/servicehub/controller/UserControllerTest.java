package com.servicehub.controller;

import com.servicehub.config.SecurityConfig;
import com.servicehub.model.Role;
import com.servicehub.model.User;
import com.servicehub.security.AuthUser;
import com.servicehub.security.CustomUserDetailsService;
import com.servicehub.security.JwtService;
import com.servicehub.security.RestSecurityHandlers;
import com.servicehub.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import({SecurityConfig.class, RestSecurityHandlers.class})
class UserControllerTest {

    @Autowired MockMvc mockMvc;

    @MockBean UserService userService;
    @MockBean JwtService jwtService;
    @MockBean CustomUserDetailsService customUserDetailsService;

    private static AuthUser authUser(long id, Role role) {
        return new AuthUser(id, "u" + id + "@hub.com", "hash", role, true);
    }

    private static User entity() {
        User u = new User();
        u.setId(1L);
        u.setFullName("Maria Silva");
        u.setEmail("maria@hub.com");
        u.setPasswordHash("segredo-nao-pode-vazar");
        u.setRole(Role.CLIENT);
        return u;
    }

    @Test
    @DisplayName("listar usuários como CLIENT deve retornar 403")
    void clienteNaoListaUsuarios() throws Exception {
        mockMvc.perform(get("/api/v1/users").with(user(authUser(3, Role.CLIENT))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("listar usuários como ADMIN retorna 200 e nunca expõe a senha")
    void adminListaUsuarios() throws Exception {
        when(userService.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(entity())));

        mockMvc.perform(get("/api/v1/users").with(user(authUser(1, Role.ADMIN))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].email").value("maria@hub.com"))
                .andExpect(jsonPath("$.content[0].passwordHash").doesNotExist())
                .andExpect(jsonPath("$.content[0].password").doesNotExist());
    }

    @Test
    @DisplayName("sem token deve retornar 401")
    void semTokenRetorna401() throws Exception {
        mockMvc.perform(get("/api/v1/users/1")).andExpect(status().isUnauthorized());
    }
}
