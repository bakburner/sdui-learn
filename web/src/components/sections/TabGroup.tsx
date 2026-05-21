import React, { useMemo } from 'react';
import type { Data } from '@sdui/models';
import type { SectionProps } from '../SectionRouter';
import { SectionList } from '../SectionRouter';
import { AtomicRouter } from '../atomic';
import { mapTabGroup } from '../../adapters/sectionUiAdapters';
import { accessibilityProps } from '../../utils/accessibility';
import { getSubsectionPrimaryAction } from '../../utils/sectionActions';
import { useColorTokenResolver, usePrefersColorScheme } from '../../utils/ColorTokenResolver';
import { currentFormFactor, resolveLayoutScalar } from '../../utils/LayoutTokenResolver';

/** Native tab-row presentation tokens (aligned with `secondaryStripSurface`). */
const TAB_LABEL_PRIMARY = 'token:nba.label.primary';
const TAB_LABEL_SECONDARY = 'token:nba.label.secondary';
const TAB_ACCENT_BRAND = 'token:nba.label.accent.brand';
const TAB_DIVIDER = 'token:nba.divider.moderate';
const TAB_PAD_H = 'token:nba.spacing.md';
const TAB_PAD_V = 'token:nba.spacing.sm';
const TAB_EMPTY_PAD = 'token:nba.spacing.lg';

/**
 * TabGroup — thin host for tabbed section routing.
 *
 * Server-owned: `section.surface` (chrome), `section.subsections` (per-tab
 * mutate actions), `data.tabs` / `data.tabContents` / `data.stateKey`.
 * Optional `data.ui` is the tab header only (atomic tree).
 *
 * Client-owned (allowed exception): platform-native tab row when `data.ui` is
 * absent — presentation realization only; selection still flows through
 * declared mutate actions, not ad-hoc state writes.
 */
export function TabGroup({ section, state, onAction, onStateChange }: SectionProps): React.ReactElement {
  const model = mapTabGroup(section, state);
  const data = section.data as Data | undefined;
  const scheme = usePrefersColorScheme();
  const resolveColor = useColorTokenResolver();
  const formFactor = currentFormFactor();

  const tokenStyles = useMemo(() => {
    const padH = resolveLayoutScalar(TAB_PAD_H, formFactor, scheme);
    const padV = resolveLayoutScalar(TAB_PAD_V, formFactor, scheme);
    const emptyPad = resolveLayoutScalar(TAB_EMPTY_PAD, formFactor, scheme);
    return {
      tabBar: {
        borderBottom: `1px solid ${resolveColor(TAB_DIVIDER) ?? 'var(--divider)'}`,
      },
      tab: {
        padding: `${padV}px ${padH}px`,
        color: resolveColor(TAB_LABEL_SECONDARY) ?? 'var(--text-secondary)',
        borderBottomColor: 'transparent',
      },
      tabActive: {
        color: resolveColor(TAB_LABEL_PRIMARY) ?? 'var(--text-primary)',
        borderBottomColor: resolveColor(TAB_ACCENT_BRAND) ?? 'var(--nba-blue)',
      },
      emptyContent: {
        padding: emptyPad,
        color: resolveColor(TAB_LABEL_SECONDARY) ?? 'var(--text-secondary)',
      },
    };
  }, [resolveColor, formFactor, scheme]);

  if (!model) {
    return <div style={styles.root}>No tabs available</div>;
  }

  const handleTabClick = (tabId: string) => {
    const action = getSubsectionPrimaryAction(section, tabId);
    if (action) {
      onAction(action);
    } else {
      const tab = model?.tabs.find((t) => t.id === tabId);
      if (tab && model) {
        console.warn('[TabGroup] missing subsection mutate action; falling back to tab stateValue', {
          sectionId: section.id,
          tabId,
        });
        onStateChange(model.stateKey, tab.stateValue);
      } else {
        console.warn('[TabGroup] missing subsection mutate action', {
          sectionId: section.id,
          tabId,
        });
      }
    }
  };

  const activePanelKey = model.tabs.find((t) => t.isActive)?.stateValue ?? 'none';

  return (
    <div style={styles.root} {...accessibilityProps(section.accessibility)}>
      {data?.ui ? (
        <AtomicRouter
          element={data.ui}
          state={state}
          onAction={onAction}
          onStateChange={onStateChange}
        />
      ) : (
        <div style={{ ...styles.tabBar, ...tokenStyles.tabBar }} role="tablist">
          {model.tabs.map((tab) => (
            <button
              key={tab.id}
              type="button"
              role="tab"
              aria-selected={tab.isActive}
              style={{
                ...styles.tab,
                ...tokenStyles.tab,
                ...(tab.isActive ? { ...styles.tabActive, ...tokenStyles.tabActive } : undefined),
              }}
              onClick={() => handleTabClick(tab.id)}
            >
              {tab.label}
            </button>
          ))}
        </div>
      )}

      <div style={styles.tabContent} role="tabpanel">
        {model.activeSections.length > 0 ? (
          <div key={activePanelKey} style={styles.tabPanelLayer}>
            <SectionList
              sections={model.activeSections}
              state={state}
              onAction={onAction}
              onStateChange={onStateChange}
            />
          </div>
        ) : (
          <div key={activePanelKey} style={{ ...styles.emptyContent, ...tokenStyles.emptyContent }}>
            No content for this tab
          </div>
        )}
      </div>
    </div>
  );
}

const styles: Record<string, React.CSSProperties> = {
  root: {
    width: '100%',
    overflow: 'hidden',
  },
  tabBar: {
    display: 'flex',
    width: '100%',
    overflowX: 'auto',
    scrollbarWidth: 'none',
  },
  tab: {
    flex: '0 0 auto',
    border: 'none',
    backgroundColor: 'transparent',
    fontSize: 'inherit',
    fontWeight: 600,
    cursor: 'pointer',
    whiteSpace: 'nowrap',
    borderBottomStyle: 'solid',
    borderBottomWidth: 2,
    borderBottomColor: 'transparent',
    marginBottom: -1,
  },
  tabActive: {},
  tabContent: {
    minHeight: 64,
    position: 'relative',
  },
  tabPanelLayer: {
    animation: 'sduiTabContentFade 200ms ease',
  },
  emptyContent: {
    textAlign: 'center',
  },
};
