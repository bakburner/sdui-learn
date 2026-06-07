import { describe, expect, it } from 'vitest';
import { render } from '@testing-library/react';
import { AtomicBox } from './AtomicBox';
import { SizingMode, AtomicElementType, type AtomicElement } from '@sdui/models';

describe('AtomicBox sizing (widthMode / heightMode)', () => {
  it('applies full width when widthMode is fill', () => {
    const element: AtomicElement = {
      id: 'fill-w',
      type: AtomicElementType.Container,
      widthMode: SizingMode.Fill,
      children: [{ id: 't', type: AtomicElementType.Text, content: 'Hi' }],
    };

    const { container } = render(
      <AtomicBox element={element}>
        <span>child</span>
      </AtomicBox>,
    );

    const box = container.firstElementChild as HTMLElement;
    expect(box.style.width).toBe('100%');
  });

  it('does not read deprecated fillWidth from the wire shape', () => {
    const element = {
      id: 'legacy',
      type: AtomicElementType.Container,
      fillWidth: true,
      children: [{ id: 't', type: AtomicElementType.Text, content: 'Hi' }],
    } as AtomicElement;

    const { container } = render(
      <AtomicBox element={element}>
        <span>child</span>
      </AtomicBox>,
    );

    const box = container.firstElementChild as HTMLElement;
    expect(box.style.width).not.toBe('100%');
  });
});
