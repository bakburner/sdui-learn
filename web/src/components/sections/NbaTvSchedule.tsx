import React from 'react';
import type { SectionProps } from '../SectionRouter';
import { DEFAULT_FALLBACK_IMAGE } from '../../utils/constants';

/**
 * NbaTvSchedule — hero image + time-slot schedule list.
 *
 * Expected data:
 *   heroImageUrl  – large banner image
 *   heroTitle     – e.g. "NBA TV Live"
 *   heroSubtitle  – e.g. "Watch Now"
 *   liveNow       – boolean
 *   slots[]       – NbaTvSlot objects { id, time, title, subtitle, liveBadge, thumbnailUrl, action }
 */
export function NbaTvSchedule({ section, onAction }: SectionProps): React.ReactElement | null {
  const data = section.data as Record<string, unknown> | undefined;
  if (!data) return null;

  const heroImageUrl = data.heroImageUrl as string | undefined;
  const heroTitle = data.heroTitle as string | undefined;
  const heroSubtitle = data.heroSubtitle as string | undefined;
  const liveNow = data.liveNow as boolean | undefined;
  const fallbackUrl = (data.fallbackThumbnailUrl as string | undefined) ?? DEFAULT_FALLBACK_IMAGE;
  const slots = (data.slots as Array<Record<string, unknown>>) ?? [];

  return (
    <div style={styles.container}>
      {/* Hero */}
      <div style={styles.hero}>
        {heroImageUrl ? (
          <img
            src={heroImageUrl}
            alt=""
            style={styles.heroImage}
            onError={(e) => {
              const img = e.currentTarget;
              if (fallbackUrl && img.src !== fallbackUrl) img.src = fallbackUrl;
            }}
          />
        ) : (
          <div style={styles.heroPlaceholder} />
        )}
        <div style={styles.heroOverlay}>
          {liveNow && <span style={styles.liveBadge}>● LIVE</span>}
          {heroTitle && <h3 style={styles.heroTitle}>{heroTitle}</h3>}
          {heroSubtitle && <span style={styles.heroSubtitle}>{heroSubtitle}</span>}
        </div>
      </div>

      {/* Schedule slots */}
      {slots.length > 0 && (
        <div style={styles.scheduleWrap}>
          <h4 style={styles.scheduleHeading}>Today's Schedule</h4>
          {slots.map((slot, i) => {
            const id = (slot.id as string) ?? `slot-${i}`;
            const time = slot.time as string | undefined;
            const title = slot.title as string | undefined;
            const subtitle = slot.subtitle as string | undefined;
            const isLive = slot.liveBadge as boolean | undefined;
            const action = slot.action as Record<string, unknown> | undefined;

            return (
              <button
                key={id}
                style={styles.slotRow}
                onClick={() => action && onAction(action as any)}
              >
                <span style={styles.slotTime}>{time ?? ''}</span>
                <div style={styles.slotInfo}>
                  <span style={styles.slotTitle}>{title}</span>
                  {subtitle && <span style={styles.slotSubtitle}>{subtitle}</span>}
                </div>
                {isLive && <span style={styles.slotLive}>LIVE</span>}
              </button>
            );
          })}
        </div>
      )}
    </div>
  );
}

const styles: Record<string, React.CSSProperties> = {
  container: {
    margin: '8px 0',
  },
  hero: {
    position: 'relative',
    width: '100%',
    height: 200,
    borderRadius: 16,
    overflow: 'hidden',
    backgroundColor: '#0a1128',
    margin: '0 0 4px',
  },
  heroImage: {
    width: '100%',
    height: '100%',
    objectFit: 'cover',
  },
  heroPlaceholder: {
    width: '100%',
    height: '100%',
    background: 'linear-gradient(135deg, #0c1b3a 0%, #1d428a 100%)',
  },
  heroOverlay: {
    position: 'absolute',
    bottom: 0,
    left: 0,
    right: 0,
    padding: '24px 16px 16px',
    background: 'linear-gradient(transparent, rgba(0,0,0,0.8))',
    display: 'flex',
    flexDirection: 'column',
    gap: 4,
  },
  liveBadge: {
    alignSelf: 'flex-start',
    fontSize: 11,
    fontWeight: 700,
    color: '#c8102e',
    background: 'rgba(200,16,46,0.15)',
    padding: '2px 8px',
    borderRadius: 4,
    letterSpacing: 0.5,
  },
  heroTitle: {
    fontSize: 20,
    fontWeight: 700,
    color: '#fff',
    margin: 0,
  },
  heroSubtitle: {
    fontSize: 13,
    color: 'rgba(255,255,255,0.65)',
  },
  scheduleWrap: {
    padding: '12px 16px',
  },
  scheduleHeading: {
    fontSize: 14,
    fontWeight: 600,
    color: '#fff',
    margin: '0 0 12px',
    textTransform: 'uppercase' as const,
    letterSpacing: 0.5,
    opacity: 0.7,
  },
  slotRow: {
    display: 'flex',
    alignItems: 'center',
    gap: 12,
    width: '100%',
    padding: '10px 0',
    borderBottom: '1px solid rgba(255,255,255,0.08)',
    background: 'none',
    border: 'none',
    borderBottomStyle: 'solid',
    borderBottomWidth: 1,
    borderBottomColor: 'rgba(255,255,255,0.08)',
    color: '#fff',
    cursor: 'pointer',
    textAlign: 'left',
  },
  slotTime: {
    fontSize: 12,
    fontWeight: 600,
    color: 'rgba(255,255,255,0.5)',
    width: 60,
    flexShrink: 0,
  },
  slotInfo: {
    flex: 1,
    display: 'flex',
    flexDirection: 'column',
    gap: 2,
  },
  slotTitle: {
    fontSize: 14,
    fontWeight: 600,
  },
  slotSubtitle: {
    fontSize: 12,
    color: 'rgba(255,255,255,0.5)',
  },
  slotLive: {
    fontSize: 10,
    fontWeight: 700,
    color: '#c8102e',
    background: 'rgba(200,16,46,0.15)',
    padding: '2px 8px',
    borderRadius: 4,
    letterSpacing: 0.5,
  },
};
