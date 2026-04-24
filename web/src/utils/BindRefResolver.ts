import { createContext } from 'react';

/**
 * Content blob flowing through an `AtomicComposite`'s subtree. Set by
 * `SectionRouter` when dispatching `AtomicComposite`, read by leaf
 * primitives that carry a `bindRef`. Non-composite contexts leave
 * this undefined.
 */
export type CompositeContent = Record<string, unknown> | undefined;

export const CompositeContentContext = createContext<CompositeContent>(undefined);

/**
 * Resolver for the `bindRef` property on atomic leaf elements.
 *
 * `bindRef` is a dot-path into the enclosing `AtomicComposite`'s
 * `data.content` object. Each primitive has a canonical live field
 * the resolver targets — `content` for `Text`, `src` for `Image`,
 * `label` for `Button`, and an object-shaped
 * `{snapshotSeconds, snapshotAt, isRunning}` for `LiveClock`.
 * Placing the reference on the consuming node (rather than declaring
 * a central path-into-tree binding) lets the composer reshape the ui
 * tree without breaking real-time updates.
 */
export function resolveBindRefValue(
  bindRef: string | undefined,
  content: CompositeContent
): unknown {
  if (!bindRef || !content) return undefined;
  const parts = bindRef.split('.');
  if (parts.length === 0) return undefined;

  let current: unknown = content[parts[0]!];
  for (let i = 1; i < parts.length; i += 1) {
    if (current === null || typeof current !== 'object') return undefined;
    current = (current as Record<string, unknown>)[parts[i]!];
  }
  return current;
}

export function resolveBindRefString(
  bindRef: string | undefined,
  content: CompositeContent
): string | undefined {
  const value = resolveBindRefValue(bindRef, content);
  if (value === undefined || value === null) return undefined;
  if (typeof value === 'string') return value;
  if (typeof value === 'number') return String(value);
  if (typeof value === 'boolean') return String(value);
  return undefined;
}

export function resolveBindRefObject(
  bindRef: string | undefined,
  content: CompositeContent
): Record<string, unknown> | undefined {
  const value = resolveBindRefValue(bindRef, content);
  if (value && typeof value === 'object' && !Array.isArray(value)) {
    return value as Record<string, unknown>;
  }
  return undefined;
}
