import type { Action, Section, FailurePolicy } from '@sdui/models';
import { pushToast } from './ToastStore';
import { actionLog, actionWarn, actionError } from './actionLogger';
import { fetchSduiScreen } from './fetchSduiScreen';
import { resolveActionPlaceholders } from './placeholderSubstitutor';

export interface ActionContext {
  /** Callback to update screen state */
  onStateChange: (key: string, value: unknown) => void;
  /** Callback to refetch the current screen (pull-to-refresh / non-parameterized refresh) */
  onRefresh: (sectionId?: string) => void;
  /**
   * Screen-channel entry point: fetches a full screen via the given endpoint
   * with user params, validates response.id against the current screen, applies
   * strict full-replace on id-match, drops on mismatch. Stores userParams for
   * replay on subsequent poll/refetch and resets the screen-level poll timer.
   */
  replaceCurrentScreen: (endpoint: string, userParams?: Record<string, string>) => Promise<void>;
  /** Callback to surgically replace a single section by ID (section channel) */
  onSectionUpdate: (sectionId: string, section: Section) => void;
  /** Mark a section as stale (refresh failed) */
  onSectionStale: (sectionId: string) => void;
  /** Navigate to an nba:// URI (triggers screen change) */
  onNavigate: (uri: string) => void;
  /** Current screen state */
  state: Record<string, unknown>;
}

// Per-type defaults matching the schema contract
const DEFAULT_FAILURE_POLICY: Record<string, FailurePolicy> = {
  navigate: 'halt' as FailurePolicy,
  mutate: 'continue' as FailurePolicy,
  refresh: 'continue' as FailurePolicy,
  fireAndForget: 'silent' as FailurePolicy,
  dismiss: 'silent' as FailurePolicy,
  toast: 'silent' as FailurePolicy,
};

function resolveFailurePolicy(action: Action): FailurePolicy {
  return (action.onFailure as FailurePolicy) ?? DEFAULT_FAILURE_POLICY[action.type ?? ''] ?? ('halt' as FailurePolicy);
}

export interface SequenceResult {
  executed: Action[];
  halted: boolean;
}

/**
 * Execute a sequence of SDUI actions, respecting failure policies.
 */
export async function executeActionSequence(
  actions: Action[],
  context: ActionContext,
): Promise<SequenceResult> {
  const executed: Action[] = [];

  for (const action of actions) {
    if (import.meta.env.DEV && action.trigger === 'onTap') {
      actionLog('deprecated_trigger_used: onTap is a deprecated alias for onActivate', action);
    }
    actionLog(`dispatch type=${action.type} trigger=${action.trigger ?? 'onActivate'}`, action);
    const success = await dispatchAction(action, context);
    executed.push(action);

    if (!success) {
      const policy = resolveFailurePolicy(action);
      if (policy === 'halt') {
        const msg = action.failureFeedback?.message ?? getDefaultErrorMessage(action);
        if (msg) showToast(msg);
        return { executed, halted: true };
      }
      if (policy === 'continue') {
        actionWarn(`${action.type} failed, continuing sequence`);
      }
      // silent: no log, just continue
    }
  }

  return { executed, halted: false };
}

/**
 * Execute a single SDUI action. Returns true on success, false on failure.
 */
async function dispatchAction(action: Action, context: ActionContext): Promise<boolean> {
  // Resolve `{{stateKey}}` placeholders against the live state map before
  // the per-type handlers run. This lets the server emit parameterised
  // navigate / refresh actions (e.g.
  // `nba://games?date={{calendar_selected_date}}`) without each renderer
  // having to splice state into server-emitted strings.
  const resolved = resolveActionPlaceholders(action, context.state);
  switch (resolved.type) {
    case 'navigate':
      return handleNavigate(resolved, context);

    case 'mutate':
      return handleMutate(resolved, context);

    case 'refresh':
      return handleRefresh(resolved, context);

    case 'fireAndForget':
      handleFireAndForget(resolved);
      return true;

    case 'dismiss':
      handleDismiss();
      return true;

    case 'toast':
      handleToast(resolved);
      return true;

    default:
      actionWarn('Unknown action type:', resolved.type);
      return false;
  }
}

