export interface ToastItem {
  id: string;
  message: string;
}

const MAX_TOASTS = 3;
const DISPLAY_MS = 2500;

type Listener = () => void;
const listeners = new Set<Listener>();

let toasts: ToastItem[] = [];

function emit(): void {
  listeners.forEach((l) => l());
}

export function subscribeToasts(listener: Listener): () => void {
  listeners.add(listener);
  return () => listeners.delete(listener);
}

export function getToastsSnapshot(): ToastItem[] {
  // useSyncExternalStore: must return the same array reference when nothing changed.
  return toasts;
}

export function pushToast(message: string): void {
  const id = `${Date.now()}-${Math.random().toString(36).slice(2, 9)}`;
  toasts = [...toasts.slice(-(MAX_TOASTS - 1)), { id, message }];
  emit();
  if (typeof window !== 'undefined') {
    window.setTimeout(() => {
      dismissToast(id);
    }, DISPLAY_MS);
  }
}

function dismissToast(id: string): void {
  if (!toasts.some((t) => t.id === id)) {
    return;
  }
  toasts = toasts.filter((t) => t.id !== id);
  emit();
}
