import React from 'react';
import type { AtomicProps } from './AtomicRouter';

/**
 * AtomicSpacer — renders an empty div sized via `size` / `width` / `height`.
 *
 * Spacer deliberately bypasses AtomicBox because its job is to add axis-
 * aligned empty space in a flex layout; a box-model wrapper around that
 * would add a redundant nesting level without any visual benefit.
 * element-level `margin` / `padding` / `background` on a Spacer are
 * intentionally treated as no-ops — if that styling is needed, the
 * server should emit an empty Container instead.
 */
export function AtomicSpacer({ element }: AtomicProps): React.ReactElement {
  const style: React.CSSProperties = {
    ...(element.size != null ? { width: element.size, height: element.size } : {}),
    ...(element.width != null ? { width: element.width } : {}),
    ...(element.height != null ? { height: element.height } : {}),
  };
  return <div style={style} aria-hidden="true" />;
}
