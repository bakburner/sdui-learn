import React from 'react';
import type { SectionProps } from '../SectionRouter';
import { mapStatLine } from '../../adapters/sectionUiAdapters';

/**
 * StatLine - displays a list of player stats.
 */
export function StatLine({ section }: SectionProps): React.ReactElement {
  const model = mapStatLine(section);
  if (!model) {
    return <div style={styles.container}>No stats available</div>;
  }

  return (
    <div style={{ ...styles.container, backgroundColor: section.backgroundColor || '#1e2a47' }}>
      {model.title && <h3 style={styles.title}>{model.title}</h3>}
      
      <div style={styles.statsList}>
        {model.stats.map((stat, index) => (
          <div 
            key={`${stat.playerId}-${index}`} 
            style={styles.statRow}
            onClick={() => {
              // Could trigger a navigate action to player detail
              console.log('[StatLine] Clicked player:', stat.playerName);
            }}
          >
            {/* Player Image */}
            <div style={styles.playerImageContainer}>
              {stat.playerImageUrl ? (
                <img 
                  src={stat.playerImageUrl} 
                  alt={stat.playerName} 
                  style={styles.playerImage} 
                />
              ) : (
                <div style={styles.playerImagePlaceholder}>
                  {stat.playerName.charAt(0)}
                </div>
              )}
            </div>

            {/* Player Info */}
            <div style={styles.playerInfo}>
              <span style={styles.playerName}>{stat.playerName}</span>
              <span style={styles.teamTricode}>{stat.teamTricode}</span>
            </div>

            {/* Stat Value */}
            <div style={styles.statValue}>
              <span style={styles.value}>{stat.statValue}</span>
              <span style={styles.statLabel}>{stat.statLabel || stat.statCategory}</span>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}

const styles: Record<string, React.CSSProperties> = {
  container: {
    padding: 16,
    borderRadius: 12,
    margin: 8,
  },
  title: {
    fontSize: 16,
    fontWeight: 600,
    color: '#ffffff',
    marginBottom: 16,
    margin: 0,
    paddingBottom: 12,
    borderBottom: '1px solid #333',
  },
  statsList: {
    display: 'flex',
    flexDirection: 'column',
    gap: 8,
  },
  statRow: {
    display: 'flex',
    alignItems: 'center',
    padding: '12px 8px',
    borderRadius: 8,
    cursor: 'pointer',
    transition: 'background-color 0.2s',
  },
  playerImageContainer: {
    width: 40,
    height: 40,
    marginRight: 12,
  },
  playerImage: {
    width: 40,
    height: 40,
    borderRadius: 20,
    objectFit: 'cover',
  },
  playerImagePlaceholder: {
    width: 40,
    height: 40,
    borderRadius: 20,
    backgroundColor: '#333',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    fontSize: 18,
    fontWeight: 600,
    color: '#888',
  },
  playerInfo: {
    display: 'flex',
    flexDirection: 'column',
    flex: 1,
  },
  playerName: {
    fontSize: 14,
    fontWeight: 600,
    color: '#ffffff',
  },
  teamTricode: {
    fontSize: 12,
    color: '#888888',
  },
  statValue: {
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'flex-end',
  },
  value: {
    fontSize: 20,
    fontWeight: 700,
    color: '#ffffff',
  },
  statLabel: {
    fontSize: 11,
    color: '#888888',
    textTransform: 'uppercase',
  },
};
