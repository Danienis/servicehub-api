# REST × GraphQL no ServiceHub (Aula 09 — Entrega 2, opção A)

O endpoint GraphQL (`POST /graphql`, GraphiQL em `/graphiql` no perfil demo) reutiliza os mesmos services e a mesma
autenticação JWT do REST. Schema: `src/main/resources/graphql/schema.graphqls`.

## Mesma necessidade, duas chamadas

Tela "prestadores de limpeza com serviços e nota":

**REST (under-fetching: N+1 chamadas e campos que a tela não usa)**
```
GET /api/v1/services?category=LIMPEZA            -> lista de serviços (com todos os campos)
GET /api/v1/users/{providerId}                   -> uma chamada por prestador (somente ADMIN/o próprio!)
GET /api/v1/services/{id}/rating                 -> uma chamada por serviço
```

**GraphQL (1 chamada, só os campos pedidos)**
```graphql
query {
  providers(category: "LIMPEZA", available: true) {
    id name rating
    services { title price }
  }
}
```
Contratar: `mutation { bookService(serviceId: 5, notes: "Apto 302") { id status totalPrice } }`
(o cliente é o usuário autenticado — **não** se envia `clientId`, evitando BOLA).

## Como medir (roteiro)

`scripts/compare-rest-graphql.sh` faz N repetições de cada abordagem com `curl -w %{time_total}` e imprime a média.
Passos: subir a API, cadastrar um prestador + serviços, rodar `TOKEN=<jwt> ./scripts/compare-rest-graphql.sh`.
Registre aqui os resultados da sua máquina:

| Abordagem | Chamadas | Tempo médio total | Bytes |
|---|---|---|---|
| REST (lista + rating por serviço) | 1 + N | _preencher_ | _preencher_ |
| GraphQL (uma query) | 1 | _preencher_ | _preencher_ |

Os números não foram medidos no ambiente em que o código foi escrito (sem Maven Central/sem execução): meça localmente.

## Quando usar cada um (resumo da Aula 09)
REST para a API pública/parceiros; GraphQL para web/mobile com telas que pedem conjuntos diferentes de dados;
gRPC para chamadas internas entre microsserviços.
