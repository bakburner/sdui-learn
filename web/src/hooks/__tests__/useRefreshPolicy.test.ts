import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { renderHook, act } from '@testing-library/react';
import { useRefreshPolicy } from '../useRefreshPolicy';
import { subscribeToChannel } from '../../runtime/AblyClient';
import type { Section } from '@sdui/models';
import { RefreshType } from '@sdui/models';

// --- Mock AblyClient ---
const mockUnsubscribeChannel = vi.fn();
const mockUnsubscribeConnection = vi.fn();

vi.mock('../../runtime/AblyClient', () => ({
  subscribeToChannel: vi.fn((_channel: string, _onMessage: (data: unknown) => void) => mockUnsubscribeChannel),
  onConnectionStateChange: vi.fn((_cb: (state: string) => void) => mockUnsubscribeConnection),
}));

beforeEach(() => {
  vi.useFakeTimers();
  vi.clearAllMocks();
  globalThis.fetch = vi.fn();
});

afterEach(() => {
  vi.useRealTimers();
  vi.restoreAllMocks();
});

function makeSection(overrides: Partial<Section> = {}): Section {
  return {
    id: 'test-section',
    type: 'AtomicComposite',
    data: { foo: 'bar' },
    refreshPolicy: undefined,
    ...overrides,
  } as Section;
}

describe('useRefreshPolicy — poll', () => {
  it('does not poll when enabled is false', async () => {
    const section = makeSection({
      refreshPolicy: { type: RefreshType.Poll, intervalMs: 1000, url: '/api/data' },
    });
    const onUpdate = vi.fn();

    renderHook(() => useRefreshPolicy({ section, onUpdate, enabled: false }));

    await act(async () => {
      vi.advanceTimersByTime(5000);
    });

    expect(fetch).not.toHaveBeenCalled();
    expect(onUpdate).not.toHaveBeenCalled();
  });

  it('starts polling when enabled is true and stops when disabled', async () => {
    const section = makeSection({
      refreshPolicy: { type: RefreshType.Poll, intervalMs: 1000, url: '/api/data' },
    });
    const onUpdate = vi.fn();
    (globalThis.fetch as ReturnType<typeof vi.fn>).mockResolvedValue({
      ok: true,
      json: async () => ({ score: 100 }),
    });

    const { rerender, unmount } = renderHook(
      ({ enabled }) => useRefreshPolicy({ section, onUpdate, enabled }),
      { initialProps: { enabled: true } },
    );

    // Initial fetch fires
    await act(async () => {
      await vi.advanceTimersByTimeAsync(0);
    });
    expect(fetch).toHaveBeenCalledTimes(1);
    expect(onUpdate).toHaveBeenCalledWith({ score: 100 });

    // Advance to first poll tick
    await act(async () => {
      await vi.advanceTimersByTimeAsync(1000);
    });
    expect(fetch).toHaveBeenCalledTimes(2);

    // Disable — cleanup should clear the timer
    rerender({ enabled: false });

    const callCountAfterDisable = (fetch as ReturnType<typeof vi.fn>).mock.calls.length;
    await act(async () => {
      await vi.advanceTimersByTimeAsync(5000);
    });
    // No additional fetches after disable
    expect(fetch).toHaveBeenCalledTimes(callCountAfterDisable);

    unmount();
  });

  it('applies exponential backoff on poll failure', async () => {
    const section = makeSection({
      refreshPolicy: { type: RefreshType.Poll, intervalMs: 1000, url: '/api/data' },
    });
    const onUpdate = vi.fn();
    const onStalenessChange = vi.fn();

    (globalThis.fetch as ReturnType<typeof vi.fn>).mockResolvedValue({ ok: false });

    renderHook(() =>
      useRefreshPolicy({ section, onUpdate, onStalenessChange, enabled: true }),
    );

    // Initial fetch (fails)
    await act(async () => {
      await vi.advanceTimersByTimeAsync(0);
    });

    // First retry scheduled after 2000ms (backoff from 1000ms)
    await act(async () => {
      await vi.advanceTimersByTimeAsync(2000);
    });
    expect(fetch).toHaveBeenCalledTimes(2);

    // After 2 failures, section should be marked stale
    expect(onStalenessChange).toHaveBeenCalledWith('test-section', true);
  });

  it('does not poll static sections', () => {
    const section = makeSection({
      refreshPolicy: { type: RefreshType.Static },
    });
    const onUpdate = vi.fn();

    renderHook(() => useRefreshPolicy({ section, onUpdate, enabled: true }));

    expect(fetch).not.toHaveBeenCalled();
  });
});

describe('useRefreshPolicy — SSE', () => {
  const mockedSubscribeToChannel = vi.mocked(subscribeToChannel);

  it('does not subscribe when enabled is false', () => {
    const section = makeSection({
      refreshPolicy: { type: RefreshType.SSE, channel: 'game:123' },
    });

    renderHook(() => useRefreshPolicy({ section, onUpdate: vi.fn(), enabled: false }));

    expect(mockedSubscribeToChannel).not.toHaveBeenCalled();
  });

  it('subscribes to SSE channel when enabled', () => {
    const section = makeSection({
      refreshPolicy: { type: RefreshType.SSE, channel: 'game:123' },
    });

    renderHook(() => useRefreshPolicy({ section, onUpdate: vi.fn(), enabled: true }));

    expect(mockedSubscribeToChannel).toHaveBeenCalledWith('game:123', expect.any(Function));
  });

  it('unsubscribes on disable (Option A behavior)', () => {
    const section = makeSection({
      refreshPolicy: { type: RefreshType.SSE, channel: 'game:123' },
    });

    const { rerender } = renderHook(
      ({ enabled }) => useRefreshPolicy({ section, onUpdate: vi.fn(), enabled }),
      { initialProps: { enabled: true } },
    );

    // Disable → should call unsubscribe
    rerender({ enabled: false });

    expect(mockUnsubscribeChannel).toHaveBeenCalled();
  });
});
