import React from 'react';
import type { SectionProps } from '../SectionRouter';
import { DEFAULT_FALLBACK_IMAGE } from '../../utils/constants';

/**
 * VideoCarousel — horizontal scrolling row of video thumbnails.
 *
 * Expected data:
 *   title     – section heading (e.g. "Tonight's Highlights")
 *   subtitle  – optional subtext
 *   items[]   – VideoThumbnail objects { id, title, subtitle, thumbnailUrl, duration, badgeText, action }
 */
export function VideoCarousel({ section, onAction }: SectionProps): React.ReactElement | null {
  const data = section.data as Record<string, unknown> | undefined;
  if (!data) return null;

  const title = data.title as string | undefined;
  const subtitle = data.subtitle as string | undefined;
  const items = (data.items as Array<Record<string, unknown>>) ?? [];
  const fallbackUrl = (data.fallbackThumbnailUrl as string | undefined) ?? DEFAULT_FALLBACK_IMAGE;
  if (items.length === 0) return null;

  return (
    <div style={styles.container}>
      {title && (
        <div style={styles.header}>
          <h3 style={styles.title}>{title}</h3>
          {subtitle && <span style={styles.subtitle}>{subtitle}</span>}
        </div>
      )}
      <div style={styles.rail}>
        {items.map((item, i) => {
          const id = (item.id as string) ?? `vc-${i}`;
          const thumbUrl = item.thumbnailUrl as string | undefined;
          const dur = item.duration as string | undefined;
          const badge = item.badgeText as string | undefined;
          const t = item.title as string | undefined;
          const sub = item.subtitle as string | undefined;
          const action = item.action as Record<string, unknown> | undefined;

          return (
            <button
              key={id}
              style={styles.card}
              onClick={() => action && onAction(action as any)}
            >
              <div style={styles.thumbWrap}>
                {thumbUrl ? (
                  <img
                    src={thumbUrl}
                    alt=""
                    style={styles.thumb}
                    onError={(e) => {
                      const img = e.currentTarget;
                      if (fallbackUrl && img.src !== fallbackUrl) img.src = fallbackUrl;
                    }}
                  />
                ) : (
                  <div style={styles.thumbPlaceholder}>▶</div>
                )}
                {dur && <span style={styles.duration}>{dur}</span>}
                {badge && <span style={styles.badge}>{badge}</span>}
              </div>
              {t && <span style={styles.itemTitle}>{t}</span>}
              {sub && <span style={styles.itemSubtitle}>{sub}</span>}
            </button>
          );
        })}
      </div>
    </div>
  );
}

const styles: Record<string, React.CSSProperties> = {
  container: {
    padding: '16px 0',
    margin: '8px 0',
  },
  header: {
    padding: '0 16px 8px',
    display: 'flex',
    flexDirection: 'column',
    gap: 2,
  },
  title: {
    fontSize: 16,
    fontWeight: 600,
    color: '#fff',
    margin: 0,
  },
  subtitle: {
    fontSize: 13,
    color: 'rgba(255,255,255,0.55)',
  },
  rail: {
    display: 'flex',
    gap: 12,
    overflowX: 'auto',
    padding: '0 16px',
    scrollbarWidth: 'none',
  },
  card: {
    flex: '0 0 240px',
    background: 'none',
    border: 'none',
    color: '#fff',
    textAlign: 'left',
    cursor: 'pointer',
    padding: 0,
  },
  thumbWrap: {
    position: 'relative',
    width: 240,
    height: 135,
    borderRadius: 10,
    overflow: 'hidden',
    backgroundColor: '#1a1a2e',
  },
  thumb: {
    width: '100%',
    height: '100%',
    objectFit: 'cover',
  },
  thumbPlaceholder: {
    width: '100%',
    height: '100%',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    fontSize: 32,
    color: 'rgba(255,255,255,0.3)',
    backgroundColor: '#1a1a2e',
  },
  duration: {
    position: 'absolute',
    bottom: 6,
    right: 6,
    fontSize: 11,
    fontWeight: 600,
    background: 'rgba(0,0,0,0.7)',
    color: '#fff',
    padding: '2px 6px',
    borderRadius: 4,
  },
  badge: {
    position: 'absolute',
    top: 6,
    left: 6,
    fontSize: 10,
    fontWeight: 700,
    textTransform: 'uppercase' as const,
    background: '#c8102e',
    color: '#fff',
    padding: '2px 8px',
    borderRadius: 4,
    letterSpacing: 0.5,
  },
  itemTitle: {
    display: 'block',
    fontSize: 13,
    fontWeight: 600,
    marginTop: 6,
    lineHeight: '1.3',
  },
  itemSubtitle: {
    display: 'block',
    fontSize: 12,
    color: 'rgba(255,255,255,0.5)',
    marginTop: 2,
  },
};
