import type { Action } from '@sdui/models';

type AtomicTrigger = NonNullable<Action['trigger']>;

const PRIMARY_ACTIVATION_TRIGGERS = new Set<AtomicTrigger>(['onActivate', 'onTap']);
const UNSUPPORTED_ATOMIC_TRIGGER_MESSAGES: Partial<Record<AtomicTrigger, string>> = {
  onSwipe: 'not_hosted_atomic_trigger',
};

/**
 * Filter an element's actions array to those matching the requested trigger.
 * Primary activation preserves the onActivate/onTap alias contract.
 */
export function selectActions(
  actions: Action[] | undefined | null,
  trigger: AtomicTrigger,
): Action[] {
  if (!actions || actions.length === 0) return [];
  if (trigger === 'onActivate') {
    return actions.filter((action) => PRIMARY_ACTIVATION_TRIGGERS.has(action.trigger));
  }
  return actions.filter((action) => action.trigger === trigger);
}

export function logUnsupportedAtomicTriggers(
  actions: Action[] | undefined | null,
  elementId: string | undefined,
): void {
  if (!actions || actions.length === 0) return;
  const loggedTriggers = new Set<AtomicTrigger>();
  for (const action of actions) {
    const message = UNSUPPORTED_ATOMIC_TRIGGER_MESSAGES[action.trigger];
    if (!message || loggedTriggers.has(action.trigger)) continue;
    loggedTriggers.add(action.trigger);
    console.debug(message, {
      trigger: action.trigger,
      elementId,
      hostedAt: action.trigger === 'onSwipe' ? 'ScrollContainer' : 'none',
    });
  }
}
