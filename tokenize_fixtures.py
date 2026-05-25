#!/usr/bin/env python3
"""Tokenize raw integer values in fixture JSON files that match design token tiers."""
import json
import os
import shutil

SPACING_TIERS = {4: 'xs', 8: 'sm', 12: 'md', 16: 'lg', 24: 'xl', 32: 'xxl', 48: 'xxxl'}
RADIUS_TIERS = {2: 'xs', 4: 'sm', 8: 'md', 12: 'lg', 16: 'xl', 24: 'xxl', 9999: 'pill'}

SPACING_ALWAYS = {'gap'}
RADIUS_ALWAYS = {'cornerRadius'}
SPACING_CONTEXT = {'height', 'width'}
SPACING_LAYOUT = {'minWidth', 'maxWidth', 'minHeight', 'maxHeight'}

SPACER_DIVIDER_TYPES = {'Spacer', 'Divider'}
CONTENT_TYPES = {'Image', 'Icon'}

FILES = [
    'game-detail-live.json',
    'game-detail-final.json',
    'game-detail-pre.json',
    'for-you.json',
    'box-model-leaves.json',
    'feed-screen-composite.json',
    'utility-card-grid-composite.json',
    'game-schedule-row-composite.json',
]

SCHEMA_DIR = 'schema/examples'
IOS_DIR = 'ios/Tests/SduiCoreTests/Fixtures'


def tokenize(node, changes, skips, path=""):
    if not isinstance(node, dict):
        if isinstance(node, list):
            for i, item in enumerate(node):
                tokenize(item, changes, skips, f"{path}[{i}]")
        return

    elem_type = node.get('type')

    for key in list(node.keys()):
        value = node[key]
        cp = f"{path}.{key}" if path else key

        if isinstance(value, int):
            if key in SPACING_ALWAYS:
                if value in SPACING_TIERS:
                    tok = f"token:nba.spacing.{SPACING_TIERS[value]}"
                    node[key] = tok
                    changes.append((cp, value, tok, elem_type))
                else:
                    skips.append((cp, value, f"off-tier {key}", elem_type))

            elif key in RADIUS_ALWAYS:
                if value in RADIUS_TIERS:
                    tok = f"token:nba.radius.{RADIUS_TIERS[value]}"
                    node[key] = tok
                    changes.append((cp, value, tok, elem_type))
                else:
                    skips.append((cp, value, "off-tier cornerRadius", elem_type))

            elif key in SPACING_CONTEXT:
                if elem_type in SPACER_DIVIDER_TYPES:
                    if value in SPACING_TIERS:
                        tok = f"token:nba.spacing.{SPACING_TIERS[value]}"
                        node[key] = tok
                        changes.append((cp, value, tok, elem_type))
                    else:
                        skips.append((cp, value, f"off-tier {key} on {elem_type}", elem_type))
                else:
                    skips.append((cp, value, f"content {key} on {elem_type}", elem_type))

            elif key in SPACING_LAYOUT:
                if elem_type not in CONTENT_TYPES:
                    if value in SPACING_TIERS:
                        tok = f"token:nba.spacing.{SPACING_TIERS[value]}"
                        node[key] = tok
                        changes.append((cp, value, tok, elem_type))
                    else:
                        skips.append((cp, value, f"off-tier {key}", elem_type))
                else:
                    skips.append((cp, value, f"content {key} on {elem_type}", elem_type))

        if isinstance(value, dict):
            tokenize(value, changes, skips, cp)
        elif isinstance(value, list):
            for i, item in enumerate(value):
                tokenize(item, changes, skips, f"{cp}[{i}]")


def process_file(filename):
    src = os.path.join(SCHEMA_DIR, filename)
    dst = os.path.join(IOS_DIR, filename)

    with open(src, 'r') as f:
        data = json.load(f)

    changes = []
    skips = []
    tokenize(data, changes, skips)

    out = json.dumps(data, indent=2, ensure_ascii=False) + '\n'
    with open(src, 'w') as f:
        f.write(out)
    shutil.copy2(src, dst)

    return changes, skips


def main():
    for filename in FILES:
        print(f"\n{'='*70}")
        print(f"FILE: {filename}")
        print(f"{'='*70}")
        changes, skips = process_file(filename)
        print(f"\n  Tokenized: {len(changes)}")
        for cp, old, new, et in changes:
            print(f"    {cp}: {old} -> {new}  ({et})")
        print(f"\n  Kept raw: {len(skips)}")
        for cp, val, reason, et in skips:
            print(f"    {cp}: {val}  ({reason})")


main()
