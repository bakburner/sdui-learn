#!/usr/bin/env python3
"""
Generates platform-specific LayoutTokenRegistry sources from token schemas.
"""

from __future__ import annotations

import json
from pathlib import Path
from typing import Any


ROOT = Path(__file__).resolve().parents[2]
SCHEMA_DIR = ROOT / "schema"

SPACING_SCHEMA = SCHEMA_DIR / "spacing-tokens.json"
RADIUS_SCHEMA = SCHEMA_DIR / "corner-radius-tokens.json"
TYPOGRAPHY_SCHEMA = SCHEMA_DIR / "typography-tokens.json"
MOTION_SCHEMA = SCHEMA_DIR / "motion-tokens.json"
SHADOW_SCHEMA = SCHEMA_DIR / "shadow-tokens.json"

IOS_OUT = ROOT / "ios/Sources/SduiCore/Generated/LayoutTokenRegistry.swift"
ANDROID_OUT = ROOT / "android/sdui-core/src/main/java/com/nba/sdui/core/generated/LayoutTokenRegistry.kt"
WEB_OUT = ROOT / "web/src/generated/LayoutTokenRegistry.ts"


def read_json(path: Path) -> dict[str, Any]:
    with path.open("r", encoding="utf-8") as file:
        return json.load(file)


def format_swift_matrix(values: dict[str, int]) -> str:
    return (
        "FormFactorMatrix("
        f"phone: {values['phone']}, tablet: {values['tablet']}, "
        f"tv: {values['tv']}, web: {values['web']})"
    )


def format_swift_web_size(value: Any) -> str:
    if isinstance(value, int):
        return f".scalar({value})"
    return (
        ".envelope("
        "WebSizeEnvelope("
        f"min: {value['min']}, max: {value['max']}, "
        f"minVw: {value['minVw']}, maxVw: {value['maxVw']}))"
    )


def generate_swift(
    spacing: dict[str, dict[str, int]],
    radius: dict[str, dict[str, int]],
    typography_categories: dict[str, dict[str, Any]],
    typography_variants: dict[str, dict[str, Any]],
    motion_duration: dict[str, dict[str, int]],
    motion_easing: dict[str, str],
    shadows: dict[str, dict[str, Any]],
) -> str:
    lines: list[str] = [
        "// THIS FILE IS GENERATED. DO NOT EDIT.",
        "",
        "import Foundation",
        "",
        "public struct FormFactorMatrix<T> {",
        "    public let phone: T",
        "    public let tablet: T",
        "    public let tv: T",
        "    public let web: T",
        "",
        "    public init(phone: T, tablet: T, tv: T, web: T) {",
        "        self.phone = phone",
        "        self.tablet = tablet",
        "        self.tv = tv",
        "        self.web = web",
        "    }",
        "}",
        "",
        "public struct WebSizeEnvelope {",
        "    public let min: Int",
        "    public let max: Int",
        "    public let minVw: Int",
        "    public let maxVw: Int",
        "}",
        "",
        "public enum WebSize {",
        "    case scalar(Int)",
        "    case envelope(WebSizeEnvelope)",
        "}",
        "",
        "public struct TypographySize {",
        "    public let phone: Int",
        "    public let tablet: Int",
        "    public let tv: Int",
        "    public let web: WebSize",
        "}",
        "",
        "public struct TypographyCategorySpec {",
        "    public let familyRef: String",
        "    public let weight: Int",
        "    public let textCase: String",
        "    public let lineHeight: Double",
        "}",
        "",
        "public struct TypographyVariantSpec {",
        "    public let categoryRef: String",
        "    public let size: TypographySize",
        "}",
        "",
        "public struct ShadowSpec {",
        "    public let type: String",
        "    public let color: String",
        "    public let radius: Int",
        "    public let offsetX: Int",
        "    public let offsetY: Int",
        "}",
        "",
        "public enum LayoutTokenRegistry {",
        "    public static let spacing: [String: FormFactorMatrix<Int>] = [",
    ]
    for token, values in spacing.items():
        lines.append(f'        "{token}": {format_swift_matrix(values)},')
    lines.extend(["    ]", "", "    public static let radius: [String: FormFactorMatrix<Int>] = ["])
    for token, values in radius.items():
        lines.append(f'        "{token}": {format_swift_matrix(values)},')
    lines.extend(
        [
            "    ]",
            "",
            "    public static let typographyCategories: [String: TypographyCategorySpec] = [",
        ]
    )
    for token, value in typography_categories.items():
        lines.append(
            "        "
            f'"{token}": TypographyCategorySpec('
            f'familyRef: "{value["familyRef"]}", '
            f'weight: {value["weight"]}, '
            f'textCase: "{value["textCase"]}", '
            f'lineHeight: {value["lineHeight"]}),'
        )
    lines.extend(
        [
            "    ]",
            "",
            "    public static let typographyVariants: [String: TypographyVariantSpec] = [",
        ]
    )
    for token, value in typography_variants.items():
        size = value["size"]
        lines.append(
            "        "
            f'"{token}": TypographyVariantSpec('
            f'categoryRef: "{value["categoryRef"]}", '
            "size: TypographySize("
            f'phone: {size["phone"]}, '
            f'tablet: {size["tablet"]}, '
            f'tv: {size["tv"]}, '
            f'web: {format_swift_web_size(size["web"])})),'
        )
    lines.extend(["    ]", "", "    public static let motionDuration: [String: FormFactorMatrix<Int>] = ["])
    for token, values in motion_duration.items():
        lines.append(f'        "{token}": {format_swift_matrix(values)},')
    lines.extend(["    ]", "", "    public static let motionEasing: [String: String] = ["])
    for token, value in motion_easing.items():
        lines.append(f'        "{token}": "{value}",')
    lines.extend(["    ]", "", "    public static let shadows: [String: ShadowSpec] = ["])
    for token, value in shadows.items():
        lines.append(
            "        "
            f'"{token}": ShadowSpec('
            f'type: "{value["type"]}", '
            f'color: "{value["color"]}", '
            f'radius: {value["radius"]}, '
            f'offsetX: {value["offsetX"]}, '
            f'offsetY: {value["offsetY"]}),'
        )
    lines.extend(["    ]", "}"])
    return "\n".join(lines) + "\n"


