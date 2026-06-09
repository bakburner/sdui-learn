import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { renderHook, act } from '@testing-library/react';
import { useRefreshPolicy } from '../useRefreshPolicy';
import { subscribeToChannel } from '../../runtime/AblyClient';
import * as fetchSduiScreenModule from '../../runtime/fetchSduiScreen';
import { SectionNotFoundError } from '../../runtime/fetchSduiScreen';
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
      refreshPolicy: [{ type: RefreshType.Poll, intervalMs: 1000, url: '/api/data' }],
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
      refreshPolicy: [{ type: RefreshType.Poll, intervalMs: 1000, url: '/api/data' }],
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
      refreshPolicy: [{ type: RefreshType.Poll, intervalMs: 1000, url: '/api/data' }],
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
      refreshPolicy: [{ type: RefreshType.Static }],
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

describe('useRefreshPolicy — concurrent policy elements', () => {
  it('runs opaque URL poll and sectionEndpoint poll concurrently', async () => {
    const replacedSection: Section = {
      id: 'test-section',
      type: 'AtomicComposite',
      data: { live: true },
      refreshPolicy: [{ type: RefreshType.Static }],
    } as Section;

    const fetchSduiSectionSpy = vi
      .spyOn(fetchSduiScreenModule, 'fetchSduiSection')
      .mockResolvedValue(replacedSection);

    const onSectionReplace = vi.fn();
    const policy = [
      {
        type: RefreshType.Poll,
        intervalMs: 1000,
        url: '/cdn/opaque',
      },
      {
        type: RefreshType.Poll,
        intervalMs: 1000,
        sectionEndpoint: '/v1/sdui/section/stats-api:game-123::AtomicComposite::scoreboard',
      },
    ];
    (globalThis.fetch as ReturnType<typeof vi.fn>).mockResolvedValue({
      ok: true,
      json: async () => ({ score: 100 }),
    });
    const onUpdate = vi.fn();

    renderHook(() =>
      useRefreshPolicy({
        sectionId: 'test-section',
        refreshPolicy: policy,
        onUpdate,
        onSectionReplace,
        enabled: true,
      }),
    );

    // First fetch fires at intervalMs (cross-platform aligned)
    await act(async () => {
      await vi.advanceTimersByTimeAsync(1000);
    });

    // Both drivers run: raw URL poll for opaque payload and sectionEndpoint for full replace.
    expect(fetch).toHaveBeenCalledWith('/cdn/opaque');
    expect(onUpdate).toHaveBeenCalledWith({ score: 100 });
    expect(fetchSduiSectionSpy).toHaveBeenCalledWith(
      expect.objectContaining({ endpoint: policy[1].sectionEndpoint }),
    );
    expect(onSectionReplace).toHaveBeenCalledWith(replacedSection);

    fetchSduiSectionSpy.mockRestore();
  });

  it('ignores extra opaque and sectionEndpoint elements beyond one each', async () => {
    const fetchSduiSectionSpy = vi
      .spyOn(fetchSduiScreenModule, 'fetchSduiSection')
      .mockResolvedValue(makeSection({ id: 'replaced-section' }));
    (globalThis.fetch as ReturnType<typeof vi.fn>).mockResolvedValue({
      ok: true,
      json: async () => ({ score: 50 }),
    });
    const warnSpy = vi.spyOn(console, 'warn').mockImplementation(() => {});

    renderHook(() =>
      useRefreshPolicy({
        sectionId: 'test-section',
        refreshPolicy: [
          { type: RefreshType.Poll, intervalMs: 1000, url: '/opaque-primary' },
          { type: RefreshType.Poll, intervalMs: 1000, url: '/opaque-extra' },
          { type: RefreshType.Poll, intervalMs: 1000, sectionEndpoint: '/v1/sdui/section/primary' },
          { type: RefreshType.Poll, intervalMs: 1000, sectionEndpoint: '/v1/sdui/section/extra' },
        ],
        onUpdate: vi.fn(),
        onSectionReplace: vi.fn(),
        enabled: true,
      }),
    );

    await act(async () => {
      await vi.advanceTimersByTimeAsync(1000);
    });

    expect(fetch).toHaveBeenCalledTimes(1);
    expect(fetch).toHaveBeenCalledWith('/opaque-primary');
    expect(fetchSduiSectionSpy).toHaveBeenCalledTimes(1);
    expect(fetchSduiSectionSpy).toHaveBeenCalledWith(
      expect.objectContaining({ endpoint: '/v1/sdui/section/primary' }),
    );
    expect(warnSpy).toHaveBeenCalledWith(
      expect.stringContaining('multiple opaque refresh policies'),
      expect.any(Object),
    );
    expect(warnSpy).toHaveBeenCalledWith(
      expect.stringContaining('multiple sectionEndpoint policies'),
      expect.any(Object),
    );

    fetchSduiSectionSpy.mockRestore();
    warnSpy.mockRestore();
  });
});

