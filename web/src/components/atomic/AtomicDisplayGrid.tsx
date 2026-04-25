import React from 'react';
import type { AtomicProps } from './AtomicRouter';
import type { Column } from '@sdui/models';
import { AtomicBox } from './AtomicBox';
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
 *
 * Box-model chrome (margin / padding / background / cornerRadius / shadow /
 * border) is applied by AtomicBox; the <table> only owns its grid.
 */
export function AtomicDisplayGrid({ element }: AtomicProps): React.ReactElement | null {
  const { columns, rows, striped } = element;
  if (!columns || !rows) return null;

  const tableStyle: React.CSSProperties = {
    width: '100%',
    tableLayout: 'fixed',
    borderCollapse: 'collapse',
  };

  const resolveWidth = (w: Column['width']): number | undefined =>
    typeof w === 'number' ? w : undefined;

  const thStyle = (col: Column): React.CSSProperties => ({
    fontSize: HEADER_FONT_SIZE,
    fontWeight: 600,
    textAlign: alignToCSS(col.align),
    padding: '4px 8px',
    background: 'var(--surface-raised, #2B2F37)',
    color: 'var(--text-primary, #FFFFFF)',
    ...(resolveWidth(col.width) != null ? { width: resolveWidth(col.width) } : {}),
  });

  const tdStyle = (col: Column): React.CSSProperties => ({
    fontSize: CELL_FONT_SIZE,
    textAlign: alignToCSS(col.align),
    padding: '4px 8px',
    ...(resolveWidth(col.width) != null ? { width: resolveWidth(col.width) } : {}),
  });

  return (
    <AtomicBox element={element} styleOverrides={{ width: '100%' }}>
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
    </AtomicBox>
  );
}
