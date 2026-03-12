import { useEffect, useRef, useCallback } from 'react';
import type { Action } from '@sdui/models';
import { useAnalyticsContext } from './useAnalyticsContext';

interface UseImpressionTrackingParams {
  ref: React.RefObject<HTMLDivElement | null>;
  sectionId: string;
  actions: Action[];
  onAction: (action: Action) => void;
}

export function useImpressionTracking({
  ref,
  sectionId,
  actions,
  onAction,
}: UseImpressionTrackingParams): void {
  const { hasFired, markFired, canFireInterval } = useAnalyticsContext();
  const dwellTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  const onVisibleActions = actions.filter(
    (a) => a.trigger === 'onVisible' && a.type === 'analytics',
  );

  const fireAction = useCallback(
    (action: Action) => {
      const dedup = action.impression?.dedup ?? 'once-per-screen';
      const dedupKey = `${sectionId}:${action.event ?? 'impression'}`;

      if (dedup === 'once-per-screen' && hasFired(dedupKey)) {
        return;
      }

      if (dedup === 'once-per-interval') {
        const intervalMs = action.impression?.intervalMs ?? 30000;
        if (!canFireInterval(dedupKey, intervalMs)) {
          return;
        }
      }

      markFired(dedupKey);
      onAction(action);
    },
    [sectionId, hasFired, markFired, canFireInterval, onAction],
  );

  useEffect(() => {
    if (!ref.current || onVisibleActions.length === 0) return;

    const element = ref.current;

    const observer = new IntersectionObserver(
      (entries) => {
        for (const entry of entries) {
          if (entry.isIntersecting && entry.intersectionRatio >= 0.5) {
            dwellTimerRef.current = setTimeout(() => {
              for (const action of onVisibleActions) {
                const dwellMs = action.impression?.threshold?.dwellMs ?? 1000;
                if (dwellMs <= 1000) {
                  fireAction(action);
                } else {
                  setTimeout(() => fireAction(action), dwellMs - 1000);
                }
              }
            }, 1000);
          } else {
            if (dwellTimerRef.current) {
              clearTimeout(dwellTimerRef.current);
              dwellTimerRef.current = null;
            }
          }
        }
      },
      { threshold: [0.5] },
    );

    observer.observe(element);

    return () => {
      observer.disconnect();
      if (dwellTimerRef.current) {
        clearTimeout(dwellTimerRef.current);
      }
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [ref, sectionId, fireAction, onVisibleActions.length]);
}
