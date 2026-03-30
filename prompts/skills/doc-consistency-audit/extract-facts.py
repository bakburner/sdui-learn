#!/usr/bin/env python3
"""Extract SDUI schema facts for doc-consistency-audit.

Run from project root:
    python3 prompts/skills/doc-consistency-audit/extract-facts.py
"""
import json
import sys
from pathlib import Path

root = Path(__file__).resolve().parents[3]

schema = json.load(open(root / "schema/sdui-schema.json"))
defs = schema.get("definitions", {})

section_def = defs.get("Section", {})
section_type_enum = section_def.get("properties", {}).get("type", {}).get("enum", [])
action_type_enum = defs.get("ActionType", {}).get("enum", [])
ae = defs.get("AtomicElement", {})
ae_type_enum = ae.get("properties", {}).get("type", {}).get("enum", [])
data_one_of = section_def.get("properties", {}).get("data", {}).get("oneOf", [])
data_refs = [item.get("$ref", "").split("/")[-1] for item in data_one_of if "$ref" in item]
all_defs = sorted(defs.keys())

print("=== SECTION TYPES ===")
print(f"Count: {len(section_type_enum)}")
for t in sorted(section_type_enum):
    print(f"  {t}")

print("\n=== ACTION TYPES ===")
print(f"Count: {len(action_type_enum)}")
for t in sorted(action_type_enum):
    print(f"  {t}")

print("\n=== ATOMIC ELEMENT TYPES ===")
print(f"Count: {len(ae_type_enum)}")
for t in sorted(ae_type_enum):
    print(f"  {t}")

print("\n=== DATA oneOf REFS ===")
print(f"Count: {len(data_refs)}")
for r in sorted(data_refs):
    print(f"  {r}")

print(f"\n=== ALL DEFINITIONS ({len(all_defs)}) ===")
for d in all_defs:
    print(f"  {d}")

# sdui-all-types.json
all_types = json.load(open(root / "schema/sdui-all-types.json"))
all_type_keys = sorted(all_types.get("properties", {}).keys())
print(f"\n=== sdui-all-types.json PROPERTY KEYS ({len(all_type_keys)}) ===")
for k in all_type_keys:
    print(f"  {k}")

# Dangling ref check
dangling = []
for k, v in all_types.get("properties", {}).items():
    ref = v.get("$ref", "")
    def_name = ref.split("/")[-1] if ref else None
    if def_name and def_name not in defs:
        dangling.append(f"{k} -> {def_name}")

if dangling:
    print("\n!!! DANGLING REFS IN sdui-all-types.json !!!")
    for r in dangling:
        print(f"  {r}")
    sys.exit(1)
else:
    print("\nNo dangling refs in sdui-all-types.json ✓")

# --- Feature status evidence ---

print("\n=== ADR STATUSES ===")
adr_dir = root / "docs" / "adr"
import re
for adr_file in sorted(adr_dir.glob("0*.md")):
    text = adr_file.read_text()
    # Look for Status line in header (format: "- Status: Accepted" or "**Status**: Accepted")
    status_match = re.search(r'(?:^|\n)-?\s*\**Status\**[:\s]*(.+)', text, re.IGNORECASE)
    status = status_match.group(1).strip().rstrip('*').strip() if status_match else "UNKNOWN"
    print(f"  {adr_file.name}: {status}")

print("\n=== KEY IMPLEMENTATION FILES ===")
impl_checks = {
    "Request envelope (server)": "server/src/main/java/com/nba/sdui/request/SduiRequestContext.java",
    "Request envelope (Android)": "android/sdui-core/src/main/java/com/nba/sdui/core/request/RequestEnvelopeBuilder.kt",
    "Request envelope (web)": "web/src/request/RequestEnvelopeBuilder.ts",
    "i18n stringTable (schema)": None,  # checked differently
    "i18n stampStringTable (server)": None,  # checked differently
}

for label, rel_path in impl_checks.items():
    if rel_path:
        full = root / rel_path
        exists = full.exists() and full.stat().st_size > 100
        print(f"  {label}: {'EXISTS' if exists else 'MISSING'} ({rel_path})")

# Check stringTable in schema
section_props = section_def.get("properties", {})
has_string_table = "stringTable" in section_props
print(f"  i18n stringTable on Section schema: {'YES' if has_string_table else 'NO'}")

# Check stampStringTableOnSections in SduiUtils
utils_path = root / "server/src/main/java/com/nba/sdui/service/SduiUtils.java"
if utils_path.exists():
    has_stamp = "stampStringTableOnSections" in utils_path.read_text()
    print(f"  i18n stampStringTableOnSections in SduiUtils: {'YES' if has_stamp else 'NO'}")
else:
    print(f"  i18n SduiUtils.java: MISSING")

# Check experiment resolution in composition service
comp_path = root / "server/src/main/java/com/nba/sdui/service/SduiCompositionService.java"
if comp_path.exists():
    comp_text = comp_path.read_text()
    has_experiment = "experiment" in comp_text.lower()
    print(f"  Experiment resolution in SduiCompositionService: {'YES' if has_experiment else 'NO'}")
else:
    print(f"  SduiCompositionService.java: MISSING")

print("\n=== FEATURE STATUS TRUTH (derived from code) ===")
# Derive ground truth from above checks
status_truth = []

# Request envelope
req_server = (root / impl_checks["Request envelope (server)"]).exists()
req_android = (root / impl_checks["Request envelope (Android)"]).exists()
req_web = (root / impl_checks["Request envelope (web)"]).exists()
if req_server and req_android and req_web:
    status_truth.append(("Request context envelope", "Built"))
elif req_server:
    status_truth.append(("Request context envelope", "Partial"))
else:
    status_truth.append(("Request context envelope", "Gap"))

# i18n
if has_string_table and utils_path.exists() and "stampStringTableOnSections" in utils_path.read_text():
    status_truth.append(("Internationalization (i18n)", "Built"))
elif has_string_table:
    status_truth.append(("Internationalization (i18n)", "Partial"))
else:
    status_truth.append(("Internationalization (i18n)", "Gap"))

# Experiments
if comp_path.exists() and "experiment" in comp_path.read_text().lower():
    status_truth.append(("Experiment/A/B testing", "Built"))
else:
    status_truth.append(("Experiment/A/B testing", "Gap"))

for feature, status in status_truth:
    print(f"  {feature}: {status}")
