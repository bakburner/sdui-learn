import React from 'react';
import type { SectionProps } from '../SectionRouter';
import { accessibilityProps } from '../../utils/accessibility';
import { resolveColorToken, usePrefersColorScheme } from '../../utils/ColorTokenResolver';

/**
 * Stub renderer for AdSlot. Reserves the ad SDK's eventual mount
 * rectangle — `data.sizes[0]` is the single source of truth for
 * dimensions, shared by the placeholder and (when the SDK lands)
 * the ad platform itself. Inner placeholder chrome (background
 * color, caption text) comes from `data.placeholder`; outer chrome
 * (margin, padding, shadow, radius) comes from `section.surface`
 * via the shared SectionContainer wrapper.
 *
 * This renderer carries no client-side chrome defaults — a payload
 * missing required `sizes` renders nothing. See AGENTS.md §15.1
 * and §15.2.
 */
export function AdSlot({ section }: SectionProps): React.ReactElement | null {
  const scheme = usePrefersColorScheme();
  const data = section.data as Record<string, unknown> | undefined;
  if (!data) return null;

  const sizes = data.sizes as number[][] | undefined;
  const first = sizes?.[0];
  if (!first || first.length < 2) return null;
  const [width, height] = first;

  const label = data.label as string | undefined;
  const placeholder = data.placeholder as { backgroundColor?: string; text?: string } | undefined;
  const placeholderBg = resolveColorToken(placeholder?.backgroundColor, scheme);
  const placeholderText = placeholder?.text ?? '';

  return (
    <div
      aria-label={label ?? 'Advertisement'}
      role="complementary"
      {...accessibilityProps(section.accessibility)}
      style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 4 }}
    >
      {label && (
        <span style={{ fontSize: 10, fontWeight: 600, color: '#888', textTransform: 'uppercase', letterSpacing: 1 }}>
          {label}
        </span>
      )}
      <div
        style={{
          width,
          height,
          backgroundColor: placeholderBg,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
        }}
      >
        {placeholderText && (
          <span style={{ fontSize: 12, color: '#888' }}>{placeholderText}</span>
        )}
      </div>
    </div>
  );
}
