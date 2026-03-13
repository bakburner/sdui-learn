import React from 'react';
import type { Action } from '@sdui/models';
import type { AtomicProps } from './AtomicRouter';

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
 * content scale, and tap actions.
 */
export function AtomicImage({ element, onAction }: AtomicProps): React.ReactElement {
  const style: React.CSSProperties = {
    objectFit: fitToObjectFit[element.fit ?? ''] ?? 'contain',
    ...(element.width != null ? { width: element.width } : {}),
    ...(element.height != null ? { height: element.height } : {}),
    ...(element.aspectRatio != null ? { aspectRatio: String(element.aspectRatio) } : {}),
  };

  const hasActions = element.actions && element.actions.length > 0;

  const handleClick = hasActions
    ? () => {
        const action = element.actions![0] as unknown as Action;
        onAction(action);
      }
    : undefined;

  return (
    <img
      src={element.src}
      alt={element.id ?? ''}
      style={{ ...style, ...(hasActions ? { cursor: 'pointer' } : {}) }}
      onClick={handleClick}
    />
  );
}
