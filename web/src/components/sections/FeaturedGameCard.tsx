import React from 'react';
import type { SectionProps } from '../SectionRouter';
import { mapFeaturedGameCard } from '../../adapters/sectionUiAdapters';
import { getPrimarySectionAction } from '../../utils/sectionActions';

export function FeaturedGameCard({ section, onAction }: SectionProps): React.ReactElement | null {
  const model = mapFeaturedGameCard(section);
  if (!model) return null;

  const primaryAction = getPrimarySectionAction(section, 'onTap');
  const isLive = model.visualState === 'LIVE';

  const gradient = isLive
    ? 'linear-gradient(135deg, #1d428a 0%, #c8102e 100%)'
    : 'linear-gradient(135deg, #1d428a 0%, #0a1128 100%)';

  return (
    <button
      style={{ ...styles.container, background: gradient }}
      onClick={() => primaryAction && onAction(primaryAction)}
    >
      {/* Top row: visualLabel + badge */}
      <div style={styles.topRow}>
        {model.visualLabel && <span style={styles.visualLabel}>{model.visualLabel}</span>}
        {model.badgeText && (
          <span style={{ ...styles.badge, background: isLive ? '#c8102e' : '#1d428a' }}>
            {model.badgeText}
          </span>
        )}
      </div>
      <div style={styles.matchup}>
        <div style={styles.teamCol}>
          {model.awayLogoUrl && (
            <img src={model.awayLogoUrl} alt={model.awayTricode} style={styles.logo} />
          )}
          <span style={styles.score}>{model.awayScore}</span>
          <span style={styles.teamName}>{model.awayName || model.awayTricode}</span>
          {model.awayRecord && <span style={styles.record}>{model.awayRecord}</span>}
        </div>
        <div style={styles.centerCol}>
          <span style={styles.statusText}>{model.statusText}</span>
        </div>
        <div style={styles.teamCol}>
          {model.homeLogoUrl && (
            <img src={model.homeLogoUrl} alt={model.homeTricode} style={styles.logo} />
          )}
          <span style={styles.score}>{model.homeScore}</span>
          <span style={styles.teamName}>{model.homeName || model.homeTricode}</span>
          {model.homeRecord && <span style={styles.record}>{model.homeRecord}</span>}
        </div>
      </div>
    </button>
  );
}

const styles: Record<string, React.CSSProperties> = {
  container: {
    position: 'relative',
    width: '100%',
    minHeight: 180,
    borderRadius: 16,
    border: 'none',
    color: '#fff',
    padding: '16px 20px',
    cursor: 'pointer',
    display: 'flex',
    flexDirection: 'column',
    justifyContent: 'space-between',
    gap: 8,
    margin: '8px 0',
    textAlign: 'center',
  },
  topRow: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  visualLabel: {
    fontSize: 12,
    fontWeight: 600,
    color: 'rgba(255,255,255,0.7)',
    textTransform: 'uppercase' as const,
    letterSpacing: 0.5,
  },
  badge: {
    fontSize: 11,
    fontWeight: 700,
    textTransform: 'uppercase' as const,
    padding: '3px 10px',
    borderRadius: 6,
    letterSpacing: 0.5,
  },
  matchup: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'space-around',
  },
  teamCol: {
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    gap: 2,
  },
  logo: {
    width: 56,
    height: 56,
    objectFit: 'contain',
  },
  score: {
    fontSize: 28,
    fontWeight: 800,
  },
  teamName: {
    fontSize: 12,
    fontWeight: 600,
    opacity: 0.85,
  },
  record: {
    fontSize: 11,
    opacity: 0.6,
  },
  centerCol: {
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    gap: 4,
  },
  statusText: {
    fontSize: 13,
    color: 'rgba(255,255,255,0.75)',
  },
};
