import React from 'react';
import type { SectionProps } from '../SectionRouter';
import { mapGameCard } from '../../adapters/sectionUiAdapters';
import { getPrimarySectionAction } from '../../utils/sectionActions';

export function GameCard({ section, onAction }: SectionProps): React.ReactElement {
  const model = mapGameCard(section);
  if (!model) {
    return <div style={styles.container}>No game data</div>;
  }

  const primaryAction = getPrimarySectionAction(section, 'onTap');

  const handleClick = () => {
    if (primaryAction) onAction(primaryAction);
  };

  return (
    <button style={styles.container} onClick={handleClick}>
      <div style={styles.teamsRow}>
        {model.awayLogoUrl && (
          <img src={model.awayLogoUrl} alt={model.awayTricode} style={styles.logo} />
        )}
        <span style={styles.team}>{model.awayTricode}</span>
        <span style={styles.score}>{model.awayScore}</span>
        <span style={styles.at}>@</span>
        <span style={styles.score}>{model.homeScore}</span>
        <span style={styles.team}>{model.homeTricode}</span>
        {model.homeLogoUrl && (
          <img src={model.homeLogoUrl} alt={model.homeTricode} style={styles.logo} />
        )}
      </div>
      <div style={styles.statusLine}>{model.statusText}</div>
    </button>
  );
}

const styles: Record<string, React.CSSProperties> = {
  container: {
    width: '100%',
    textAlign: 'left',
    border: '1px solid #2a2f45',
    borderRadius: 10,
    background: '#161b2b',
    color: '#fff',
    padding: '12px 14px',
    margin: '8px 0',
    cursor: 'pointer',
  },
  teamsRow: {
    display: 'flex',
    alignItems: 'center',
    gap: 8,
    fontWeight: 700,
    fontSize: 16,
  },
  team: {
    minWidth: 40,
  },
  score: {
    minWidth: 20,
  },
  logo: {
    width: 28,
    height: 28,
    objectFit: 'contain' as const,
  },
  at: {
    opacity: 0.6,
    margin: '0 4px',
  },
  statusLine: {
    marginTop: 8,
    fontSize: 13,
    color: '#b4c0d3',
  },
};
