// THIS FILE IS GENERATED. DO NOT EDIT.

import Foundation

public struct FormFactorMatrix<T> {
    public let phone: T
    public let tablet: T
    public let tv: T
    public let web: T

    public init(phone: T, tablet: T, tv: T, web: T) {
        self.phone = phone
        self.tablet = tablet
        self.tv = tv
        self.web = web
    }
}

public struct WebSizeEnvelope {
    public let min: Int
    public let max: Int
    public let minVw: Int
    public let maxVw: Int
}

public enum WebSize {
    case scalar(Int)
    case envelope(WebSizeEnvelope)
}

public struct TypographySize {
    public let phone: Int
    public let tablet: Int
    public let tv: Int
    public let web: WebSize
}

public struct TypographyCategorySpec {
    public let familyRef: String
    public let weight: Int
    public let textCase: String
    public let lineHeight: Double
}

public struct TypographyVariantSpec {
    public let categoryRef: String
    public let size: TypographySize
}

public struct ShadowSpec {
    public let type: String
    public let color: String
    public let radius: Int
    public let offsetX: Int
    public let offsetY: Int
}

public enum LayoutTokenRegistry {
    public static let spacing: [String: FormFactorMatrix<Int>] = [
        "nba.spacing.xs": FormFactorMatrix(phone: 2, tablet: 2, tv: 4, web: 2),
        "nba.spacing.sm": FormFactorMatrix(phone: 4, tablet: 6, tv: 6, web: 4),
        "nba.spacing.md": FormFactorMatrix(phone: 12, tablet: 15, tv: 18, web: 12),
        "nba.spacing.lg": FormFactorMatrix(phone: 16, tablet: 20, tv: 24, web: 16),
        "nba.spacing.xl": FormFactorMatrix(phone: 32, tablet: 40, tv: 48, web: 32),
        "nba.spacing.2xl": FormFactorMatrix(phone: 40, tablet: 48, tv: 56, web: 40),
    ]

    public static let radius: [String: FormFactorMatrix<Int>] = [
        "nba.radius.xs": FormFactorMatrix(phone: 2, tablet: 2, tv: 2, web: 2),
        "nba.radius.sm": FormFactorMatrix(phone: 4, tablet: 4, tv: 4, web: 4),
        "nba.radius.md": FormFactorMatrix(phone: 12, tablet: 12, tv: 12, web: 12),
        "nba.radius.lg": FormFactorMatrix(phone: 16, tablet: 16, tv: 16, web: 16),
        "nba.radius.xl": FormFactorMatrix(phone: 24, tablet: 24, tv: 24, web: 24),
        "nba.radius.2xl": FormFactorMatrix(phone: 32, tablet: 32, tv: 32, web: 32),
        "nba.radius.full": FormFactorMatrix(phone: 9999, tablet: 9999, tv: 9999, web: 9999),
    ]

    public static let typographyCategories: [String: TypographyCategorySpec] = [
        "nba.typography.headline": TypographyCategorySpec(familyRef: "nba.font.knockout", weight: 360, textCase: "uppercase", lineHeight: 0.8),
        "nba.typography.display": TypographyCategorySpec(familyRef: "nba.font.knockout", weight: 395, textCase: "uppercase", lineHeight: 0.8),
        "nba.typography.title": TypographyCategorySpec(familyRef: "nba.font.roboto", weight: 500, textCase: "none", lineHeight: 1.2),
        "nba.typography.body": TypographyCategorySpec(familyRef: "nba.font.roboto", weight: 400, textCase: "none", lineHeight: 1.2),
        "nba.typography.label": TypographyCategorySpec(familyRef: "nba.font.roboto", weight: 400, textCase: "uppercase", lineHeight: 1.0),
        "nba.typography.data": TypographyCategorySpec(familyRef: "nba.font.roboto.condensed", weight: 400, textCase: "uppercase", lineHeight: 1.0),
        "nba.typography.score": TypographyCategorySpec(familyRef: "nba.font.knockout", weight: 360, textCase: "uppercase", lineHeight: 0.8),
        "nba.typography.button": TypographyCategorySpec(familyRef: "nba.font.roboto", weight: 700, textCase: "none", lineHeight: 1.0),
        "nba.typography.caption": TypographyCategorySpec(familyRef: "nba.font.roboto", weight: 400, textCase: "none", lineHeight: 1.2),
    ]

