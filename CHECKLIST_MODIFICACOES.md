# Checklist de modificações (Checkpoint 1 → versão atual)

## Aula 08 — Segurança
- [x] `spring-boot-starter-security`; `SecurityConfig` com `SecurityFilterChain` stateless, rotas públicas (`/api/auth/**`, Swagger) e autenticadas
- [x] `BCryptPasswordEncoder`; senha nunca em texto puro nem em DTO/`toString`
- [x] Bean Validation (`@Valid @NotBlank @Size @Email @Pattern @Min @Max @Future`) e erro 400 padronizado com `campo: mensagem`
- [x] Sanitização (`InputSanitizer`, Commons Text) na camada de service; consultas só com parâmetros (JPA/Specification)
- [x] `@PreAuthorize` por papel + checagem de dono no service (BOLA); DTOs separados contra mass assignment (cadastro público rejeita ADMIN)
- [x] CORS com origens explícitas; `server.error.include-stacktrace=never`; limite de `size` de página (50) e de payload; H2 console/GraphiQL só no perfil demo
- [ ] Rate limiting (Bucket4j) — não pedido na entrega; fica como evolução

## Aula 09 — JWT e padrões de API
- [x] `POST /api/auth/login` (+ `/register`, `/refresh`), `JwtService` (HS256, 24h/7d), `JwtAuthFilter`, roles no payload
- [x] Papéis ADMIN / PROVIDER / CLIENT aplicados aos endpoints existentes
- [x] Entrega 2 (opção A): GraphQL `providers`, `servicesByProvider`, `bookService` + `docs/REST_vs_GRAPHQL.md` e script de medição
- [ ] Preencher a tabela de tempos com a **sua** medição

## Aula 10 — Testes
- [x] Unitários (Mockito): User, ServiceOffering, ServiceOrder, Review, JwtService, InputSanitizer, OrderStatus
- [x] MockMvc (`@WebMvcTest`): status 401/403/400/201/404/204
- [x] Integração (`@SpringBootTest` + H2): fluxo completo, segurança, GraphQL, paginação
- [x] JaCoCo (relatório em `target/site/jacoco`)
- [ ] Rodar `./mvnw clean verify` e conferir cobertura (meta 80%)

## Aula 11 — Índices
- [x] Índices do schema (V3–V6), índice parcial de categoria, remoção do redundante
- [x] `docs/INDICES_E_EXPLAIN.md` com consultas para `EXPLAIN (ANALYZE, BUFFERS)`
- [ ] Rodar as consultas com volume e anexar as saídas

## Aula 12 — Docker Compose
- [x] `Dockerfile` multi-stage (usuário sem privilégios), `docker-compose.yml` (api + postgres, rede, volume `pg_data`, healthchecks, `restart`, `depends_on: service_healthy`)
- [x] `.env` / `.env.example` (`.env` no `.gitignore`), overrides `dev` e `prod`, Actuator só com `/health`

## Schema de referência (`servicehub_schema.sql`)
- [x] Migrations V3 (users), V4 (services), V5 (service_requests), V6 (reviews) sem editar V1/V2, com guardas para dados existentes
- [x] Entidades `User`, `ServiceOffering`, `ServiceOrder`, `Review` alinhadas (validate); enum `ServiceCategory` com as 21 categorias
- [x] Regras: preço copiado na ordem, uma avaliação por ordem concluída, máquina de estados, exclusão lógica bloqueada por ordens ativas
- [ ] Validar em banco vazio: `docker run ... postgres:16-alpine` + `psql -f servicehub_schema.sql` e comparar com o resultado do Flyway

## Correções no que existia
- [x] `LazyInitializationException` ao listar serviços (provider LAZY + open-in-view=false) → `@EntityGraph`
- [x] Handler genérico transformava 401/403/404 do Spring em 500 → handlers específicos
- [x] Listagens agora paginadas (contrato mudou: `{content, page, size, totalElements, totalPages, last}`)
- [x] Categorias e rotas: `providerId` agora opcional no `ServiceRequest` (PROVIDER usa o token)

## Contrato que mudou (impacta clientes da API)
`POST /api/v1/users` agora exige `password` e `role` e é ADMIN-only (cadastro público é `/api/auth/register`);
`GET` de listas devolve página; `DELETE` é lógico; todas as rotas (exceto `/api/auth/**`/Swagger) exigem Bearer token.
