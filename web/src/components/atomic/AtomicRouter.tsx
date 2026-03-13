import React from 'react';
import type { Action } from '@sdui/models';
import type { AtomicElement } from './AtomicElement';

import { AtomicContainer } from './AtomicContainer';
import { AtomicText } from './AtomicText';
import { AtomicImage } from './AtomicImage';
import { AtomicButton } from './AtomicButton';
import { AtomicSpacer } from './AtomicSpacer';
import { AtomicDivider } from './AtomicDivider';
import { AtomicScrollContainer } from './AtomicScrollContainer';
import { AtomicConditional } from './AtomicConditional';
import { AtomicDataTable } from './AtomicDataTable';

export interface AtomicProps {
  element: AtomicElement;
  state: Record<string, unknown>;
  onAction: (action: Action) => void;
}

/**
 * AtomicRouter — routes an AtomicElement to the correct primitive renderer.
 */
export function AtomicRouter({ element, state, onAction }: AtomicProps): React.ReactElement | null {
  switch (element.type) {
    case 'Container':
      return <AtomicContainer element={element} state={state} onAction={onAction} />;
    case 'Text':
      return <AtomicText element={element} state={state} onAction={onAction} />;
    case 'Image':
      return <AtomicImage element={element} state={state} onAction={onAction} />;
    case 'Button':
      return <AtomicButton element={element} state={state} onAction={onAction} />;
    case 'Spacer':
      return <AtomicSpacer element={element} state={state} onAction={onAction} />;
    case 'Divider':
      return <AtomicDivider element={element} state={state} onAction={onAction} />;
    case 'ScrollContainer':
      return <AtomicScrollContainer element={element} state={state} onAction={onAction} />;
    case 'Conditional':
      return <AtomicConditional element={element} state={state} onAction={onAction} />;
    case 'DataTable':
      return <AtomicDataTable element={element} state={state} onAction={onAction} />;
    default:
      console.debug(`[AtomicRouter] Unknown element type: ${element.type}`);
      return null;
  }
}
