import React from 'react';
import type { SectionProps } from '../SectionRouter';

/**
 * AdSlot — placeholder renderer for ad placements.
 *
 * In production this component would delegate to the platform ad SDK
 * (e.g. Google Ad Manager).  For the prototype it renders a clearly-labelled
 * placeholder that shows the slot metadata (provider, sizes, targeting).
 */
export function AdSlot({ section }: SectionProps): React.ReactElement {
  const data = section.data as Record<string, unknown> | undefined;

  const provider = (data?.provider as string) ?? 'unknown';
  const adUnitPath = (data?.adUnitPath as string) ?? '';
  const sizes = (data?.sizes as number[][]) ?? [];
  const targeting = (data?.targeting as Record<string, string>) ?? {};
  const collapseOnEmpty = data?.collapseOnEmpty as boolean | undefined;
  const label = (data?.label as string) ?? 'Advertisement';

  // Pick the largest size to set placeholder dimensions
  const [width, height] = sizes.length > 0
    ? sizes.reduce((a, b) => (a[0] * a[1] >= b[0] * b[1] ? a : b))
    : [728, 90];

  return (
    <div style={styles.wrapper}>
      <span style={styles.label}>{label}</span>
      <div
        style={{
          ...styles.placeholder,
          width: Math.min(width, 728),
          height,
        }}
      >
        <div style={styles.inner}>
          <span style={styles.icon}>📢</span>
          <span style={styles.provider}>{provider.toUpperCase()}</span>
          <span style={styles.path}>{adUnitPath}</span>
          <span style={styles.meta}>
            {sizes.map(s => `${s[0]}×${s[1]}`).join(', ')}
          </span>
          {Object.keys(targeting).length > 0 && (
            <span style={styles.targeting}>
              {Object.entries(targeting).map(([k, v]) => `${k}=${v}`).join(' · ')}
            </span>
          )}
          {collapseOnEmpty && (
            <span style={styles.badge}>collapse-on-empty</span>
          )}
        </div>
      </div>
    </div>
  );
}

const styles: Record<string, React.CSSProperties> = {
  wrapper: {
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    margin: '8px 0',
    gap: 4,
  },
  label: {
    fontSize: 10,
    fontWeight: 600,
    color: '#888',
    textTransform: 'uppercase',
    letterSpacing: 1,
  },
  placeholder: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    border: '2px dashed #555',
    borderRadius: 8,
    backgroundColor: '#1a1a2e',
    overflow: 'hidden',
  },
  inner: {
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    gap: 4,
    padding: 12,
  },
  icon: {
    fontSize: 24,
  },
  provider: {
    fontSize: 14,
    fontWeight: 700,
    color: '#6c63ff',
    letterSpacing: 2,
  },
  path: {
    fontSize: 11,
    color: '#aaa',
    fontFamily: 'monospace',
  },
  meta: {
    fontSize: 11,
    color: '#ccc',
  },
  targeting: {
    fontSize: 10,
    color: '#888',
    fontStyle: 'italic',
  },
  badge: {
    fontSize: 9,
    fontWeight: 600,
    color: '#ffa726',
    border: '1px solid #ffa726',
    borderRadius: 4,
    padding: '1px 6px',
    marginTop: 2,
  },
};
