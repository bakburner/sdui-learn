import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { renderHook, act } from '@testing-library/react';
import { useRefreshPolicy } from '../useRefreshPolicy';
import { subscribeToChannel } from '../../runtime/AblyClient';
import * as fetchSduiScreenModule from '../../runtime/fetchSduiScreen';
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

    renderHook(() =>
      useRefreshPolicy({
        sectionId: section.id,
        refreshPolicy: section.refreshPolicy,
        onUpdate,
        enabled: false,
      }),
    );

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
      ({ enabled }) =>
        useRefreshPolicy({
          sectionId: section.id,
          refreshPolicy: section.refreshPolicy,
          onUpdate,
          enabled,
        }),
      { initialProps: { enabled: true } },
    );

    // Cross-platform aligned: first fetch waits `intervalMs`. Nothing fires at t=0.
    await act(async () => {
      await vi.advanceTimersByTimeAsync(0);
    });
    expect(fetch).not.toHaveBeenCalled();

    // First poll tick fires at intervalMs
    await act(async () => {
      await vi.advanceTimersByTimeAsync(1000);
    });
    expect(fetch).toHaveBeenCalledTimes(1);
    expect(onUpdate).toHaveBeenCalledWith({ score: 100 });

    // Second tick fires at 2 * intervalMs
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
      useRefreshPolicy({
        sectionId: section.id,
        refreshPolicy: section.refreshPolicy,
        onUpdate,
        onStalenessChange,
        enabled: true,
      }),
    );

    // First fetch fires at intervalMs (fails)
    await act(async () => {
      await vi.advanceTimersByTimeAsync(1000);
    });
    expect(fetch).toHaveBeenCalledTimes(1);

    // Backoff doubles to 2000ms; second fetch fires after another 2000ms (fails)
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

    renderHook(() =>
      useRefreshPolicy({
        sectionId: section.id,
        refreshPolicy: section.refreshPolicy,
        onUpdate,
        enabled: true,
      }),
    );

    expect(fetch).not.toHaveBeenCalled();
  });
});

describe('useRefreshPolicy — sectionEndpoint precedence', () => {
  it('routes to sectionEndpoint when both url and sectionEndpoint are present', async () => {
    const replacedSection: Section = {
      id: 'test-section',
      type: 'AtomicComposite',
      data: { live: true },
      refreshPolicy: { type: RefreshType.Static },
    } as Section;

    const fetchSduiSectionSpy = vi
      .spyOn(fetchSduiScreenModule, 'fetchSduiSection')
      .mockResolvedValue(replacedSection);

    const onSectionReplace = vi.fn();
    const policy = {
      type: RefreshType.Poll,
      intervalMs: 1000,
      url: '/cdn/should-not-be-called',
      sectionEndpoint: '/v1/sdui/section/stats-api:game-123::AtomicComposite::scoreboard',
    };

    renderHook(() =>
      useRefreshPolicy({
        sectionId: 'test-section',
        refreshPolicy: policy,
        onUpdate: vi.fn(),
        onSectionReplace,
        enabled: true,
      }),
    );

    // First fetch fires at intervalMs (cross-platform aligned)
    await act(async () => {
      await vi.advanceTimersByTimeAsync(1000);
    });

    // fetchSduiSection (sectionEndpoint) must have been called, not the raw url fetch
    expect(fetchSduiSectionSpy).toHaveBeenCalledWith(
      expect.objectContaining({ endpoint: policy.sectionEndpoint }),
    );
    expect(onSectionReplace).toHaveBeenCalledWith(replacedSection);
    // Raw fetch must not have been called for the CDN url
    expect(fetch).not.toHaveBeenCalledWith('/cdn/should-not-be-called', expect.anything());

    fetchSduiSectionSpy.mockRestore();
  });
});

describe('useRefreshPolicy — SSE', () => {
  const mockedSubscribeToChannel = vi.mocked(subscribeToChannel);

  it('does not subscribe when enabled is false', () => {
    const section = makeSection({
      refreshPolicy: { type: RefreshType.SSE, channel: 'game:123' },
    });

    renderHook(() =>
      useRefreshPolicy({
        sectionId: section.id,
        refreshPolicy: section.refreshPolicy,
        onUpdate: vi.fn(),
        enabled: false,
      }),
    );

    expect(mockedSubscribeToChannel).not.toHaveBeenCalled();
  });

  it('subscribes to SSE channel when enabled', () => {
    const section = makeSection({
      refreshPolicy: { type: RefreshType.SSE, channel: 'game:123' },
    });

    renderHook(() =>
      useRefreshPolicy({
        sectionId: section.id,
        refreshPolicy: section.refreshPolicy,
        onUpdate: vi.fn(),
        enabled: true,
      }),
    );

    expect(mockedSubscribeToChannel).toHaveBeenCalledWith('game:123', expect.any(Function));
  });

  it('unsubscribes on disable (Option A behavior)', () => {
    const section = makeSection({
      refreshPolicy: { type: RefreshType.SSE, channel: 'game:123' },
    });

    const { rerender } = renderHook(
      ({ enabled }) =>
        useRefreshPolicy({
          sectionId: section.id,
          refreshPolicy: section.refreshPolicy,
          onUpdate: vi.fn(),
          enabled,
        }),
      { initialProps: { enabled: true } },
    );

    // Disable → should call unsubscribe
    rerender({ enabled: false });

    expect(mockUnsubscribeChannel).toHaveBeenCalled();
  });
});
