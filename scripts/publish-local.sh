#!/usr/bin/env bash
# Publishes all artifacts to the local Maven cache for a given Kotlin version.
#
# Usage:
#   ./scripts/publish-local.sh <kotlin-version>
#
# Example:
#   ./scripts/publish-local.sh 2.1.20
#   ./scripts/publish-local.sh 2.2.0
#   ./scripts/publish-local.sh 2.3.20

set -euo pipefail

LIBS_TOML="gradle/libs.toml"

# ── Argument validation ───────────────────────────────────────────────────────

if [[ $# -ne 1 ]]; then
  echo "Usage: $0 <kotlin-version>" >&2
  echo "Example: $0 2.3.20" >&2
  exit 1
fi

KOTLIN_VERSION="$1"

if [[ ! "$KOTLIN_VERSION" =~ ^[0-9]+\.[0-9]+(\.[0-9]+)?$ ]]; then
  echo "Error: '$KOTLIN_VERSION' does not look like a valid Kotlin version (e.g. 2.3.20)" >&2
  exit 1
fi

if [[ ! -f "$LIBS_TOML" ]]; then
  echo "Error: $LIBS_TOML not found. Run this script from the repository root." >&2
  exit 1
fi

# ── Save & restore original version ──────────────────────────────────────────

ORIGINAL_VERSION=$(grep '^kotlin = ' "$LIBS_TOML" | sed 's/kotlin = "\(.*\)"/\1/')

restore() {
  echo ""
  echo "Restoring Kotlin version to $ORIGINAL_VERSION..."
  sed -i '' "s/^kotlin = \".*\"/kotlin = \"$ORIGINAL_VERSION\"/" "$LIBS_TOML"
}

trap restore EXIT

# ── Switch version ────────────────────────────────────────────────────────────

if [[ "$KOTLIN_VERSION" != "$ORIGINAL_VERSION" ]]; then
  echo "Switching Kotlin version: $ORIGINAL_VERSION → $KOTLIN_VERSION"
  sed -i '' "s/^kotlin = \".*\"/kotlin = \"$KOTLIN_VERSION\"/" "$LIBS_TOML"
else
  echo "Kotlin version is already $KOTLIN_VERSION"
fi

# ── Publish ───────────────────────────────────────────────────────────────────

echo ""
echo "Publishing all artifacts for Kotlin $KOTLIN_VERSION to local Maven..."
echo ""

./gradlew \
  :pika-api:publishToMavenLocal \
  :pika-compiler:publishToMavenLocal \
  :pika-gradle:publishToMavenLocal \
  --no-configuration-cache

echo ""
echo "Done. Artifacts published to ~/.m2/repository/io/github/lukmccall/pika/"
