#!/bin/bash

# SDUI Multi-Platform Code Generation Script
# Generates typed models from the JSON Schema for multiple platforms

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SCHEMA_FILE="$SCRIPT_DIR/../schema/sdui-schema.json"
OUTPUT_DIR="$SCRIPT_DIR/output"
IOS_MODELS_OUT="$SCRIPT_DIR/../ios/Sources/SduiCore/Models/SduiModels.swift"
WEB_MODELS_OUT="$SCRIPT_DIR/../web/src/generated/SduiModels.ts"

echo "=== SDUI Code Generation ==="
echo "Schema: $SCHEMA_FILE"
echo ""

# Check if schema exists
if [ ! -f "$SCHEMA_FILE" ]; then
    echo "Error: Schema file not found at $SCHEMA_FILE"
    exit 1
fi

# Create output directories
mkdir -p "$OUTPUT_DIR/kotlin"
mkdir -p "$(dirname "$IOS_MODELS_OUT")"
mkdir -p "$(dirname "$WEB_MODELS_OUT")"

# Check if quicktype is installed
if ! command -v quicktype &> /dev/null; then
    echo "Warning: quicktype not found. Install with: npm install -g quicktype"
    echo "Skipping Swift and TypeScript generation."
    echo ""
    echo "Running Java/Kotlin generation via Gradle..."
    cd "$SCRIPT_DIR"
    ./gradlew generateJsonSchema2Pojo
    echo ""
    echo "Java POJOs generated in: $SCRIPT_DIR/build/generated-sources/jsonschema2pojo/"
    exit 0
fi

echo "1. Generating Kotlin models (via quicktype)..."
quicktype \
    --src "$SCHEMA_FILE" \
    --src-lang schema \
    --lang kotlin \
    --package com.nba.sdui.models.quicktype \
    --framework kotlinx-serialization \
    --out "$OUTPUT_DIR/kotlin/SduiModels.kt" \
    2>/dev/null || echo "   Kotlin generation completed with warnings"

echo "2. Generating Swift models (iOS SduiCore)..."
# Writes directly into the iOS SwiftPM source tree. The iOS client
# consumes this file as its authoritative model layer; there is no
# intermediate copy. Renderer-level helper enums (TextVariant,
# ButtonVariant, ContainerVariant, ImageVariant) are hand-written
# alongside the renderers in ios/Sources/SduiCore/Rendering/*Resolver.swift
# and are intentionally not part of this generated file.
quicktype \
    --src "$SCHEMA_FILE" \
    --src-lang schema \
    --lang swift \
    --struct-or-class struct \
    --swift-5-support \
    --out "$IOS_MODELS_OUT" \
    2>/dev/null || echo "   Swift generation completed with warnings"

echo "3. Generating TypeScript models (web src tree)..."
# Writes directly into web/src/generated/. The web client consumes this
# file through the '@sdui/models' Vite/tsconfig alias; there is no
# intermediate copy. Renderer-level helper enums and resolvers
# (ColorTokenResolver, ContainerVariantResolver, ImageVariantResolver,
# ...) are hand-written under web/src/utils/ and are intentionally not
# part of this generated file.
quicktype \
    --src "$SCHEMA_FILE" \
    --src-lang schema \
    --lang typescript \
    --just-types \
    --out "$WEB_MODELS_OUT" \
    2>/dev/null || echo "   TypeScript generation completed with warnings"

echo ""
echo "4. Post-processing Swift models (iOS lenient routing types)..."
# Strip the strict `String, Codable` enums that quicktype emits for
# the two routing-type fields — `Section.type` (quicktype name:
# `OverlayType`) and `AtomicElement.type` (quicktype name: `UIType`) —
# and rewrite both fields as plain `String`.
#
# Why this exists (cross-platform parity):
#   - Android's jsonschema2pojo emits both fields as plain `String`,
#     so the Android routers silently skip unknown types with a log
#     (forward-compat by default).
#   - Web consumes quicktype's TypeScript enums, but TypeScript string
#     enums are erased at runtime; each router's `default:` branch
#     naturally catches unknown wire values.
#   - Quicktype Swift, alone among the three, emits runtime-strict
#     `String, Codable` enums. A wire value outside either enum fails
#     the containing struct's `init(from:)` — for `Section.type` that
#     kills the entire screen decode, and for `AtomicElement.type`
#     that kills the entire atomic subtree it appears in. iOS is the
#     only platform where unknown routing values are decode-fatal.
#
# Rewriting `OverlayType` and `UIType` to `String` aligns the iOS
# runtime shape with Android and web so `SectionRouter` and
# `AtomicRouter` can switch on string literals with a `default:`
# branch, matching the other two clients. Applied to the generated
# file before it reaches the SwiftPM target, so no hand edits are
# needed in ios/Sources/SduiCore/Models/.
#
# TODO: replace this sed with a first-class solution. Options:
#   - A quicktype flag that targets specific fields (today quicktype
#     only offers `--no-enums` globally, which would also strip
#     legitimately-strict enums like Direction, Alignment, ScaleType).
#   - A schema change that drops the inline `enum: [...]` on the two
#     routing-type fields in favour of a description-documented
#     vocabulary, so all three codegens natively emit String. This
#     route removes the need for any post-processing.
TMP_SWIFT="$(mktemp)"
sed '/^enum OverlayType:/,/^}/d
     /^enum UIType:/,/^}/d
     s/OverlayType/String/g
     s/UIType/String/g' \
    "$IOS_MODELS_OUT" > "$TMP_SWIFT" && mv "$TMP_SWIFT" "$IOS_MODELS_OUT"

echo ""
echo "5. Generating Java POJOs (via jsonschema2pojo)..."
cd "$SCRIPT_DIR"
./gradlew generateJsonSchema2Pojo --quiet

echo ""
echo "=== Generation Complete ==="
echo ""
echo "Output locations:"
echo "  Java (Android):     $SCRIPT_DIR/build/generated-sources/jsonschema2pojo/"
echo "  Swift (iOS):        $IOS_MODELS_OUT"
echo "  TypeScript (web):   $WEB_MODELS_OUT"
echo "  Kotlin (demo):      $OUTPUT_DIR/kotlin/SduiModels.kt"
echo ""
echo "Every client consumes its generated model file in place — there is no"
echo "copy step. Android picks up the Java POJOs from the Gradle classpath,"
echo "iOS includes SduiModels.swift in the SduiCore SwiftPM target, and web"
echo "resolves the TypeScript file through the '@sdui/models' Vite alias."
echo "The Kotlin quicktype output is demonstration-only (Android uses the"
echo "Java POJOs)."
