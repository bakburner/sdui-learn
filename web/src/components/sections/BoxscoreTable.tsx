import React, { useMemo } from 'react';
import type { SectionProps } from '../SectionRouter';
import { mapBoxscoreTable } from '../../adapters/sectionUiAdapters';
import { DEFAULT_FALLBACK_IMAGE } from '../../utils/constants';
import { accessibilityProps } from '../../utils/accessibility';

/**
 * BoxscoreTable — semantic tabular stat section for one team.
 *
 * Client owns all rendering decisions:
 * - Frozen player column (sticky left) with headshot, name, jersey/position
 * - Horizontally scrollable stat columns
 * - Client-side sort via screen state (mutate actions)
 * - Frozen totals row at bottom, excluded from sort
 * - DNP players with reason text
 * - Empty state when no player data
 */
export function BoxscoreTable({ section, state, onAction, onStateChange }: SectionProps): React.ReactElement {
  const model = mapBoxscoreTable(section, state);
  if (!model) {
    return <div style={styles.container}>No boxscore data available</div>;
  }

  // ── Empty state ──────────────────────────────────────────────────
  if (model.players.length === 0 && model.emptyMessage) {
    return (
      <div style={{ ...styles.container, backgroundColor: section.backgroundColor || 'var(--surface)' }}>
        {model.teamName && <div style={styles.teamHeader}>{model.teamName}</div>}
        <div style={styles.emptyMessage}>{model.emptyMessage}</div>
      </div>
    );
  }

  // ── Sort ──────────────────────────────────────────────────────────
  const sortCol = model.sortColumn;
  const sortDir = model.sortDirection;

  const sortedPlayers = useMemo(() => {
    if (!sortCol) return model.players;
    return [...model.players].sort((a, b) => {
      const aVal = a.stats[sortCol];
      const bVal = b.stats[sortCol];
      // DNP rows (no stats) sink to bottom
      if (aVal === undefined || aVal === null) return 1;
      if (bVal === undefined || bVal === null) return -1;
      const aNum = typeof aVal === 'number' ? aVal : parseFloat(String(aVal));
      const bNum = typeof bVal === 'number' ? bVal : parseFloat(String(bVal));
      if (isNaN(aNum) || isNaN(bNum)) return 0;
      return sortDir === 'asc' ? aNum - bNum : bNum - aNum;
    });
  }, [model.players, sortCol, sortDir]);

  const handleSort = (colKey: string) => {
    if (!model.sortStateKey || !model.sortDirectionStateKey) return;
    if (sortCol === colKey) {
      // Toggle direction
      onStateChange(model.sortDirectionStateKey, sortDir === 'asc' ? 'desc' : 'asc');
    } else {
      onStateChange(model.sortStateKey, colKey);
      onStateChange(model.sortDirectionStateKey, 'desc');
    }
  };

  const teamAccent = model.teamColor || 'var(--nba-blue)';

  return (
    <div style={{ ...styles.container, backgroundColor: section.backgroundColor || 'var(--surface)' }} {...accessibilityProps(section.accessibility)}>
      {/* Team header */}
      {model.teamName && (
        <div style={styles.teamHeader}>
          {model.teamLogoUrl && (
            <img src={model.teamLogoUrl} alt={model.teamTricode} style={styles.teamLogo}
              onError={(e) => { const img = e.currentTarget; if (img.src !== DEFAULT_FALLBACK_IMAGE) img.src = DEFAULT_FALLBACK_IMAGE; }} />
          )}
          <span>{model.teamName}</span>
        </div>
      )}

      {/* Table wrapper — horizontal scroll for stat columns */}
      <div style={styles.tableWrapper}>
        <table style={styles.table} cellSpacing={0} cellPadding={0}>
          {/* Column headers */}
          <thead>
            <tr>
              {/* Frozen player column header */}
              <th scope="col" style={{ ...styles.th, ...styles.frozenCol, ...styles.headerFrozen, borderBottom: `2px solid ${teamAccent}` }}>
                PLAYER
              </th>
              {model.columns.map((col) => {
                const isActive = sortCol === col.key;
                return (
                  <th
                    scope="col"
                    key={col.key}
                    style={{
                      ...styles.th,
                      cursor: col.sortable !== false ? 'pointer' : 'default',
                      fontWeight: col.highlighted || isActive ? 700 : 600,
                      color: isActive ? 'var(--text-primary)' : 'var(--text-secondary)',
                      borderBottom: `2px solid ${teamAccent}`,
                    }}
                    onClick={() => col.sortable !== false && handleSort(col.key)}
                    title={col.sortable !== false ? `Sort by ${col.label}` : undefined}
                  >
                    {col.label}
                    {isActive && <span style={styles.sortArrow}>{sortDir === 'asc' ? ' ▲' : ' ▼'}</span>}
                  </th>
                );
              })}
            </tr>
          </thead>

          <tbody>
            {/* Starters / bench divider is inferred from starter flag */}
            {sortedPlayers.map((player, idx) => {
              const isDnp = Object.keys(player.stats).length <= 1 && typeof player.stats.min === 'string' && player.stats.min !== '';
              const showDivider =
                idx > 0 &&
                sortedPlayers[idx - 1]?.starter &&
                !player.starter &&
                !sortCol; // divider only when unsorted or sorted by default

              return (
                <React.Fragment key={player.playerId}>
                  {showDivider && (
                    <tr>
                      <td colSpan={model.columns.length + 1} style={styles.benchDivider}>
                        BENCH
                      </td>
                    </tr>
                  )}
                  <tr
                    style={{
                      ...styles.row,
                      opacity: isDnp ? 0.5 : 1,
                      cursor: player.actions?.length ? 'pointer' : 'default',
                    }}
                    onClick={() => {
                      const act = player.actions?.find((a) => a.trigger === 'onTap') ?? player.actions?.[0];
                      if (act) onAction(act);
                    }}
                  >
                    {/* Frozen player cell */}
                    <td style={{ ...styles.td, ...styles.frozenCol, ...styles.playerCell }}>
                      <div style={styles.playerInner}>
                        {player.imageUrl ? (
                          <img
                            src={player.imageUrl}
                            alt={player.name}
                            style={styles.headshot}
                            loading="lazy"
                            onError={(e) => { const img = e.currentTarget; if (img.src !== DEFAULT_FALLBACK_IMAGE) img.src = DEFAULT_FALLBACK_IMAGE; }}
                          />
                        ) : (
                          <div style={styles.headshotPlaceholder}>
                            {player.name.charAt(0)}
                          </div>
                        )}
                        <div style={styles.playerInfo}>
                          <span style={styles.playerName}>{player.name}</span>
                          <span style={styles.playerMeta}>
                            #{player.jerseyNumber} {player.position}
                          </span>
                        </div>
                      </div>
                    </td>

                    {/* Stat cells */}
                    {model.columns.map((col) => {
                      if (isDnp && col.key !== 'min') {
                        return <td key={col.key} style={{ ...styles.td, ...styles.statCell }}>—</td>;
                      }
                      const val = player.stats[col.key];
                      return (
                        <td
                          key={col.key}
                          style={{
                            ...styles.td,
                            ...styles.statCell,
                            fontWeight: col.highlighted ? 700 : 400,
                          }}
                        >
                          {val !== undefined && val !== null ? String(val) : '—'}
                        </td>
                      );
                    })}
                  </tr>
                </React.Fragment>
              );
            })}

            {/* Team totals row — frozen at bottom, excluded from sort */}
            {model.teamTotals && (
              <tr style={styles.totalsRow}>
                <td style={{ ...styles.td, ...styles.frozenCol, ...styles.totalsLabel }}>
                  TOTALS
                </td>
                {model.columns.map((col) => {
                  const val = model.teamTotals![col.key];
                  return (
                    <td
                      key={col.key}
                      style={{
                        ...styles.td,
                        ...styles.statCell,
                        fontWeight: 700,
                        borderTop: `1px solid ${teamAccent}`,
                      }}
                    >
                      {val !== undefined && val !== null ? String(val) : '—'}
                    </td>
                  );
                })}
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}

// ── Styles ───────────────────────────────────────────────────────────

const styles: Record<string, React.CSSProperties> = {
  container: {
    borderRadius: 8,
    margin: 8,
    overflow: 'hidden',
  },
  teamHeader: {
    display: 'flex',
    alignItems: 'center',
    gap: 8,
    padding: '12px 16px',
    fontSize: 15,
    fontWeight: 700,
    fontFamily: 'var(--font-body)',
    color: 'var(--text-primary)',
    borderBottom: '1px solid var(--divider)',
  },
  teamLogo: {
    width: 24,
    height: 24,
    objectFit: 'contain',
  },
  emptyMessage: {
    padding: 32,
    textAlign: 'center',
    color: 'var(--text-secondary)',
    fontSize: 14,
  },
  tableWrapper: {
    overflowX: 'auto',
    WebkitOverflowScrolling: 'touch',
  },
  table: {
    width: 'max-content',
    minWidth: '100%',
    borderCollapse: 'collapse',
  },
  th: {
    padding: '8px 12px',
    fontSize: 11,
    fontWeight: 600,
    color: 'var(--text-secondary)',
    textAlign: 'right',
    whiteSpace: 'nowrap',
    userSelect: 'none',
    position: 'relative',
    fontFamily: 'var(--font-body)',
    letterSpacing: '0.04em',
    textTransform: 'uppercase' as const,
  },
  frozenCol: {
    position: 'sticky',
    left: 0,
    zIndex: 2,
    backgroundColor: 'var(--surface)',
    textAlign: 'left',
  },
  headerFrozen: {
    zIndex: 3,
    fontSize: 11,
    fontWeight: 700,
    color: 'var(--text-secondary)',
  },
  sortArrow: {
    fontSize: 9,
  },
  td: {
    padding: '8px 12px',
    fontSize: 13,
    color: 'var(--text-primary)',
    whiteSpace: 'nowrap',
    borderBottom: '1px solid var(--divider)',
    fontFamily: 'var(--font-body)',
  },
  playerCell: {
    minWidth: 140,
    maxWidth: 180,
  },
  playerInner: {
    display: 'flex',
    alignItems: 'center',
    gap: 8,
  },
  headshot: {
    width: 32,
    height: 32,
    borderRadius: 16,
    objectFit: 'cover',
    flexShrink: 0,
    backgroundColor: 'var(--surface-raised)',
  },
  headshotPlaceholder: {
    width: 32,
    height: 32,
    borderRadius: 16,
    backgroundColor: 'var(--surface-raised)',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    fontSize: 14,
    fontWeight: 700,
    color: 'var(--text-secondary)',
    flexShrink: 0,
  },
  playerInfo: {
    display: 'flex',
    flexDirection: 'column',
    overflow: 'hidden',
  },
  playerName: {
    fontSize: 13,
    fontWeight: 600,
    color: 'var(--text-primary)',
    textOverflow: 'ellipsis',
    overflow: 'hidden',
    whiteSpace: 'nowrap',
  },
  playerMeta: {
    fontSize: 11,
    color: 'var(--text-secondary)',
  },
  statCell: {
    textAlign: 'right',
    fontVariantNumeric: 'tabular-nums',
    minWidth: 44,
  },
  row: {
    cursor: 'pointer',
    transition: 'background-color 0.15s',
  },
  benchDivider: {
    padding: '4px 16px',
    fontSize: 10,
    fontWeight: 700,
    color: 'var(--text-secondary)',
    backgroundColor: 'var(--surface-alt)',
    letterSpacing: 1,
  },
  totalsRow: {
    backgroundColor: 'var(--surface-alt)',
  },
  totalsLabel: {
    fontWeight: 700,
    fontSize: 11,
    color: 'var(--text-secondary)',
    letterSpacing: 1,
  },
};
