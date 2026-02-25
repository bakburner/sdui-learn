#!/bin/bash

# SDUI Multi-Platform Code Generation Script
# Generates typed models from the JSON Schema for multiple platforms

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SCHEMA_FILE="$SCRIPT_DIR/../schema/sdui-schema.json"
OUTPUT_DIR="$SCRIPT_DIR/output"

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
mkdir -p "$OUTPUT_DIR/swift"
mkdir -p "$OUTPUT_DIR/typescript"

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

echo "2. Generating Swift models..."
quicktype \
    --src "$SCHEMA_FILE" \
    --src-lang schema \
    --lang swift \
    --struct-or-class struct \
    --swift-5-support \
    --out "$OUTPUT_DIR/swift/SduiModels.swift" \
    2>/dev/null || echo "   Swift generation completed with warnings"

echo "3. Generating TypeScript models..."
quicktype \
    --src "$SCHEMA_FILE" \
    --src-lang schema \
    --lang typescript \
    --just-types \
    --out "$OUTPUT_DIR/typescript/SduiModels.ts" \
    2>/dev/null || echo "   TypeScript generation completed with warnings"

echo ""
echo "4. Generating Java POJOs (via jsonschema2pojo)..."
cd "$SCRIPT_DIR"
./gradlew generateJsonSchema2Pojo --quiet

echo ""
echo "=== Generation Complete ==="
echo ""
echo "Output locations:"
echo "  Java (primary):     $SCRIPT_DIR/build/generated-sources/jsonschema2pojo/"
echo "  Kotlin (quicktype): $OUTPUT_DIR/kotlin/"
echo "  Swift (demo):       $OUTPUT_DIR/swift/"
echo "  TypeScript (demo):  $OUTPUT_DIR/typescript/"
echo ""
echo "For Android, use the Java POJOs from jsonschema2pojo."
echo "Swift and TypeScript outputs are for multi-platform demonstration only."
