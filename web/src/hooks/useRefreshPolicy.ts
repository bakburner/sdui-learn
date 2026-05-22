import { useEffect, useRef, useCallback } from 'react';
import type { RefreshPolicy, Section } from '@sdui/models';
import { subscribeToChannel, onConnectionStateChange } from '../runtime/AblyClient';
import { fetchSduiSection, SectionNotFoundError, SchemaVersionMismatchError } from '../runtime/fetchSduiScreen';

const POLL_FAILURE_THRESHOLD = 2;
const MAX_BACKOFF_MS = 30_000;
const SSE_STALE_DELAY_MS = 10_000;

interface UseRefreshPolicyOptions {
  sectionId: string;
  refreshPolicy: Section['refreshPolicy'];
  onUpdate: (data: unknown) => void;
  onSectionReplace?: (section: Section) => void;
  onSectionGone?: () => void;
  onStalenessChange?: (sectionId: string, isStale: boolean) => void;
  onUpgradeRequired?: () => void;
  enabled?: boolean;
  /** Screen-level trace ID — propagated to section endpoint fetches so server
   *  logs can correlate the refresh response with its parent screen (§4.1.1). */
  traceId?: string;
}

/**
 * Hook to handle refresh policies (poll, sse) for a section.
 *
 * - poll: re-fetches at the configured interval with exponential backoff on failure
 * - sse: subscribes to Ably channel for real-time updates with disconnect staleness
 * - static: no-op
 */
export function useRefreshPolicy(options: UseRefreshPolicyOptions): void {
  const {
    sectionId,
    refreshPolicy: policy,
    onUpdate,
    onSectionReplace,
    onSectionGone,
    onStalenessChange,
    onUpgradeRequired,
    enabled = true,
    traceId,
  } = options;
  const timeoutRef = useRef<number | null>(null);
  const pollFailureCount = useRef(0);
  const currentIntervalRef = useRef<number | null>(null);
  const isStaleFlagRef = useRef(false);
  const isSectionGoneRef = useRef(false);

  const markStale = useCallback((isStale: boolean) => {
    if (isStaleFlagRef.current !== isStale) {
      isStaleFlagRef.current = isStale;
      onStalenessChange?.(sectionId, isStale);
    }
  }, [sectionId, onStalenessChange]);

  const fetchData = useCallback(async (): Promise<boolean> => {
    if (!policy) return false;

    const sectionEndpoint = policy.sectionEndpoint;
    const pollUrl = policy.url;

    if (sectionEndpoint) {
      try {
        const section = await fetchSduiSection({ endpoint: sectionEndpoint, traceId });
        onSectionReplace?.(section);
        return true;
      } catch (err) {
        if (err instanceof SectionNotFoundError) {
          console.warn(`[RefreshPolicy] Section ${sectionId} returned 404 — stopping poll`);
          isSectionGoneRef.current = true;
          onSectionGone?.();
          return false;
        }
        if (err instanceof SchemaVersionMismatchError) {
          console.warn(`[RefreshPolicy] Schema version mismatch on section ${sectionId} — upgrade required`);
          isSectionGoneRef.current = true;
          onUpgradeRequired?.();
          return false;
        }
        console.error(`[RefreshPolicy] sectionEndpoint fetch failed for ${sectionId}:`, err);
        return false;
      }
    }

    if (!pollUrl) {
      return false;
    }

    try {
      const response = await fetch(pollUrl);
      if (response.ok) {
        const data = await response.json();
        onUpdate(data);
        return true;
      }
      return false;
    } catch (err) {
      console.error(`[RefreshPolicy] Poll failed for ${sectionId}:`, err);
      return false;
    }
  }, [sectionId, policy, onUpdate, onSectionReplace, onSectionGone, traceId]);

  // Poll with exponential backoff
  useEffect(() => {
    if (!enabled || !policy || policy.type !== 'poll' || !policy.intervalMs) return;

    const baseInterval = policy.intervalMs;
    pollFailureCount.current = 0;
    currentIntervalRef.current = baseInterval;
    isStaleFlagRef.current = false;
    isSectionGoneRef.current = false;

    console.log(`[RefreshPolicy] Starting poll for ${sectionId} every ${baseInterval}ms`);

    const schedulePoll = () => {
      timeoutRef.current = window.setTimeout(async () => {
        if (isSectionGoneRef.current) return;

        const success = await fetchData();

        if (success) {
          pollFailureCount.current = 0;
          currentIntervalRef.current = baseInterval;
          markStale(false);
        } else {
          pollFailureCount.current += 1;
          // Exponential backoff: double on failure, cap at MAX_BACKOFF_MS
          currentIntervalRef.current = Math.min(
            (currentIntervalRef.current ?? baseInterval) * 2,
            MAX_BACKOFF_MS
          );
          if (pollFailureCount.current >= POLL_FAILURE_THRESHOLD) {
            markStale(true);
          }
        }

        if (!isSectionGoneRef.current) {
          schedulePoll();
        }
      }, currentIntervalRef.current ?? baseInterval);
    };

    // Cross-platform aligned: Android/iOS wait `intervalMs` before the first
    // tick — the initial render is the first refresh, so do not fire an extra
    // fetch immediately on mount. Just schedule.
    schedulePoll();

    return () => {
      if (timeoutRef.current !== null) {
        clearTimeout(timeoutRef.current);
        timeoutRef.current = null;
      }
    };
  }, [enabled, policy, sectionId, fetchData, markStale]);

  // SSE subscription with disconnect staleness
  useEffect(() => {
    if (!enabled || !policy || policy.type !== 'sse' || !policy.channel) return;

    console.log(`[RefreshPolicy] Subscribing to Ably channel "${policy.channel}" for ${sectionId}`);
    isStaleFlagRef.current = false;
    let staleTimerId: number | null = null;

    const unsubscribeChannel = subscribeToChannel(policy.channel, (data) => {
      // Successful message — clear staleness
      markStale(false);
      if (staleTimerId !== null) {
        clearTimeout(staleTimerId);
        staleTimerId = null;
      }
      onUpdate(data);
    });

    const unsubscribeConnection = onConnectionStateChange((state) => {
      if (state === 'disconnected' || state === 'suspended' || state === 'failed') {
        if (staleTimerId === null) {
          staleTimerId = window.setTimeout(() => {
            markStale(true);
            staleTimerId = null;
          }, SSE_STALE_DELAY_MS);
        }
      } else if (state === 'connected') {
        if (staleTimerId !== null) {
          clearTimeout(staleTimerId);
          staleTimerId = null;
        }
        // Don't clear staleness here — wait for an actual message (above)
      }
    });

    return () => {
      unsubscribeChannel();
      unsubscribeConnection();
      if (staleTimerId !== null) {
        clearTimeout(staleTimerId);
      }
    };
  }, [enabled, policy, sectionId, onUpdate, markStale]);
}

/**
 * Extract effective refresh policy — section-level takes precedence over screen default.
 */
export function getEffectiveRefreshPolicy(
  section: Section,
  defaultPolicy?: RefreshPolicy,
): RefreshPolicy | undefined {
  return section.refreshPolicy ?? defaultPolicy;
}
