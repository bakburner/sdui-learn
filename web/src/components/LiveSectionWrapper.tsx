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
  const effectivePolicy = getEffectiveRefreshPolicy(section, defaultRefreshPolicy);

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

      // If we have data bindings, apply them to map incoming -> section data
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

      return currentData;
    });
  }, [section.id, section.dataBinding, hasDataBindings, section.stringTable, traceId]);
  useRefreshPolicy({
    sectionId: section.id,
    refreshPolicy: effectivePolicy,
    onUpdate: handleUpdate,
    onStalenessChange,
    enabled,
  });

  if (!liveData && hasRefreshPolicy) {
    return <div ref={sectionRef}><SectionSkeleton sectionStates={section.sectionStates} /></div>;
  }

  return <div ref={sectionRef}>{children(liveData)}</div>;
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

      return currentData;
    });
  }, [section.id, section.dataBinding, hasDataBindings, section.stringTable, traceId]);

  useRefreshPolicy({
    sectionId: section.id,
    refreshPolicy: effectivePolicy,
    onUpdate: handleUpdate,
    enabled: hasRefreshPolicy,
  });

  return liveData;
}
