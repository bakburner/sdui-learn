import React from 'react';
import type { AtomicProps } from './AtomicRouter';
import { AtomicBox } from './AtomicBox';
import { useColorTokenResolver } from '../../utils/ColorTokenResolver';

/**
 * AtomicDivider — renders a horizontal or vertical rule. The divider
 * itself carries only its thickness / orientation / color; element-level
 * margin / padding / opacity come from AtomicBox.
 */
export function AtomicDivider({ element }: AtomicProps): React.ReactElement {
  const resolveColor = useColorTokenResolver();
  const thickness = element.thickness ?? 1;
  const color = resolveColor(element.color) ?? 'var(--divider)';
  const isVertical = element.orientation === 'vertical';

  const rule: React.CSSProperties = isVertical
    ? { width: thickness, alignSelf: 'stretch', backgroundColor: color }
    : { height: thickness, width: '100%', backgroundColor: color };

  return (
    <AtomicBox element={element}>
      <div style={rule} aria-hidden="true" />
    </AtomicBox>
  );
}
