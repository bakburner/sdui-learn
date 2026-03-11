import React from 'react';
import type { SectionProps } from '../SectionRouter';
import { mapSectionHeader } from '../../adapters/sectionUiAdapters';

export function SectionHeader({ section, onAction }: SectionProps): React.ReactElement | null {
  const model = mapSectionHeader(section);
  if (!model) return null;

  return (
    <div style={styles.container}>
      <div style={styles.textCol}>
        <div style={styles.title}>{model.title}</div>
        {model.subtitle && <div style={styles.subtitle}>{model.subtitle}</div>}
      </div>
      {model.action && (
        <button
          style={styles.actionBtn}
          onClick={() => model.action && onAction(model.action)}
        >
          {model.action.label || 'See All'}
        </button>
      )}
    </div>
  );
}

const styles: Record<string, React.CSSProperties> = {
  container: {
    display: 'flex',
    alignItems: 'baseline',
    justifyContent: 'space-between',
    padding: '16px 0 6px',
  },
  textCol: {
    display: 'flex',
    flexDirection: 'column',
    gap: 2,
  },
  title: {
    fontSize: 18,
    fontWeight: 700,
    color: '#fff',
  },
  subtitle: {
    fontSize: 13,
    color: '#7a8baa',
  },
  actionBtn: {
    background: 'none',
    border: 'none',
    color: '#5b9cf6',
    fontSize: 13,
    fontWeight: 600,
    cursor: 'pointer',
    padding: 0,
    whiteSpace: 'nowrap',
  },
};
