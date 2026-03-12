import React, { createContext, useContext, useRef, useCallback } from 'react';

interface ImpressionRecord {
  lastFiredMs: number;
}

interface AnalyticsContextValue {
  hasFired: (sectionId: string) => boolean;
  markFired: (sectionId: string) => void;
  canFireInterval: (sectionId: string, intervalMs: number) => boolean;
  reset: () => void;
}

const AnalyticsContext = createContext<AnalyticsContextValue | null>(null);

export function AnalyticsProvider({ children }: { children: React.ReactNode }): React.ReactElement {
  const registryRef = useRef<Map<string, ImpressionRecord>>(new Map());

  const hasFired = useCallback((sectionId: string): boolean => {
    return registryRef.current.has(sectionId);
  }, []);

  const markFired = useCallback((sectionId: string): void => {
    registryRef.current.set(sectionId, { lastFiredMs: Date.now() });
  }, []);

  const canFireInterval = useCallback((sectionId: string, intervalMs: number): boolean => {
    const record = registryRef.current.get(sectionId);
    if (!record) return true;
    return Date.now() - record.lastFiredMs >= intervalMs;
  }, []);

  const reset = useCallback((): void => {
    registryRef.current.clear();
  }, []);

  return (
    <AnalyticsContext.Provider value={{ hasFired, markFired, canFireInterval, reset }}>
      {children}
    </AnalyticsContext.Provider>
  );
}

export function useAnalyticsContext(): AnalyticsContextValue {
  const ctx = useContext(AnalyticsContext);
  if (!ctx) {
    return {
      hasFired: () => false,
      markFired: () => {},
      canFireInterval: () => true,
      reset: () => {},
    };
  }
  return ctx;
}
