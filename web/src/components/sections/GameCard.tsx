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
        <div style={styles.teamSide}>
          {model.awayLogoUrl && (
            <img src={model.awayLogoUrl} alt={model.awayTricode} style={styles.logo} />
          )}
          <div style={styles.teamInfo}>
            <span style={styles.tricode}>{model.awayTricode}</span>
            {model.awayRecord && <span style={styles.record}>{model.awayRecord}</span>}
          </div>
        </div>
        <span style={styles.score}>{model.awayScore}</span>
        <span style={styles.at}>@</span>
        <span style={styles.score}>{model.homeScore}</span>
        <div style={{ ...styles.teamSide, flexDirection: 'row-reverse' }}>
          {model.homeLogoUrl && (
            <img src={model.homeLogoUrl} alt={model.homeTricode} style={styles.logo} />
          )}
          <div style={{ ...styles.teamInfo, alignItems: 'flex-end' }}>
            <span style={styles.tricode}>{model.homeTricode}</span>
            {model.homeRecord && <span style={styles.record}>{model.homeRecord}</span>}
          </div>
        </div>
      </div>
      <div style={styles.footer}>
        <span style={styles.statusLine}>{model.statusText}</span>
        {model.broadcaster && <span style={styles.broadcaster}>{model.broadcaster}</span>}
      </div>
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
    justifyContent: 'space-between',
    fontWeight: 700,
    fontSize: 16,
  },
  teamSide: {
    display: 'flex',
    alignItems: 'center',
    gap: 8,
    flex: 1,
  },
  teamInfo: {
    display: 'flex',
    flexDirection: 'column',
  },
  tricode: {
    fontWeight: 700,
    fontSize: 15,
  },
  record: {
    fontSize: 11,
    color: '#8892a4',
    fontWeight: 400,
  },
  score: {
    minWidth: 20,
    fontSize: 18,
    fontWeight: 800,
  },
  logo: {
    width: 28,
    height: 28,
    objectFit: 'contain' as const,
  },
  at: {
    opacity: 0.6,
    margin: '0 6px',
    fontSize: 13,
  },
  footer: {
    marginTop: 8,
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  statusLine: {
    fontSize: 13,
    color: '#b4c0d3',
  },
  broadcaster: {
    fontSize: 11,
    color: '#8892a4',
  },
};
