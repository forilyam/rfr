#!/usr/bin/env bash
set -uo pipefail
limit=${WAIT_TIMEOUT:-180}
deadline=$(( $(date +%s) + limit ))
await() {
  local name=$1
  shift
  until "$@" >/dev/null 2>&1; do
    if [ "$(date +%s)" -ge "$deadline" ]; then
      echo "$name не поднялся за $limit с" >&2
      exit 1
    fi
    sleep 2
  done
  echo "$name готов"
}
await MySQL bash -c 'exec 3<>/dev/tcp/127.0.0.1/3306'
await rfr-auth curl -fsS -m 5 -o /dev/null http://localhost:9001/.well-known/openid-configuration
await rfr-api curl -sS -m 5 -o /dev/null -X POST -H 'Content-Type: application/json' -d '{"query":"{__typename}"}' http://localhost:8081/graphql
await rfr-gql-client curl -fsS -m 5 -o /dev/null http://localhost:3001