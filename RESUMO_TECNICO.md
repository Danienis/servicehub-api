# Resumo técnico — ServiceHub API (Checkpoint 1 → 2)

**Banco (schema SQL + Aula 11):** migrations V3–V6 (users, services, service_requests, reviews) sem editar V1/V2, com guardas para dados existentes; índice parcial de categoria; índice redundante removido.
**Segurança (Aula 08):** Spring Security stateless, BCrypt, `@PreAuthorize` por papel + checagem de dono (anti-BOLA), validação, sanitização, CORS restrito, sem stack trace, paginação máx. 50, handler global com 400/401/403/404/405/415/409.
**JWT (Aula 09):** `/api/auth/register|login|refresh`, access 24h e refresh 7d, roles no payload, usuário inativo perde o acesso. GraphQL (opção A) em `/graphql`.
**Domínio novo:** ordens (`/api/v1/orders`) com máquina de estados e preço copiado; avaliações 1–5 (uma por ordem COMPLETED); listagem de serviços paginada com filtros; exclusão lógica bloqueada por ordens ativas.
**Docker (Aula 12):** Dockerfile multi-stage, compose com healthchecks, `.env`, overrides dev/prod.
**Testes (Aula 10):** unitários (Mockito), MockMvc, integração (H2), JaCoCo.
**Correções:** LazyInitializationException na listagem (EntityGraph); erros do Spring que viravam 500.

**Não verificado:** sem acesso ao Maven Central no ambiente, o projeto não foi compilado nem testado. Rode `mvn clean verify` e me envie o primeiro erro, se houver.
