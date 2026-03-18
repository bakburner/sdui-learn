import React, { useState, useCallback, useEffect } from 'react';
import type { Section, Action, Data } from '@sdui/models';
import { useRefreshPolicy, getEffectiveRefreshPolicy } from '../hooks/useRefreshPolicy';
import { applyDataBindings } from '../runtime/DataBindingApplier';
import { SectionSkeleton } from './SectionSkeleton';

interface LiveSectionWrapperProps {
  section: Section;
  state: Record<string, unknown>;
  onAction: (action: Action) => void;
  onStateChange: (key: string, value: unknown) => void;
  /** Optional screen-level default refresh policy */
  defaultRefreshPolicy?: Section['refreshPolicy'];
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
  children,
}: LiveSectionWrapperProps): React.ReactElement | null {
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
          incomingPayload as Record<string, unknown>
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
  }, [section.id, section.dataBinding, hasDataBindings]);
  useRefreshPolicy({
    section: {
      ...section,
      refreshPolicy: effectivePolicy,
    },
    onUpdate: handleUpdate,
    enabled: hasRefreshPolicy,
  });

  if (!liveData && hasRefreshPolicy) {
    return <SectionSkeleton sectionStates={section.sectionStates} />;
  }

  return children(liveData);
}

/**
 * Hook version for simpler consumption in section components.
 * Returns the live data for a section, handling refresh and bindings internally.
 */
export function useLiveData(
  section: Section,
  defaultRefreshPolicy?: Section['refreshPolicy']
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
          incomingPayload as Record<string, unknown>
        );
        return updated as Data;
      }

      if (typeof incomingPayload === 'object' && incomingPayload !== null) {
        return { ...currentData, ...incomingPayload } as Data;
      }

      return currentData;
    });
  }, [section.id, section.dataBinding, hasDataBindings]);

  useRefreshPolicy({
    section: {
      ...section,
      refreshPolicy: effectivePolicy,
    },
    onUpdate: handleUpdate,
    enabled: hasRefreshPolicy,
  });

  return liveData;
}
