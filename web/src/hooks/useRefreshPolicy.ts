import { useEffect, useRef, useCallback } from 'react';
import type { RefreshPolicy, Section } from '@sdui/models';
import { subscribeToChannel, onConnectionStateChange } from '../runtime/AblyClient';

const POLL_FAILURE_THRESHOLD = 2;
const MAX_BACKOFF_MS = 30_000;
const SSE_STALE_DELAY_MS = 10_000;

interface UseRefreshPolicyOptions {
  section: Section;
  onUpdate: (data: unknown) => void;
  onStalenessChange?: (sectionId: string, isStale: boolean) => void;
  enabled?: boolean;
}

/**
 * Hook to handle refresh policies (poll, sse) for a section.
 *
 * - poll: re-fetches at the configured interval with exponential backoff on failure
 * - sse: subscribes to Ably channel for real-time updates with disconnect staleness
 * - static: no-op
 */
export function useRefreshPolicy(options: UseRefreshPolicyOptions): void {
  const { section, onUpdate, onStalenessChange, enabled = true } = options;
  const policy = section.refreshPolicy;
  const timeoutRef = useRef<number | null>(null);
  const pollFailureCount = useRef(0);
  const currentIntervalRef = useRef<number | null>(null);
  const isStaleFlagRef = useRef(false);

  const markStale = useCallback((isStale: boolean) => {
    if (isStaleFlagRef.current !== isStale) {
      isStaleFlagRef.current = isStale;
      onStalenessChange?.(section.id, isStale);
    }
  }, [section.id, onStalenessChange]);

  const fetchData = useCallback(async (): Promise<boolean> => {
    if (!policy) return false;

    const pollUrl = policy.url;
    if (!pollUrl) {
      console.warn(`[RefreshPolicy] No url in refreshPolicy for section ${section.id}, skipping poll`);
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
      console.error(`[RefreshPolicy] Poll failed for ${section.id}:`, err);
      return false;
    }
  }, [section.id, policy, onUpdate]);

  // Poll with exponential backoff
  useEffect(() => {
    if (!enabled || !policy || policy.type !== 'poll' || !policy.intervalMs) return;

    const baseInterval = policy.intervalMs;
    pollFailureCount.current = 0;
    currentIntervalRef.current = baseInterval;
    isStaleFlagRef.current = false;

    console.log(`[RefreshPolicy] Starting poll for ${section.id} every ${baseInterval}ms`);

    const schedulePoll = () => {
      timeoutRef.current = window.setTimeout(async () => {
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

        schedulePoll();
      }, currentIntervalRef.current ?? baseInterval);
    };

    // Initial fetch, then start the schedule
    fetchData().then((success) => {
      if (!success) {
        pollFailureCount.current += 1;
      }
      schedulePoll();
    });

    return () => {
      if (timeoutRef.current !== null) {
        clearTimeout(timeoutRef.current);
        timeoutRef.current = null;
      }
    };
  }, [enabled, policy, section.id, fetchData, markStale]);

  // SSE subscription with disconnect staleness
  useEffect(() => {
    if (!enabled || !policy || policy.type !== 'sse' || !policy.channel) return;

    console.log(`[RefreshPolicy] Subscribing to Ably channel "${policy.channel}" for ${section.id}`);
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
  }, [enabled, policy, section.id, onUpdate, markStale]);
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
