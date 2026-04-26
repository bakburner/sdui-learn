import '@testing-library/jest-dom';

// Defensive polyfill: under vitest 4 + Node 22, Node's experimental
// `localStorage` shim can shadow jsdom's implementation and surface as
// `window.localStorage.getItem is not a function` for components that
// touch the color-scheme cache (ColorTokenResolver). Install an
// in-memory shim only when the running environment hasn't already
// provided a usable Storage.
if (
  typeof window !== 'undefined' &&
  typeof window.localStorage?.getItem !== 'function'
) {
  const store = new Map<string, string>();
  Object.defineProperty(window, 'localStorage', {
    configurable: true,
    value: {
      getItem: (k: string) => store.get(k) ?? null,
      setItem: (k: string, v: string) => {
        store.set(k, String(v));
      },
      removeItem: (k: string) => {
        store.delete(k);
      },
      clear: () => store.clear(),
      key: (i: number) => Array.from(store.keys())[i] ?? null,
      get length() {
        return store.size;
      },
    },
  });
}
