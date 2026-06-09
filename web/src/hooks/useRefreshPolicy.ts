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
  onStalenessChange?: (sectionId: string, isStale: boolean) => void;
  onUpgradeRequired?: () => void;
  enabled?: boolean;
  isAppVisible?: boolean;
  isNearViewport?: boolean;
  /** Screen-level correlation ID — propagated to section endpoint fetches so
   *  server logs can correlate the refresh response with its parent screen
   *  (§4.1.1). Sent on the wire as `X-Correlation-ID`. */
  correlationId?: string;
}

interface EffectiveRefreshPolicy {
  allPolicies: RefreshPolicy[];
  opaquePolicy?: RefreshPolicy;
  sectionRefreshPolicy?: RefreshPolicy;
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
    refreshPolicy: policies,
    onUpdate,
    onSectionReplace,
    onStalenessChange,
    onUpgradeRequired,
    enabled = true,
    isAppVisible = true,
    isNearViewport = true,
    correlationId,
  } = options;
  const {
    opaquePolicy,
    sectionRefreshPolicy,
  } = resolveRefreshPolicyElements(policies, sectionId);

  const opaqueTimeoutRef = useRef<number | null>(null);
  const sectionTimeoutRef = useRef<number | null>(null);
  const opaquePollFailureCount = useRef(0);
  const sectionPollFailureCount = useRef(0);
  const opaqueCurrentIntervalRef = useRef<number | null>(null);
  const sectionCurrentIntervalRef = useRef<number | null>(null);
  const isOpaqueStaleRef = useRef(false);
  const isSectionStaleRef = useRef(false);
  const isSectionGoneRef = useRef(false);
  const isCombinedStaleRef = useRef(false);

  const emitCombinedStaleness = useCallback(() => {
    const isStale = isOpaqueStaleRef.current || isSectionStaleRef.current;
    if (isCombinedStaleRef.current !== isStale) {
      isCombinedStaleRef.current = isStale;
      onStalenessChange?.(sectionId, isStale);
    }
  }, [sectionId, onStalenessChange]);

  const markOpaqueStale = useCallback((isStale: boolean) => {
    isOpaqueStaleRef.current = isStale;
    emitCombinedStaleness();
  }, [emitCombinedStaleness]);

  const markSectionStale = useCallback((isStale: boolean) => {
    isSectionStaleRef.current = isStale;
    emitCombinedStaleness();
  }, [emitCombinedStaleness]);

  const opaquePauseWhenOffScreen = opaquePolicy?.pauseWhenOffScreen ?? true;
  const sectionPauseWhenOffScreen = sectionRefreshPolicy?.pauseWhenOffScreen ?? true;
  const baseEnabled = enabled && isAppVisible;
  const opaqueEnabled = baseEnabled && (opaquePauseWhenOffScreen ? isNearViewport : true);
  const sectionEnabled = baseEnabled && (sectionPauseWhenOffScreen ? isNearViewport : true);

  const opaqueType = opaquePolicy?.type;
  const opaqueChannel = opaquePolicy?.channel;
  const opaquePollUrl = opaquePolicy?.url;
  const opaqueIntervalMs = opaquePolicy?.intervalMs;
  const sectionEndpoint = sectionRefreshPolicy?.sectionEndpoint;
  const sectionIntervalMs = sectionRefreshPolicy?.intervalMs;

  const fetchOpaquePoll = useCallback(async (): Promise<boolean> => {
    if (!opaquePollUrl) {
      return false;
    }

    try {
      const response = await fetch(opaquePollUrl);
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
  }, [opaquePollUrl, onUpdate, sectionId]);

  const fetchSectionRefresh = useCallback(async (): Promise<boolean> => {
    if (!sectionEndpoint) return false;

    try {
      const section = await fetchSduiSection({ endpoint: sectionEndpoint, correlationId });
      onSectionReplace?.(section);
      return true;
    } catch (err) {
      if (err instanceof SectionNotFoundError) {
        console.warn(`[RefreshPolicy] Section ${sectionId} returned 404 — stopping poll`);
        isSectionGoneRef.current = true;
        markSectionStale(true);
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
  }, [sectionEndpoint, correlationId, onSectionReplace, sectionId, onUpgradeRequired, markSectionStale]);

  const runPollWithBackoff = useCallback((args: {
    effectName: string;
    enabledFlag: boolean;
    intervalMs?: number;
    timeoutRef: React.MutableRefObject<number | null>;
    failureRef: React.MutableRefObject<number>;
    currentIntervalRef: React.MutableRefObject<number | null>;
    shouldStop?: () => boolean;
    fetcher: () => Promise<boolean>;
    markStale: (isStale: boolean) => void;
  }) => {
    const {
      effectName,
      enabledFlag,
      intervalMs,
      timeoutRef,
      failureRef,
      currentIntervalRef,
      shouldStop,
      fetcher,
      markStale,
    } = args;
    if (!enabledFlag || !intervalMs) {
      return () => {};
    }

    failureRef.current = 0;
    currentIntervalRef.current = intervalMs;
    markStale(false);

    console.log(`[RefreshPolicy] Starting ${effectName} poll for ${sectionId} every ${intervalMs}ms`);

    const schedulePoll = () => {
      timeoutRef.current = window.setTimeout(async () => {
        if (shouldStop?.()) return;

        const success = await fetcher();

        if (success) {
          failureRef.current = 0;
          currentIntervalRef.current = intervalMs;
          markStale(false);
        } else {
          failureRef.current += 1;
          currentIntervalRef.current = Math.min(
            (currentIntervalRef.current ?? intervalMs) * 2,
            MAX_BACKOFF_MS,
          );
          if (failureRef.current >= POLL_FAILURE_THRESHOLD) {
            markStale(true);
          }
        }

        if (!shouldStop?.()) {
          schedulePoll();
        }
      }, currentIntervalRef.current ?? intervalMs);
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
  }, [sectionId]);

  // Opaque element execution: SSE channel or URL poll.
  useEffect(() => {
    if (!opaqueEnabled || !opaqueType) {
      markOpaqueStale(false);
      return;
    }

    if (opaqueType === 'poll') {
      return runPollWithBackoff({
        effectName: 'opaque',
        enabledFlag: opaqueEnabled,
        intervalMs: opaqueIntervalMs,
        timeoutRef: opaqueTimeoutRef,
        failureRef: opaquePollFailureCount,
        currentIntervalRef: opaqueCurrentIntervalRef,
        fetcher: fetchOpaquePoll,
        markStale: markOpaqueStale,
      });
    }

    if (opaqueType !== 'sse' || !opaqueChannel) {
      return;
    }

    console.log(`[RefreshPolicy] Subscribing to Ably channel "${opaqueChannel}" for ${sectionId}`);
    markOpaqueStale(false);
    let staleTimerId: number | null = null;

    const unsubscribeChannel = subscribeToChannel(opaqueChannel, (data) => {
      markOpaqueStale(false);
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
            markOpaqueStale(true);
            staleTimerId = null;
          }, SSE_STALE_DELAY_MS);
        }
      } else if (state === 'connected' && staleTimerId !== null) {
        clearTimeout(staleTimerId);
        staleTimerId = null;
      }
    });

    return () => {
      unsubscribeChannel();
      unsubscribeConnection();
      if (staleTimerId !== null) {
        clearTimeout(staleTimerId);
      }
    };
  }, [
    opaqueEnabled,
    opaqueType,
    opaqueChannel,
    opaqueIntervalMs,
    sectionId,
    onUpdate,
    fetchOpaquePoll,
    markOpaqueStale,
    runPollWithBackoff,
  ]);

  // Section refresh element execution: sectionEndpoint poll only.
  useEffect(() => {
    if (!sectionEnabled || !sectionEndpoint || sectionRefreshPolicy?.type !== 'poll') {
      markSectionStale(false);
      return;
    }

    isSectionGoneRef.current = false;
    return runPollWithBackoff({
      effectName: 'sectionEndpoint',
      enabledFlag: sectionEnabled,
      intervalMs: sectionIntervalMs,
      timeoutRef: sectionTimeoutRef,
      failureRef: sectionPollFailureCount,
      currentIntervalRef: sectionCurrentIntervalRef,
      shouldStop: () => isSectionGoneRef.current,
      fetcher: fetchSectionRefresh,
      markStale: markSectionStale,
    });
  }, [
    sectionEnabled,
    sectionEndpoint,
    sectionIntervalMs,
    sectionRefreshPolicy?.type,
    fetchSectionRefresh,
    markSectionStale,
    runPollWithBackoff,
  ]);
}

