import React, { useMemo } from 'react';
import type { AtomicProps } from './AtomicRouter';
import type { Section } from '@sdui/models';
import { SectionRouter } from '../SectionRouter';

const MAX_SECTION_SLOT_DEPTH = 2;

/**
 * AtomicSectionSlot — delegates rendering back to SectionRouter, completing
 * the bidirectional bridge between the atomic and section layers.
 *
 * The circular module dependency (SectionRouter → atomic/ → AtomicRouter →
 * AtomicSectionSlot → SectionRouter) is safe because all imports are only
 * used inside React component functions, not at module evaluation time.
 */
export function AtomicSectionSlot({
  element,
  state,
  onAction,
  onStateChange,
  sectionSlotDepth = 0,
  onSectionReplace,
  onSectionGone,
}: AtomicProps): React.ReactElement | null {
  if (sectionSlotDepth >= MAX_SECTION_SLOT_DEPTH) {
    console.warn(`[AtomicSectionSlot] Max SectionSlot depth (${MAX_SECTION_SLOT_DEPTH}) exceeded — skipping element: ${element.id}`);
    return null;
  }

  if (!element.section) {
    console.warn(`[AtomicSectionSlot] SectionSlot element ${element.id} has no section payload`);
    return null;
  }

  const section = element.section as unknown as Section;
  const noopStateChange = useMemo(() => onStateChange ?? ((_k: string, _v: unknown) => {}), [onStateChange]);

  return (
    <SectionRouter
      section={section}
      state={state}
      onAction={onAction}
      onStateChange={noopStateChange}
      onSectionReplace={onSectionReplace}
      onSectionGone={onSectionGone}
    />
  );
}
