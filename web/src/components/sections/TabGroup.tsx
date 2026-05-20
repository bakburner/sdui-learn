import React from 'react';
import type { SectionProps } from '../SectionRouter';
import { SectionList } from '../SectionRouter';
import { mapTabGroup } from '../../adapters/sectionUiAdapters';
import { accessibilityProps } from '../../utils/accessibility';

/**
 * TabGroup - tabbed container with nested sections per tab.
 */
export function TabGroup({ section, state, onAction, onStateChange }: SectionProps): React.ReactElement {
  const model = mapTabGroup(section, state);
  if (!model) {
    return <div style={styles.container}>No tabs available</div>;
  }

  const handleTabClick = (stateValue: string) => {
    onStateChange(model.stateKey, stateValue);
  };

  const activeTab = model.tabs.find((t) => t.isActive);
  const activePanelKey = activeTab?.stateValue ?? activeTab?.id ?? 'none';

  return (
    <div style={{ ...styles.container, backgroundColor: section.backgroundColor || 'transparent' }} {...accessibilityProps(section.accessibility)}>
      {/* Tab Bar */}
      <div style={styles.tabBar} role="tablist">
        {model.tabs.map((tab) => (
          <button
            key={tab.id}
            role="tab"
            aria-selected={tab.isActive}
            style={{
              ...styles.tab,
              ...(tab.isActive ? styles.activeTab : {}),
            }}
            onClick={() => handleTabClick(tab.stateValue)}
          >
            {tab.label}
          </button>
        ))}
      </div>

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
          <div key={activePanelKey} style={styles.emptyContent}>
            No content for this tab
          </div>
        )}
      </div>
    </div>
  );
}

const styles: Record<string, React.CSSProperties> = {
  container: {
    margin: 8,
    borderRadius: 12,
    overflow: 'hidden',
  },
  tabBar: {
    display: 'flex',
    gap: 4,
    padding: '8px 12px',
    backgroundColor: '#1a1a2e',
    overflowX: 'auto',
    scrollbarWidth: 'none',
  },
  tab: {
    padding: '10px 20px',
    border: 'none',
    borderRadius: 20,
    backgroundColor: 'transparent',
    color: '#888888',
    fontSize: 14,
    fontWeight: 600,
    cursor: 'pointer',
    transition: 'all 0.2s',
    whiteSpace: 'nowrap',
  },
  activeTab: {
    backgroundColor: '#3a3a5e',
    color: '#ffffff',
  },
  tabContent: {
    padding: 8,
    minHeight: 64,
    position: 'relative',
  },
  tabPanelLayer: {
    transition: 'opacity 200ms ease',
    animation: 'sduiTabContentFade 200ms ease',
  },
  emptyContent: {
    padding: 24,
    textAlign: 'center',
    color: '#666666',
  },
};
