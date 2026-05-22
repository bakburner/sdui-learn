import React, { useRef } from 'react';
import type { Section, Action, Data, RefreshPolicy } from '@sdui/models';
import { TabGroup } from './sections/TabGroup';
import { BoxscoreTable } from './sections/BoxscoreTable';
import { Form } from './sections/Form';
import { AdSlot } from './sections/AdSlot';
import { SeasonLeadersTable } from './sections/SeasonLeadersTable';
import { SubscribeBanner } from './sections/SubscribeBanner';
import { SubscribeHero } from './sections/SubscribeHero';
import { VideoPlayerStub } from './sections/VideoPlayerStub';
import { AtomicRouter } from './atomic';
import type { AtomicCompositeData } from './atomic';
import { CompositeContentContext } from '../utils/BindRefResolver';
import { LiveSectionWrapper, sectionPolicyKey } from './LiveSectionWrapper';
import { SectionErrorBoundary } from './SectionErrorBoundary';
import { SectionContainer } from './SectionContainer';
import { useImpressionTracking } from '../hooks/useImpressionTracking';

export interface SectionProps {
  section: Section;
  state: Record<string, unknown>;
  onAction: (action: Action | Action[]) => void;
  onStateChange: (key: string, value: unknown) => void;
  /** Callback when a sectionEndpoint refresh returns a replacement section */
  onSectionReplace?: (section: Section) => void;
  /** Callback when a sectionEndpoint refresh returns 404 — section is gone */
  onSectionGone?: (sectionId: string) => void;
  /** React key (passed by parent, not used in component) */
  key?: React.Key;
}

export interface SectionRouterProps extends SectionProps {
  /** Optional screen-level default refresh policy */
  defaultRefreshPolicy?: RefreshPolicy;
  /** Callback when section data channel becomes stale or recovers */
  onStalenessChange?: (sectionId: string, isStale: boolean) => void;
  /** Callback when the section endpoint signals the client schema is too old */
  onUpgradeRequired?: () => void;
  /** Screen-level traceId for log correlation */
  traceId?: string;
}

/**
 * Internal component that renders the appropriate section based on type.
 */
function SectionRenderer({ 
  section, 
  state, 
  onAction, 
  onStateChange,
  onSectionReplace,
  onSectionGone,
}: SectionProps): React.ReactElement | null {
  const commonProps = { section, state, onAction, onStateChange, onSectionReplace, onSectionGone };
  // Every section, permanent or AtomicComposite, is wrapped by
  // SectionContainer so outer chrome is server-driven via
  // `section.surface`. `SectionContainer` is a no-op when
  // `surface` is undefined, so AtomicComposites whose root Container
  // already carries its own padding/background/shadow are unaffected —
  // composers opt into outer margin/chrome by emitting a `surface`
  // block on the section envelope.
  const wrap = (node: React.ReactElement): React.ReactElement => (
    <SectionContainer surface={section.surface}>{node}</SectionContainer>
  );

  switch (section.type) {
    case 'TabGroup':
      return wrap(<TabGroup {...commonProps} />);

    case 'BoxscoreTable':
      return wrap(<BoxscoreTable {...commonProps} />);

    case 'Form':
      return wrap(<Form {...commonProps} />);

    case 'AdSlot':
      return wrap(<AdSlot {...commonProps} />);

    case 'SeasonLeadersTable':
      return wrap(<SeasonLeadersTable {...commonProps} />);

    case 'SubscribeBanner':
      return wrap(<SubscribeBanner {...commonProps} />);

    case 'SubscribeHero':
      return wrap(<SubscribeHero {...commonProps} />);

    case 'VideoPlayer':
      return wrap(<VideoPlayerStub {...commonProps} />);

    case 'AtomicComposite': {
      const compositeData = section.data as unknown as AtomicCompositeData | undefined;
      if (!compositeData?.ui) {
        console.debug(`[SectionRouter] AtomicComposite section ${section.id} has no ui element`);
        return null;
      }
      return wrap(
        <CompositeContentContext.Provider value={compositeData.content as Record<string, unknown> | undefined}>
          <AtomicRouter
            element={compositeData.ui}
            state={state}
            onAction={onAction}
            onStateChange={onStateChange}
            onSectionReplace={onSectionReplace}
            onSectionGone={onSectionGone}
          />
        </CompositeContentContext.Provider>
      );
    }
      
    default:
      // Ignore unknown section types (forward compatibility)
      console.debug(`[SectionRouter] Ignoring unknown section type: ${section.type}`);
      return null;
  }
}

