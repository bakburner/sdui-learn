import spacingJson from '../../../schema/spacing-tokens.json';
import radiusJson from '../../../schema/corner-radius-tokens.json';
import typographyJson from '../../../schema/typography-tokens.json';
import motionJson from '../../../schema/motion-tokens.json';
import shadowJson from '../../../schema/shadow-tokens.json';

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

type RawTypographyVariant = {
  categoryRef: string;
  size: {
    phone: number;
    tablet: number;
    tv: number;
    web: number | WebSizeEnvelope;
  };
};

function buildTypographyVariants(
  variants: Record<string, RawTypographyVariant>,
): Record<string, TypographyVariantSpec> {
  const normalized: Record<string, TypographyVariantSpec> = {};
  for (const [tokenName, variant] of Object.entries(variants)) {
    const webSize = variant.size.web;
    normalized[tokenName] = {
      categoryRef: variant.categoryRef,
      size: {
        phone: variant.size.phone,
        tablet: variant.size.tablet,
        tv: variant.size.tv,
        web:
          typeof webSize === 'number'
            ? webSize
            : {
                min: webSize.min,
                max: webSize.max,
                minVw: webSize.minVw,
                maxVw: webSize.maxVw,
              },
      },
    };
  }
  return normalized;
}

export const LayoutTokenRegistry = {
  spacing: spacingJson.spacing as Record<string, FormFactorMatrix>,
  radius: radiusJson.radius as Record<string, FormFactorMatrix>,
  typographyCategories: typographyJson.categories as Record<string, TypographyCategorySpec>,
  typographyVariants: buildTypographyVariants(
    typographyJson.variants as Record<string, RawTypographyVariant>,
  ),
  motionDuration: motionJson.duration as Record<string, FormFactorMatrix>,
  motionEasing: motionJson.easing as Record<string, string>,
  shadows: shadowJson.shadows as Record<string, ShadowSpec>,
};
