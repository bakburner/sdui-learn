import React from 'react';
import type { Navigation, NavigationItem } from '@sdui/models';

interface TopNavigationBarProps {
  navigation?: Navigation;
  onNavigate: (targetUri: string) => void;
}

export function TopNavigationBar({ navigation, onNavigate }: TopNavigationBarProps): React.ReactElement | null {
  const items = navigation?.items ?? [];
  if (!items.length) return null;

  return (
    <nav style={styles.nav}>
      {items.map((item) => (
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
            {item.label}
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
      ))}
    </nav>
  );
}

const styles: Record<string, React.CSSProperties> = {
  nav: {
    display: 'flex',
    gap: 8,
    alignItems: 'center',
    padding: '8px 16px',
    borderBottom: '1px solid #2a2f45',
    backgroundColor: '#11172a',
    position: 'sticky',
    top: 0,
    zIndex: 10,
  },
  itemWrapper: {
    position: 'relative',
  },
  itemButton: {
    border: 'none',
    background: 'transparent',
    color: '#d0d7e5',
    padding: '8px 10px',
    borderRadius: 8,
    cursor: 'pointer',
    fontWeight: 600,
  },
  itemButtonSelected: {
    background: '#ff6b6b',
    color: '#fff',
  },
  dropdown: {
    display: 'flex',
    flexDirection: 'column',
    position: 'absolute',
    top: '100%',
    left: 0,
    background: '#1a1f2e',
    border: '1px solid #2a2f45',
    borderRadius: 8,
    minWidth: 180,
    padding: 6,
  },
  dropdownItem: {
    width: '100%',
    border: 'none',
    background: 'transparent',
    color: '#d0d7e5',
    textAlign: 'left',
    padding: '8px',
    borderRadius: 6,
    cursor: 'pointer',
  },
};
