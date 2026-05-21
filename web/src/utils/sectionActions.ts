import type { Action, Section } from '@sdui/models';

/**
 * Shared section interaction helpers to keep section components
 * focused on rendering, not action parsing.
 */
export function getSectionActions(section: Section): Action[] {
  return section.actions ?? [];
}

export function getPrimarySectionAction(section: Section, trigger: string = 'onActivate'): Action | undefined {
  const normalizedTrigger = trigger.toLowerCase();
  const list = getSectionActions(section);
  const match = list.find((action) => (action.trigger ?? '').toLowerCase() === normalizedTrigger);
  if (match) return match;
  if (normalizedTrigger === 'onactivate') {
    return list.find((action) => (action.trigger ?? '').toLowerCase() === 'ontap');
  }
  return undefined;
}

export function getSubsectionActions(section: Section, subsectionId: string): Action[] {
  const subsection = section.subsections?.find((s) => s.id === subsectionId);
  return subsection?.actions ?? [];
}

export function getSubsectionPrimaryAction(
  section: Section,
  subsectionId: string,
  trigger: string = 'onActivate'
): Action | undefined {
  const normalizedTrigger = trigger.toLowerCase();
  const list = getSubsectionActions(section, subsectionId);
  const match = list.find((action) => (action.trigger ?? '').toLowerCase() === normalizedTrigger);
  if (match) return match;
  if (normalizedTrigger === 'onactivate') {
    return list.find((action) => (action.trigger ?? '').toLowerCase() === 'ontap');
  }
  return undefined;
}

