import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { renderHook, act } from '@testing-library/react';
import { useSectionVisibility } from '../useSectionVisibility';

// --- IntersectionObserver mock ---
type IOCallback = (entries: Partial<IntersectionObserverEntry>[]) => void;

let ioCallback: IOCallback;
let ioDisconnect: ReturnType<typeof vi.fn>;
let ioObserve: ReturnType<typeof vi.fn>;

beforeEach(() => {
  ioDisconnect = vi.fn();
  ioObserve = vi.fn();

  vi.stubGlobal(
    'IntersectionObserver',
    class {
      constructor(cb: IOCallback, _opts?: IntersectionObserverInit) {
        ioCallback = cb;
      }
      observe = ioObserve;
      disconnect = ioDisconnect;
      unobserve = vi.fn();
    },
  );

  vi.useFakeTimers();
});

afterEach(() => {
  vi.useRealTimers();
  vi.restoreAllMocks();
});

function makeRef(el: HTMLElement | null = document.createElement('div')) {
  return { current: el };
}

describe('useSectionVisibility', () => {
  it('starts as not near viewport', () => {
    const ref = makeRef();
    const { result } = renderHook(() => useSectionVisibility(ref));
    expect(result.current).toBe(false);
  });

  it('returns true immediately when element enters intersection', () => {
    const ref = makeRef();
    const { result } = renderHook(() => useSectionVisibility(ref));

    act(() => {
      ioCallback([{ isIntersecting: true }]);
    });

    expect(result.current).toBe(true);
  });

  it('debounces exit by 500ms', () => {
    const ref = makeRef();
    const { result } = renderHook(() => useSectionVisibility(ref));

    // Enter
    act(() => {
      ioCallback([{ isIntersecting: true }]);
    });
    expect(result.current).toBe(true);

    // Exit — should NOT be false immediately
    act(() => {
      ioCallback([{ isIntersecting: false }]);
    });
    expect(result.current).toBe(true);

    // After 500ms debounce → false
    act(() => {
      vi.advanceTimersByTime(500);
    });
    expect(result.current).toBe(false);
  });

  it('cancels exit debounce if element re-enters within 500ms', () => {
    const ref = makeRef();
    const { result } = renderHook(() => useSectionVisibility(ref));

    // Enter
    act(() => {
      ioCallback([{ isIntersecting: true }]);
    });

    // Exit
    act(() => {
      ioCallback([{ isIntersecting: false }]);
    });

    // Re-enter within debounce window
    act(() => {
      vi.advanceTimersByTime(200);
    });
    act(() => {
      ioCallback([{ isIntersecting: true }]);
    });

    // Advance past original debounce time
    act(() => {
      vi.advanceTimersByTime(500);
    });

    // Should still be true — exit was cancelled
    expect(result.current).toBe(true);
  });

  it('observes the element and passes correct rootMargin', () => {
    const el = document.createElement('div');
    const ref = makeRef(el);

    renderHook(() => useSectionVisibility(ref));

    expect(ioObserve).toHaveBeenCalledWith(el);
  });

  it('disconnects observer on unmount', () => {
    const ref = makeRef();
    const { unmount } = renderHook(() => useSectionVisibility(ref));

    unmount();
    expect(ioDisconnect).toHaveBeenCalled();
  });

  it('does not observe when ref is null', () => {
    const ref = makeRef(null);
    renderHook(() => useSectionVisibility(ref));
    expect(ioObserve).not.toHaveBeenCalled();
  });
});
