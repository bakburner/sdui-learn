import React from 'react';
import type { SectionProps } from '../SectionRouter';
import { mapPromoBanner } from '../../adapters/sectionUiAdapters';
import { DEFAULT_FALLBACK_IMAGE } from '../../utils/constants';

/**
 * PromoBanner - promotional banner with image and call-to-action.
 */
export function PromoBanner({ section, onAction }: SectionProps): React.ReactElement {
  const model = mapPromoBanner(section);
  if (!model) {
    return <div style={styles.container}>No promo data</div>;
  }

  const handleClick = () => {
    if (model.primaryAction) {
      onAction(model.primaryAction);
    }
  };

  const fallbackUrl = ((section.data as Record<string, unknown> | undefined)?.fallbackThumbnailUrl as string | undefined) ?? DEFAULT_FALLBACK_IMAGE;

  const hasBackground = Boolean(model.backgroundImageUrl);

  return (
    <div 
      style={{
        ...styles.container,
        ...(hasBackground ? {} : { backgroundColor: section.backgroundColor || '#2a2a5e' }),
      }}
      onClick={handleClick}
    >
      {/* Background image + gradient overlay — no URL concatenation (Rule 5) */}
      {hasBackground && (
        <>
          <img
            src={model.backgroundImageUrl}
            alt=""
            style={styles.backgroundImage}
            onError={(e) => {
              const img = e.currentTarget;
              if (fallbackUrl && img.src !== fallbackUrl) img.src = fallbackUrl;
            }}
          />
          <div style={styles.backgroundOverlay} />
        </>
      )}

      {model.imageUrl && (
        <img
          src={model.imageUrl}
          alt=""
          style={styles.image}
          onError={(e) => {
            const img = e.currentTarget;
            if (fallbackUrl && img.src !== fallbackUrl) img.src = fallbackUrl;
          }}
        />
      )}
      
      <div style={styles.content}>
        {model.title && <span style={styles.title}>{model.title}</span>}
        {model.headline && <h3 style={styles.headline}>{model.headline}</h3>}
        {model.subhead && <p style={styles.subhead}>{model.subhead}</p>}
        
        {model.primaryAction && (
          <button style={styles.cta}>
            Learn More
          </button>
        )}
      </div>
    </div>
  );
}

const styles: Record<string, React.CSSProperties> = {
  container: {
    position: 'relative',
    display: 'flex',
    alignItems: 'center',
    padding: 20,
    borderRadius: 12,
    margin: 8,
    cursor: 'pointer',
    transition: 'transform 0.2s',
    minHeight: 120,
    overflow: 'hidden',
  },
  backgroundImage: {
    position: 'absolute',
    inset: 0,
    width: '100%',
    height: '100%',
    objectFit: 'cover',
    zIndex: 0,
  },
  backgroundOverlay: {
    position: 'absolute',
    inset: 0,
    background: 'linear-gradient(rgba(0,0,0,0.4), rgba(0,0,0,0.7))',
    zIndex: 1,
  },
  image: {
    position: 'relative',
    zIndex: 2,
    width: 80,
    height: 80,
    objectFit: 'contain',
    marginRight: 16,
  },
  content: {
    position: 'relative',
    zIndex: 2,
    flex: 1,
    display: 'flex',
    flexDirection: 'column',
    gap: 4,
  },
  title: {
    fontSize: 11,
    fontWeight: 700,
    color: '#ff6b6b',
    textTransform: 'uppercase',
    letterSpacing: 1,
  },
  headline: {
    fontSize: 18,
    fontWeight: 700,
    color: '#ffffff',
    margin: 0,
    lineHeight: 1.2,
  },
  subhead: {
    fontSize: 13,
    color: '#cccccc',
    margin: 0,
  },
  cta: {
    marginTop: 12,
    padding: '8px 16px',
    border: 'none',
    borderRadius: 20,
    backgroundColor: '#ffffff',
    color: '#1a1a2e',
    fontSize: 12,
    fontWeight: 700,
    cursor: 'pointer',
    alignSelf: 'flex-start',
  },
};
