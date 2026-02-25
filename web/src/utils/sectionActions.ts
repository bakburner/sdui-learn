import type { Action, Section } from '@sdui/models';

/**
 * Shared section interaction helpers to keep section components
 * focused on rendering, not action parsing.
 *
 * Resolves from schema-level section.actions first, falling back
 * to legacy section.data.actions for backward compatibility.
 */
export function getSectionActions(section: Section): Action[] {
  if (section.actions?.length) {
    return section.actions;
  }
  const data = section.data as Record<string, unknown> | undefined;
  const actions = data?.actions as Action[] | undefined;
  return actions ?? [];
}

export function getPrimarySectionAction(section: Section, trigger: string = 'onTap'): Action | undefined {
  const normalizedTrigger = trigger.toLowerCase();
  return getSectionActions(section).find(
    (action) => (action.trigger ?? '').toLowerCase() === normalizedTrigger
  );
}

export function getSubsectionActions(section: Section, subsectionId: string): Action[] {
  const subsection = section.subsections?.find((s) => s.id === subsectionId);
  return subsection?.actions ?? [];
}

export function getSubsectionPrimaryAction(
  section: Section,
  subsectionId: string,
  trigger: string = 'onTap'
): Action | undefined {
  const normalizedTrigger = trigger.toLowerCase();
  return getSubsectionActions(section, subsectionId).find(
    (action) => (action.trigger ?? '').toLowerCase() === normalizedTrigger
  );
}

