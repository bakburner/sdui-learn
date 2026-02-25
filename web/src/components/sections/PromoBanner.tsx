import React from 'react';
import type { SectionProps } from '../SectionRouter';
import { mapPromoBanner } from '../../adapters/sectionUiAdapters';

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

  const backgroundStyle: React.CSSProperties = model.backgroundImageUrl
    ? {
        backgroundImage: `linear-gradient(rgba(0,0,0,0.4), rgba(0,0,0,0.7)), url(${model.backgroundImageUrl})`,
        backgroundSize: 'cover',
        backgroundPosition: 'center',
      }
    : {
        backgroundColor: section.backgroundColor || '#2a2a5e',
      };

  return (
    <div 
      style={{ ...styles.container, ...backgroundStyle }}
      onClick={handleClick}
    >
      {model.imageUrl && (
        <img src={model.imageUrl} alt="" style={styles.image} />
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
    display: 'flex',
    alignItems: 'center',
    padding: 20,
    borderRadius: 12,
    margin: 8,
    cursor: 'pointer',
    transition: 'transform 0.2s',
    minHeight: 120,
  },
  image: {
    width: 80,
    height: 80,
    objectFit: 'contain',
    marginRight: 16,
  },
  content: {
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