/**
 * Routes sections to their appropriate renderers based on type.
 * Wraps sections in LiveSectionWrapper to handle refresh policies and data bindings.
 * Wraps in SectionErrorBoundary for graceful render error handling.
 * Attaches impression tracking for onVisible analytics actions.
 */
export function SectionRouter({ 
  section, 
  state, 
  onAction, 
  onStateChange,
  onSectionReplace,
  onSectionGone,
  defaultRefreshPolicy,
  onStalenessChange,
  onUpgradeRequired,
  traceId,
}: SectionRouterProps): React.ReactElement | null {
  const trackingRef = useRef<HTMLDivElement>(null);

  useImpressionTracking({
    ref: trackingRef,
    sectionId: section.id,
    actions: section.actions ?? [],
    onAction,
  });

  const needsLiveWrapper = Boolean(
    (section.refreshPolicy?.type && section.refreshPolicy.type !== 'static') ||
    (section.dataBinding?.bindings?.length) ||
    (defaultRefreshPolicy?.type && defaultRefreshPolicy.type !== 'static')
  );

  let content: React.ReactElement | null;

  if (needsLiveWrapper) {
    content = (
      <LiveSectionWrapper
        key={sectionPolicyKey(section)}
        section={section}
        state={state}
        onAction={onAction}
        onStateChange={onStateChange}
        defaultRefreshPolicy={defaultRefreshPolicy}
        onStalenessChange={onStalenessChange}
        onSectionReplace={onSectionReplace}
        onSectionGone={onSectionGone}
        onUpgradeRequired={onUpgradeRequired}
        traceId={traceId}
      >
        {(liveData: Data | undefined) => {
          const liveSection: Section = {
            ...section,
            data: liveData,
          };
          return (
            <SectionRenderer
              section={liveSection}
              state={state}
              onAction={onAction}
              onStateChange={onStateChange}
              onSectionReplace={onSectionReplace}
              onSectionGone={onSectionGone}
            />
          );
        }}
      </LiveSectionWrapper>
    );
  } else {
    content = (
      <SectionRenderer
        section={section}
        state={state}
        onAction={onAction}
        onStateChange={onStateChange}
        onSectionReplace={onSectionReplace}
        onSectionGone={onSectionGone}
      />
    );
  }

  return (
    <div ref={trackingRef}>
      <SectionErrorBoundary
        sectionStates={section.sectionStates}
        sectionId={section.id}
        sectionType={section.type}
        onAction={onAction}
      >
        {content}
      </SectionErrorBoundary>
    </div>
  );
}

/**
 * Render a list of sections.
 */
export function SectionList({ 
  sections, 
  state, 
  onAction, 
  onStateChange,
  onSectionReplace,
  onSectionGone,
  onStalenessChange,
  onUpgradeRequired,
  defaultRefreshPolicy,
  traceId,
}: { 
  sections: Section[]; 
  state: Record<string, unknown>;
  onAction: (action: Action | Action[]) => void;
  onStateChange: (key: string, value: unknown) => void;
  onSectionReplace?: (section: Section) => void;
  onSectionGone?: (sectionId: string) => void;
  onStalenessChange?: (sectionId: string, isStale: boolean) => void;
  onUpgradeRequired?: () => void;
  defaultRefreshPolicy?: RefreshPolicy;
  traceId?: string;
}): React.ReactElement {
  return (
    <>
      {sections.map((section) => (
        <div
          key={section.id}
          style={{
            marginTop: section.layoutHints?.marginTop ?? 0,
            marginBottom: section.layoutHints?.marginBottom ?? 0,
          }}
        >
          {section.layoutHints?.dividerAbove && <hr className="sdui-divider" />}
          <SectionRouter
            section={section}
            state={state}
            onAction={onAction}
            onStateChange={onStateChange}
            onSectionReplace={onSectionReplace}
            onSectionGone={onSectionGone}
            onStalenessChange={onStalenessChange}
            onUpgradeRequired={onUpgradeRequired}
            defaultRefreshPolicy={defaultRefreshPolicy}
            traceId={traceId}
          />
          {section.layoutHints?.dividerBelow && <hr className="sdui-divider" />}
        </div>
      ))}
    </>
  );
}
