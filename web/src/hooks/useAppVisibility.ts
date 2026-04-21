import { useSyncExternalStore } from 'react';

/**
 * Module-level visibility state backed by the Page Visibility API.
 *
 * When the document is hidden (tab switch, minimize), `isAppVisible` is false.
 * Subscribers are notified synchronously via `useSyncExternalStore`.
 */
let _isVisible = typeof document !== 'undefined' ? !document.hidden : true;
const listeners = new Set<() => void>();

function subscribe(callback: () => void): () => void {
  listeners.add(callback);
  return () => listeners.delete(callback);
}

function getSnapshot(): boolean {
  return _isVisible;
}

if (typeof document !== 'undefined') {
  document.addEventListener('visibilitychange', () => {
    _isVisible = !document.hidden;
    listeners.forEach((cb) => cb());
  });
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
