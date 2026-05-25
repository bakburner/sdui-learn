import React from 'react';
import type { SectionProps } from '../SectionRouter';
import { accessibilityProps } from '../../utils/accessibility';

interface Column {
  key: string;
  label: string;
  sortable?: boolean;
  highlighted?: boolean;
  width?: string;
}

interface LeadersPlayerRow {
  rank: number;
  playerId: string;
  name: string;
  team: string;
  imageUrl?: string;
  stats: Record<string, unknown>;
}

interface LeadersTableModel {
  title?: string;
  subtitle?: string;
  columns: Column[];
  players: LeadersPlayerRow[];
  totalRows?: number;
  page?: number;
  pageSize?: number;
  sortColumn?: string;
  sortDirection?: string;
  emptyMessage?: string;
}

/**
 * SeasonLeadersTable — renders a sortable, ranked table of season
 * statistical leaders, matching the NBA.com "Official Leaders" layout.
 */
export function SeasonLeadersTable({ section }: SectionProps): React.ReactElement {
  const data = section.data as unknown as LeadersTableModel | undefined;

  if (!data?.columns || !data?.players?.length) {
    return (
      <div style={styles.container}>
        <div style={styles.empty}>{data?.emptyMessage || 'No leaders data available'}</div>
      </div>
    );
  }

  const { columns, players, title, subtitle, totalRows, page, pageSize, sortColumn, sortDirection } = data;

  return (
    <div style={{ ...styles.container, backgroundColor: 'var(--surface)' }} {...accessibilityProps(section.accessibility)}>
      {/* Header */}
      {(title || subtitle) && (
        <div style={styles.header}>
          {title && <h3 style={styles.title}>{title}</h3>}
          {subtitle && <span style={styles.subtitle}>{subtitle}</span>}
        </div>
      )}

      {/* Pagination info */}
      {totalRows != null && (
        <div style={styles.paginationBar}>
          <span style={styles.rowCount}>{totalRows} Rows</span>
          {page != null && pageSize != null && (
            <span style={styles.pageInfo}>
              Page {page} of {Math.ceil(totalRows / pageSize)}
            </span>
          )}
        </div>
      )}

      {/* Scrollable table */}
      <div style={styles.tableWrapper}>
        <table style={styles.table}>
          <thead>
            <tr>
              <th scope="col" style={{ ...styles.th, ...styles.rankCol }}>#</th>
              <th scope="col" style={{ ...styles.th, ...styles.playerCol }}>PLAYER</th>
              <th scope="col" style={{ ...styles.th, ...styles.teamCol }}>TEAM</th>
              {columns.map((col) => (
                <th
                  key={col.key}
                  style={{
                    ...styles.th,
                    ...(col.highlighted ? styles.highlightedTh : {}),
                    ...(sortColumn === col.key ? styles.sortedTh : {}),
                    width: col.width || 'auto',
                  }}
                >
                  {col.label}
                  {sortColumn === col.key && (
                    <span style={styles.sortArrow}>{sortDirection === 'asc' ? ' ▲' : ' ▼'}</span>
                  )}
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {players.map((player) => (
              <tr key={player.playerId} style={styles.row}>
                <td style={{ ...styles.td, ...styles.rankCol }}>{player.rank}</td>
                <td style={{ ...styles.td, ...styles.playerCol, ...styles.playerName }}>
                  {player.name}
                </td>
                <td style={{ ...styles.td, ...styles.teamCol, ...styles.teamText }}>
                  {player.team}
                </td>
                {columns.map((col) => {
                  const val = player.stats[col.key];
                  const display = val !== undefined && val !== null ? String(val) : '-';
                  return (
                    <td
                      key={col.key}
                      style={{
                        ...styles.td,
                        ...(col.highlighted ? styles.highlightedTd : {}),
                        ...(sortColumn === col.key ? styles.sortedTd : {}),
                      }}
                    >
                      {display}
                    </td>
                  );
                })}
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}

// ── Styles ───────────────────────────────────────────────────────────

const styles: Record<string, React.CSSProperties> = {
  container: {
    margin: 8,
    borderRadius: 8,
    overflow: 'hidden',
  },
  header: {
    padding: '16px 16px 8px',
  },
  title: {
    margin: 0,
    fontSize: 16,
    fontWeight: 700,
    fontFamily: 'var(--font-body)',
    color: 'var(--text-primary)',
  },
  subtitle: {
    fontSize: 12,
    color: 'var(--text-secondary)',
    marginTop: 2,
    display: 'block',
  },
  paginationBar: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    padding: '4px 16px 8px',
  },
  rowCount: {
    fontSize: 11,
    color: 'var(--text-secondary)',
    fontFamily: 'var(--font-mono)',
  },
  pageInfo: {
    fontSize: 11,
    color: 'var(--text-secondary)',
    fontFamily: 'var(--font-mono)',
  },
  tableWrapper: {
    overflowX: 'auto',
    WebkitOverflowScrolling: 'touch',
  },
  table: {
    width: '100%',
    borderCollapse: 'collapse',
    fontSize: 13,
    minWidth: 700,
    fontFamily: 'var(--font-body)',
  },
  th: {
    padding: '8px 10px',
    textAlign: 'right' as const,
    fontSize: 11,
    fontWeight: 600,
    color: 'var(--text-secondary)',
    borderBottom: '2px solid var(--divider)',
    whiteSpace: 'nowrap',
    position: 'sticky' as const,
    top: 0,
    backgroundColor: 'var(--surface)',
    letterSpacing: '0.04em',
    textTransform: 'uppercase' as const,
  },
  rankCol: {
    textAlign: 'center' as const,
    width: 36,
  },
  playerCol: {
    textAlign: 'left' as const,
    minWidth: 140,
  },
  teamCol: {
    textAlign: 'center' as const,
    width: 50,
  },
  highlightedTh: {
    color: 'var(--nba-tint)',
    fontWeight: 700,
  },
  sortedTh: {
    color: 'var(--link)',
  },
  sortArrow: {
    fontSize: 9,
  },
  row: {
    borderBottom: '1px solid var(--divider)',
    transition: 'background-color 0.15s',
  },
  td: {
    padding: '7px 10px',
    textAlign: 'right' as const,
    color: 'var(--text-primary)',
    whiteSpace: 'nowrap',
    fontVariantNumeric: 'tabular-nums',
  },
  playerName: {
    textAlign: 'left' as const,
    fontWeight: 600,
    color: 'var(--link)',
  },
  teamText: {
    textAlign: 'center' as const,
    color: 'var(--text-secondary)',
    fontSize: 12,
  },
  highlightedTd: {
    fontWeight: 700,
    color: 'var(--text-primary)',
  },
  sortedTd: {
    backgroundColor: 'rgba(105,171,243,0.06)',
  },
  empty: {
    padding: 24,
    textAlign: 'center',
    color: 'var(--text-secondary)',
    fontSize: 14,
  },
};
