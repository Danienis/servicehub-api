# ServiceHub API

API REST em **Spring Boot 3.2 + Java 21** que conecta prestadores de serviço e clientes.
Projeto da disciplina de Desenvolvimento de API Back-End (Engenharia de Software, 6º período).

## Stack

Spring Web · Spring Data JPA · Bean Validation · **Spring Security + JWT (jjwt)** · **Spring for GraphQL** ·
PostgreSQL 16 + Flyway · H2 (demo/testes) · SpringDoc/Swagger · Actuator (health) · Commons Text ·
JUnit 5 + Mockito + MockMvc + JaCoCo · Docker Compose · Maven

## Estrutura de pacotes

```
com.servicehub
├── config/       SecurityConfig, OpenApiConfig, DataSeeder
├── security/     JwtService, JwtAuthFilter, AuthUser, CustomUserDetailsService, RestSecurityHandlers
├── controller/   Auth, User, ServiceOffering, ServiceOrder (só orquestram)
├── graphql/      ServiceHubGraphQlController (endpoint alternativo)
├── service/      regras de negócio (+ InputSanitizer, AuthService, ReviewService)
├── repository/   Spring Data JPA (+ Specifications para os filtros)
├── model/        User, ServiceOffering, ServiceOrder, Review + enums Role, OrderStatus, ServiceCategory
├── dto/          *Request / *Response (nunca expõe entidade JPA)
└── exception/    GlobalExceptionHandler, ErrorResponse
```

## Como rodar

> O repositório não inclui o Maven Wrapper. Se `./mvnw` não existir, rode `mvn -N wrapper:wrapper` uma vez ou use `mvn` no lugar de `./mvnw`.

### 1. Modo rápido (H2 em memória, sem Docker)
```bash
./mvnw clean verify          # compila e roda os testes (relatório JaCoCo em target/site/jacoco)
./mvnw spring-boot:run
```
Swagger: http://localhost:8080/swagger-ui.html · GraphiQL: http://localhost:8080/graphiql · H2: /h2-console  
Administrador de demonstração: `admin@servicehub.com` / `Admin@123` (apenas no perfil H2).

### 2. Docker Compose (API + PostgreSQL)
```bash
cp .env.example .env         # preencha DB_PASS, JWT_SECRET (openssl rand -base64 48) e ADMIN_PASSWORD
docker compose up -d --build
docker compose ps && docker compose logs -f api
docker compose down          # (-v apaga também o volume pg_data)
```
Ambientes: `docker compose -f docker-compose.yml -f docker-compose.dev.yml --env-file .env up -d --build`
(dev expõe 5432 e debug 5005) ou `-f docker-compose.prod.yml` (banco não exposto, GraphiQL desligado).

### 3. Só o PostgreSQL no Docker + API local
```bash
docker compose up -d postgres
JWT_SECRET=... DB_HOST=localhost ./mvnw spring-boot:run -Dspring-boot.run.profiles=postgres
```

## Autenticação e autorização

Fluxo: `POST /api/auth/register` → `POST /api/auth/login` → usar `Authorization: Bearer <accessToken>`.
Access token 24 h, refresh token 7 dias (`POST /api/auth/refresh`). Token inválido/expirado/refresh-usado-como-access → 401.

| Recurso | ADMIN | PROVIDER | CLIENT |
|---|---|---|---|
| `/api/auth/**` | público | público | público |
| `GET /api/v1/services`, `/{id}`, `/{id}/rating` | ✔ | ✔ | ✔ |
| `POST/PUT/DELETE /api/v1/services` | ✔ | só os próprios | ✘ |
| `GET /api/v1/users`, `POST /users`, `PUT /users/{id}/role` | ✔ | ✘ | ✘ |
| `GET/PUT/DELETE /api/v1/users/{id}`, `/users/me` | ✔ | só o próprio | só o próprio |
| `POST /api/v1/orders` (contratar) | ✘ | ✘ | ✔ |
| `GET /api/v1/orders`, `/{id}` | todas | dos seus serviços | as suas |
| `PATCH /api/v1/orders/{id}/status` | ✔ | aceitar/iniciar/concluir/cancelar | cancelar |
| `POST /api/v1/orders/{id}/review` | ✘ | ✘ | só o cliente da ordem, após COMPLETED |
| `POST /graphql` | autenticado; mutation `bookService` só CLIENT | | |

### Exemplo (cURL)
```bash
curl -X POST localhost:8080/api/auth/register -H 'Content-Type: application/json' \
  -d '{"fullName":"João Prestador","email":"joao@hub.com","password":"Senha123","role":"PROVIDER"}'
TOKEN=$(curl -s -X POST localhost:8080/api/auth/login -H 'Content-Type: application/json' \
  -d '{"email":"joao@hub.com","password":"Senha123"}' | python3 -c 'import sys,json;print(json.load(sys.stdin)["accessToken"])')
curl -X POST localhost:8080/api/v1/services -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"title":"Limpeza Residencial","description":"Completa","price":150.00,"category":"LIMPEZA"}'
curl "localhost:8080/api/v1/services?category=LIMPEZA&minPrice=100&maxPrice=300&page=0&size=10&sort=price,asc" \
  -H "Authorization: Bearer $TOKEN"
```

## Banco de dados e migrations

`src/main/resources/db/migration`: V1/V2 (originais, intocadas) + **V3** users · **V4** services · **V5** service_requests ·
**V6** reviews, levando o banco ao estado final de `servicehub_schema.sql`. O perfil `postgres` usa
`ddl-auto=validate`; o perfil H2 usa `create-drop`.
Antes de migrar um banco com dados leia os avisos no topo de cada migration: e-mails duplicados (ignorando caixa) e
`price <= 0` abortam a migração com mensagem clara; usuários legados recebem um marcador de senha que nunca autentica
(precisam de senha nova) e quem já tinha serviços vira PROVIDER.

## Testes

```bash
./mvnw test        # unitários (Mockito), controllers (MockMvc) e integração (H2)
./mvnw verify      # + relatório de cobertura JaCoCo
```

## Documentação adicional

- `docs/REST_vs_GRAPHQL.md` — comparação e roteiro de medição de desempenho (Entrega 2, Aula 09)
- `docs/INDICES_E_EXPLAIN.md` — índices do schema e consultas para analisar com `EXPLAIN (ANALYZE, BUFFERS)` (Aula 11)
- `docs/servicehub_schema.sql` — schema de referência (NÃO executar sobre o banco do projeto; compare com as migrations)
- `CHECKLIST_MODIFICACOES.md` — tudo o que mudou em relação ao Checkpoint 1
