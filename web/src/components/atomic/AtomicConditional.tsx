import React from 'react';
import type { AtomicProps } from './AtomicRouter';
import { AtomicRouter } from './AtomicRouter';

/**
 * AtomicConditional — evaluates a dot-path condition against state
 * and renders either trueChild or falseChild.
 */
export function AtomicConditional({ element, state, onAction, depth = 0 }: AtomicProps): React.ReactElement | null {
  const conditionMet = evaluateCondition(element.condition, state);
  const child = conditionMet ? element.trueChild : element.falseChild;
  if (!child) return null;
  return <AtomicRouter element={child} state={state} onAction={onAction} depth={depth} />;
}

function evaluateCondition(condition: string | undefined, state: Record<string, unknown>): boolean {
  if (!condition) return false;
  const parts = condition.split('.');
  let current: unknown = state;
  for (const part of parts) {
    if (current != null && typeof current === 'object' && part in (current as Record<string, unknown>)) {
      current = (current as Record<string, unknown>)[part];
    } else {
      return false;
    }
  }
  if (typeof current === 'boolean') return current;
  if (typeof current === 'string') return current.length > 0;
  if (typeof current === 'number') return current !== 0;
  return current != null;
}
