import React from 'react';
import type { SectionProps } from '../SectionRouter';
import { SectionList } from '../SectionRouter';
import type { Section } from '@sdui/models';

/**
 * Row – Responsive layout primitive.
 *
 * Renders child sections side-by-side when viewport width >= breakpoint,
 * and stacks them vertically when viewport width < breakpoint.
 *
 * Data contract:
 *   children:   Section[]  – required
 *   spacing:    number     – gap in px (default 16)
 *   breakpoint: number     – collapse threshold in px (default 600)
 */
export function Row({ section, state, onAction, onStateChange }: SectionProps): React.ReactElement | null {
  const data = section.data as Record<string, unknown> | undefined;
  if (!data) return null;

  const children = (data.children ?? []) as Section[];
  const spacing = (data.spacing as number) ?? 16;
  const breakpoint = (data.breakpoint as number) ?? 600;

  if (children.length === 0) return null;

  // Unique class name per instance to avoid style collisions
  const className = `sdui-row-${section.id}`;

  return (
    <div className={className} style={{ width: '100%' }}>
      {children.map((child) => (
        <div key={child.id} className={`${className}__child`}>
          <SectionList
            sections={[child]}
            state={state}
            onAction={onAction}
            onStateChange={onStateChange}
          />
        </div>
      ))}

      {/* Responsive CSS */}
      <style>{`
        .${className} {
          display: flex;
          flex-direction: row;
          gap: ${spacing}px;
        }
        .${className}__child {
          flex: 1 1 0%;
          min-width: 0;
        }
        @media (max-width: ${breakpoint}px) {
          .${className} {
            flex-direction: column;
          }
        }
      `}</style>
    </div>
  );
}
