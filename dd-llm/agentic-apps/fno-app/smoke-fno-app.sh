#!/usr/bin/env bash
set -euo pipefail

# =================================================
# CGO FNO Alpha2 — smoke.sh
# - macOS OOB (no python, no jq)
# - Contract: INGEST -> QUERY -> OBSERVE
# - Modes:
#     default: DEV VERBOSE (prints request/response bodies)
#     QUIET=1: minimal output (CI-friendly)
# - Overrides:
#     BASE_URL=http://127.0.0.1:8100  (default)
#     PORT=8100                       (optional)
# =================================================

QUIET="${QUIET:-0}"
BASE_URL="${BASE_URL:-http://127.0.0.1:8100}"
PORT="${PORT:-}"

if [[ -n "$PORT" ]]; then
  # If user sets PORT, replace any trailing :<digits> or append if missing.
  if [[ "$BASE_URL" =~ :[0-9]+$ ]]; then
    BASE_URL="${BASE_URL%:*}:$PORT"
  else
    BASE_URL="$BASE_URL:$PORT"
  fi
fi

INGEST_URL="$BASE_URL/fno/ingest"
QUERY_URL="$BASE_URL/fno/query"
OBSERVE_URL="$BASE_URL/fno/observe?queryKind=validate_task"

log() { if [[ "$QUIET" != "1" ]]; then echo "$@"; fi; }
hr()  { if [[ "$QUIET" != "1" ]]; then echo "$@"; fi; }

hr "================================================="
hr " CGO FNO Alpha2 — smoke.sh ($( [[ "$QUIET" == "1" ]] && echo "QUIET" || echo "DEV VERBOSE" ))"
hr " BASE_URL=$BASE_URL"
hr "================================================="

TMP_DIR="$(mktemp -d)"
trap "rm -rf $TMP_DIR" EXIT

log "[smoke] temp dir: $TMP_DIR"
log

# -------------------------------------------------
# INGEST PAYLOAD (ARRAY CONTRACT)
# -------------------------------------------------
cat > "$TMP_DIR/flights.array.json" <<'EOF'
[
  {
    "id": "F100",
    "origin": "AUS",
    "dest": "DFW",
    "dep_utc": "2025-10-22T10:00:00Z",
    "arr_utc": "2025-10-22T11:10:00Z"
  },
  {
    "id": "F102",
    "origin": "DFW",
    "dest": "IAH",
    "dep_utc": "2025-10-22T12:00:00Z",
    "arr_utc": "2025-10-22T13:10:00Z"
  }
]
EOF

if [[ "$QUIET" != "1" ]]; then
  echo "---------------- INGEST REQUEST ----------------"
  cat "$TMP_DIR/flights.array.json"
  echo
fi

# -------------------------------------------------
# 1) INGEST
# -------------------------------------------------
INGEST_RESP="$TMP_DIR/ingest.response.txt"

HTTP_CODE=$(curl -s -w "%{http_code}" -o "$INGEST_RESP" \
  -H "Content-Type: application/json" \
  --data-binary @"$TMP_DIR/flights.array.json" \
  "$INGEST_URL")

if [[ "$QUIET" != "1" ]]; then
  echo "---------------- INGEST RESPONSE (HTTP $HTTP_CODE) ----------------"
  cat "$INGEST_RESP"
  echo
fi

if [[ "$HTTP_CODE" != "200" ]]; then
  echo "❌ INGEST FAILED (HTTP $HTTP_CODE)"
  cat "$INGEST_RESP"
  exit 1
fi

grep -q "Flight:F100" "$INGEST_RESP" || { echo "❌ missing Flight:F100"; exit 1; }
grep -q "Airport:AUS" "$INGEST_RESP" || { echo "❌ missing Airport:AUS"; exit 1; }

log "✅ ingest + graph sanity OK"
log

# -------------------------------------------------
# QUERY PAYLOAD
# -------------------------------------------------
cat > "$TMP_DIR/query.request.json" <<'EOF'
{
  "meta": {
    "version": "v1",
    "query_kind": "validate_task",
    "description": "smoke: validate ingested graph"
  },
  "task": {
    "description": "validate Flight:F102 exists post-ingest",
    "factId": "Flight:F102"
  },
  "context": {
    "nodes": {}
  }
}
EOF

if [[ "$QUIET" != "1" ]]; then
  echo "---------------- QUERY REQUEST ----------------"
  cat "$TMP_DIR/query.request.json"
  echo
fi

# -------------------------------------------------
# 2) QUERY
# -------------------------------------------------
QUERY_RESP="$TMP_DIR/query.response.json"

HTTP_CODE=$(curl -s -w "%{http_code}" -o "$QUERY_RESP" \
  -H "Content-Type: application/json" \
  -d @"$TMP_DIR/query.request.json" \
  "$QUERY_URL")

if [[ "$QUIET" != "1" ]]; then
  echo "---------------- QUERY RESPONSE (HTTP $HTTP_CODE) ----------------"
  cat "$QUERY_RESP"
  echo
fi

if [[ "$HTTP_CODE" != "200" ]]; then
  echo "❌ QUERY FAILED (HTTP $HTTP_CODE)"
  cat "$QUERY_RESP"
  exit 1
fi

log "✅ query OK (history should now exist)"
log







