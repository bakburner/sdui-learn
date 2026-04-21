import { useState, useEffect, useRef, type RefObject } from 'react';

const EXIT_DEBOUNCE_MS = 500;

/**
 * Track whether a section element is near the viewport.
 *
 * Uses a single IntersectionObserver with 1.5× viewport lookahead
 * (rootMargin: "50% 0px"). Entry triggers an immediate `true`;
 * exit is debounced by 500ms to absorb scroll bounce.
 */
export function useSectionVisibility(ref: RefObject<HTMLElement | null>): boolean {
  const [isNearViewport, setIsNearViewport] = useState(false);
  const exitTimer = useRef<ReturnType<typeof setTimeout>>();

  useEffect(() => {
    const element = ref.current;
    if (!element) return;

    const observer = new IntersectionObserver(
      ([entry]) => {
        if (entry.isIntersecting) {
          // Resume immediately — no debounce on entry
          clearTimeout(exitTimer.current);
          setIsNearViewport(true);
        } else {
          // Debounce exit by 500ms to absorb scroll bounce
          exitTimer.current = setTimeout(() => setIsNearViewport(false), EXIT_DEBOUNCE_MS);
        }
      },
      { rootMargin: '50% 0px' } // 1.5× viewport lookahead
    );

    observer.observe(element);

    return () => {
      observer.disconnect();
      clearTimeout(exitTimer.current);
    };
  }, [ref]);

  return isNearViewport;
}
