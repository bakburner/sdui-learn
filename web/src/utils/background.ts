import { CSSProperties } from 'react';

export interface BackgroundGradient {
  colors: string[];
  direction?: 'horizontal' | 'vertical' | 'diagonal';
}

export interface BackgroundImage {
  imageUrl: string;
  scaleType?: 'cover' | 'fill' | 'contain';
  overlay?: string | BackgroundGradient;
}

export type Background = string | BackgroundGradient | BackgroundImage;

/**
 * Optional color resolver hook. When provided, every color string in
 * the background (solid, gradient stops, image overlay) is passed
 * through the mapper first. Callers use this to route
 * `token:...` references through `ColorTokenResolver`; raw hex
 * values pass through unchanged.
 *
 * If the mapper returns `undefined` for a given input, the original
 * string is kept — we never want to silently drop a color on a
 * resolver miss because the downstream CSS would become invalid.
 */
export type ColorMapper = (value: string) => string | undefined;

const identity: ColorMapper = (v) => v;

function mapColor(mapper: ColorMapper, value: string): string {
  return mapper(value) ?? value;
}

export function resolveBackgroundCSS(
  bg: Background | undefined | null,
  mapColorFn: ColorMapper = identity,
): CSSProperties {
  if (!bg) return {};
  if (typeof bg === 'string') {
    return { background: mapColor(mapColorFn, bg) };
  }
  if ('imageUrl' in bg) {
    const size = bg.scaleType === 'fill' ? '100% 100%'
               : bg.scaleType === 'contain' ? 'contain'
               : 'cover';
    const base: CSSProperties = {
      backgroundImage: `url(${bg.imageUrl})`,
      backgroundSize: size,
      backgroundPosition: 'center',
      backgroundRepeat: 'no-repeat',
    };
    if (bg.overlay) {
      if (typeof bg.overlay === 'string') {
        const overlay = mapColor(mapColorFn, bg.overlay);
        base.backgroundImage = `linear-gradient(${overlay}, ${overlay}), url(${bg.imageUrl})`;
      } else if ('colors' in bg.overlay) {
        const overlayColors = bg.overlay.colors.map((c) => mapColor(mapColorFn, c));
        const dir = bg.overlay.direction === 'horizontal' ? 'to right'
                  : bg.overlay.direction === 'diagonal' ? 'to bottom right'
                  : 'to bottom';
        base.backgroundImage = `linear-gradient(${dir}, ${overlayColors.join(', ')}), url(${bg.imageUrl})`;
      }
    }
    return base;
  }
  if ('colors' in bg) {
    const dir = bg.direction === 'horizontal' ? 'to right'
              : bg.direction === 'diagonal' ? 'to bottom right'
              : 'to bottom';
    const stops = bg.colors.map((c) => mapColor(mapColorFn, c));
    return { background: `linear-gradient(${dir}, ${stops.join(', ')})` };
  }
  return {};
}