/**
 * Extract effective refresh policy — section-level takes precedence over screen default.
 */
export function getEffectiveRefreshPolicy(
  section: Section,
  defaultPolicy?: RefreshPolicy,
): EffectiveRefreshPolicy {
  const allPolicies = section.refreshPolicy ?? (defaultPolicy ? [defaultPolicy] : []);
  const { opaquePolicy, sectionRefreshPolicy } = resolveRefreshPolicyElements(allPolicies, section.id);
  return {
    allPolicies,
    opaquePolicy,
    sectionRefreshPolicy,
  };
}

function resolveRefreshPolicyElements(
  policies: Section['refreshPolicy'] | undefined,
  sectionId: string,
): Pick<EffectiveRefreshPolicy, 'opaquePolicy' | 'sectionRefreshPolicy'> {
  let opaquePolicy: RefreshPolicy | undefined;
  let sectionRefreshPolicy: RefreshPolicy | undefined;

  if (!policies) {
    return { opaquePolicy, sectionRefreshPolicy };
  }

  for (const policy of policies) {
    const isOpaque = Boolean(policy.channel || policy.url);
    const isSectionRefresh = Boolean(policy.sectionEndpoint);

    if (policy.type === 'static') {
      if (policies.length > 1) {
        console.warn(
          `[RefreshPolicy] Section ${sectionId} has static in a multi-policy array; ignoring static element.`,
          policy,
        );
      }
      continue;
    }

    if (isOpaque) {
      if (!opaquePolicy) {
        opaquePolicy = policy;
      } else {
        console.warn(
          `[RefreshPolicy] Section ${sectionId} has multiple opaque refresh policies; ignoring extra element.`,
          policy,
        );
      }
    }

    if (isSectionRefresh) {
      if (!sectionRefreshPolicy) {
        sectionRefreshPolicy = policy;
      } else {
        console.warn(
          `[RefreshPolicy] Section ${sectionId} has multiple sectionEndpoint policies; ignoring extra element.`,
          policy,
        );
      }
    }
  }

  return { opaquePolicy, sectionRefreshPolicy };
}
