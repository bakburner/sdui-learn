import React from 'react';
import type { SectionProps } from '../SectionRouter';
import { DEFAULT_FALLBACK_IMAGE } from '../../utils/constants';

/**
 * SubscribeBanner — inline subscription upsell with gradient + CTA button.
 *
 * Expected data:
 *   title              – heading text
 *   subtitle           – supporting text
 *   backgroundImageUrl – optional background
 *   ctaLabel           – button text
 *   ctaAction          – Action on CTA tap
 */
export function SubscribeBanner({ section, onAction }: SectionProps): React.ReactElement | null {
  const data = section.data as Record<string, unknown> | undefined;
  if (!data) return null;

  const title = data.title as string | undefined;
  const subtitle = data.subtitle as string | undefined;
  const bgUrl = data.backgroundImageUrl as string | undefined;
  const ctaLabel = (data.ctaLabel as string) ?? 'Subscribe';
  const ctaAction = data.ctaAction as Record<string, unknown> | undefined;
  const fallbackUrl = (data.fallbackThumbnailUrl as string | undefined) ?? DEFAULT_FALLBACK_IMAGE;

  return (
    <div style={styles.wrapper}>
      <div style={styles.banner}>
        {bgUrl && (
          <img
            src={bgUrl}
            alt=""
            style={styles.bgImage}
            onError={(e) => {
              const img = e.currentTarget;
              if (fallbackUrl && img.src !== fallbackUrl) img.src = fallbackUrl;
            }}
          />
        )}
        <div style={styles.content}>
          {title && <h3 style={styles.title}>{title}</h3>}
          {subtitle && <p style={styles.subtitle}>{subtitle}</p>}
          <button
            style={styles.cta}
            onClick={() => ctaAction && onAction(ctaAction as any)}
          >
            {ctaLabel}
          </button>
        </div>
      </div>
    </div>
  );
}

const styles: Record<string, React.CSSProperties> = {
  wrapper: {
    padding: '8px 16px',
  },
  banner: {
    position: 'relative',
    borderRadius: 12,
    overflow: 'hidden',
    background: 'linear-gradient(135deg, #1d428a 0%, #862633 100%)',
    padding: '24px 20px',
  },
  bgImage: {
    position: 'absolute',
    inset: 0,
    width: '100%',
    height: '100%',
    objectFit: 'cover',
    opacity: 0.3,
    pointerEvents: 'none',
  },
  content: {
    position: 'relative',
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
    color: 'rgba(255,255,255,0.8)',
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