function getDefaultErrorMessage(action: Action): string | undefined {
  switch (action.type) {
    case 'navigate': return 'Unable to open page';
    case 'refresh': return 'Refresh failed';
    default: return undefined;
  }
}

function handleNavigate(action: Action, context: ActionContext): boolean {
  const uri = action.targetUri?.startsWith('nba://') ? action.targetUri : action.webUrl ?? action.targetUri;
  if (!uri) {
    actionWarn('Navigate: no webUrl or targetUri');
    return false;
  }
  actionLog(`navigate uri=${uri} presentation=${action.presentation ?? 'push'}`);
  context.onNavigate(uri);
  return true;
}

function handleMutate(action: Action, context: ActionContext): boolean {
  if (!action.target) {
    actionWarn('Mutate: no target');
    return false;
  }

  const key = action.target;
  const current = context.state[key];
  const operation = action.operation ?? 'set';

  switch (operation) {
    case 'set':
      actionLog(`mutate set ${key}=${String(action.value)}`);
      context.onStateChange(key, action.value);
      return true;

    case 'toggle':
      if (typeof current === 'boolean') {
        actionLog(`mutate toggle ${key}=${String(!current)}`);
        context.onStateChange(key, !current);
        return true;
      }
      console.warn('[SDUI/Action] mutate toggle noop: current value is not boolean', { key, current });
      return false;

    case 'increment': {
      if (typeof current === 'number' && Number.isFinite(current)) {
        const delta = asDouble(action.value) ?? 1;
        const next = current + delta;
        const normalized = Number.isInteger(next) ? Math.trunc(next) : next;
        actionLog(`mutate increment ${key}=${String(normalized)}`);
        context.onStateChange(key, normalized);
        return true;
      }
      console.warn('[SDUI/Action] mutate increment noop: current value is not numeric', { key, current });
      return false;
    }

    case 'append':
      if (Array.isArray(current)) {
        const next = [...current, action.value];
        actionLog(`mutate append ${key}=${String(action.value)}`);
        context.onStateChange(key, next);
        return true;
      }
      if (typeof current === 'string' && typeof action.value === 'string') {
        actionLog(`mutate append ${key}=${current + action.value}`);
        context.onStateChange(key, current + action.value);
        return true;
      }
      if (current === undefined && action.value !== undefined) {
        actionLog(`mutate append ${key}=[${String(action.value)}]`);
        context.onStateChange(key, [action.value]);
        return true;
      }
      console.warn('[SDUI/Action] mutate append noop: incompatible value types', {
        key,
        current,
        incoming: action.value,
      });
      return false;

    default:
      console.warn('[SDUI/Action] mutate noop: unknown operation', { key, operation, current, incoming: action.value });
      return false;
  }
}

function asDouble(value: unknown): number | undefined {
  switch (typeof value) {
    case 'number':
      return Number.isFinite(value) ? value : undefined;
    case 'boolean':
      return value ? 1 : 0;
    case 'string': {
      const parsed = Number(value);
      return Number.isFinite(parsed) ? parsed : undefined;
    }
    default:
      return undefined;
  }
}

