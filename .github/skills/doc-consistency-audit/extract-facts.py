#!/usr/bin/env python3
"""Extract SDUI schema facts for doc-consistency-audit.

Run from project root:
    python3 .github/skills/doc-consistency-audit/extract-facts.py
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