    public static let typographyVariants: [String: TypographyVariantSpec] = [
        "nba.typography.displayLarge": TypographyVariantSpec(categoryRef: "nba.typography.display", size: TypographySize(phone: 57, tablet: 64, tv: 96, web: .envelope(WebSizeEnvelope(min: 45, max: 96, minVw: 320, maxVw: 1440)))),
        "nba.typography.displayMedium": TypographyVariantSpec(categoryRef: "nba.typography.display", size: TypographySize(phone: 45, tablet: 56, tv: 80, web: .envelope(WebSizeEnvelope(min: 36, max: 80, minVw: 320, maxVw: 1440)))),
        "nba.typography.displaySmall": TypographyVariantSpec(categoryRef: "nba.typography.display", size: TypographySize(phone: 36, tablet: 48, tv: 64, web: .envelope(WebSizeEnvelope(min: 32, max: 64, minVw: 320, maxVw: 1440)))),
        "nba.typography.headlineLarge": TypographyVariantSpec(categoryRef: "nba.typography.headline", size: TypographySize(phone: 32, tablet: 40, tv: 64, web: .envelope(WebSizeEnvelope(min: 28, max: 64, minVw: 320, maxVw: 1440)))),
        "nba.typography.headlineMedium": TypographyVariantSpec(categoryRef: "nba.typography.headline", size: TypographySize(phone: 28, tablet: 32, tv: 48, web: .envelope(WebSizeEnvelope(min: 24, max: 48, minVw: 320, maxVw: 1440)))),
        "nba.typography.headlineSmall": TypographyVariantSpec(categoryRef: "nba.typography.headline", size: TypographySize(phone: 24, tablet: 28, tv: 40, web: .envelope(WebSizeEnvelope(min: 22, max: 40, minVw: 320, maxVw: 1440)))),
        "nba.typography.titleLarge": TypographyVariantSpec(categoryRef: "nba.typography.title", size: TypographySize(phone: 22, tablet: 24, tv: 32, web: .envelope(WebSizeEnvelope(min: 20, max: 28, minVw: 320, maxVw: 1440)))),
        "nba.typography.titleMedium": TypographyVariantSpec(categoryRef: "nba.typography.title", size: TypographySize(phone: 16, tablet: 18, tv: 24, web: .scalar(16))),
        "nba.typography.titleSmall": TypographyVariantSpec(categoryRef: "nba.typography.title", size: TypographySize(phone: 14, tablet: 16, tv: 20, web: .scalar(14))),
        "nba.typography.bodyLarge": TypographyVariantSpec(categoryRef: "nba.typography.body", size: TypographySize(phone: 16, tablet: 18, tv: 24, web: .envelope(WebSizeEnvelope(min: 16, max: 18, minVw: 320, maxVw: 1440)))),
        "nba.typography.bodyMedium": TypographyVariantSpec(categoryRef: "nba.typography.body", size: TypographySize(phone: 14, tablet: 16, tv: 20, web: .envelope(WebSizeEnvelope(min: 14, max: 18, minVw: 320, maxVw: 1440)))),
        "nba.typography.bodySmall": TypographyVariantSpec(categoryRef: "nba.typography.body", size: TypographySize(phone: 12, tablet: 14, tv: 18, web: .scalar(12))),
        "nba.typography.labelLarge": TypographyVariantSpec(categoryRef: "nba.typography.label", size: TypographySize(phone: 14, tablet: 14, tv: 18, web: .scalar(14))),
        "nba.typography.labelMedium": TypographyVariantSpec(categoryRef: "nba.typography.label", size: TypographySize(phone: 12, tablet: 12, tv: 16, web: .scalar(12))),
        "nba.typography.labelSmall": TypographyVariantSpec(categoryRef: "nba.typography.label", size: TypographySize(phone: 11, tablet: 11, tv: 14, web: .scalar(11))),
        "nba.typography.score": TypographyVariantSpec(categoryRef: "nba.typography.score", size: TypographySize(phone: 32, tablet: 48, tv: 80, web: .envelope(WebSizeEnvelope(min: 28, max: 56, minVw: 320, maxVw: 1440)))),
    ]

    public static let motionDuration: [String: FormFactorMatrix<Int>] = [
        "nba.motion.duration.fast": FormFactorMatrix(phone: 150, tablet: 180, tv: 250, web: 200),
        "nba.motion.duration.default": FormFactorMatrix(phone: 200, tablet: 250, tv: 350, web: 300),
        "nba.motion.duration.slow": FormFactorMatrix(phone: 400, tablet: 500, tv: 700, web: 600),
        "nba.motion.duration.hero": FormFactorMatrix(phone: 500, tablet: 600, tv: 900, web: 800),
    ]

    public static let motionEasing: [String: String] = [
        "nba.motion.easing.default": "cubic-bezier(0.16, 1, 0.3, 1)",
        "nba.motion.easing.linear": "linear",
    ]

    public static let shadows: [String: ShadowSpec] = [
        "nba.shadow.sm": ShadowSpec(type: "drop", color: "rgba(0,0,0,0.12)", radius: 3, offsetX: 0, offsetY: 1),
        "nba.shadow.md": ShadowSpec(type: "drop", color: "rgba(0,0,0,0.15)", radius: 8, offsetX: 0, offsetY: 2),
        "nba.shadow.lg": ShadowSpec(type: "drop", color: "rgba(0,0,0,0.12)", radius: 16, offsetX: 0, offsetY: 4),
        "nba.shadow.xl": ShadowSpec(type: "drop", color: "rgba(0,0,0,0.25)", radius: 32, offsetX: 0, offsetY: 8),
    ]
}
