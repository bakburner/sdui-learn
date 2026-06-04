import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { AtomicOverlayContainer } from './AtomicOverlayContainer';
import type { AtomicElement } from '@sdui/models';
import { AtomicElementType } from '@sdui/models';

describe('AtomicOverlayContainer — DOM structure', () => {
  it('renders base then overlay layers with absolute positioning', () => {
    const base: AtomicElement = {
      id: 'img-base',
      type: AtomicElementType.Image,
      src: 'https://example.com/hero.jpg',
      fit: 'cover',
      widthMode: 'fill',
    } as AtomicElement;

    const element: AtomicElement = {
      id: 'oc-1',
      type: AtomicElementType.OverlayContainer,
      base,
      overlays: [
        {
          alignment: 'bottomStart',
          element: {
            id: 'copy',
            type: AtomicElementType.Text,
            content: 'Title',
            variant: 'titleMedium',
            accessibility: { label: 'Hero headline' },
          } as AtomicElement,
        },
        {
          alignment: 'topEnd',
          element: {
            id: 'btn',
            type: AtomicElementType.Button,
            label: 'More',
            variant: 'text',
          } as AtomicElement,
        },
      ],
    } as AtomicElement;

    const { container } = render(
      <AtomicOverlayContainer element={element} state={{}} onAction={() => {}} depth={0} />,
    );

    const abs = container.querySelectorAll('[style*="position: absolute"]');
    expect(abs.length).toBeGreaterThanOrEqual(2);

    const withLabel = screen.getByLabelText('Hero headline');
    expect(withLabel).toBeTruthy();
    // Overlay *layers* are plain positioned wrappers; the leaf Text still receives
    // `accessibility` from the wire (see AtomicText).
  });
});
