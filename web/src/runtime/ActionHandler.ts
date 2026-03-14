import type { Action, Section, FailurePolicy } from '@sdui/models';
import { SDUI_PATH_PREFIX, API_PROXY_PREFIX } from '../utils/constants';

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
  analytics: 'silent' as FailurePolicy,
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
    console.log('[Action]', action.type, action);
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
        console.warn(`[Action] ${action.type} failed, continuing sequence`);
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

    case 'analytics':
      handleAnalytics(action);
      return true;

    case 'dismiss':
      handleDismiss();
      return true;

    case 'toast':
      handleToast(action);
      return true;

    default:
      console.warn('[Action] Unknown action type:', action.type);
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
    console.warn('[Action] Navigate: no targetUri or fallbackUrl');
    return false;
  }
  console.log('[Action] Navigate:', uri);
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
    console.log('[Action] Mutating state:', action.stateKey, '=', action.stateValue);
    context.onStateChange(action.stateKey, action.stateValue);
    return true;
  }
  console.warn('[Action] Mutate: no stateKey');
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

    console.log('[Action] Parameterized refresh:', url || `params=${params.toString()}`);

    if (url) {
      const fetchUrl = url.startsWith(SDUI_PATH_PREFIX) ? `${API_PROXY_PREFIX}${url}` : url;
      try {
        const res = await fetch(fetchUrl);
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        const screen = await res.json();
        console.log('[Action] Parameterized refresh succeeded, response id:', screen.id);

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
            console.log('[Action] Surgical section update:', targetSectionId);
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
        console.error('[Action] Parameterized refresh failed:', err);
        if (action.target) {
          context.onSectionStale(action.target);
        }
        return false;
      }
    }
  }

  console.log('[Action] Refreshing:', action.target || 'full screen');
  context.onRefresh(action.target);
  return true;
}

function handleAnalytics(action: Action): void {
  const event = action.event ?? (action as Record<string, unknown>).eventName;
  const params = action.params ?? (action as Record<string, unknown>).eventParams ?? {};
  const destinations = action.destinations ?? ['all'];

  const beacon = {
    event,
    params,
    destinations,
    timestamp: new Date().toISOString(),
  };

  console.log('[Analytics]', JSON.stringify(beacon, null, 2));

  for (const dest of destinations) {
    switch (dest) {
      case 'adobe':
        console.log('[Analytics:Adobe]', event, params);
        break;
      case 'firebase':
        console.log('[Analytics:Firebase]', event, params);
        break;
      case 'internal':
        console.log('[Analytics:Internal]', event, params);
        break;
      case 'all':
      default:
        console.log('[Analytics:All]', event, params);
        break;
    }
  }
}

function handleDismiss(): void {
  console.log('[Action] Dismiss (not implemented in prototype)');
}

function handleToast(action: Action): void {
  const message = (action as Record<string, unknown>).message as string | undefined;
  if (message) {
    showToast(message);
  } else {
    console.warn('[Action] Toast action missing message');
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

/** Ephemeral toast notification for the demo */
export function showToast(message: string): void {
  const existing = document.getElementById('sdui-toast');
  if (existing) existing.remove();

  const toast = document.createElement('div');
  toast.id = 'sdui-toast';
  Object.assign(toast.style, {
    position: 'fixed',
    bottom: '72px',
    left: '50%',
    transform: 'translateX(-50%)',
    padding: '10px 20px',
    borderRadius: '8px',
    backgroundColor: '#333',
    color: '#fff',
    fontSize: '13px',
    zIndex: '9999',
    maxWidth: '90vw',
    textAlign: 'center',
    transition: 'opacity 0.3s',
  } as CSSStyleDeclaration);
  toast.textContent = message;
  document.body.appendChild(toast);

  setTimeout(() => {
    toast.style.opacity = '0';
    setTimeout(() => toast.remove(), 300);
  }, 2500);
}