def format_kotlin_matrix(values: dict[str, int]) -> str:
    return (
        "FormFactorMatrix("
        f"phone = {values['phone']}, tablet = {values['tablet']}, "
        f"tv = {values['tv']}, web = {values['web']})"
    )


def format_kotlin_web_size(value: Any) -> str:
    if isinstance(value, int):
        return f"WebSize.Scalar({value})"
    return (
        "WebSize.Envelope("
        f"min = {value['min']}, max = {value['max']}, "
        f"minVw = {value['minVw']}, maxVw = {value['maxVw']})"
    )


def generate_kotlin(
    spacing: dict[str, dict[str, int]],
    radius: dict[str, dict[str, int]],
    typography_categories: dict[str, dict[str, Any]],
    typography_variants: dict[str, dict[str, Any]],
    motion_duration: dict[str, dict[str, int]],
    motion_easing: dict[str, str],
    shadows: dict[str, dict[str, Any]],
) -> str:
    lines: list[str] = [
        "// THIS FILE IS GENERATED. DO NOT EDIT.",
        "",
        "package com.nba.sdui.core.generated",
        "",
        "data class FormFactorMatrix<T>(",
        "    val phone: T,",
        "    val tablet: T,",
        "    val tv: T,",
        "    val web: T,",
        ")",
        "",
        "data class WebSizeEnvelope(",
        "    val min: Int,",
        "    val max: Int,",
        "    val minVw: Int,",
        "    val maxVw: Int,",
        ")",
        "",
        "sealed class WebSize {",
        "    data class Scalar(val value: Int) : WebSize()",
        "    data class Envelope(val min: Int, val max: Int, val minVw: Int, val maxVw: Int) : WebSize()",
        "}",
        "",
        "data class TypographySize(",
        "    val phone: Int,",
        "    val tablet: Int,",
        "    val tv: Int,",
        "    val web: WebSize,",
        ")",
        "",
        "data class TypographyCategorySpec(",
        "    val familyRef: String,",
        "    val weight: Int,",
        "    val textCase: String,",
        "    val lineHeight: Double,",
        ")",
        "",
        "data class TypographyVariantSpec(",
        "    val categoryRef: String,",
        "    val size: TypographySize,",
        ")",
        "",
        "data class ShadowSpec(",
        "    val type: String,",
        "    val color: String,",
        "    val radius: Int,",
        "    val offsetX: Int,",
        "    val offsetY: Int,",
        ")",
        "",
        "object LayoutTokenRegistry {",
        "    val spacing: Map<String, FormFactorMatrix<Int>> = linkedMapOf(",
    ]
    for token, values in spacing.items():
        lines.append(f'        "{token}" to {format_kotlin_matrix(values)},')
    lines.extend(["    )", "", "    val radius: Map<String, FormFactorMatrix<Int>> = linkedMapOf("])
    for token, values in radius.items():
        lines.append(f'        "{token}" to {format_kotlin_matrix(values)},')
    lines.extend(["    )", "", "    val typographyCategories: Map<String, TypographyCategorySpec> = linkedMapOf("])
    for token, value in typography_categories.items():
        lines.append(
            "        "
            f'"{token}" to TypographyCategorySpec('
            f'familyRef = "{value["familyRef"]}", '
            f'weight = {value["weight"]}, '
            f'textCase = "{value["textCase"]}", '
            f'lineHeight = {value["lineHeight"]}),'
        )
    lines.extend(["    )", "", "    val typographyVariants: Map<String, TypographyVariantSpec> = linkedMapOf("])
    for token, value in typography_variants.items():
        size = value["size"]
        lines.append(
            "        "
            f'"{token}" to TypographyVariantSpec('
            f'categoryRef = "{value["categoryRef"]}", '
            "size = TypographySize("
            f'phone = {size["phone"]}, '
            f'tablet = {size["tablet"]}, '
            f'tv = {size["tv"]}, '
            f'web = {format_kotlin_web_size(size["web"])})),'
        )
    lines.extend(["    )", "", "    val motionDuration: Map<String, FormFactorMatrix<Int>> = linkedMapOf("])
    for token, values in motion_duration.items():
        lines.append(f'        "{token}" to {format_kotlin_matrix(values)},')
    lines.extend(["    )", "", "    val motionEasing: Map<String, String> = linkedMapOf("])
    for token, value in motion_easing.items():
        lines.append(f'        "{token}" to "{value}",')
    lines.extend(["    )", "", "    val shadows: Map<String, ShadowSpec> = linkedMapOf("])
    for token, value in shadows.items():
        lines.append(
            "        "
            f'"{token}" to ShadowSpec('
            f'type = "{value["type"]}", '
            f'color = "{value["color"]}", '
            f'radius = {value["radius"]}, '
            f'offsetX = {value["offsetX"]}, '
            f'offsetY = {value["offsetY"]}),'
        )
    lines.extend(["    )", "}"])
    return "\n".join(lines) + "\n"


