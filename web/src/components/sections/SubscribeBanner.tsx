import React from 'react';
import type { SectionProps } from '../SectionRouter';
import { accessibilityProps } from '../../utils/accessibility';

/**
 * SubscribeBanner — inline subscription upsell.
 *
 * Outer chrome (margin, radius, gradient background, inner padding)
 * comes from `section.display` via `SectionContainer` — this renderer
 * only lays out the banner's content (title, subtitle, CTA).
 * See AGENTS.md §15.3.
 *
 * Expected data:
 *   title     – heading text
 *   subtitle  – supporting text
 *   ctaLabel  – button text
 *   ctaAction – Action on CTA tap
 */
export function SubscribeBanner({ section, onAction }: SectionProps): React.ReactElement | null {
  const data = section.data as Record<string, unknown> | undefined;
  if (!data) return null;

  const title = data.title as string | undefined;
  const subtitle = data.subtitle as string | undefined;
  const ctaLabel = (data.ctaLabel as string) ?? 'Subscribe';
  const ctaAction = data.ctaAction as Record<string, unknown> | undefined;

  return (
    <div style={styles.content} {...accessibilityProps(section.accessibility)}>
      {title && <h3 style={styles.title}>{title}</h3>}
      {subtitle && <p style={styles.subtitle}>{subtitle}</p>}
      <button
        style={styles.cta}
        aria-label={`Subscribe: ${title}`}
        onClick={() => ctaAction && onAction(ctaAction as any)}
      >
        {ctaLabel}
      </button>
    </div>
  );
}

const styles: Record<string, React.CSSProperties> = {
  content: {
    display: 'flex',
    flexDirection: 'column',
    gap: 8,
    alignItems: 'flex-start',
  },
  title: {
    fontSize: 18,
    fontWeight: 700,
    color: '#fff',
    margin: 0,
  },
  subtitle: {
    fontSize: 14,
    color: 'rgba(255,255,255,0.85)',
    margin: 0,
    lineHeight: '1.4',
  },
  cta: {
    marginTop: 4,
    padding: '10px 24px',
    fontSize: 14,
    fontWeight: 700,
    color: '#1d428a',
    backgroundColor: '#fff',
    border: 'none',
    borderRadius: 8,
    cursor: 'pointer',
  },
};
