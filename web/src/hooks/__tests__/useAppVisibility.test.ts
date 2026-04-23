import { describe, it, expect, beforeEach, afterEach } from 'vitest';
import { renderHook, act } from '@testing-library/react';
import { useAppVisibility } from '../useAppVisibility';

describe('useAppVisibility', () => {
  let originalHidden: boolean;

  beforeEach(() => {
    originalHidden = document.hidden;
  });

  afterEach(() => {
    // Restore document.hidden
    Object.defineProperty(document, 'hidden', {
      value: originalHidden,
      configurable: true,
    });
  });

  it('returns true when document is visible', () => {
    Object.defineProperty(document, 'hidden', {
      value: false,
      configurable: true,
    });

    const { result } = renderHook(() => useAppVisibility());
    expect(result.current).toBe(true);
  });

  it('returns false after visibilitychange to hidden', () => {
    Object.defineProperty(document, 'hidden', {
      value: false,
      configurable: true,
    });

    const { result } = renderHook(() => useAppVisibility());
    expect(result.current).toBe(true);

    // Simulate tab going to background
    act(() => {
      Object.defineProperty(document, 'hidden', {
        value: true,
        configurable: true,
      });
      document.dispatchEvent(new Event('visibilitychange'));
    });

    expect(result.current).toBe(false);
  });

  it('returns true after visibilitychange back to visible', () => {
    Object.defineProperty(document, 'hidden', {
      value: true,
      configurable: true,
    });

    const { result } = renderHook(() => useAppVisibility());

    // Simulate re-foregrounding
    act(() => {
      Object.defineProperty(document, 'hidden', {
        value: false,
        configurable: true,
      });
      document.dispatchEvent(new Event('visibilitychange'));
    });

    expect(result.current).toBe(true);
  });
});
