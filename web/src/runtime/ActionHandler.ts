import type { Action, Section, FailurePolicy } from '@sdui/models';
import { SDUI_PATH_PREFIX, API_PROXY_PREFIX } from '../utils/constants';
import { pushToast } from './ToastStore';
import { actionLog, actionWarn, actionError } from './actionLogger';

export interface ActionContext {
  /** Callback to update screen state */
  onStateChange: (key: string, value: unknown) => void;
  /** Callback to refresh a section or full screen */
  onRefresh: (sectionId?: string) => void;
  /** Callback to surgically replace a single section by ID */
  onSectionUpdate: (sectionId: string, section: Section) => void;
  /** Mark a section as stale (refresh failed) */
  onSectionStale: (sectionId: string) => void;
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
    actionLog(`dispatch type=${action.type} trigger=${action.trigger ?? 'onTap'}`, action);
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
  switch (action.type) {
    case 'navigate':
      return handleNavigate(action);

    case 'mutate':
      return handleMutate(action, context);

    case 'refresh':
      return handleRefresh(action, context);

    case 'fireAndForget':
      handleFireAndForget(action);
      return true;

    case 'dismiss':
      handleDismiss();
      return true;

    case 'toast':
      handleToast(action);
      return true;

    default:
      actionWarn('Unknown action type:', action.type);
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

function handleNavigate(action: Action): boolean {
  const uri = action.targetUri || action.fallbackUrl;
  if (!uri) {
    actionWarn('Navigate: no targetUri or fallbackUrl');
    return false;
  }
  actionLog(`navigate uri=${uri} presentation=${action.presentation ?? 'push'}`);
  // In-app navigation is handled by App.tsx before reaching here.
  // This handler runs only for URIs not intercepted by the app shell.
  const name = uri
    .replace('nba://', '')
    .replace(/\//g, ' ')
    .replace(/-/g, ' ')
    .replace(/^\w/, (c: string) => c.toUpperCase());
  showToast(`Navigating to ${name} (not implemented)`);
  return true;
}

function handleMutate(action: Action, context: ActionContext): boolean {
  if (action.stateKey !== undefined) {
    actionLog(`mutate ${action.stateKey}=${String(action.stateValue)}`);
    context.onStateChange(action.stateKey, action.stateValue);
    return true;
  }
  actionWarn('Mutate: no stateKey');
  return false;
}

async function handleRefresh(action: Action, context: ActionContext): Promise<boolean> {
  // Resolve paramBindings from screen state (Form submit support)
  if (action.paramBindings && Object.keys(action.paramBindings).length > 0) {
    const params = new URLSearchParams();
    for (const [paramName, rawStateKey] of Object.entries(action.paramBindings)) {
      // Strip mustache delimiters: "{{form_season}}" → "form_season"
      const stateKey = rawStateKey.replace(/^\{\{|\}\}$/g, '');
      const value = context.state[stateKey];
      if (value !== undefined && value !== null) {
        params.set(paramName, String(value));
      }
    }

    const baseUrl = action.endpoint || '';
    const separator = baseUrl.includes('?') ? '&' : '?';
    const url = baseUrl ? `${baseUrl}${separator}${params.toString()}` : undefined;

    actionLog(`refresh parameterized url=${url ?? 'inherit'} params=${params.toString()}`);

    if (url) {
      const fetchUrl = url.startsWith(SDUI_PATH_PREFIX) ? `${API_PROXY_PREFIX}${url}` : url;
      try {
        const res = await fetch(fetchUrl);
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        const screen = await res.json();
        actionLog(`refresh succeeded id=${screen.id}`);

        // Merge new state from refresh response
        if (screen.state) {
          for (const [k, v] of Object.entries(screen.state)) {
            context.onStateChange(k, v);
          }
        }

        // Surgical section-level merge
        const targetSectionId = action.target;
        const responseSections = screen.sections as Section[] | undefined;

        if (targetSectionId && responseSections?.length) {
          const updatedSection = responseSections.find(
            (s: Section) => s.id === targetSectionId,
          );
          if (updatedSection) {
            actionLog(`refresh surgical-update section=${targetSectionId}`);
            context.onSectionUpdate(targetSectionId, updatedSection);
            return true;
          }
        }

        // Fallback: replace all sections from the response
        if (responseSections?.length) {
          for (const s of responseSections) {
            context.onSectionUpdate(s.id, s);
          }
          return true;
        }

        // Last resort: full screen refresh
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
 * Wraps a single action into a one-element sequence.
 */
export function createActionHandler(context: ActionContext) {
  return (action: Action) => {
    executeActionSequence([action], context);
  };
}

// ---------- internal helpers ----------

export function showToast(message: string): void {
  pushToast(message);
}
