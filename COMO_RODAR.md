# ServiceHub — como rodar

Estrutura do repositório:
```
Servicehub/
├── servicehub-api/        # projeto Spring Boot (código, testes, Docker, docs)
├── apresentacao/          # ServiceHub_API_Checkpoint_2.pptx
├── RESUMO_TECNICO.md
└── COMO_RODAR.md
```
Requisitos: **Java 21**, **Maven 3.9+** (ou o wrapper, se existir) e, para a opção 2, **Docker**.

## Opção 1 — rápida (H2 em memória, sem Docker)
```bash
cd servicehub-api
mvn clean verify            # compila e roda os testes (cobertura em target/site/jacoco/index.html)
mvn spring-boot:run         # sobe em http://localhost:8080
```
- Swagger: http://localhost:8080/swagger-ui.html
- GraphiQL: http://localhost:8080/graphiql
- Admin de demonstração: `admin@servicehub.com` / `Admin@123`

## Opção 2 — Docker Compose (API + PostgreSQL)
```bash
cd servicehub-api
cp .env.example .env        # Windows: copy .env.example .env
# edite o .env: DB_PASS, JWT_SECRET (gere com: openssl rand -base64 48) e ADMIN_PASSWORD
docker compose up -d --build
docker compose ps
docker compose logs -f api
docker compose down         # para tudo (adicione -v para apagar também o banco)
```
Ambientes: `docker compose -f docker-compose.yml -f docker-compose.dev.yml --env-file .env up -d --build` (dev) ou `-f docker-compose.prod.yml` (prod).

## Opção 3 — só o Postgres no Docker, API local
```bash
cd servicehub-api
docker compose up -d postgres
export JWT_SECRET="$(openssl rand -base64 48)" DB_HOST=localhost DB_NAME=servicehub DB_USER=servicehub DB_PASS=<a do .env>
mvn spring-boot:run -Dspring-boot.run.profiles=postgres
```

## Testando a API
```bash
curl -X POST localhost:8080/api/auth/register -H 'Content-Type: application/json' \
  -d '{"fullName":"João Prestador","email":"joao@hub.com","password":"Senha123","role":"PROVIDER"}'
curl -X POST localhost:8080/api/auth/login -H 'Content-Type: application/json' \
  -d '{"email":"joao@hub.com","password":"Senha123"}'      # copie o accessToken
curl localhost:8080/api/v1/services -H "Authorization: Bearer <accessToken>"
```
No Swagger, clique em **Authorize** e cole o `accessToken`.

## Problemas comuns
- `JWT_SECRET` ausente no perfil postgres: a API não sobe (propositalmente). Defina no `.env`.
- Porta 8080/5432 ocupada: pare o outro serviço ou altere `ports` no compose.
- Build falhou: rode `mvn clean verify` e envie o primeiro erro (o código ainda não foi compilado por quem o escreveu).
- Banco com dados antigos: leia os avisos no topo das migrations V3/V4 (e-mails duplicados, preços ≤ 0).
