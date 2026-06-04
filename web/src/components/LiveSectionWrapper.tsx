import React, { useState, useCallback, useEffect, useRef } from 'react';
import type { Section, Action, Data } from '@sdui/models';
import { useRefreshPolicy, getEffectiveRefreshPolicy } from '../hooks/useRefreshPolicy';
import { useSectionVisibility } from '../hooks/useSectionVisibility';
import { useAppVisibility } from '../hooks/useAppVisibility';
import { applyDataBindings } from '../runtime/DataBindingApplier';
import { SectionSkeleton } from './SectionSkeleton';

interface LiveSectionWrapperProps {
  section: Section;
  state: Record<string, unknown>;
  onAction: (action: Action | Action[]) => void;
  onStateChange: (key: string, value: unknown) => void;
  /** Optional screen-level default refresh policy */
  defaultRefreshPolicy?: Section['refreshPolicy'];
  /** Callback when section data channel becomes stale or recovers */
  onStalenessChange?: (sectionId: string, isStale: boolean) => void;
  /** Callback when a sectionEndpoint refresh returns a replacement section */
  onSectionReplace?: (section: Section) => void;
  /** Callback when a sectionEndpoint refresh returns 404 — section is gone */
  onSectionGone?: (sectionId: string) => void;
  /** Callback when the section endpoint signals the client schema is too old */
  onUpgradeRequired?: () => void;
  /** Screen-level traceId for log correlation */
  traceId?: string;
  children: (effectiveData: Data | undefined) => React.ReactElement | null;
}

/**
 * Wrapper that handles live data updates for sections with refreshPolicy.
 * 
 * - Holds local "live" data state (initialized from section.data)
 * - Calls useRefreshPolicy to start poll/SSE
 * - Applies data bindings from incoming payloads
 * - Passes updated data to child via render prop
 */
export function LiveSectionWrapper({
  section,
  defaultRefreshPolicy,
  onStalenessChange,
  onSectionReplace,
  onSectionGone,
  onUpgradeRequired,
  traceId,
  children,
}: LiveSectionWrapperProps): React.ReactElement | null {
  const sectionRef = useRef<HTMLDivElement>(null);
  const isNearViewport = useSectionVisibility(sectionRef);
  const isAppVisible = useAppVisibility();

  // Initialize local data from section.data
  const [liveData, setLiveData] = useState<Data | undefined>(section.data as Data | undefined);

  // Reset liveData when section.data changes (e.g., full screen refetch)
  useEffect(() => {
    setLiveData(section.data as Data | undefined);
  }, [section.data]);

  // Get effective refresh policy (section-level takes precedence)
  const baseEffectivePolicy = getEffectiveRefreshPolicy(section, defaultRefreshPolicy);

  // Guard: sectionEndpoint and a non-static screen defaultRefreshPolicy are mutually exclusive.
  // The screen-level refresh owns the section when both are present.
  const screenIsRefreshing = defaultRefreshPolicy && defaultRefreshPolicy.type !== 'static';
  if (section.refreshPolicy?.sectionEndpoint && screenIsRefreshing) {
    console.warn(
      `[LiveSectionWrapper] Section '${section.id}' has sectionEndpoint but screen defaultRefreshPolicy ` +
      `is '${defaultRefreshPolicy?.type}' — skipping sectionEndpoint poll; screen-level refresh owns this section.`
    );
  }
  const effectivePolicy =
    section.refreshPolicy?.sectionEndpoint && screenIsRefreshing ? undefined : baseEffectivePolicy;

  // Determine if this section has refresh/binding capabilities
  const hasRefreshPolicy = Boolean(effectivePolicy?.type && effectivePolicy.type !== 'static');
  const hasDataBindings = Boolean(section.dataBinding?.bindings?.length);

  // Visibility-gated enabled: respects pauseWhenOffScreen from server + app visibility
  const pauseWhenOffScreen = effectivePolicy?.pauseWhenOffScreen ?? true;
  const enabled = hasRefreshPolicy && isAppVisible && (pauseWhenOffScreen ? isNearViewport : true);

  // Handle incoming data from poll/SSE
  const handleUpdate = useCallback((incomingPayload: unknown) => {
    console.log(`[LiveSectionWrapper] Received update for ${section.id}:`, incomingPayload);

    setLiveData((currentData) => {
      if (!currentData) return currentData;

      // Per-message isolation: a thrown exception inside applyDataBindings
      // (or the shallow-merge branch below) must not propagate out of
      // setLiveData, which would unmount the component tree via React's
      // error boundary path and silently end this section's subscription.
      // Catch here so the next message still gets a chance.
      try {
        if (hasDataBindings && section.dataBinding) {
          const updated = applyDataBindings(
            currentData as Record<string, unknown>,
            section.dataBinding,
            incomingPayload as Record<string, unknown>,
            (section as Record<string, unknown>).stringTable as Record<string, string> | undefined,
            section.id,
            traceId
          );
          return updated as Data;
        }

        // If no bindings, try to merge incoming payload directly
        // (assumes incoming payload has same shape as section.data)
        if (typeof incomingPayload === 'object' && incomingPayload !== null) {
          return { ...currentData, ...incomingPayload } as Data;
        }
      } catch (err) {
        console.error(
          `[LiveSectionWrapper] Failed to apply update for ${section.id} (subscription stays open):`,
          err,
        );
      }

      return currentData;
    });
  }, [section.id, section.dataBinding, hasDataBindings, section.stringTable, traceId]);
  const handleSectionGone = useCallback(() => {
    onSectionGone?.(section.id);
  }, [onSectionGone, section.id]);

  useRefreshPolicy({
    sectionId: section.id,
    refreshPolicy: effectivePolicy,
    onUpdate: handleUpdate,
    onSectionReplace,
    onSectionGone: handleSectionGone,
    onStalenessChange,
    onUpgradeRequired,
    enabled,
    correlationId: traceId,
  });

  if (!liveData && hasRefreshPolicy) {
    return <div ref={sectionRef}><SectionSkeleton sectionStates={section.sectionStates} /></div>;
  }

  return <div ref={sectionRef}>{children(liveData)}</div>;
}

