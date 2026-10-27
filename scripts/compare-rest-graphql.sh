#!/usr/bin/env bash
# Compara o tempo REST (1+N chamadas) x GraphQL (1 chamada) para listar serviços de uma categoria.
# Uso: TOKEN=<accessToken> [BASE=http://localhost:8080] [RUNS=30] [CATEGORY=LIMPEZA] ./scripts/compare-rest-graphql.sh
set -euo pipefail
: "${TOKEN:?defina TOKEN com o accessToken do login}"
BASE="${BASE:-http://localhost:8080}"; RUNS="${RUNS:-30}"; CATEGORY="${CATEGORY:-LIMPEZA}"
AUTH=(-H "Authorization: Bearer $TOKEN")

rest_once() {
  local ids
  ids=$(curl -s "${AUTH[@]}" "$BASE/api/v1/services?category=$CATEGORY&size=50" | python3 -c 'import sys,json;print(" ".join(str(s["id"]) for s in json.load(sys.stdin)["content"]))')
  for id in $ids; do curl -s -o /dev/null "${AUTH[@]}" "$BASE/api/v1/services/$id/rating"; done
}
gql_once() {
  curl -s -o /dev/null "${AUTH[@]}" -H 'Content-Type: application/json' "$BASE/graphql" \
    -d "{\"query\":\"{ providers(category: \\\"$CATEGORY\\\") { id name rating services { title price } } }\"}"
}
measure() { # $1 = função
  local start end
  start=$(date +%s.%N)
  for _ in $(seq "$RUNS"); do "$1"; done
  end=$(date +%s.%N)
  python3 -c "print(f'{($end-$start)/$RUNS*1000:.1f} ms por repetição ($RUNS repetições)')"
}
rest_once; gql_once   # aquecimento da JVM
echo -n "REST    : "; measure rest_once
echo -n "GraphQL : "; measure gql_once
