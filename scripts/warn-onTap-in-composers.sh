#!/usr/bin/env bash
# Warn-only: server composers should emit onActivate, not deprecated onTap.
# Exit 0 always (CI / make lint-sdui-warn friendly).
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PATTERN='put("trigger", "onTap")'
if grep -R "$PATTERN" --include='*.java' "$ROOT/server/src/main/java/com/nba/sdui/service" 2>/dev/null; then
  echo "WARN: found deprecated onTap in server composers — use onActivate (see Phase 1 SDUI plan)." >&2
fi
