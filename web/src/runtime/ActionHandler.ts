import type { Action } from '@sdui/models';

export interface ActionContext {
  /** Callback to update screen state */
  onStateChange: (key: string, value: unknown) => void;
  /** Callback to refresh a section or full screen */
  onRefresh: (sectionId?: string) => void;
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
    for (const [paramName, stateKey] of Object.entries(action.paramBindings)) {
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
      // Fetch new screen data from the parameterized endpoint
      fetch(url)
        .then((res) => {
          if (!res.ok) throw new Error(`HTTP ${res.status}`);
          return res.json();
        })
        .then((screen) => {
          console.log('[Action] Parameterized refresh succeeded, new screen:', screen.id);
          // Merge new state from refresh response
          if (screen.state) {
            for (const [k, v] of Object.entries(screen.state)) {
              context.onStateChange(k, v);
            }
          }
          // Trigger UI refresh
          context.onRefresh(action.target);
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
  console.log('[Analytics]', action.eventName, action.eventParams);
  // In production, send to analytics service
}

function handleDismiss(): void {
  console.log('[Action] Dismiss (not implemented in prototype)');
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
