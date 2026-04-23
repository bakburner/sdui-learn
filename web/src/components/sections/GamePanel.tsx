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
    return <div style={{ color: 'var(--text-primary)', padding: 16, fontFamily: 'var(--font-body)' }}>No game data</div>;
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
  const isFeatured = model.variant === 'featured';
  const styles = buildStyles(config, isLive, isFeatured);

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

function buildStyles(config: GamePanelDisplayConfig, isLive: boolean, isFeatured: boolean): Record<string, React.CSSProperties> {
  const bg = isLive && config.liveBackground
    ? resolveBackgroundCSS(config.liveBackground)
    : resolveBackgroundCSS(config.background);

  const fallbackBg = Object.keys(bg).length === 0 ? { background: 'var(--game-card-bg)' } : {};
  const badgeColor = config.badgeColor ?? (isLive ? 'var(--live)' : 'var(--text-secondary)');
  const isProminent = config.scoreTextStyle === 'prominent';
  // featured = wider emphasis card (carousel lead); standard/missing keeps the displayConfig-driven defaults.
  const effectiveRadius = isFeatured && !config.cornerRadius ? 16 : (config.cornerRadius ?? 4);
  const featuredShadow = '0 6px 20px rgba(0,0,0,0.28), 0 2px 6px rgba(0,0,0,0.16)';
  const legacyShadow = config.elevation > 0
    ? `0 ${config.elevation}px ${config.elevation * 2}px rgba(0,0,0,0.3)`
    : 'none';

  return {
    container: {
      position: 'relative',
      width: '100%',
      minHeight: config.cardHeight ? config.cardHeight : 'auto',
      borderRadius: effectiveRadius,
      border: 'none',
      color: 'var(--text-primary)',
      padding: isFeatured ? '20px 24px' : '16px 20px',
      cursor: 'pointer',
      display: 'flex',
      flexDirection: 'column',
      justifyContent: 'space-between',
      gap: isFeatured ? 12 : 8,
      margin: '8px 0',
      textAlign: 'center',
      boxShadow: isFeatured ? featuredShadow : legacyShadow,
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
      fontWeight: 700,
      fontFamily: 'var(--font-body)',
      color: 'var(--text-on-image-50)',
      textTransform: 'uppercase' as const,
      letterSpacing: '0.08em',
    },
    badge: {
      fontSize: 11,
      fontWeight: 700,
      fontFamily: 'var(--font-body)',
      textTransform: 'uppercase' as const,
      padding: '3px 10px',
      borderRadius: 4,
      letterSpacing: '0.04em',
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
      fontSize: isProminent ? 32 : 20,
      fontWeight: 800,
      fontFamily: 'var(--font-headline)',
      fontVariantNumeric: 'tabular-nums',
    },
    teamName: {
      fontSize: 12,
      fontWeight: 600,
      fontFamily: 'var(--font-body)',
      opacity: 0.85,
    },
    record: {
      fontSize: 11,
      fontFamily: 'var(--font-body)',
      color: 'var(--text-on-image-50)',
    },
    centerCol: {
      display: 'flex',
      flexDirection: 'column',
      alignItems: 'center',
      gap: 4,
    },
    statusText: {
      fontSize: 13,
      fontFamily: 'var(--font-body)',
      color: 'var(--text-on-image-50)',
    },
    broadcaster: {
      fontSize: 11,
      fontFamily: 'var(--font-body)',
      color: 'var(--text-on-image-50)',
    },
  };
}
