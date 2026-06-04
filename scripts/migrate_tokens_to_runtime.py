#!/usr/bin/env python3
"""One-shot migration: rewrite token-class constants to runtime Tokens calls.

Replaces references like `LayoutTokens.SPACING_LG` with `tokens.spacing("lg")`
throughout server/src/main and server/src/test. The receiver `tokens` must
already be in scope at every call site (constructor wiring is done manually).
"""
from __future__ import annotations
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
TARGETS = [
    ROOT / "server/src/main/java/com/nba/sdui/domain",
    ROOT / "server/src/main/java/com/nba/sdui/controller",
]

# Constant name -> replacement expression (without "tokens." prefix).
# Caller substitutes the actual receiver variable.
LAYOUT = {
    "SPACING_XS":  'spacing("xs")',
    "SPACING_SM":  'spacing("sm")',
    "SPACING_MD":  'spacing("md")',
    "SPACING_LG":  'spacing("lg")',
    "SPACING_XL":  'spacing("xl")',
    "SPACING_2XL": 'spacing("2xl")',
    "RADIUS_XS":   'radius("xs")',
    "RADIUS_SM":   'radius("sm")',
    "RADIUS_MD":   'radius("md")',
    "RADIUS_LG":   'radius("lg")',
    "RADIUS_XL":   'radius("xl")',
    "RADIUS_2XL":  'radius("2xl")',
    "RADIUS_FULL": 'radius("full")',
}
COLOR = {
    "BRAND_NBA":               'color("nba.label.accent.brand")',
    "BRAND_LIVE":              'color("nba.label.accent.live")',
    "SURFACE_CANVAS":          'color("nba.bg.primary")',
    "SURFACE_RAISED":          'color("nba.bg.secondary")',
    "SURFACE_SUNKEN":          'color("nba.bg.tertiary")',
    "SURFACE_PROMO":           'color("nba.bg.splash-screen")',
    "TEXT_PRIMARY":            'color("nba.label.primary")',
    "TEXT_SECONDARY":          'color("nba.label.secondary")',
    "TEXT_TERTIARY":           'color("nba.label.tertiary")',
    "TEXT_INVERSE":            'color("nba.label-inverted.primary")',
    "TEXT_ON_DARK_MEDIA":      'color("nba.label-dark.primary")',
    "TEXT_ON_BRAND":           'color("nba.label-dark.primary")',
    "LABEL_ACCENT_GOLD_ON_DARK": 'color("nba.color.secondary.70")',
    "SURFACE_TIER_ON_DARK":    'color("nba.color.t-white.10")',
    "TEXT_DIM_ON_DARK":        'color("nba.color.t-white.60")',
    "BORDER_DEFAULT":          'color("nba.divider.moderate")',
    "BORDER_SUBTLE":           'color("nba.divider.subtle")',
    "OVERLAY_SCRIM":           'color("nba.effect.scrim")',
    "FEEDBACK_SUCCESS":        'color("nba.color.feedback.success.50")',
    "FEEDBACK_ERROR":          'color("nba.color.feedback.error.50")',
    "FEEDBACK_WARNING":        'color("nba.color.feedback.warning.50")',
    "PALETTE_BLUE_30":         'color("nba.color.blue.30")',
    "PALETTE_BLUE_50":         'color("nba.color.blue.50")',
}
MOTION = {
    "EASING_DEFAULT":  'motionEasing("default")',
    "EASING_LINEAR":   'motionEasing("linear")',
    "DURATION_FAST":   'motionDuration("fast")',
    "DURATION_DEFAULT":'motionDuration("default")',
    "DURATION_SLOW":   'motionDuration("slow")',
    "DURATION_HERO":   'motionDuration("hero")',
}
SHADOW = {
    "SM": 'shadow("sm")',
    "MD": 'shadow("md")',
    "LG": 'shadow("lg")',
    "XL": 'shadow("xl")',
}
TYPO = {
    "DISPLAY_LARGE":  'typography("displayLarge")',
    "DISPLAY_MEDIUM": 'typography("displayMedium")',
    "DISPLAY_SMALL":  'typography("displaySmall")',
    "HEADLINE_LARGE": 'typography("headlineLarge")',
    "HEADLINE_MEDIUM":'typography("headlineMedium")',
    "HEADLINE_SMALL": 'typography("headlineSmall")',
    "TITLE_LARGE":    'typography("titleLarge")',
    "TITLE_MEDIUM":   'typography("titleMedium")',
    "TITLE_SMALL":    'typography("titleSmall")',
    "BODY_LARGE":     'typography("bodyLarge")',
    "BODY_MEDIUM":    'typography("bodyMedium")',
    "BODY_SMALL":     'typography("bodySmall")',
    "LABEL_LARGE":    'typography("labelLarge")',
    "LABEL_MEDIUM":   'typography("labelMedium")',
    "LABEL_SMALL":    'typography("labelSmall")',
    "SCORE":          'typography("score")',
}
ICON = {
    "HOME":        'icon("home")',
    "BASKETBALL":  'icon("basketball")',
    "VIDEO":       'icon("video")',
    "LEADERBOARD": 'icon("leaderboard")',
    "GRID":        'icon("grid")',
    "BACK":        'icon("back")',
    "SHARE":       'icon("share")',
    "WARNING":     'icon("warning")',
    "LOCK":        'icon("lock")',
    "PLAY":        'icon("play")',
    "MORE":        'icon("more")',
}

CLASSES = [
    ("LayoutTokens", LAYOUT),
    ("ColorTokens", COLOR),
    ("MotionTokens", MOTION),
    ("ShadowTokens", SHADOW),
    ("TypographyTokens", TYPO),
    ("IconTokens", ICON),
]

IMPORT_LINES = [
    "import com.nba.sdui.domain.tokens.ColorTokens;",
    "import com.nba.sdui.domain.tokens.IconTokens;",
    "import com.nba.sdui.domain.tokens.LayoutTokens;",
    "import com.nba.sdui.domain.tokens.MotionTokens;",
    "import com.nba.sdui.domain.tokens.ShadowTokens;",
    "import com.nba.sdui.domain.tokens.TypographyTokens;",
]

def receiver_for(text: str) -> str:
    """Return the receiver variable name for tokens calls in this file.

    Files use either `tokens` (instance field/param) or `this.tokens` —
    we always emit `tokens.<method>(...)` and rely on Java scoping.
    """
    return "tokens"

def rewrite(text: str) -> tuple[str, int]:
    n = 0
    receiver = receiver_for(text)
    for cls, mapping in CLASSES:
        for name, expr in mapping.items():
            pattern = re.compile(r"\b" + cls + r"\." + name + r"\b")
            new_text, count = pattern.subn(receiver + "." + expr, text)
            text = new_text
            n += count
    # Drop now-unused imports of the constant classes.
    for line in IMPORT_LINES:
        text = re.sub(r"^" + re.escape(line) + r"\n", "", text, flags=re.MULTILINE)
    return text, n

def main() -> int:
    changed = 0
    total = 0
    for root in TARGETS:
        for path in root.rglob("*.java"):
            # Skip the constants files themselves and the registry.
            if "/tokens/" in str(path):
                continue
            original = path.read_text()
            updated, n = rewrite(original)
            if n > 0 and updated != original:
                path.write_text(updated)
                changed += 1
                total += n
                print(f"  {path.relative_to(ROOT)}: {n} substitutions")
    print(f"Total: {total} substitutions across {changed} files")
    return 0

if __name__ == "__main__":
    sys.exit(main())
