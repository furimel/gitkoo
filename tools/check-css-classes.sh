#!/usr/bin/env bash
#
# Fails if a template uses a CSS class that neither primer.css nor gitkoo.css defines.
#
# The original hand-written stylesheet drifted from the templates: `rounded-full`,
# `ml-2`, `markdown-body` and five others were used on every page and defined
# nowhere, so badges were not pills and rendered Markdown had no spacing at all.
# Nothing caught it because a missing CSS class is silent. This does.
#
# Usage:  tools/check-css-classes.sh
# Exit:   0 clean, 1 undefined classes found.

set -euo pipefail

cd "$(dirname "$0")/.."

TEMPLATES="src/main/resources/templates"
CSS_DIR="src/main/resources/static/css"

# Hooks that JavaScript queries but never styles, so they need no rule.
ALLOWLIST='^(theme-toggle|htmx-.*)$'

used=$(mktemp)
defined=$(mktemp)
trap 'rm -f "$used" "$defined"' EXIT

# Literal class="..." attributes, plus the branches of th:classappend ternaries.
# For a ternary we keep only what follows the "?" - the text before it is the
# condition (e.g. "${tab == 'code'}") and its literals are values, not classes.
{
  grep -rhoE 'class="[^"]*"' "$TEMPLATES" | sed 's/^class="//; s/"$//'
  grep -rhoE 'th:classappend="[^"]*"' "$TEMPLATES" \
    | sed 's/^th:classappend="//; s/"$//' \
    | grep -F '?' | sed 's/^[^?]*?//' \
    | grep -oE "'[^']*'" | tr -d "'"
} \
  | tr ' ' '\n' \
  | grep -vE '^\$?\{' \
  | grep -oE '^[A-Za-z][A-Za-z0-9_-]*$' \
  | sort -u > "$used"

# Every class selector defined anywhere in the stylesheets.
cat "$CSS_DIR"/*.css \
  | grep -oE '\.[A-Za-z][A-Za-z0-9_-]*' \
  | sed 's/^\.//' \
  | sort -u > "$defined"

missing=$(comm -23 "$used" "$defined" | grep -vE "$ALLOWLIST" || true)

if [ -n "$missing" ]; then
  echo "Undefined CSS classes used in templates:"
  echo "$missing" | sed 's/^/  /'
  echo
  echo "Define them in $CSS_DIR/gitkoo.css or use a Primer class that exists."
  exit 1
fi

echo "OK: every class used in $TEMPLATES is defined in $CSS_DIR."
