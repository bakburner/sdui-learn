import React from 'react';
import type { SectionProps } from '../SectionRouter';
import type { GamePanelUiModel } from '../../adapters/sectionUiAdapters';
import { mapGamePanel } from '../../adapters/sectionUiAdapters';
import { getPrimarySectionAction } from '../../utils/sectionActions';
import { DEFAULT_FALLBACK_IMAGE } from '../../utils/constants';

export function GamePanel({ section, onAction }: SectionProps): React.ReactElement {
  const model = mapGamePanel(section);
  if (!model) {
    return <div style={standardStyles.container}>No game data</div>;
  }

  if (model.variant === 'featured') {
    return <FeaturedGamePanelView model={model} section={section} onAction={onAction} />;
  }

  return <StandardGamePanelView model={model} section={section} onAction={onAction} />;
}

function StandardGamePanelView({
  model,
  section,
  onAction,
}: {
  model: GamePanelUiModel;
  section: SectionProps['section'];
  onAction: SectionProps['onAction'];
}): React.ReactElement {
  const primaryAction = getPrimarySectionAction(section, 'onTap');

  const handleClick = () => {
    if (primaryAction) onAction(primaryAction);
  };

  return (
    <button style={standardStyles.container} onClick={handleClick}>
      <div style={standardStyles.teamsRow}>
        <div style={standardStyles.teamSide}>
          {model.awayLogoUrl && (
            <img src={model.awayLogoUrl} alt={model.awayTricode} style={standardStyles.logo}
              onError={(e) => { const img = e.currentTarget; if (img.src !== DEFAULT_FALLBACK_IMAGE) img.src = DEFAULT_FALLBACK_IMAGE; }} />
          )}
          <div style={standardStyles.teamInfo}>
            <span style={standardStyles.tricode}>{model.awayTricode}</span>
            {model.awayRecord && <span style={standardStyles.record}>{model.awayRecord}</span>}
          </div>
        </div>
        <span style={standardStyles.score}>{model.awayScore}</span>
        <span style={standardStyles.at}>@</span>
        <span style={standardStyles.score}>{model.homeScore}</span>
        <div style={{ ...standardStyles.teamSide, flexDirection: 'row-reverse' }}>
          {model.homeLogoUrl && (
            <img src={model.homeLogoUrl} alt={model.homeTricode} style={standardStyles.logo}
              onError={(e) => { const img = e.currentTarget; if (img.src !== DEFAULT_FALLBACK_IMAGE) img.src = DEFAULT_FALLBACK_IMAGE; }} />
          )}
          <div style={{ ...standardStyles.teamInfo, alignItems: 'flex-end' }}>
            <span style={standardStyles.tricode}>{model.homeTricode}</span>
            {model.homeRecord && <span style={standardStyles.record}>{model.homeRecord}</span>}
          </div>
        </div>
      </div>
      <div style={standardStyles.footer}>
        <span style={standardStyles.statusLine}>{model.statusText}</span>
        {model.broadcaster && <span style={standardStyles.broadcaster}>{model.broadcaster}</span>}
      </div>
    </button>
  );
}

function FeaturedGamePanelView({
  model,
  section,
  onAction,
}: {
  model: GamePanelUiModel;
  section: SectionProps['section'];
  onAction: SectionProps['onAction'];
}): React.ReactElement {
  const primaryAction = getPrimarySectionAction(section, 'onTap');
  const isLive = model.visualState === 'LIVE';

  const gradient = isLive
    ? 'linear-gradient(135deg, #1d428a 0%, #c8102e 100%)'
    : 'linear-gradient(135deg, #1d428a 0%, #0a1128 100%)';

  return (
    <button
      style={{ ...featuredStyles.container, background: gradient }}
      onClick={() => primaryAction && onAction(primaryAction)}
    >
      <div style={featuredStyles.topRow}>
        {model.visualLabel && <span style={featuredStyles.visualLabel}>{model.visualLabel}</span>}
        {model.badgeText && (
          <span style={{ ...featuredStyles.badge, background: isLive ? '#c8102e' : '#1d428a' }}>
            {model.badgeText}
          </span>
        )}
      </div>
      <div style={featuredStyles.matchup}>
        <div style={featuredStyles.teamCol}>
          {model.awayLogoUrl && (
            <img src={model.awayLogoUrl} alt={model.awayTricode} style={featuredStyles.logo}
              onError={(e) => { const img = e.currentTarget; if (img.src !== DEFAULT_FALLBACK_IMAGE) img.src = DEFAULT_FALLBACK_IMAGE; }} />
          )}
          <span style={featuredStyles.score}>{model.awayScore}</span>
          <span style={featuredStyles.teamName}>{model.awayName || model.awayTricode}</span>
          {model.awayRecord && <span style={featuredStyles.record}>{model.awayRecord}</span>}
        </div>
        <div style={featuredStyles.centerCol}>
          <span style={featuredStyles.statusText}>{model.statusText}</span>
        </div>
        <div style={featuredStyles.teamCol}>
          {model.homeLogoUrl && (
            <img src={model.homeLogoUrl} alt={model.homeTricode} style={featuredStyles.logo}
              onError={(e) => { const img = e.currentTarget; if (img.src !== DEFAULT_FALLBACK_IMAGE) img.src = DEFAULT_FALLBACK_IMAGE; }} />
          )}
          <span style={featuredStyles.score}>{model.homeScore}</span>
          <span style={featuredStyles.teamName}>{model.homeName || model.homeTricode}</span>
          {model.homeRecord && <span style={featuredStyles.record}>{model.homeRecord}</span>}
        </div>
      </div>
    </button>
  );
}

const standardStyles: Record<string, React.CSSProperties> = {
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

const featuredStyles: Record<string, React.CSSProperties> = {
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
