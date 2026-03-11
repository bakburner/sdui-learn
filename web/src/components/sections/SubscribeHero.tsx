import React from 'react';
import type { SectionProps } from '../SectionRouter';
import { DEFAULT_FALLBACK_IMAGE } from '../../utils/constants';

/**
 * SubscribeHero — full-screen subscription upsell with feature list and pricing tiers.
 *
 * Expected data:
 *   title              – main heading
 *   subtitle           – supporting tagline
 *   backgroundImageUrl – hero background
 *   logoUrl            – brand logo
 *   features[]         – bullet-point strings
 *   tiers[]            – SubscriptionTier objects { id, name, price, originalPrice, badgeText, features[], ctaLabel, ctaAction }
 */
export function SubscribeHero({ section, onAction }: SectionProps): React.ReactElement | null {
  const data = section.data as Record<string, unknown> | undefined;
  if (!data) return null;

  const title = data.title as string | undefined;
  const subtitle = data.subtitle as string | undefined;
  const bgUrl = data.backgroundImageUrl as string | undefined;
  const logoUrl = data.logoUrl as string | undefined;
  const features = (data.features as string[]) ?? [];
  const tiers = (data.tiers as Array<Record<string, unknown>>) ?? [];
  const fallbackUrl = (data.fallbackThumbnailUrl as string | undefined) ?? DEFAULT_FALLBACK_IMAGE;

  return (
    <div style={styles.wrapper}>
      <div style={styles.hero}>
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
          {logoUrl && <img src={logoUrl} alt="NBA League Pass" style={styles.logo}
            onError={(e) => { const img = e.currentTarget; if (img.src !== DEFAULT_FALLBACK_IMAGE) img.src = DEFAULT_FALLBACK_IMAGE; }} />}
          {title && <h2 style={styles.title}>{title}</h2>}
          {subtitle && <p style={styles.subtitle}>{subtitle}</p>}

          {/* Feature bullets */}
          {features.length > 0 && (
            <ul style={styles.featureList}>
              {features.map((f, i) => (
                <li key={i} style={styles.feature}>
                  <span style={styles.check}>✓</span> {f}
                </li>
              ))}
            </ul>
          )}

          {/* Pricing tiers */}
          <div style={styles.tiersRow}>
            {tiers.map((tier, i) => {
              const name = tier.name as string ?? '';
              const price = tier.price as string ?? '';
              const originalPrice = tier.originalPrice as string | undefined;
              const badgeText = tier.badgeText as string | undefined;
              const tierFeatures = (tier.features as string[]) ?? [];
              const ctaLabel = (tier.ctaLabel as string) ?? 'Subscribe';
              const ctaAction = tier.ctaAction as Record<string, unknown> | undefined;

              return (
                <div key={(tier.id as string) ?? i} style={styles.tierCard}>
                  {badgeText && <span style={styles.tierBadge}>{badgeText}</span>}
                  <span style={styles.tierName}>{name}</span>
                  <div style={styles.priceRow}>
                    <span style={styles.tierPrice}>{price}</span>
                    {originalPrice && <span style={styles.tierOriginal}>{originalPrice}</span>}
                  </div>
                  {tierFeatures.length > 0 && (
                    <ul style={styles.tierFeatures}>
                      {tierFeatures.map((f, j) => (
                        <li key={j} style={styles.tierFeatureItem}>• {f}</li>
                      ))}
                    </ul>
                  )}
                  <button
                    style={styles.tierCta}
                    onClick={() => ctaAction && onAction(ctaAction as any)}
                  >
                    {ctaLabel}
                  </button>
                </div>
              );
            })}
          </div>
        </div>
      </div>
    </div>
  );
}

const styles: Record<string, React.CSSProperties> = {
  wrapper: {
    padding: '8px 16px',
  },
  hero: {
    position: 'relative',
    borderRadius: 16,
    overflow: 'hidden',
    background: 'linear-gradient(180deg, #0c1b3a 0%, #1d428a 100%)',
  },
  bgImage: {
    position: 'absolute',
    inset: 0,
    width: '100%',
    height: '100%',
    objectFit: 'cover',
    opacity: 0.2,
    pointerEvents: 'none',
  },
  content: {
    position: 'relative',
    padding: '32px 24px',
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    textAlign: 'center',
    gap: 8,
  },
  logo: {
    height: 36,
    objectFit: 'contain',
    marginBottom: 8,
  },
  title: {
    fontSize: 22,
    fontWeight: 800,
    color: '#fff',
    margin: 0,
  },
  subtitle: {
    fontSize: 14,
    color: 'rgba(255,255,255,0.75)',
    margin: 0,
    lineHeight: '1.4',
  },
  featureList: {
    listStyle: 'none',
    padding: 0,
    margin: '12px 0',
    display: 'flex',
    flexDirection: 'column',
    gap: 6,
    alignItems: 'flex-start',
  },
  feature: {
    fontSize: 13,
    color: 'rgba(255,255,255,0.85)',
  },
  check: {
    color: '#00c853',
    fontWeight: 700,
    marginRight: 4,
  },
  tiersRow: {
    display: 'flex',
    gap: 16,
    marginTop: 16,
    flexWrap: 'wrap',
    justifyContent: 'center',
  },
  tierCard: {
    flex: '0 1 220px',
    background: 'rgba(255,255,255,0.1)',
    borderRadius: 12,
    padding: '20px 16px',
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    gap: 6,
  },
  tierBadge: {
    fontSize: 10,
    fontWeight: 700,
    textTransform: 'uppercase' as const,
    background: '#c8102e',
    color: '#fff',
    padding: '2px 10px',
    borderRadius: 4,
    letterSpacing: 0.5,
  },
  tierName: {
    fontSize: 16,
    fontWeight: 700,
    color: '#fff',
  },
  priceRow: {
    display: 'flex',
    alignItems: 'center',
    gap: 8,
  },
  tierPrice: {
    fontSize: 22,
    fontWeight: 800,
    color: '#fff',
  },
  tierOriginal: {
    fontSize: 14,
    color: 'rgba(255,255,255,0.4)',
    textDecoration: 'line-through',
  },
  tierFeatures: {
    listStyle: 'none',
    padding: 0,
    margin: '4px 0',
    textAlign: 'left',
  },
  tierFeatureItem: {
    fontSize: 12,
    color: 'rgba(255,255,255,0.7)',
    lineHeight: '1.6',
  },
  tierCta: {
    marginTop: 8,
    padding: '10px 24px',
    fontSize: 14,
    fontWeight: 700,
    color: '#1d428a',
    backgroundColor: '#fff',
    border: 'none',
    borderRadius: 8,
    cursor: 'pointer',
    width: '100%',
  },
};
