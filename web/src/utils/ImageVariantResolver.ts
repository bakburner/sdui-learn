/**
 * ImageVariantResolver — resolves the server-emitted `variant` wire
 * string on an Image atomic element into a web-native style spec.
 *
 * Mirrors the `web` tier of `schema/style-tokens.json` ImageVariant.
 * Unknown variants log a diagnostic and fall through to primitive
 * defaults (the renderer keeps its existing behavior).
 *
 * The `clip` flag distinguishes logos (which must never be clipped to a
 * rounded shape, even when a `cornerRadius` is supplied inline) from
 * artwork (which expects clipping when rounded). The renderer consults
 * this flag before applying `borderRadius`.
 */

import type { OverridePolicy } from './ContainerVariantResolver';

export type ImageVariantName = 'thumbnail';

export interface ImageVariantSpec {
  cornerRadius?: number;
  /** CSS `aspect-ratio` value. Numbers are serialized as "N / 1" by the renderer. */
  aspectRatio?: number | string;
  objectFit?: 'cover' | 'contain' | 'fill' | 'none';
  fillWidth?: boolean;
  /** When false, the renderer must not apply `border-radius` / `overflow: hidden`. */
  clip?: boolean;
  overrideMatrix: Record<string, OverridePolicy>;
}

const SPECS: Record<ImageVariantName, ImageVariantSpec> = {
  thumbnail: {
    cornerRadius: 8,
    aspectRatio: undefined,
    objectFit: 'cover',
    fillWidth: false,
    clip: true,
    overrideMatrix: {
      padding: 'allow',
      cornerRadius: 'allow',
      background: 'allow',
      shadow: 'allow',
      color: 'allow',
      opacity: 'allow',
      border: 'allow',
    },
  },
};

const KNOWN: ReadonlyArray<ImageVariantName> = ['thumbnail'];

export function resolveImageVariant(
  variant?: string | null,
  _formFactor?: string,
): ImageVariantSpec | undefined {
  if (!variant) return undefined;
  if (!KNOWN.includes(variant as ImageVariantName)) {
    if (typeof console !== 'undefined') {
      console.warn('variant_resolver_missing', { variant });
    }
    return undefined;
  }
  // Web uses CSS responsive sizing — no form-factor adjustment needed.
  return SPECS[variant as ImageVariantName];
}
