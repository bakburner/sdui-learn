#!/usr/bin/env python3
"""One-shot test migration: adapt unit tests to runtime Tokens.

1. Rewrites `<TokenClass>.<CONST>` references to `TestTokens.INSTANCE.<method>(...)`.
2. Inserts `TestTokens.INSTANCE` arg into constructor calls for SduiUtils,
   SectionSurfaces, AtomicCompositeBuilder, and every composer (after
   `surfaces`).
3. Drops imports of the deleted constant classes; adds TestTokens import
   when needed.
"""
from __future__ import annotations
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
TEST_ROOT = ROOT / "server/src/test/java/com/nba/sdui"

# Reuse the same tables from the main migration script.
sys.path.insert(0, str(ROOT / "scripts"))
from migrate_tokens_to_runtime import CLASSES, IMPORT_LINES  # type: ignore

RECEIVER = "TestTokens.INSTANCE"

COMPOSER_NAMES = (
    "Demo(?:Screen)?|ForYou|GameDetail|Live|Scoreboard|Watch|Schedule|Home"
)


def rewrite_constants(text: str) -> int:
    n = 0
    for cls, mapping in CLASSES:
        for name, expr in mapping.items():
            pattern = re.compile(r"\b" + cls + r"\." + name + r"\b")
            text2, c = pattern.subn(RECEIVER + "." + expr, text)
            if c:
                n += c
                text = text2
    return text, n


def add_tokens_arg_simple(text: str, ctor: str) -> tuple[str, int]:
    """Insert `, TestTokens.INSTANCE` at the end of the args list of `new <ctor>(...)`."""
    # Match `new <ctor>(` then capture balanced parens (no nesting in our calls).
    pattern = re.compile(r"\bnew " + ctor + r"\(([^)]*?)\)")
    count = 0

    def sub(m: re.Match) -> str:
        nonlocal count
        args = m.group(1)
        if "TestTokens.INSTANCE" in args:
            return m.group(0)
        count += 1
        return "new " + ctor + "(" + args + ", " + RECEIVER + ")"

    text = pattern.sub(sub, text)
    return text, count


def add_tokens_arg_after_surfaces(text: str) -> tuple[str, int]:
    """Insert `, TestTokens.INSTANCE` immediately after `surfaces` in composer ctors.

    Walks balanced parens manually so nested calls like `mock(Foo.class)` and
    `new StatsApiAdapter(client)` inside the composer ctor are handled.
    """
    open_re = re.compile(r"\bnew (?:" + COMPOSER_NAMES + r")Composer\(")
    out = []
    i = 0
    count = 0
    while i < len(text):
        m = open_re.search(text, i)
        if not m:
            out.append(text[i:])
            break
        out.append(text[i:m.end()])
        # Walk balanced parens to find the matching close.
        depth = 1
        j = m.end()
        ctor_start = j
        while j < len(text) and depth > 0:
            ch = text[j]
            if ch == '(':
                depth += 1
            elif ch == ')':
                depth -= 1
                if depth == 0:
                    break
            j += 1
        # text[ctor_start:j] is the args list at depth 0.
        args = text[ctor_start:j]
        # Find `surfaces` at depth 0 within args.
        new_args = _insert_tokens_after_surfaces(args)
        if new_args != args:
            count += 1
        out.append(new_args)
        out.append(text[j])  # the ')'
        i = j + 1
    return "".join(out), count


def _insert_tokens_after_surfaces(args: str) -> str:
    """Within a balanced arg list (no surrounding parens), insert
    `, TestTokens.INSTANCE` after the first depth-0 occurrence of `surfaces`.
    """
    depth = 0
    k = 0
    while k < len(args):
        ch = args[k]
        if ch == '(':
            depth += 1
        elif ch == ')':
            depth -= 1
        elif depth == 0 and args[k:k + 8] == "surfaces" and (
            k + 8 >= len(args) or not (args[k + 8].isalnum() or args[k + 8] == '_')
        ) and (k == 0 or not (args[k - 1].isalnum() or args[k - 1] == '_' or args[k - 1] == '.')):
            # Skip if TestTokens already follows.
            tail = args[k + 8:].lstrip()
            if tail.startswith(", " + RECEIVER) or tail.startswith(RECEIVER):
                return args
            return args[:k + 8] + ", " + RECEIVER + args[k + 8:]
        k += 1
    return args


def maybe_add_test_tokens_import(text: str) -> str:
    if RECEIVER not in text:
        return text
    if "import com.nba.sdui.testsupport.TestTokens;" in text:
        return text
    # Insert after package declaration / first import block. Simplest: after package line.
    return re.sub(
        r"(^package [^;]+;\n)",
        r"\1\nimport com.nba.sdui.testsupport.TestTokens;\n",
        text,
        count=1,
        flags=re.MULTILINE,
    )


def drop_old_imports(text: str) -> str:
    for line in IMPORT_LINES:
        text = re.sub(r"^" + re.escape(line) + r"\n", "", text, flags=re.MULTILINE)
    return text


def process(path: Path) -> int:
    original = path.read_text()
    text = original

    text, n_const = rewrite_constants(text)

    n_sdui = n_surf = n_acb = n_comp = 0
    text, n_sdui = add_tokens_arg_simple(text, "SduiUtils")
    text, n_surf = add_tokens_arg_simple(text, "SectionSurfaces")
    text, n_acb = add_tokens_arg_simple(text, "AtomicCompositeBuilder")
    text, n_comp = add_tokens_arg_after_surfaces(text)

    text = drop_old_imports(text)
    text = maybe_add_test_tokens_import(text)

    if text != original:
        path.write_text(text)
        total = n_const + n_sdui + n_surf + n_acb + n_comp
        print(f"  {path.relative_to(ROOT)}: const={n_const} sdui={n_sdui} surf={n_surf} acb={n_acb} comp={n_comp}")
        return total
    return 0


def main() -> int:
    total = 0
    files = 0
    for path in TEST_ROOT.rglob("*.java"):
        if path.name == "TestTokens.java":
            continue
        n = process(path)
        if n:
            files += 1
            total += n
    print(f"Total: {total} substitutions across {files} files")
    return 0


if __name__ == "__main__":
    sys.exit(main())
