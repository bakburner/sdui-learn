import React from 'react';
import type { Action } from '@sdui/models';
import type { AtomicProps } from './AtomicRouter';
import { accessibilityProps } from '../../utils/accessibility';

const DEFAULT_FALLBACK = 'https://cdn.nba.com/manage/2025/04/nba-247-logoman-yt-thumbnail__1_.png';

const fitToObjectFit: Record<string, React.CSSProperties['objectFit']> = {
  cover: 'cover',
  contain: 'contain',
  fill: 'fill',
  fitWidth: 'cover',
  fitHeight: 'cover',
  none: 'none',
};

/**
 * AtomicImage — renders an <img> with optional sizing, aspect ratio,
 * content scale, corner radius, and fallback on load error.
 */
export function AtomicImage({ element, onAction }: AtomicProps): React.ReactElement {
  const style: React.CSSProperties = {
    objectFit: fitToObjectFit[element.fit ?? ''] ?? 'contain',
    ...(element.fillWidth ? { width: '100%' } : element.width != null ? { width: element.width } : {}),
    ...(element.height != null ? { height: element.height } : {}),
    ...(element.aspectRatio != null ? { aspectRatio: String(element.aspectRatio) } : {}),
    ...(element.cornerRadius != null ? { borderRadius: element.cornerRadius, overflow: 'hidden' } : {}),
  };

  const hasActions = element.actions && element.actions.length > 0;

  const handleClick = hasActions
    ? () => {
        const action = element.actions![0] as unknown as Action;
        onAction(action);
      }
    : undefined;

  const fallbackUrl = element.placeholder || DEFAULT_FALLBACK;

  const handleError = (e: React.SyntheticEvent<HTMLImageElement>) => {
    const img = e.currentTarget;
    if (fallbackUrl && img.src !== fallbackUrl) {
      img.src = fallbackUrl;
    }
  };

  return (
    <img
      src={element.src}
      alt={element.accessibility?.label ?? element.alt ?? ''}
      style={{ ...style, ...(hasActions ? { cursor: 'pointer' } : {}) }}
      onClick={handleClick}
      onError={handleError}
      {...(element.accessibility?.hidden ? { 'aria-hidden': true } : {})}
    />
  );
}