def format_ts_web_size(value: Any) -> str:
    if isinstance(value, int):
        return str(value)
    return (
        "{ "
        f"min: {value['min']}, max: {value['max']}, "
        f"minVw: {value['minVw']}, maxVw: {value['maxVw']} "
        "}"
    )


def generate_typescript(
    spacing: dict[str, dict[str, int]],
    radius: dict[str, dict[str, int]],
    typography_categories: dict[str, dict[str, Any]],
    typography_variants: dict[str, dict[str, Any]],
    motion_duration: dict[str, dict[str, int]],
    motion_easing: dict[str, str],
    shadows: dict[str, dict[str, Any]],
) -> str:
    lines: list[str] = [
        "// THIS FILE IS GENERATED. DO NOT EDIT.",
        "",
        "export type FormFactorMatrix<T extends number = number> = {",
        "  phone: T;",
        "  tablet: T;",
        "  tv: T;",
        "  web: T;",
        "};",
        "",
        "export type WebSizeEnvelope = {",
        "  min: number;",
        "  max: number;",
        "  minVw: number;",
        "  maxVw: number;",
        "};",
        "",
        "export type WebSize = number | WebSizeEnvelope;",
        "",
        "export type TypographySize = {",
        "  phone: number;",
        "  tablet: number;",
        "  tv: number;",
        "  web: WebSize;",
        "};",
        "",
        "export type TypographyCategorySpec = {",
        "  familyRef: string;",
        "  weight: number;",
        "  textCase: string;",
        "  lineHeight: number;",
        "};",
        "",
        "export type TypographyVariantSpec = {",
        "  categoryRef: string;",
        "  size: TypographySize;",
        "};",
        "",
        "export type ShadowSpec = {",
        "  type: string;",
        "  color: string;",
        "  radius: number;",
        "  offsetX: number;",
        "  offsetY: number;",
        "};",
        "",
        "export const LayoutTokenRegistry = {",
        "  spacing: {",
    ]
    for token, values in spacing.items():
        lines.append(
            f'    "{token}": {{ phone: {values["phone"]}, tablet: {values["tablet"]}, tv: {values["tv"]}, web: {values["web"]} }},'
        )
    lines.extend(["  },", "", "  radius: {"])
    for token, values in radius.items():
        lines.append(
            f'    "{token}": {{ phone: {values["phone"]}, tablet: {values["tablet"]}, tv: {values["tv"]}, web: {values["web"]} }},'
        )
    lines.extend(["  },", "", "  typographyCategories: {"])
    for token, value in typography_categories.items():
        lines.append(
            "    "
            f'"{token}": {{ '
            f'familyRef: "{value["familyRef"]}", '
            f'weight: {value["weight"]}, '
            f'textCase: "{value["textCase"]}", '
            f'lineHeight: {value["lineHeight"]} '
            "},"
        )
    lines.extend(["  },", "", "  typographyVariants: {"])
    for token, value in typography_variants.items():
        size = value["size"]
        lines.append(
            "    "
            f'"{token}": {{ '
            f'categoryRef: "{value["categoryRef"]}", '
            "size: { "
            f'phone: {size["phone"]}, '
            f'tablet: {size["tablet"]}, '
            f'tv: {size["tv"]}, '
            f'web: {format_ts_web_size(size["web"])} '
            "} "
            "},"
        )
    lines.extend(["  },", "", "  motionDuration: {"])
    for token, values in motion_duration.items():
        lines.append(
            f'    "{token}": {{ phone: {values["phone"]}, tablet: {values["tablet"]}, tv: {values["tv"]}, web: {values["web"]} }},'
        )
    lines.extend(["  },", "", "  motionEasing: {"])
    for token, value in motion_easing.items():
        lines.append(f'    "{token}": "{value}",')
    lines.extend(["  },", "", "  shadows: {"])
    for token, value in shadows.items():
        lines.append(
            "    "
            f'"{token}": {{ '
            f'type: "{value["type"]}", '
            f'color: "{value["color"]}", '
            f'radius: {value["radius"]}, '
            f'offsetX: {value["offsetX"]}, '
            f'offsetY: {value["offsetY"]} '
            "},"
        )
    lines.extend(["  },", "} as const;"])
    return "\n".join(lines) + "\n"


