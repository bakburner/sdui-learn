import React from 'react';
import type { SectionProps } from '../SectionRouter';
import type { GamePanelUiModel, GamePanelDisplayConfig } from '../../adapters/sectionUiAdapters';
import { mapGamePanel } from '../../adapters/sectionUiAdapters';
import { getPrimarySectionAction } from '../../utils/sectionActions';
import { resolveBackgroundCSS } from '../../utils/background';
import { DEFAULT_FALLBACK_IMAGE } from '../../utils/constants';
import { accessibilityProps } from '../../utils/accessibility';

export function GamePanel({ section, onAction }: SectionProps): React.ReactElement {
  const model = mapGamePanel(section);
  if (!model) {
    return <div style={{ color: '#fff', padding: 16 }}>No game data</div>;
  }
  return <GamePanelView model={model} section={section} onAction={onAction} />;
}

function GamePanelView({
  model,
  section,
  onAction,
}: {
  model: GamePanelUiModel;
  section: SectionProps['section'];
  onAction: SectionProps['onAction'];
}): React.ReactElement {
  const primaryAction = getPrimarySectionAction(section, 'onTap');
  const config = model.displayConfig;
  const isLive = model.visualState === 'LIVE';
  const styles = buildStyles(config, isLive);

  return (
    <button style={styles.container} onClick={() => primaryAction && onAction(primaryAction)} aria-label={`${model.awayName || model.awayTricode} vs ${model.homeName || model.homeTricode}`} {...accessibilityProps(section.accessibility)}>
      <div style={styles.topRow}>
        {model.visualLabel && <span style={styles.visualLabel}>{model.visualLabel}</span>}
        {model.badgeText && <span style={styles.badge}>{model.badgeText}</span>}
      </div>

      <div style={styles.matchup}>
        <div style={styles.teamCol}>
          {model.awayLogoUrl && (
            <img
              src={model.awayLogoUrl}
              alt={model.awayTricode}
              style={styles.logo}
              onError={(e) => { const img = e.currentTarget; if (img.src !== DEFAULT_FALLBACK_IMAGE) img.src = DEFAULT_FALLBACK_IMAGE; }}
            />
          )}
          <span style={styles.score} aria-live="polite">{model.awayScore}</span>
          <span style={styles.teamName}>{model.awayName || model.awayTricode}</span>
          {model.awayRecord && <span style={styles.record}>{model.awayRecord}</span>}
        </div>

        <div style={styles.centerCol}>
          <span style={styles.statusText}>{model.statusText}</span>
          {model.broadcaster && <span style={styles.broadcaster}>{model.broadcaster}</span>}
        </div>

        <div style={styles.teamCol}>
          {model.homeLogoUrl && (
            <img
              src={model.homeLogoUrl}
              alt={model.homeTricode}
              style={styles.logo}
              onError={(e) => { const img = e.currentTarget; if (img.src !== DEFAULT_FALLBACK_IMAGE) img.src = DEFAULT_FALLBACK_IMAGE; }}
            />
          )}
          <span style={styles.score} aria-live="polite">{model.homeScore}</span>
          <span style={styles.teamName}>{model.homeName || model.homeTricode}</span>
          {model.homeRecord && <span style={styles.record}>{model.homeRecord}</span>}
        </div>
      </div>
    </button>
  );
}

function buildStyles(config: GamePanelDisplayConfig, isLive: boolean): Record<string, React.CSSProperties> {
  const bg = isLive && config.liveBackground
    ? resolveBackgroundCSS(config.liveBackground)
    : resolveBackgroundCSS(config.background);

  const fallbackBg = Object.keys(bg).length === 0 ? { background: '#161b2b' } : {};
  const badgeColor = config.badgeColor ?? (isLive ? '#C8102E' : '#666666');
  const isProminent = config.scoreTextStyle === 'prominent';

  return {
    container: {
      position: 'relative',
      width: '100%',
      minHeight: config.cardHeight ? config.cardHeight : 'auto',
      borderRadius: config.cornerRadius,
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
      boxShadow: config.elevation > 0
        ? `0 ${config.elevation}px ${config.elevation * 2}px rgba(0,0,0,0.3)`
        : 'none',
      ...fallbackBg,
      ...bg,
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
      background: badgeColor,
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
      width: config.logoSize,
      height: config.logoSize,
      objectFit: 'contain' as const,
    },
    score: {
      fontSize: isProminent ? 28 : 18,
      fontWeight: isProminent ? 800 : 700,
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
    broadcaster: {
      fontSize: 11,
      color: 'rgba(255,255,255,0.6)',
    },
  };
}
