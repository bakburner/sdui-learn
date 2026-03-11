import React from 'react';
import type { SectionProps } from '../SectionRouter';
import { mapFollowingRail } from '../../adapters/sectionUiAdapters';
import { DEFAULT_FALLBACK_IMAGE } from '../../utils/constants';

export function FollowingRail({ section, onAction }: SectionProps): React.ReactElement | null {
  const model = mapFollowingRail(section);
  if (!model) return null;
  const fallbackUrl = ((section.data as Record<string, unknown> | undefined)?.fallbackThumbnailUrl as string | undefined) ?? DEFAULT_FALLBACK_IMAGE;

  return (
    <div style={styles.container}>
      {model.title && <div style={styles.title}>{model.title}</div>}
      <div style={styles.rail}>
        {model.items.map((item) => (
          <button
            key={item.id}
            style={styles.item}
            onClick={() => item.action && onAction(item.action)}
          >
            <div style={styles.avatarWrap}>
              {item.imageUrl ? (
                <img
                  src={item.imageUrl}
                  alt={item.name}
                  style={styles.avatar}
                  onError={(e) => {
                    const img = e.currentTarget;
                    if (fallbackUrl && img.src !== fallbackUrl) img.src = fallbackUrl;
                  }}
                />
              ) : (
                <div style={styles.avatarFallback}>
                  {item.name.slice(0, 3).toUpperCase()}
                </div>
              )}
            </div>
            <span style={styles.name}>{item.name}</span>
          </button>
        ))}
      </div>
    </div>
  );
}

const styles: Record<string, React.CSSProperties> = {
  container: {
    padding: '12px 0',
  },
  title: {
    fontSize: 15,
    fontWeight: 600,
    color: '#b4c0d3',
    marginBottom: 10,
    paddingLeft: 4,
  },
  rail: {
    display: 'flex',
    gap: 16,
    overflowX: 'auto',
    paddingBottom: 4,
  },
  item: {
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    gap: 6,
    background: 'none',
    border: 'none',
    cursor: 'pointer',
    padding: 0,
    minWidth: 64,
  },
  avatarWrap: {
    width: 56,
    height: 56,
    borderRadius: '50%',
    overflow: 'hidden',
    background: '#1e2438',
    border: '2px solid #2a2f45',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
  },
  avatar: {
    width: '100%',
    height: '100%',
    objectFit: 'cover',
  },
  avatarFallback: {
    fontSize: 12,
    fontWeight: 700,
    color: '#7a8baa',
  },
  name: {
    fontSize: 11,
    color: '#b4c0d3',
    maxWidth: 64,
    overflow: 'hidden',
    textOverflow: 'ellipsis',
    whiteSpace: 'nowrap',
  },
};