/**
 * Derive a stable policy fingerprint for use as a React key on LiveSectionWrapper.
 * The parent render site must use this key so policy changes remount the wrapper,
 * tearing down the old useRefreshPolicy instance and starting fresh with the new policy.
 */
export function sectionPolicyKey(section: Section): string {
  const p = section.refreshPolicy;
  if (!p) return `${section.id}::static`;
  return `${section.id}::${p.type}::${p.channel ?? p.sectionEndpoint ?? ''}`;
}

/**
 * Hook version for simpler consumption in section components.
 * Returns the live data for a section, handling refresh and bindings internally.
 */
export function useLiveData(
  section: Section,
  defaultRefreshPolicy?: Section['refreshPolicy'],
  traceId?: string,
): Data | undefined {
  const [liveData, setLiveData] = useState<Data | undefined>(section.data as Data | undefined);

  useEffect(() => {
    setLiveData(section.data as Data | undefined);
  }, [section.data]);

  const effectivePolicy = getEffectiveRefreshPolicy(section, defaultRefreshPolicy);
  const hasRefreshPolicy = Boolean(effectivePolicy?.type && effectivePolicy.type !== 'static');
  const hasDataBindings = Boolean(section.dataBinding?.bindings?.length);

  const handleUpdate = useCallback((incomingPayload: unknown) => {
    console.log(`[useLiveData] Received update for ${section.id}:`, incomingPayload);

    setLiveData((currentData) => {
      if (!currentData) return currentData;

      // Per-message isolation: see LiveSectionWrapper.handleUpdate above.
      try {
        if (hasDataBindings && section.dataBinding) {
          const updated = applyDataBindings(
            currentData as Record<string, unknown>,
            section.dataBinding,
            incomingPayload as Record<string, unknown>,
            (section as Record<string, unknown>).stringTable as Record<string, string> | undefined,
            section.id,
            traceId
          );
          return updated as Data;
        }

        if (typeof incomingPayload === 'object' && incomingPayload !== null) {
          return { ...currentData, ...incomingPayload } as Data;
        }
      } catch (err) {
        console.error(
          `[useLiveData] Failed to apply update for ${section.id} (subscription stays open):`,
          err,
        );
      }

      return currentData;
    });
  }, [section.id, section.dataBinding, hasDataBindings, section.stringTable, traceId]);

  useRefreshPolicy({
    sectionId: section.id,
    refreshPolicy: effectivePolicy,
    onUpdate: handleUpdate,
    enabled: hasRefreshPolicy,
    correlationId: traceId,
  });

  return liveData;
}
