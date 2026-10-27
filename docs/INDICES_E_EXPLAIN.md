# Índices e otimização de consultas (Aula 11)

## Índices do schema e a consulta que cada um atende

| Índice | Atende |
|---|---|
| `uk_users_email` (UNIQUE, B-tree) | login e `existsByEmail` — igualdade em e-mail já normalizado (sem `LOWER()` na consulta ⇒ sargável) |
| `idx_services_provider (provider_id)` | serviços de um prestador; checagem de dono; FK |
| `idx_services_category (category) WHERE is_active` | **índice parcial**: só serviços ativos (a listagem filtra `is_active`) — menor e mais rápido |
| `idx_requests_service / _client / _status` | ordens por serviço, por cliente (CLIENT) e por status; bloqueio de exclusão (`status IN (ativos)`) |
| `uk_reviews_request` (UNIQUE) | uma avaliação por ordem + junção ordem→avaliação |
| `idx_reviews_reviewer` | avaliações de um usuário; FK |

Decisões: removido `idx_users_email` (V1) por redundância com a UNIQUE (superindexação prejudica escritas);
`idx_services_category` recriado como parcial (V4). Não foram criados índices fora do schema de referência.

## Consultas para analisar

Rode no PostgreSQL com dados de volume (ex.: `INSERT ... SELECT generate_series`) e depois `ANALYZE`:

```sql
-- 1. Listagem filtrada (usa idx_services_category parcial)
EXPLAIN (ANALYZE, BUFFERS)
SELECT id, title, price FROM services
 WHERE is_active AND category = 'LIMPEZA' AND price BETWEEN 100 AND 300
 ORDER BY price LIMIT 20;

-- 2. Média de avaliações por serviço (junções por FK/UNIQUE)
EXPLAIN (ANALYZE, BUFFERS)
SELECT s.id, s.title, ROUND(AVG(r.rating), 1), COUNT(r.id)
  FROM services s
  JOIN service_requests sr ON sr.service_id = s.id
  JOIN reviews r           ON r.request_id  = sr.id
 GROUP BY s.id, s.title;

-- 3. Ordens ativas de um prestador (bloqueiam exclusão)
EXPLAIN (ANALYZE, BUFFERS)
SELECT sr.* FROM service_requests sr JOIN services s ON s.id = sr.service_id
 WHERE s.provider_id = 42 AND sr.status IN ('PENDING','ACCEPTED','IN_PROGRESS');

-- 4. Minhas ordens, mais recentes primeiro (candidata a índice composto)
EXPLAIN (ANALYZE, BUFFERS)
SELECT * FROM service_requests WHERE client_id = 42 ORDER BY created_at DESC LIMIT 20;
```

## O que observar
`Seq Scan` x `Index Scan`/`Bitmap Heap Scan`, `Sort` desnecessário, `rows` estimadas x reais, `Buffers: shared hit/read`.
Se a consulta 4 mostrar `Sort` caro, avalie `CREATE INDEX CONCURRENTLY idx_requests_client_created ON service_requests (client_id, created_at DESC);`
numa nova migration — só depois de medir (índice extra encarece INSERT/UPDATE).

## Antipadrões evitados no código
- Sem `LIKE '%termo'` indexado: a busca `q` por título é um filtro opcional sobre o conjunto já filtrado por categoria/preço.
- Sem `WHERE LOWER(email) = ...`: o e-mail é normalizado na entrada (CHECK `email = LOWER(email)`).
- Listagens sempre paginadas (`size` ≤ 50) e `SELECT` só das colunas necessárias nas consultas nativas de exemplo.

> Os resultados reais de `EXPLAIN` dependem do seu volume de dados; cole aqui as saídas da sua execução.
