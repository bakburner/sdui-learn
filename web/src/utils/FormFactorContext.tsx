import React, { createContext, useContext, useSyncExternalStore } from 'react';
import { currentFormFactor, type FormFactor } from './LayoutTokenResolver';

type FormFactorContextValue = FormFactor;

const FormFactorContext = createContext<FormFactorContextValue>('web');

const WEB_BREAKPOINT_QUERY = '(min-width: 1024px)';
const COARSE_POINTER_QUERY = '(pointer: coarse)';
const RESIZE_DEBOUNCE_MS = 150;

function addMqlListener(mql: MediaQueryList, listener: () => void): () => void {
  if (typeof mql.addEventListener === 'function') {
    mql.addEventListener('change', listener);
    return () => mql.removeEventListener('change', listener);
  }
  mql.addListener(listener);
  return () => mql.removeListener(listener);
}

function subscribeFormFactor(onStoreChange: () => void): () => void {
  if (typeof window === 'undefined' || typeof window.matchMedia !== 'function') {
    return () => undefined;
  }

  let debounceTimer: number | undefined;
  const notify = (): void => {
    if (debounceTimer != null) {
      window.clearTimeout(debounceTimer);
    }
    debounceTimer = window.setTimeout(() => {
      debounceTimer = undefined;
      onStoreChange();
    }, RESIZE_DEBOUNCE_MS);
  };

  const widthMedia = window.matchMedia(WEB_BREAKPOINT_QUERY);
  const pointerMedia = window.matchMedia(COARSE_POINTER_QUERY);
  const removeWidthListener = addMqlListener(widthMedia, notify);
  const removePointerListener = addMqlListener(pointerMedia, notify);

  window.addEventListener('resize', notify);

  let resizeObserver: ResizeObserver | undefined;
  if (typeof ResizeObserver !== 'undefined' && typeof document !== 'undefined' && document.body) {
    resizeObserver = new ResizeObserver(() => {
      notify();
    });
    resizeObserver.observe(document.body);
  }

  return () => {
    if (debounceTimer != null) {
      window.clearTimeout(debounceTimer);
    }
    removeWidthListener();
    removePointerListener();
    window.removeEventListener('resize', notify);
    resizeObserver?.disconnect();
  };
}

function getSnapshot(): FormFactor {
  return currentFormFactor();
}

function getServerSnapshot(): FormFactor {
  return 'web';
}

export function FormFactorProvider({ children }: { children: React.ReactNode }): React.ReactElement {
  const value = useSyncExternalStore(subscribeFormFactor, getSnapshot, getServerSnapshot);
  return <FormFactorContext.Provider value={value}>{children}</FormFactorContext.Provider>;
}

export function useFormFactor(): FormFactor {
  return useContext(FormFactorContext);
}

export { FormFactorContext };
