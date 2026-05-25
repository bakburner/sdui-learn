// THIS FILE IS GENERATED. DO NOT EDIT.

export type FormFactorMatrix<T extends number = number> = {
  phone: T;
  tablet: T;
  tv: T;
  web: T;
};

export type WebSizeEnvelope = {
  min: number;
  max: number;
  minVw: number;
  maxVw: number;
};

export type WebSize = number | WebSizeEnvelope;

export type TypographySize = {
  phone: number;
  tablet: number;
  tv: number;
  web: WebSize;
};

export type TypographyCategorySpec = {
  familyRef: string;
  weight: number;
  textCase: string;
  lineHeight: number;
};

export type TypographyVariantSpec = {
  categoryRef: string;
  size: TypographySize;
};

export type ShadowSpec = {
  type: string;
  color: string;
  radius: number;
  offsetX: number;
  offsetY: number;
};

export const LayoutTokenRegistry = {
  spacing: {
    "nba.spacing.xs": { phone: 2, tablet: 2, tv: 4, web: 2 },
    "nba.spacing.sm": { phone: 4, tablet: 6, tv: 6, web: 4 },
    "nba.spacing.md": { phone: 12, tablet: 15, tv: 18, web: 12 },
    "nba.spacing.lg": { phone: 16, tablet: 20, tv: 24, web: 16 },
    "nba.spacing.xl": { phone: 32, tablet: 40, tv: 48, web: 32 },
    "nba.spacing.2xl": { phone: 40, tablet: 48, tv: 56, web: 40 },
  },

  radius: {
    "nba.radius.xs": { phone: 2, tablet: 2, tv: 2, web: 2 },
    "nba.radius.sm": { phone: 4, tablet: 4, tv: 4, web: 4 },
    "nba.radius.md": { phone: 12, tablet: 12, tv: 12, web: 12 },
    "nba.radius.lg": { phone: 16, tablet: 16, tv: 16, web: 16 },
    "nba.radius.xl": { phone: 24, tablet: 24, tv: 24, web: 24 },
    "nba.radius.2xl": { phone: 32, tablet: 32, tv: 32, web: 32 },
    "nba.radius.full": { phone: 9999, tablet: 9999, tv: 9999, web: 9999 },
  },

  typographyCategories: {
    "nba.typography.headline": { familyRef: "nba.font.knockout", weight: 360, textCase: "uppercase", lineHeight: 0.8 },
    "nba.typography.display": { familyRef: "nba.font.knockout", weight: 395, textCase: "uppercase", lineHeight: 0.8 },
    "nba.typography.title": { familyRef: "nba.font.roboto", weight: 500, textCase: "none", lineHeight: 1.2 },
    "nba.typography.body": { familyRef: "nba.font.roboto", weight: 400, textCase: "none", lineHeight: 1.2 },
    "nba.typography.label": { familyRef: "nba.font.roboto", weight: 400, textCase: "uppercase", lineHeight: 1.0 },
    "nba.typography.data": { familyRef: "nba.font.roboto.condensed", weight: 400, textCase: "uppercase", lineHeight: 1.0 },
    "nba.typography.score": { familyRef: "nba.font.knockout", weight: 360, textCase: "uppercase", lineHeight: 0.8 },
    "nba.typography.button": { familyRef: "nba.font.roboto", weight: 700, textCase: "none", lineHeight: 1.0 },
    "nba.typography.caption": { familyRef: "nba.font.roboto", weight: 400, textCase: "none", lineHeight: 1.2 },
  },

  typographyVariants: {
    "nba.typography.displayLarge": { categoryRef: "nba.typography.display", size: { phone: 57, tablet: 64, tv: 96, web: { min: 45, max: 96, minVw: 320, maxVw: 1440 } } },
    "nba.typography.displayMedium": { categoryRef: "nba.typography.display", size: { phone: 45, tablet: 56, tv: 80, web: { min: 36, max: 80, minVw: 320, maxVw: 1440 } } },
    "nba.typography.displaySmall": { categoryRef: "nba.typography.display", size: { phone: 36, tablet: 48, tv: 64, web: { min: 32, max: 64, minVw: 320, maxVw: 1440 } } },
    "nba.typography.headlineLarge": { categoryRef: "nba.typography.headline", size: { phone: 32, tablet: 40, tv: 64, web: { min: 28, max: 64, minVw: 320, maxVw: 1440 } } },
    "nba.typography.headlineMedium": { categoryRef: "nba.typography.headline", size: { phone: 28, tablet: 32, tv: 48, web: { min: 24, max: 48, minVw: 320, maxVw: 1440 } } },
    "nba.typography.headlineSmall": { categoryRef: "nba.typography.headline", size: { phone: 24, tablet: 28, tv: 40, web: { min: 22, max: 40, minVw: 320, maxVw: 1440 } } },
    "nba.typography.titleLarge": { categoryRef: "nba.typography.title", size: { phone: 22, tablet: 24, tv: 32, web: { min: 20, max: 28, minVw: 320, maxVw: 1440 } } },
    "nba.typography.titleMedium": { categoryRef: "nba.typography.title", size: { phone: 16, tablet: 18, tv: 24, web: 16 } },
    "nba.typography.titleSmall": { categoryRef: "nba.typography.title", size: { phone: 14, tablet: 16, tv: 20, web: 14 } },
    "nba.typography.bodyLarge": { categoryRef: "nba.typography.body", size: { phone: 16, tablet: 18, tv: 24, web: { min: 16, max: 18, minVw: 320, maxVw: 1440 } } },
    "nba.typography.bodyMedium": { categoryRef: "nba.typography.body", size: { phone: 14, tablet: 16, tv: 20, web: { min: 14, max: 18, minVw: 320, maxVw: 1440 } } },
    "nba.typography.bodySmall": { categoryRef: "nba.typography.body", size: { phone: 12, tablet: 14, tv: 18, web: 12 } },
    "nba.typography.labelLarge": { categoryRef: "nba.typography.label", size: { phone: 14, tablet: 14, tv: 18, web: 14 } },
    "nba.typography.labelMedium": { categoryRef: "nba.typography.label", size: { phone: 12, tablet: 12, tv: 16, web: 12 } },
    "nba.typography.labelSmall": { categoryRef: "nba.typography.label", size: { phone: 11, tablet: 11, tv: 14, web: 11 } },
    "nba.typography.score": { categoryRef: "nba.typography.score", size: { phone: 32, tablet: 48, tv: 80, web: { min: 28, max: 56, minVw: 320, maxVw: 1440 } } },
  },

  motionDuration: {
    "nba.motion.duration.fast": { phone: 150, tablet: 180, tv: 250, web: 200 },
    "nba.motion.duration.default": { phone: 200, tablet: 250, tv: 350, web: 300 },
    "nba.motion.duration.slow": { phone: 400, tablet: 500, tv: 700, web: 600 },
    "nba.motion.duration.hero": { phone: 500, tablet: 600, tv: 900, web: 800 },
  },

  motionEasing: {
    "nba.motion.easing.default": "cubic-bezier(0.16, 1, 0.3, 1)",
    "nba.motion.easing.linear": "linear",
  },

  shadows: {
    "nba.shadow.sm": { type: "drop", color: "rgba(0,0,0,0.12)", radius: 3, offsetX: 0, offsetY: 1 },
    "nba.shadow.md": { type: "drop", color: "rgba(0,0,0,0.15)", radius: 8, offsetX: 0, offsetY: 2 },
    "nba.shadow.lg": { type: "drop", color: "rgba(0,0,0,0.12)", radius: 16, offsetX: 0, offsetY: 4 },
    "nba.shadow.xl": { type: "drop", color: "rgba(0,0,0,0.25)", radius: 32, offsetX: 0, offsetY: 8 },
  },
} as const;
