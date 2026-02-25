import { useEffect, useRef, useCallback } from 'react';
import type { RefreshPolicy, Section } from '@sdui/models';
import { subscribeToChannel } from '../runtime/AblyClient';

interface UseRefreshPolicyOptions {
  section: Section;
  onUpdate: (data: unknown) => void;
  enabled?: boolean;
}

/**
 * Hook to handle refresh policies (poll, sse) for a section.
 *
 * - poll: re-fetches at the configured interval
 * - sse: subscribes to Ably channel for real-time updates
 * - static: no-op
 */
export function useRefreshPolicy(options: UseRefreshPolicyOptions): void {
  const { section, onUpdate, enabled = true } = options;
  const intervalRef = useRef<number | null>(null);
  const policy = section.refreshPolicy;

  const fetchData = useCallback(async () => {
    if (!policy) return;

    const pollUrl = policy.url;
    if (!pollUrl) {
      console.warn(`[RefreshPolicy] No url in refreshPolicy for section ${section.id}, skipping poll`);
      return;
    }

    try {
      const response = await fetch(pollUrl);
      if (response.ok) {
        const data = await response.json();
        onUpdate(data);
      }
    } catch (err) {
      console.error(`[RefreshPolicy] Poll failed for ${section.id}:`, err);
    }
  }, [section.id, policy, onUpdate]);

  useEffect(() => {
    if (!enabled || !policy) return;

    if (policy.type === 'poll' && policy.intervalMs) {
      console.log(`[RefreshPolicy] Starting poll for ${section.id} every ${policy.intervalMs}ms`);

      fetchData();
      intervalRef.current = window.setInterval(fetchData, policy.intervalMs);

      return () => {
        if (intervalRef.current !== null) {
          clearInterval(intervalRef.current);
          intervalRef.current = null;
        }
      };
    }

    if (policy.type === 'sse' && policy.channel) {
      console.log(`[RefreshPolicy] Subscribing to Ably channel "${policy.channel}" for ${section.id}`);
      const unsubscribe = subscribeToChannel(policy.channel, (data) => {
        onUpdate(data);
      });
      return unsubscribe;
    }

    return undefined;
  }, [enabled, policy, section.id, fetchData, onUpdate]);
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
