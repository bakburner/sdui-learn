import type { Action } from '@sdui/models';

/**
 * Replaces `{{stateKey}}` placeholders inside server-emitted strings with
 * the current screen-state value for that key.
 *
 * The server emits actions like
 * `targetUri = "nba://games?date={{calendar_selected_date}}"` so a single
 * composed payload can carry parameterised navigation / refresh intent
 * without the server having to know the client's current state. The
 * substitution happens on the client at action-dispatch time, after any
 * preceding `mutate` (or renderer-driven `onStateChange`) has updated
 * the state map.
 *
 * - `keepUnknown=true` (URI / endpoint fields) leaves unrecognised
 *   placeholders intact so the failure surfaces at the network layer
 *   instead of producing a half-substituted URL.
 * - `keepUnknown=false` (`paramBindings` values) resolves unknown keys to
 *   the empty string so the dispatcher can drop them — the long-standing
 *   wire contract for optional filter params.
 *
 * Keys follow `[A-Za-z_][A-Za-z0-9_.]*` so dotted state paths like
 * `filters.team` are recognised; arbitrary characters are not, which keeps
 * the regex narrow enough to skip `{{ … }}` patterns embedded in unrelated
 * human-readable text.
 *
 * Mirrors Android's and iOS's `PlaceholderSubstitutor`.
 */

const PLACEHOLDER_PATTERN = /\{\{([A-Za-z_][A-Za-z0-9_.]*)\}\}/g;

function stringValue(value: unknown): string {
  if (value === null || value === undefined) return '';
  if (typeof value === 'string') return value;
  if (typeof value === 'number' || typeof value === 'boolean') return String(value);
  return String(value);
}

export function substitutePlaceholders(
  template: string,
  state: Record<string, unknown>,
  options: { keepUnknown?: boolean } = {},
): string {
  if (!template || !template.includes('{{')) return template;
  const keepUnknown = options.keepUnknown ?? true;
  return template.replace(PLACEHOLDER_PATTERN, (match, key: string) => {
    const value = state[key];
    if (value === undefined || value === null) {
      return keepUnknown ? match : '';
    }
    return stringValue(value);
  });
}

/**
 * Build a copy of `action` with every placeholder-bearing string field
 * resolved against `state`. Returns the input unchanged when no field
 * carries a `{{` token.
 */
export function resolveActionPlaceholders(
  action: Action,
  state: Record<string, unknown>,
): Action {
  const newTargetUri = action.targetUri != null
    ? substitutePlaceholders(action.targetUri, state)
    : action.targetUri;
  const newWebUrl = action.webUrl != null
    ? substitutePlaceholders(action.webUrl, state)
    : action.webUrl;
  const newEndpoint = action.endpoint != null
    ? substitutePlaceholders(action.endpoint, state)
    : action.endpoint;

  let newParamBindings = action.paramBindings;
  if (action.paramBindings) {
    const next: Record<string, string> = {};
    for (const [k, v] of Object.entries(action.paramBindings)) {
      next[k] = substitutePlaceholders(v, state, { keepUnknown: false });
    }
    newParamBindings = next;
  }

  if (
    newTargetUri === action.targetUri &&
    newWebUrl === action.webUrl &&
    newEndpoint === action.endpoint &&
    newParamBindings === action.paramBindings
  ) {
    return action;
  }

  return {
    ...action,
    targetUri: newTargetUri,
    webUrl: newWebUrl,
    endpoint: newEndpoint,
    paramBindings: newParamBindings,
  };
}
