import React from 'react';
import type { AtomicProps } from './AtomicRouter';

/**
 * AtomicSpacer — renders an empty div with configurable width, height, or uniform size.
 */
export function AtomicSpacer({ element }: AtomicProps): React.ReactElement {
  const style: React.CSSProperties = {
    ...(element.size != null ? { width: element.size, height: element.size } : {}),
    ...(element.width != null ? { width: element.width } : {}),
    ...(element.height != null ? { height: element.height } : {}),
  };
  return <div style={style} />;
}