async function handleRefresh(action: Action, context: ActionContext): Promise<boolean> {
  // Parameterized refresh: paramBindings values were already resolved
  // against the state map by resolveActionPlaceholders() in dispatchAction.
  // Drop empty values so the transport doesn't emit dangling `?key=`
  // query params.
  if (action.paramBindings && action.endpoint && Object.keys(action.paramBindings).length > 0) {
    const userParams: Record<string, string> = {};
    for (const [paramName, value] of Object.entries(action.paramBindings)) {
      if (value !== undefined && value !== null && value !== '') {
        userParams[paramName] = String(value);
      }
    }

    const isScreenChannel = action.endpoint.includes('/v1/sdui/screen/');

    if (isScreenChannel) {
      // Screen channel: strict full-replace via replaceCurrentScreen.
      actionLog(
        `refresh screen-channel endpoint=${action.endpoint} params=${JSON.stringify(userParams)}`,
      );
      try {
        await context.replaceCurrentScreen(action.endpoint, userParams);
        return true;
      } catch (err) {
        actionError(`refresh screen-channel failed: ${(err as Error).message ?? err}`);
        return false;
      }
    }

    // Section channel: fetch the section and replace in place.
    actionLog(
      `refresh section-channel endpoint=${action.endpoint} params=${JSON.stringify(userParams)}`,
    );
    try {
      const { screen, url, method } = await fetchSduiScreen({
        endpoint: action.endpoint,
        userParams,
      });
      actionLog(`refresh section-channel succeeded id=${screen.id} method=${method} url=${url}`);

      if (screen.state) {
        for (const [k, v] of Object.entries(screen.state)) {
          context.onStateChange(k, v);
        }
      }

      const targetSectionId = action.target;
      const responseSections = screen.sections as Section[] | undefined;

      if (targetSectionId && responseSections?.length) {
        const updatedSection = responseSections.find((s: Section) => s.id === targetSectionId);
        if (updatedSection) {
          context.onSectionUpdate(targetSectionId, updatedSection);
          return true;
        }
      }

      context.onRefresh(targetSectionId);
      return true;
    } catch (err) {
      actionError(`refresh failed: ${(err as Error).message ?? err}`);
      if (action.target) {
        context.onSectionStale(action.target);
      }
      return false;
    }
  }

  // Non-parameterized refresh: if there's an endpoint on the screen channel,
  // route through replaceCurrentScreen (no params). Otherwise fall back to
  // generic refetch.
  if (action.endpoint?.includes('/v1/sdui/screen/')) {
    actionLog(`refresh screen-channel (no params) endpoint=${action.endpoint}`);
    try {
      await context.replaceCurrentScreen(action.endpoint);
      return true;
    } catch (err) {
      actionError(`refresh screen-channel failed: ${(err as Error).message ?? err}`);
      return false;
    }
  }

  actionLog(`refresh target=${action.target ?? 'screen'}`);
  context.onRefresh(action.target);
  return true;
}

function handleFireAndForget(action: Action): void {
  const event = action.event ?? (action as Record<string, unknown>).eventName;
  const params = action.params ?? (action as Record<string, unknown>).eventParams ?? {};
  const destinations = action.destinations ?? ['all'];

  // Fire-and-forget has no on-screen side effect, so debug logging is the
  // only way to verify the beacon was actually emitted during local
  // testing. iOS and Android mirror this behaviour. Keep the payload
  // shape compact (one line per dispatch) so the dev-tools console
  // stays scannable when many beacons fire from a single screen view.
  actionLog(`fireAndForget event=${event} destinations=${destinations.join(',')}`, params);
}

function handleDismiss(): void {
  actionLog('dismiss (not implemented in prototype)');
}

function handleToast(action: Action): void {
  const message = (action as Record<string, unknown>).message as string | undefined;
  if (message) {
    actionLog(`toast message=${message}`);
    showToast(message);
  } else {
    actionWarn('Toast action missing message');
  }
}

/**
 * Create action handler bound to a context.
 * Normalizes single actions or arrays into a sequence for the executor.
 */
export function createActionHandler(context: ActionContext) {
  return (actionOrActions: Action | Action[]) => {
    const actions = Array.isArray(actionOrActions) ? actionOrActions : [actionOrActions];
    executeActionSequence(actions, context);
  };
}

// ---------- internal helpers ----------

export function showToast(message: string): void {
  pushToast(message);
}
