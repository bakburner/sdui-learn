#!/usr/bin/env bash
# scripts/lint-a11y-labels.sh
#
# Warn-only lint: checks that Image and Container elements with actions
# carry an accessibility block in schema/examples/ JSON files.
#
# Exit 0 always (warn-only). Warnings are printed to stderr.

set -euo pipefail
EXAMPLES_DIR="$(cd "$(dirname "$0")/../schema/examples" && pwd)"
WARN_COUNT=0

if ! command -v jq &>/dev/null; then
  echo "WARN: jq not installed — skipping a11y lint" >&2
  exit 0
fi

for file in "$EXAMPLES_DIR"/*.json; do
  [ -f "$file" ] || continue
  basename="$(basename "$file")"

  # Find Image elements that have actions but no accessibility.label
  images_missing=$(jq -r '
    [.. | objects | select(.type == "Image" and .actions != null and (.accessibility.label == null))]
    | length
  ' "$file" 2>/dev/null || echo "0")

  if [ "$images_missing" -gt 0 ]; then
    echo "WARN: $basename has $images_missing Image element(s) with actions but no accessibility.label" >&2
    WARN_COUNT=$((WARN_COUNT + images_missing))
  fi

  # Find Container elements that have actions but no accessibility.label
  containers_missing=$(jq -r '
    [.. | objects | select(.type == "Container" and .actions != null and (.accessibility.label == null))]
    | length
  ' "$file" 2>/dev/null || echo "0")

  if [ "$containers_missing" -gt 0 ]; then
    echo "WARN: $basename has $containers_missing Container element(s) with actions but no accessibility.label" >&2
    WARN_COUNT=$((WARN_COUNT + containers_missing))
  fi
done

if [ "$WARN_COUNT" -gt 0 ]; then
  echo "" >&2
  echo "a11y lint: $WARN_COUNT element(s) with actions missing accessibility.label (warn-only)" >&2
else
  echo "a11y lint: all actionable elements in schema/examples/ have accessibility labels" >&2
fi

exit 0
