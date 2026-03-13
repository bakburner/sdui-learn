import React from 'react';
import type { AtomicProps } from './AtomicRouter';
import type { DataTableColumn } from './AtomicElement';

const variantFontSize: Record<string, number> = {
  labelSmall: 11, labelMedium: 12, labelLarge: 14,
  bodySmall: 12, bodyMedium: 14, bodyLarge: 16,
  titleSmall: 14, titleMedium: 16, titleLarge: 22,
};

function alignToCSS(align?: string): React.CSSProperties['textAlign'] {
  if (align === 'center') return 'center';
  if (align === 'end') return 'right';
  return 'left';
}

/**
 * AtomicDataTable — stateless, server-ordered grid.
 *
 * Use for simple tabular layouts. For sort, scroll-sync, frozen columns,
 * or row interactivity, use a dedicated section renderer.
 */
export function AtomicDataTable({ element }: AtomicProps): React.ReactElement | null {
  const { columns, rows, headerVariant, cellVariant, striped } = element;
  if (!columns || !rows) return null;

  const headerSize = variantFontSize[headerVariant ?? 'labelMedium'] ?? 12;
  const cellSize = variantFontSize[cellVariant ?? 'bodyMedium'] ?? 14;

  const tableStyle: React.CSSProperties = {
    width: '100%',
    borderCollapse: 'collapse',
  };

  const thStyle = (col: DataTableColumn): React.CSSProperties => ({
    fontSize: headerSize,
    fontWeight: 600,
    textAlign: alignToCSS(col.align),
    padding: '4px 8px',
    background: 'var(--surface-variant, #e0e0e0)',
    ...(col.width && col.width !== 'flex' ? { width: col.width } : {}),
  });

  const tdStyle = (col: DataTableColumn): React.CSSProperties => ({
    fontSize: cellSize,
    textAlign: alignToCSS(col.align),
    padding: '4px 8px',
    ...(col.width && col.width !== 'flex' ? { width: col.width } : {}),
  });

  return (
    <table style={tableStyle}>
      <thead>
        <tr>
          {columns.map((col) => (
            <th key={col.key} style={thStyle(col)}>{col.label}</th>
          ))}
        </tr>
      </thead>
      <tbody>
        {rows.map((row, idx) => (
          <tr
            key={idx}
            style={striped && idx % 2 === 1 ? { background: 'var(--surface-variant-dim, #f5f5f5)' } : undefined}
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
