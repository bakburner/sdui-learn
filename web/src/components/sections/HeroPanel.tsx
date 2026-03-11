import React from 'react';
import type { SectionProps } from '../SectionRouter';
import { mapHeroPanel } from '../../adapters/sectionUiAdapters';
import { DEFAULT_FALLBACK_IMAGE } from '../../utils/constants';

/**
 * HeroPanel - Displays a single content card (article, video, etc.)
 */
export function HeroPanel({ section, onAction }: SectionProps): React.ReactElement {
  const model = mapHeroPanel(section);
  if (!model) {
    return <div style={styles.container}>Unable to load content</div>;
  }

  const handleClick = () => {
    if (model.action) {
      onAction(model.action);
    }
  };

  return (
    <div
      style={{ ...styles.container, backgroundColor: section.backgroundColor || 'transparent' }}
    >
      <div
        style={styles.card}
        onClick={handleClick}
        role={model.action ? 'button' : undefined}
      >
        {/* Thumbnail */}
        {model.thumbnailUrl && (
          <div style={styles.thumbnailContainer}>
            <img
              src={model.thumbnailUrl}
              alt=""
              style={styles.thumbnail}
              onError={(e) => {
                const img = e.currentTarget;
                const fb = model.fallbackThumbnailUrl || DEFAULT_FALLBACK_IMAGE;
                if (img.src !== fb) img.src = fb;
              }}
            />
            {model.duration && <span style={styles.duration}>{model.duration}</span>}
            {model.contentType && <span style={styles.contentType}>{model.contentType}</span>}
          </div>
        )}

        {/* Text */}
        <div style={styles.textContainer}>
          <h3 style={styles.headline}>{model.headline}</h3>
          {model.subhead && <p style={styles.subhead}>{model.subhead}</p>}
        </div>
      </div>
    </div>
  );
}

const styles: Record<string, React.CSSProperties> = {
  container: {
    padding: '8px 16px',
  },
  card: {
    display: 'flex',
    flexDirection: 'column',
    borderRadius: 12,
    overflow: 'hidden',
    backgroundColor: '#1a1a2e',
    cursor: 'pointer',
    transition: 'transform 0.2s',
  },
  thumbnailContainer: {
    position: 'relative',
    width: '100%',
    aspectRatio: '16/9',
  },
  thumbnail: {
    width: '100%',
    height: '100%',
    objectFit: 'cover',
  },
  duration: {
    position: 'absolute',
    bottom: 8,
    right: 8,
    background: 'rgba(0,0,0,0.75)',
    color: '#fff',
    padding: '2px 6px',
    borderRadius: 4,
    fontSize: 12,
  },
  contentType: {
    position: 'absolute',
    top: 8,
    left: 8,
    background: '#17408B',
    color: '#fff',
    padding: '2px 8px',
    borderRadius: 4,
    fontSize: 11,
    textTransform: 'uppercase',
    fontWeight: 600,
  },
  textContainer: {
    padding: 16,
  },
  headline: {
    margin: 0,
    fontSize: 18,
    fontWeight: 700,
    color: '#ffffff',
    lineHeight: '1.3',
  },
  subhead: {
    margin: '6px 0 0',
    fontSize: 14,
    color: '#aaaaaa',
    lineHeight: '1.4',
  },
};