describe('useRefreshPolicy — sectionEndpoint error semantics', () => {
  it('marks section stale and stops polling when sectionEndpoint returns 404', async () => {
    const fetchSduiSectionSpy = vi
      .spyOn(fetchSduiScreenModule, 'fetchSduiSection')
      .mockRejectedValue(new SectionNotFoundError('Section not found: /v1/sdui/section/gone'));

    const onSectionReplace = vi.fn();
    const onStalenessChange = vi.fn();
    const section = makeSection();
    const policy = [{
      type: RefreshType.Poll,
      intervalMs: 1000,
      sectionEndpoint: '/v1/sdui/section/gone',
    }];

    renderHook(() =>
      useRefreshPolicy({
        sectionId: section.id,
        refreshPolicy: policy,
        onUpdate: vi.fn(),
        onSectionReplace,
        onStalenessChange,
        enabled: true,
      }),
    );

    // First poll fires at intervalMs — throws SectionNotFoundError
    await act(async () => {
      await vi.advanceTimersByTimeAsync(1000);
    });
    expect(onStalenessChange).toHaveBeenCalledWith(section.id, true);
    expect(onSectionReplace).not.toHaveBeenCalled();

    const callCountAfterGone = fetchSduiSectionSpy.mock.calls.length;

    // No further polls — isSectionGoneRef stops rescheduling
    await act(async () => {
      await vi.advanceTimersByTimeAsync(2000);
    });
    expect(fetchSduiSectionSpy.mock.calls.length).toBe(callCountAfterGone);

    fetchSduiSectionSpy.mockRestore();
  });

  it('marks section stale after POLL_FAILURE_THRESHOLD consecutive sectionEndpoint failures', async () => {
    const POLL_FAILURE_THRESHOLD = 2;

    const fetchSduiSectionSpy = vi
      .spyOn(fetchSduiScreenModule, 'fetchSduiSection')
      .mockRejectedValue(new Error('Server error'));

    const onStalenessChange = vi.fn();
    const section = makeSection();
    const policy = [{
      type: RefreshType.Poll,
      intervalMs: 1000,
      sectionEndpoint: '/v1/sdui/section/scoreboard',
    }];

    renderHook(() =>
      useRefreshPolicy({
        sectionId: section.id,
        refreshPolicy: policy,
        onUpdate: vi.fn(),
        onStalenessChange,
        enabled: true,
      }),
    );

    // First failure at 1000ms — below threshold, not yet stale
    await act(async () => {
      await vi.advanceTimersByTimeAsync(1000);
    });
    expect(onStalenessChange).not.toHaveBeenCalledWith(section.id, true);

    // Backoff doubles to 2000ms — second failure reaches POLL_FAILURE_THRESHOLD
    await act(async () => {
      await vi.advanceTimersByTimeAsync(2000);
    });
    expect(fetchSduiSectionSpy.mock.calls.length).toBeGreaterThanOrEqual(POLL_FAILURE_THRESHOLD);
    expect(onStalenessChange).toHaveBeenCalledWith(section.id, true);

    fetchSduiSectionSpy.mockRestore();
  });
});

describe('useRefreshPolicy — SSE', () => {
  const mockedSubscribeToChannel = vi.mocked(subscribeToChannel);

  it('does not subscribe when enabled is false', () => {
    const section = makeSection({
      refreshPolicy: [{ type: RefreshType.SSE, channel: 'game:123' }],
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
      refreshPolicy: [{ type: RefreshType.SSE, channel: 'game:123' }],
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
      refreshPolicy: [{ type: RefreshType.SSE, channel: 'game:123' }],
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
