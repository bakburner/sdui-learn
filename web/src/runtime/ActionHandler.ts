import type { Action, Section } from '@sdui/models';
import { SDUI_PATH_PREFIX, API_PROXY_PREFIX } from '../utils/constants';

export interface ActionContext {
  /** Callback to update screen state */
  onStateChange: (key: string, value: unknown) => void;
  /** Callback to refresh a section or full screen */
  onRefresh: (sectionId?: string) => void;
  /** Callback to surgically replace a single section by ID */
  onSectionUpdate: (sectionId: string, section: Section) => void;
  /** Current screen state */
  state: Record<string, unknown>;
}

/**
 * Execute an SDUI action.
 */
export function executeAction(action: Action, context: ActionContext): void {
  console.log('[Action]', action.type, action);

  switch (action.type) {
    case 'navigate':
      handleNavigate(action);
      break;

    case 'mutate':
      handleMutate(action, context);
      break;

    case 'refresh':
      handleRefresh(action, context);
      break;

    case 'analytics':
      handleAnalytics(action);
      break;

    case 'dismiss':
      handleDismiss();
      break;

    case 'toast':
    case 'showToast':
      handleToast(action);
      break;

    default:
      console.warn('[Action] Unknown action type:', action.type);
  }
}

function handleNavigate(action: Action): void {
  const uri = action.targetUri || action.fallbackUrl;
  if (!uri) return;
  console.log('[Action] Navigate:', uri);
  const name = uri
    .replace('nba://', '')
    .replace(/\//g, ' ')
    .replace(/-/g, ' ')
    .replace(/^\w/, (c) => c.toUpperCase());
  showToast(`Navigating to ${name} (not implemented)`);
}

function handleMutate(action: Action, context: ActionContext): void {
  if (action.stateKey !== undefined) {
    console.log('[Action] Mutating state:', action.stateKey, '=', action.stateValue);
    context.onStateChange(action.stateKey, action.stateValue);
  }
}

function handleRefresh(action: Action, context: ActionContext): void {
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
      // Fetch new data from the parameterized endpoint.
      // The proxy strips /api, so prefix with the proxy prefix for Vite/Express.
      const fetchUrl = url.startsWith(SDUI_PATH_PREFIX) ? `${API_PROXY_PREFIX}${url}` : url;
      fetch(fetchUrl)
        .then((res) => {
          if (!res.ok) throw new Error(`HTTP ${res.status}`);
          return res.json();
        })
        .then((screen) => {
          console.log('[Action] Parameterized refresh succeeded, response id:', screen.id);

          // Merge new state from refresh response (e.g. form field values echoed back)
          if (screen.state) {
            for (const [k, v] of Object.entries(screen.state)) {
              context.onStateChange(k, v);
            }
          }

          // Surgical section-level merge: if the response carries sections and
          // the action targets a specific section, replace only that section.
          const targetSectionId = action.target;
          const responseSections = screen.sections as Section[] | undefined;

          if (targetSectionId && responseSections?.length) {
            // Find the section in the response that matches the target ID.
            const updatedSection = responseSections.find(
              (s: Section) => s.id === targetSectionId,
            );
            if (updatedSection) {
              console.log('[Action] Surgical section update:', targetSectionId);
              context.onSectionUpdate(targetSectionId, updatedSection);
              return; // Done — no full-screen refresh needed.
            }
          }

          // Fallback: if no targeted section found, replace all sections
          // by merging each one from the response.
          if (responseSections?.length) {
            for (const s of responseSections) {
              context.onSectionUpdate(s.id, s);
            }
            return;
          }

          // Last resort: full screen refresh
          context.onRefresh(targetSectionId);
        })
        .catch((err) => {
          console.error('[Action] Parameterized refresh failed:', err);
          showToast(`Refresh failed: ${err.message}`);
        });
      return;
    }
  }

  console.log('[Action] Refreshing:', action.target || 'full screen');
  context.onRefresh(action.target);
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
 */
export function createActionHandler(context: ActionContext) {
  return (action: Action) => executeAction(action, context);
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