def write_file(path: Path, content: str) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(content, encoding="utf-8")


def main() -> None:
    spacing_data = read_json(SPACING_SCHEMA)
    radius_data = read_json(RADIUS_SCHEMA)
    typography_data = read_json(TYPOGRAPHY_SCHEMA)
    motion_data = read_json(MOTION_SCHEMA)
    shadow_data = read_json(SHADOW_SCHEMA)

    spacing = spacing_data["spacing"]
    radius = radius_data["radius"]
    typography_categories = typography_data["categories"]
    typography_variants = typography_data["variants"]
    motion_duration = motion_data["duration"]
    motion_easing = motion_data["easing"]
    shadows = shadow_data["shadows"]

    write_file(
        IOS_OUT,
        generate_swift(
            spacing=spacing,
            radius=radius,
            typography_categories=typography_categories,
            typography_variants=typography_variants,
            motion_duration=motion_duration,
            motion_easing=motion_easing,
            shadows=shadows,
        ),
    )
    write_file(
        ANDROID_OUT,
        generate_kotlin(
            spacing=spacing,
            radius=radius,
            typography_categories=typography_categories,
            typography_variants=typography_variants,
            motion_duration=motion_duration,
            motion_easing=motion_easing,
            shadows=shadows,
        ),
    )
    write_file(
        WEB_OUT,
        generate_typescript(
            spacing=spacing,
            radius=radius,
            typography_categories=typography_categories,
            typography_variants=typography_variants,
            motion_duration=motion_duration,
            motion_easing=motion_easing,
            shadows=shadows,
        ),
    )


if __name__ == "__main__":
    main()
