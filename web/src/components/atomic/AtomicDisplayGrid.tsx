import React from 'react';
import type { AtomicProps } from './AtomicRouter';
import type { DisplayGridColumn } from './AtomicElement';
import { accessibilityProps } from '../../utils/accessibility';

const HEADER_FONT_SIZE = 12;
const CELL_FONT_SIZE = 14;

function alignToCSS(align?: string): React.CSSProperties['textAlign'] {
  if (align === 'center') return 'center';
  if (align === 'end') return 'right';
  return 'left';
}

/**
 * AtomicDisplayGrid — display-only, non-interactive, server-ordered grid of text cells.
 *
 * Zero client interaction: no sort, no filter, no expand, no select, no tap.
 * Cell values are pre-formatted strings — no client-side formatting or computation.
 *
 * For sort, scroll-sync, frozen columns, pagination, or row interactivity,
 * use a dedicated section renderer.
 */
export function AtomicDisplayGrid({ element }: AtomicProps): React.ReactElement | null {
  const { columns, rows, striped } = element;
  if (!columns || !rows) return null;

  const tableStyle: React.CSSProperties = {
    width: '100%',
    borderCollapse: 'collapse',
  };

  const thStyle = (col: DisplayGridColumn): React.CSSProperties => ({
    fontSize: HEADER_FONT_SIZE,
    fontWeight: 600,
    textAlign: alignToCSS(col.align),
    padding: '4px 8px',
    background: 'var(--surface-raised, #2B2F37)',
    color: 'var(--text-primary, #FFFFFF)',
    ...(col.width && col.width !== 'flex' ? { width: col.width } : {}),
  });

  const tdStyle = (col: DisplayGridColumn): React.CSSProperties => ({
    fontSize: CELL_FONT_SIZE,
    textAlign: alignToCSS(col.align),
    padding: '4px 8px',
    ...(col.width && col.width !== 'flex' ? { width: col.width } : {}),
  });

  return (
    <table style={tableStyle} {...accessibilityProps(element.accessibility)}>
      <thead>
        <tr>
          {columns.map((col) => (
            <th key={col.key} style={thStyle(col)} scope="col">{col.label}</th>
          ))}
        </tr>
      </thead>
      <tbody>
        {rows.map((row, idx) => (
          <tr
            key={idx}
            style={striped && idx % 2 === 1 ? { background: 'var(--surface-alt, #2B2F37)' } : { background: 'var(--surface, #191C23)' }}
          >
            {columns.map((col) => (
              <td key={col.key} style={tdStyle(col)}>{row[col.key] ?? ''}</td>
            ))}
          </tr>
        ))}
      </tbody>
    </table>
  );
}
