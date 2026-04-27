import React from 'react';
import type { Navigation, NavigationItem } from '@sdui/models';
import { IconTokenResolver } from '../utils/IconTokenResolver';

interface TopNavigationBarProps {
  navigation?: Navigation;
  onNavigate: (targetUri: string) => void;
}

export function TopNavigationBar({ navigation, onNavigate }: TopNavigationBarProps): React.ReactElement | null {
  const items = navigation?.items ?? [];
  if (!items.length) return null;

  return (
    <nav style={styles.nav}>
      {items.map((item) => {
        const iconName = IconTokenResolver.resolve(item.icon);
        return (
          <div key={item.id} style={styles.itemWrapper}>
            <button
              style={{
                ...styles.itemButton,
                ...(item.selected ? styles.itemButtonSelected : {}),
              }}
              onClick={() => {
                if (item.targetUri) onNavigate(item.targetUri);
              }}
            >
              {iconName ? (
                <span
                  className="material-icons"
                  style={styles.icon}
                  aria-hidden="true"
                >
                  {iconName}
                </span>
              ) : null}
              <span>{item.label}</span>
            </button>
            {item.children?.length ? (
              <div style={styles.dropdown}>
                {item.children.map((child: NavigationItem) => (
                  <button
                    key={child.id}
                    style={styles.dropdownItem}
                    onClick={() => child.targetUri && onNavigate(child.targetUri)}
                  >
                    {child.label}
                  </button>
                ))}
              </div>
            ) : null}
          </div>
        );
      })}
    </nav>
  );
}

const styles: Record<string, React.CSSProperties> = {
  nav: {
    display: 'flex',
    gap: 4,
    alignItems: 'center',
    padding: '8px 16px',
    borderBottom: '1px solid var(--divider)',
    backgroundColor: 'var(--surface)',
    position: 'sticky',
    top: 0,
    zIndex: 10,
    overflowX: 'auto',
    overflowY: 'hidden',
    WebkitOverflowScrolling: 'touch',
    scrollbarWidth: 'none',
  },
  itemWrapper: {
    position: 'relative',
    flex: '0 0 auto',
  },
  itemButton: {
    border: 'none',
    background: 'transparent',
    color: 'var(--text-secondary)',
    padding: '8px 12px',
    borderRadius: 'var(--rounded-base)',
    cursor: 'pointer',
    fontWeight: 600,
    fontSize: 14,
    fontFamily: 'var(--font-body)',
    transition: 'color 150ms ease, background 150ms ease',
    letterSpacing: '0.01em',
    display: 'inline-flex',
    alignItems: 'center',
    gap: 6,
    whiteSpace: 'nowrap',
  },
  itemButtonSelected: {
    background: 'var(--nba-blue)',
    color: '#FFFFFF',
  },
  icon: {
    fontSize: 18,
    lineHeight: 1,
  },
  dropdown: {
    display: 'flex',
    flexDirection: 'column',
    position: 'absolute',
    top: '100%',
    left: 0,
    background: 'var(--surface-raised)',
    border: '1px solid var(--divider)',
    borderRadius: 'var(--rounded-lg)',
    minWidth: 180,
    padding: 6,
    boxShadow: 'var(--shadow-lg)',
  },
  dropdownItem: {
    width: '100%',
    border: 'none',
    background: 'transparent',
    color: 'var(--text-primary)',
    textAlign: 'left',
    padding: '8px 12px',
    borderRadius: 'var(--rounded-base)',
    cursor: 'pointer',
    fontSize: 14,
    fontFamily: 'var(--font-body)',
    transition: 'background 150ms ease',
  },
};
