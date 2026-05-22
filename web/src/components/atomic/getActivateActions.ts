import type { Action } from '@sdui/models';
import { ActionTrigger } from '@sdui/models';

const PRIMARY_ACTIVATION_TRIGGERS = new Set<ActionTrigger>([
  ActionTrigger.OnActivate,
  ActionTrigger.OnTap,
]);
const UNSUPPORTED_ATOMIC_TRIGGER_MESSAGES: Partial<Record<ActionTrigger, string>> = {
  [ActionTrigger.OnSwipe]: 'not_hosted_atomic_trigger',
};

/**
 * Filter an element's actions array to those matching the requested trigger.
 * Primary activation preserves the onActivate/onTap alias contract.
 */
export function selectActions(
  actions: Action[] | undefined | null,
  trigger: ActionTrigger,
): Action[] {
  if (!actions || actions.length === 0) return [];
  if (trigger === ActionTrigger.OnActivate) {
    return actions.filter((action) => PRIMARY_ACTIVATION_TRIGGERS.has(action.trigger));
  }
  return actions.filter((action) => action.trigger === trigger);
}

export function logUnsupportedAtomicTriggers(
  actions: Action[] | undefined | null,
  elementId: string | undefined,
): void {
  if (!actions || actions.length === 0) return;
  const loggedTriggers = new Set<ActionTrigger>();
  for (const action of actions) {
    const message = UNSUPPORTED_ATOMIC_TRIGGER_MESSAGES[action.trigger];
    if (!message || loggedTriggers.has(action.trigger)) continue;
    loggedTriggers.add(action.trigger);
    console.debug(message, {
      trigger: action.trigger,
      elementId,
      hostedAt: action.trigger === ActionTrigger.OnSwipe ? 'ScrollContainer' : 'none',
    });
  }
}
