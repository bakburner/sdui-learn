import React from 'react';
import type { SectionProps } from '../SectionRouter';
import { mapScoreboardHeader } from '../../adapters/sectionUiAdapters';
import { getPrimarySectionAction } from '../../utils/sectionActions';
import { DEFAULT_FALLBACK_IMAGE } from '../../utils/constants';

/**
 * Scoreboard Header - displays team logos, scores, and game status.
 * Matches the Android ScoreboardHeaderRenderer layout:
 *   Away Logo | Away Tricode | Away Score | Status | Home Score | Home Tricode | Home Logo
 */
export function ScoreboardHeader({ section, onAction }: SectionProps): React.ReactElement {
  const model = mapScoreboardHeader(section);
  if (!model) {
    return <div style={styles.container}>No scoreboard data</div>;
  }

  const primaryAction = getPrimarySectionAction(section, 'onTap');

  return (
    <button
      type="button"
      onClick={() => primaryAction && onAction(primaryAction)}
      style={{
        ...styles.container,
        backgroundColor: section.backgroundColor || '#16213e',
        cursor: primaryAction ? 'pointer' : 'default',
      }}
    >
      {/* Away Team */}
      <div style={styles.teamCol}>
        {model.awayTeam?.logoUrl && (
          <img src={model.awayTeam.logoUrl} alt={model.awayTeam.teamName} style={styles.logo}
            onError={(e) => { const img = e.currentTarget; if (img.src !== DEFAULT_FALLBACK_IMAGE) img.src = DEFAULT_FALLBACK_IMAGE; }} />
        )}
        <span style={styles.tricode}>{model.awayTeam?.teamTricode}</span>
        <span style={styles.score}>{model.awayTeam?.score ?? '-'}</span>
      </div>

      {/* Game Status */}
      <div style={styles.statusSection}>
        <span style={model.isLive ? styles.statusLive : styles.statusText}>
          {model.statusText}
        </span>
        {model.periodLabel && (
          <span style={styles.period}>{model.periodLabel}</span>
        )}
      </div>

      {/* Home Team */}
      <div style={styles.teamCol}>
        {model.homeTeam?.logoUrl && (
          <img src={model.homeTeam.logoUrl} alt={model.homeTeam.teamName} style={styles.logo}
            onError={(e) => { const img = e.currentTarget; if (img.src !== DEFAULT_FALLBACK_IMAGE) img.src = DEFAULT_FALLBACK_IMAGE; }} />
        )}
        <span style={styles.tricode}>{model.homeTeam?.teamTricode}</span>
        <span style={styles.score}>{model.homeTeam?.score ?? '-'}</span>
      </div>
    </button>
  );
}

const styles: Record<string, React.CSSProperties> = {
  container: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'space-evenly',
    padding: '24px 16px',
    margin: 8,
    borderRadius: 12,
    width: 'calc(100% - 16px)',
    boxSizing: 'border-box',
    border: 'none',
    textAlign: 'left',
  },
  teamCol: {
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    width: 100,
    gap: 4,
  },
  logo: {
    width: 60,
    height: 60,
    objectFit: 'contain',
  },
  tricode: {
    fontSize: 14,
    fontWeight: 700,
    color: '#ffffff',
  },
  score: {
    fontSize: 36,
    fontWeight: 700,
    color: '#ffffff',
    textAlign: 'center',
  },
  statusSection: {
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    gap: 2,
    flex: 1,
  },
  statusLive: {
    fontSize: 14,
    fontWeight: 500,
    color: '#ffffff',
  },
  period: {
    fontSize: 12,
    color: 'rgba(255,255,255,0.8)',
  },
  statusText: {
    fontSize: 14,
    color: '#888888',
    textAlign: 'center',
  },
};
