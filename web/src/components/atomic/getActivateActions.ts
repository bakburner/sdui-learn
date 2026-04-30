import type { Action } from '@sdui/models';

/**
 * Filter an element's actions array to those matching the primary activation
 * trigger (onActivate or the deprecated alias onTap). Returns them in
 * declared order so the action executor can run the full sequence with
 * proper failure-policy semantics.
 */
export function getActivateActions(actions: Action[] | undefined | null): Action[] {
  if (!actions || actions.length === 0) return [];
  return actions.filter(
    (a) => a.trigger === 'onActivate' || a.trigger === 'onTap',
  );
}
