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

export function resolveBackgroundCSS(bg: Background | undefined | null): CSSProperties {
  if (!bg) return {};
  if (typeof bg === 'string') {
    return { background: bg };
  }
  if ('imageUrl' in bg) {
    const size = bg.scaleType === 'fill' ? '100% 100%'
               : bg.scaleType === 'contain' ? 'contain'
               : 'cover';
    return {
      backgroundImage: `url(${bg.imageUrl})`,
      backgroundSize: size,
      backgroundPosition: 'center',
      backgroundRepeat: 'no-repeat',
    };
  }
  if ('colors' in bg) {
    const dir = bg.direction === 'horizontal' ? 'to right'
              : bg.direction === 'diagonal' ? 'to bottom right'
              : 'to bottom';
    return { background: `linear-gradient(${dir}, ${bg.colors.join(', ')})` };
  }
  return {};
}
