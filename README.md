# ServiceHub API — Checkpoint

API REST desenvolvida com **Spring Boot 3.2 + Java 21**, para a disciplina de Engenharia de Software (6º período), atendendo aos requisitos do **Checkpoint do Projeto ServiceHub API**.

## Stack

- Spring Boot 3.2.5 (Spring Web, Spring Data JPA, Bean Validation)
- PostgreSQL (produção) + H2 em memória (perfil de demonstração local, sem Docker)
- Flyway (migrations versionadas, perfil `postgres`)
- SpringDoc OpenAPI / Swagger UI (`springdoc-openapi-starter-webmvc-ui` 2.5.0)
- Lombok
- Maven

## Estrutura de pacotes

```
com.servicehub
├── config/          # OpenApiConfig (Swagger)
├── controller/       # UserController, ServiceOfferingController
├── service/          # UserService, ServiceOfferingService (regras de negócio)
├── repository/        # UserRepository, ServiceOfferingRepository (Spring Data JPA)
├── model/            # User, ServiceOffering, ServiceCategory
├── dto/              # *Request / *Response
└── exception/         # GlobalExceptionHandler (@ControllerAdvice), ErrorResponse
```

## Recursos com CRUD completo

- **`/api/v1/users`** — POST, GET (lista e por ID), PUT, DELETE
- **`/api/v1/services`** — POST, GET (lista e por ID), PUT, DELETE (recurso principal exigido pelo checkpoint)

Cada `ServiceOffering` pertence a um `User` (`providerId`), então crie um usuário antes de criar um serviço.

## Como rodar localmente (modo rápido, sem Docker)

O perfil padrão usa **H2 em memória** — não precisa instalar nada além de Java 21 e Maven.

```bash
# 1. Instale as dependências e rode os testes
./mvnw clean verify

# 2. Suba a aplicação (perfil padrão = H2)
./mvnw spring-boot:run
```

A aplicação sobe em `http://localhost:8080`.

> Se o wrapper `mvnw` não estiver presente no seu ambiente (por exemplo, se você recriou o projeto a partir destes arquivos), gere-o com `mvn -N wrapper:wrapper` ou use `mvn` diretamente no lugar de `./mvnw`.

## Como rodar com PostgreSQL (perfil `postgres`)

```bash
# 1. Suba o banco de dados
docker compose up -d

# 2. Rode a aplicação com o perfil postgres (usa Flyway automaticamente)
./mvnw spring-boot:run -Dspring-boot.run.profiles=postgres
```

## Testando a API

- **Swagger UI:** http://localhost:8080/swagger-ui.html
- **OpenAPI JSON:** http://localhost:8080/v3/api-docs
- **H2 Console** (apenas perfil padrão): http://localhost:8080/h2-console (JDBC URL: `jdbc:h2:mem:servicehub`, user: `sa`, senha em branco)

### Exemplo com cURL

```bash
# Criar um usuário (prestador)
curl -X POST http://localhost:8080/api/v1/users \
  -H "Content-Type: application/json" \
  -d '{"fullName":"Maria Silva","email":"maria@servicehub.com","phone":"(62) 99999-0000"}'

# Criar um serviço (usando o id do usuário criado acima)
curl -X POST http://localhost:8080/api/v1/services \
  -H "Content-Type: application/json" \
  -d '{"title":"Instalação de Ar-Condicionado","description":"Instalação completa","price":250.00,"category":"MANUTENCAO","providerId":1}'

# Listar serviços
curl http://localhost:8080/api/v1/services

# Buscar por ID
curl http://localhost:8080/api/v1/services/1

# Atualizar
curl -X PUT http://localhost:8080/api/v1/services/1 \
  -H "Content-Type: application/json" \
  -d '{"title":"Instalação de Ar-Condicionado Split","description":"Atualizado","price":280.00,"category":"MANUTENCAO","providerId":1}'

# Excluir
curl -X DELETE http://localhost:8080/api/v1/services/1
```

## ⚠️ Aviso importante sobre este pacote de código

Este projeto foi **escrito integralmente à mão, seguindo fielmente os padrões e exemplos das aulas** (estrutura em camadas, anotações JPA, DTOs, `@ControllerAdvice`, anotações SpringDoc `@Tag`/`@Operation`/`@ApiResponse`/`@Schema`, migrations Flyway com a convenção `V{n}__descricao.sql` etc.).

**No entanto, o ambiente onde este código foi gerado não tem acesso ao Maven Central** (só permite alguns domínios específicos como npm, pip e GitHub), então **não foi possível baixar as dependências do Spring Boot e compilar/rodar o projeto de fato para validar 100%**. Ou seja: a estrutura, a lógica e a sintaxe seguem os padrões corretos do Spring Boot 3.2 / Java 21, mas você **precisa compilar na sua máquina** para confirmar que está tudo certo antes de entregar.

### Passo a passo para você validar antes de entregar

1. **Extraia o `.zip`** em uma pasta no seu computador.
2. Certifique-se de ter **Java 21** e **Maven** instalados (ou use o Maven Wrapper, se presente; se não, rode `mvn -N wrapper:wrapper` uma vez com o Maven instalado).
3. Abra o projeto na **IntelliJ IDEA** (ou VS Code com extensão Java) como projeto Maven.
4. Rode `./mvnw clean verify` (ou `mvn clean verify`) para baixar as dependências, compilar e rodar o teste de contexto.
5. Se aparecer **algum erro de compilação**, ele provavelmente será algo pequeno (import faltando, versão de dependência) — me envie a mensagem de erro completa que eu ajusto o código imediatamente.
6. Depois de compilar com sucesso, rode `./mvnw spring-boot:run` e acesse `http://localhost:8080/swagger-ui.html` para conferir todos os endpoints.
7. Teste o CRUD completo (crie um usuário, crie um serviço, liste, busque por ID, atualize, delete) usando o próprio Swagger UI, Postman ou os comandos cURL acima.
8. Depois de validado, faça `git init`, `git add .`, `git commit` e suba para o GitHub (ou compacte a pasta local) para entregar junto com os slides no AVA.

## Checklist de entrega (baseado na atividade)

- [x] Projeto Spring Boot inicializado com Spring Web, Spring Data JPA, PostgreSQL, SpringDoc OpenAPI
- [x] Recurso com CRUD completo (`ServiceOffering`, além de `User`)
- [x] Swagger UI configurado em `/swagger-ui.html` com `@Operation`, `@Schema` e exemplos de payload
- [x] Separação Controller / Service / Repository, sem lógica de negócio no Controller
- [x] Tratamento de erros com `@ControllerAdvice` (400, 404, 409)
- [ ] **Você**: compilar/rodar na sua máquina para confirmar e gravar o vídeo/print da demonstração
- [ ] **Você**: gerar/anexar a apresentação de slides do projeto
- [ ] **Você**: subir para o repositório (GitHub/GitLab) ou compactar e enviar no AVA
