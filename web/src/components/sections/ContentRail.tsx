import React from 'react';
import type { SectionProps } from '../SectionRouter';
import type { HeroPanelData } from '@sdui/models';
import { mapContentRail } from '../../adapters/sectionUiAdapters';
import { DEFAULT_FALLBACK_IMAGE } from '../../utils/constants';

/**
 * ContentRail - horizontal scrolling list of content cards.
 */
export function ContentRail({ section, onAction }: SectionProps): React.ReactElement {
  const model = mapContentRail(section);
  if (!model) {
    return <div style={styles.container}>No content available</div>;
  }

  const fallbackUrl = ((section.data as Record<string, unknown> | undefined)?.fallbackThumbnailUrl as string | undefined) ?? DEFAULT_FALLBACK_IMAGE;

  const handleCardClick = (card: HeroPanelData) => {
    if (card.action) {
      onAction(card.action);
    }
  };

  return (
    <div style={{ ...styles.container, backgroundColor: section.backgroundColor || 'transparent' }}>
      {model.title && <h3 style={styles.title}>{model.title}</h3>}
      
      <div style={styles.rail}>
        {model.cards.map((card) => (
          <div 
            key={card.id}
            style={styles.card}
            onClick={() => handleCardClick(card)}
          >
            {/* Thumbnail */}
            <div style={styles.thumbnailContainer}>
              {card.thumbnailUrl ? (
                <img 
                  src={card.thumbnailUrl} 
                  alt="" 
                  style={styles.thumbnail}
                  onError={(e) => {
                    const img = e.currentTarget;
                    if (fallbackUrl && img.src !== fallbackUrl) img.src = fallbackUrl;
                  }}
                />
              ) : (
                <div style={styles.thumbnailPlaceholder}>
                  {card.contentType === 'video' ? '▶' : '📄'}
                </div>
              )}
              
              {/* Duration badge for videos */}
              {card.duration && (
                <span style={styles.duration}>{card.duration}</span>
              )}
              
              {/* Content type badge */}
              {card.contentType && (
                <span style={styles.contentType}>{card.contentType}</span>
              )}
            </div>

            {/* Card Info - headline only (not duplicated) */}
            <div style={styles.cardInfo}>
              <span style={styles.headline}>{card.headline}</span>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}

const styles: Record<string, React.CSSProperties> = {
  container: {
    padding: '16px 0',
    margin: '8px 0',
  },
  title: {
    fontSize: 16,
    fontWeight: 600,
    color: '#ffffff',
    margin: '0 16px 12px',
  },
  rail: {
    display: 'flex',
    overflowX: 'auto',
    gap: 12,
    padding: '0 16px 8px',
    scrollbarWidth: 'thin',
    scrollbarColor: '#555 transparent',
  },
  card: {
    flexShrink: 0,
    width: 200,
    cursor: 'pointer',
    transition: 'transform 0.2s',
  },
  thumbnailContainer: {
    position: 'relative',
    width: '100%',
    height: 112,
    borderRadius: 8,
    overflow: 'hidden',
    backgroundColor: '#2a2a4a',
  },
  thumbnail: {
    width: '100%',
    height: '100%',
    objectFit: 'cover',
  },
  thumbnailPlaceholder: {
    width: '100%',
    height: '100%',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    fontSize: 32,
    color: '#666',
  },
  duration: {
    position: 'absolute',
    bottom: 6,
    right: 6,
    backgroundColor: 'rgba(0, 0, 0, 0.8)',
    color: '#ffffff',
    fontSize: 11,
    fontWeight: 600,
    padding: '2px 6px',
    borderRadius: 4,
  },
  contentType: {
    position: 'absolute',
    top: 6,
    left: 6,
    backgroundColor: '#ff6b6b',
    color: '#ffffff',
    fontSize: 10,
    fontWeight: 700,
    padding: '2px 6px',
    borderRadius: 4,
    textTransform: 'uppercase',
  },
  cardInfo: {
    padding: '8px 0',
    display: 'flex',
    flexDirection: 'column',
    gap: 4,
  },
  headline: {
    fontSize: 13,
    fontWeight: 600,
    color: '#ffffff',
    lineHeight: 1.3,
    display: '-webkit-box',
    WebkitLineClamp: 2,
    WebkitBoxOrient: 'vertical',
    overflow: 'hidden',
  },
  subhead: {
    fontSize: 11,
    color: '#888888',
  },
};
