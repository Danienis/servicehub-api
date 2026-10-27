package com.servicehub.integration;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Teste de integração: sobe o contexto completo (Security + JWT + JPA + H2) e percorre o fluxo real
 * cadastro -> login -> serviço -> contratação -> status -> avaliação, além de casos de segurança.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ServiceHubFlowIntegrationTest {

    @Autowired MockMvc mockMvc;

    // ------------------------------------------------------------------ helpers

    private String uniqueEmail(String prefix) {
        return prefix + "-" + UUID.randomUUID() + "@hub.com";
    }

    private String json(MvcResult r, String path) throws Exception {
        return JsonPath.read(r.getResponse().getContentAsString(), path).toString();
    }

    private void register(String email, String role) throws Exception {
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Teste " + role + "\",\"email\":\"" + email
                                + "\",\"password\":\"Senha123\",\"role\":\"" + role + "\"}"))
                .andExpect(status().isCreated());
    }

    private String login(String email) throws Exception {
        MvcResult r = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + email + "\",\"password\":\"Senha123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andReturn();
        return json(r, "$.accessToken");
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private String createService(String providerToken) throws Exception {
        MvcResult r = mockMvc.perform(post("/api/v1/services").header("Authorization", bearer(providerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Limpeza Residencial\",\"description\":\"<b>completa</b>\","
                                + "\"price\":150.00,\"category\":\"LIMPEZA\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.description").value("&lt;b&gt;completa&lt;/b&gt;"))
                .andReturn();
        return json(r, "$.id");
    }

    private String book(String clientToken, String serviceId) throws Exception {
        MvcResult r = mockMvc.perform(post("/api/v1/orders").header("Authorization", bearer(clientToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"serviceId\":" + serviceId + ",\"notes\":\"Apto 302\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.totalPrice").value(150.0))
                .andReturn();
        return json(r, "$.id");
    }

    private void setStatus(String token, String orderId, String newStatus, int expected) throws Exception {
        mockMvc.perform(patch("/api/v1/orders/" + orderId + "/status").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"" + newStatus + "\"}"))
                .andExpect(status().is(expected));
    }

    // -------------------------------------------------------------------- testes

    @Test
    @DisplayName("fluxo completo: cadastro, login, serviço, contratação, status e avaliação")
    void fluxoCompleto() throws Exception {
        String providerEmail = uniqueEmail("prov");
        String clientEmail = uniqueEmail("cli");
        register(providerEmail, "PROVIDER");
        register(clientEmail, "CLIENT");
        String providerToken = login(providerEmail);
        String clientToken = login(clientEmail);

        String serviceId = createService(providerToken);
        String orderId = book(clientToken, serviceId);

        // transições: cliente não aceita; prestador percorre o ciclo; salto inválido = 409
        setStatus(clientToken, orderId, "ACCEPTED", 403);
        setStatus(providerToken, orderId, "COMPLETED", 409);
        setStatus(providerToken, orderId, "ACCEPTED", 200);
        setStatus(providerToken, orderId, "IN_PROGRESS", 200);

        // enquanto ativa, a ordem bloqueia a exclusão do serviço
        mockMvc.perform(delete("/api/v1/services/" + serviceId).header("Authorization", bearer(providerToken)))
                .andExpect(status().isConflict());

        // avaliar antes de concluir = 409
        mockMvc.perform(post("/api/v1/orders/" + orderId + "/review").header("Authorization", bearer(clientToken))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"rating\":5}"))
                .andExpect(status().isConflict());

        setStatus(providerToken, orderId, "COMPLETED", 200);

        mockMvc.perform(post("/api/v1/orders/" + orderId + "/review").header("Authorization", bearer(clientToken))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"rating\":5,\"comment\":\"Ótimo!\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rating").value(5));

        // segunda avaliação da mesma ordem = 409; nota fora do intervalo = 400
        mockMvc.perform(post("/api/v1/orders/" + orderId + "/review").header("Authorization", bearer(clientToken))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"rating\":4}"))
                .andExpect(status().isConflict());
        mockMvc.perform(post("/api/v1/orders/" + orderId + "/review").header("Authorization", bearer(clientToken))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"rating\":9}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/v1/services/" + serviceId + "/rating").header("Authorization", bearer(clientToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.average").value(5.0))
                .andExpect(jsonPath("$.totalReviews").value(1));

        // com a ordem concluída, o serviço pode ser excluído (logicamente) e some da busca
        mockMvc.perform(delete("/api/v1/services/" + serviceId).header("Authorization", bearer(providerToken)))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/services/" + serviceId).header("Authorization", bearer(clientToken)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("segurança: 401 sem token, 403 por papel e BOLA entre usuários")
    void seguranca() throws Exception {
        String clientA = uniqueEmail("a");
        String clientB = uniqueEmail("b");
        register(clientA, "CLIENT");
        register(clientB, "CLIENT");
        String tokenA = login(clientA);
        String tokenB = login(clientB);

        mockMvc.perform(get("/api/v1/services")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/services").header("Authorization", "Bearer token.invalido.aqui"))
                .andExpect(status().isUnauthorized());

        // CLIENT não cria serviço nem lista usuários
        mockMvc.perform(post("/api/v1/services").header("Authorization", bearer(tokenA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"X Y Z\",\"price\":10,\"category\":\"OUTROS\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/users").header("Authorization", bearer(tokenA)))
                .andExpect(status().isForbidden());

        // BOLA: B não lê o usuário de A
        MvcResult me = mockMvc.perform(get("/api/v1/users/me").header("Authorization", bearer(tokenA)))
                .andExpect(status().isOk()).andReturn();
        String idA = json(me, "$.id");
        mockMvc.perform(get("/api/v1/users/" + idA).header("Authorization", bearer(tokenB)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/users/" + idA).header("Authorization", bearer(tokenA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    @DisplayName("cadastro: ADMIN rejeitado, e-mail duplicado (mesmo com caixa diferente) = 409, senha fraca = 400")
    void cadastro() throws Exception {
        String email = uniqueEmail("dup");
        register(email, "CLIENT");

        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Duplicado\",\"email\":\"" + email.toUpperCase()
                                + "\",\"password\":\"Senha123\"}"))
                .andExpect(status().isConflict());
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Hacker\",\"email\":\"" + uniqueEmail("h")
                                + "\",\"password\":\"Senha123\",\"role\":\"ADMIN\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Fraca\",\"email\":\"" + uniqueEmail("f")
                                + "\",\"password\":\"fraca\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details").isNotEmpty());
    }

    @Test
    @DisplayName("login: senha errada = 401; refresh token gera novos tokens; usuário excluído perde o acesso")
    void loginRefreshEExclusao() throws Exception {
        String email = uniqueEmail("log");
        register(email, "CLIENT");

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"Errada999\"}"))
                .andExpect(status().isUnauthorized());

        MvcResult ok = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"Senha123\"}"))
                .andExpect(status().isOk()).andReturn();
        String access = json(ok, "$.accessToken");
        String refresh = json(ok, "$.refreshToken");
        String userId = json(ok, "$.user.id");

        // refresh token NÃO serve como access token; access token NÃO serve como refresh
        mockMvc.perform(get("/api/v1/users/me").header("Authorization", bearer(refresh)))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + access + "\"}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refresh + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());

        // exclusão lógica: o JWT antigo deixa de valer e o login passa a falhar
        mockMvc.perform(delete("/api/v1/users/" + userId).header("Authorization", bearer(access)))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/users/me").header("Authorization", bearer(access)))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"Senha123\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GraphQL: providers/servicesByProvider e a mutation bookService (autenticados)")
    void graphQl() throws Exception {
        String providerEmail = uniqueEmail("gp");
        String clientEmail = uniqueEmail("gc");
        register(providerEmail, "PROVIDER");
        register(clientEmail, "CLIENT");
        String providerToken = login(providerEmail);
        String clientToken = login(clientEmail);
        String serviceId = createService(providerToken);

        mockMvc.perform(post("/graphql").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"{ providers { id } }\"}"))
                .andExpect(status().isUnauthorized());

        MvcResult q = mockMvc.perform(post("/graphql").header("Authorization", bearer(clientToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"{ providers(category: \\\"LIMPEZA\\\") { id name services { title price } } }\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errors").doesNotExist())
                .andReturn();
        assertThat(q.getResponse().getContentAsString()).contains("Limpeza Residencial");

        MvcResult m = mockMvc.perform(post("/graphql").header("Authorization", bearer(clientToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"mutation { bookService(serviceId: " + serviceId
                                + ") { id status totalPrice service { title } } }\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.bookService.status").value("PENDING"))
                .andReturn();
        assertThat(m.getResponse().getContentAsString()).contains("\"totalPrice\":150");

        // PROVIDER não pode contratar via GraphQL (@PreAuthorize hasRole CLIENT)
        mockMvc.perform(post("/graphql").header("Authorization", bearer(providerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"mutation { bookService(serviceId: " + serviceId + ") { id } }\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errors").isNotEmpty());
    }

    @Test
    @DisplayName("paginação e filtros: size acima do máximo é limitado e minPrice > maxPrice = 400")
    void paginacaoEFiltros() throws Exception {
        String email = uniqueEmail("pg");
        register(email, "CLIENT");
        String token = login(email);

        mockMvc.perform(get("/api/v1/services?size=500").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(50));
        mockMvc.perform(get("/api/v1/services?minPrice=500&maxPrice=100").header("Authorization", bearer(token)))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/services?category=INEXISTENTE").header("Authorization", bearer(token)))
                .andExpect(status().isBadRequest());
    }
}
