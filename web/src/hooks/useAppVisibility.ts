import { useSyncExternalStore } from 'react';

/**
 * Module-level visibility state backed by the Page Visibility API.
 *
 * When the document is hidden (tab switch, minimize), `isAppVisible` is false.
 * Subscribers are notified synchronously via `useSyncExternalStore`.
 */
let _isVisible = typeof document !== 'undefined' ? !document.hidden : true;
const listeners = new Set<() => void>();
let visibilityListenerAttached = false;

function onDocumentVisibilityChange(): void {
  _isVisible = !document.hidden;
  listeners.forEach((cb) => cb());
}

function ensureVisibilityListener(): void {
  if (typeof document === 'undefined' || visibilityListenerAttached) {
    return;
  }
  document.addEventListener('visibilitychange', onDocumentVisibilityChange);
  visibilityListenerAttached = true;
}

function maybeRemoveVisibilityListener(): void {
  if (typeof document === 'undefined' || !visibilityListenerAttached || listeners.size > 0) {
    return;
  }
  document.removeEventListener('visibilitychange', onDocumentVisibilityChange);
  visibilityListenerAttached = false;
}

function subscribe(callback: () => void): () => void {
  ensureVisibilityListener();
  listeners.add(callback);
  return () => {
    listeners.delete(callback);
    maybeRemoveVisibilityListener();
  };
}

function getSnapshot(): boolean {
  return _isVisible;
}

/**
 * Returns `true` when the app/tab is in the foreground, `false` when backgrounded.
 *
 * This is Phase 0 of visibility-gated refresh — pauses ALL refresh activity
 * when the tab is hidden, which is higher impact than scroll-based pausing.
 */
export function useAppVisibility(): boolean {
  return useSyncExternalStore(subscribe, getSnapshot);
}
