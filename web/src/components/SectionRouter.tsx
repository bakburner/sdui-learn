import React, { useRef } from 'react';
import type { Section, Action, Data, RefreshPolicy } from '@sdui/models';
import { ScoreboardHeader } from './sections/ScoreboardHeader';
import { StatLine } from './sections/StatLine';
import { HeroPanel } from './sections/HeroPanel';
import { TabGroup } from './sections/TabGroup';
import { GamePanel } from './sections/GamePanel';
import { Row } from './sections/Row';
import { BoxscoreTable } from './sections/BoxscoreTable';
import { Form } from './sections/Form';
import { AdSlot } from './sections/AdSlot';
import { SeasonLeadersTable } from './sections/SeasonLeadersTable';
import { VideoCarousel } from './sections/VideoCarousel';
import { NbaTvSchedule } from './sections/NbaTvSchedule';
import { SubscribeBanner } from './sections/SubscribeBanner';
import { SubscribeHero } from './sections/SubscribeHero';
import { AtomicRouter } from './atomic';
import type { AtomicCompositeData } from './atomic';
import { LiveSectionWrapper } from './LiveSectionWrapper';
import { SectionErrorBoundary } from './SectionErrorBoundary';
import { useImpressionTracking } from '../hooks/useImpressionTracking';

export interface SectionProps {
  section: Section;
  state: Record<string, unknown>;
  onAction: (action: Action) => void;
  onStateChange: (key: string, value: unknown) => void;
  /** React key (passed by parent, not used in component) */
  key?: React.Key;
}

export interface SectionRouterProps extends SectionProps {
  /** Optional screen-level default refresh policy */
  defaultRefreshPolicy?: RefreshPolicy;
}

/**
 * Internal component that renders the appropriate section based on type.
 */
function SectionRenderer({ 
  section, 
  state, 
  onAction, 
  onStateChange 
}: SectionProps): React.ReactElement | null {
  const commonProps = { section, state, onAction, onStateChange };

  switch (section.type) {
    case 'ScoreboardHeader':
      return <ScoreboardHeader {...commonProps} />;
      
    case 'StatLine':
      return <StatLine {...commonProps} />;
      
    case 'TabGroup':
      return <TabGroup {...commonProps} />;
      
    case 'GamePanel':
      return <GamePanel {...commonProps} />;

    case 'Row':
      return <Row {...commonProps} />;

    case 'BoxscoreTable':
      return <BoxscoreTable {...commonProps} />;

    case 'Form':
      return <Form {...commonProps} />;

    case 'AdSlot':
      return <AdSlot {...commonProps} />;

    case 'SeasonLeadersTable':
      return <SeasonLeadersTable {...commonProps} />;

    case 'HeroPanel':
      return <HeroPanel {...commonProps} />;

    case 'VideoCarousel':
      return <VideoCarousel {...commonProps} />;

    case 'NbaTvSchedule':
      return <NbaTvSchedule {...commonProps} />;

    case 'SubscribeBanner':
      return <SubscribeBanner {...commonProps} />;

    case 'SubscribeHero':
      return <SubscribeHero {...commonProps} />;

    case 'AtomicComposite': {
      const compositeData = section.data as unknown as AtomicCompositeData | undefined;
      if (!compositeData?.ui) {
        console.debug(`[SectionRouter] AtomicComposite section ${section.id} has no ui element`);
        return null;
      }
      return <AtomicRouter element={compositeData.ui} state={state} onAction={onAction} />;
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
  defaultRefreshPolicy,
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
    (section.dataBindings?.bindings?.length) ||
    (defaultRefreshPolicy?.type && defaultRefreshPolicy.type !== 'static')
  );

  let content: React.ReactElement | null;

  if (needsLiveWrapper) {
    content = (
      <LiveSectionWrapper
        section={section}
        state={state}
        onAction={onAction}
        onStateChange={onStateChange}
        defaultRefreshPolicy={defaultRefreshPolicy}
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
      />
    );
  }

  return (
    <div ref={trackingRef}>
      <SectionErrorBoundary
        sectionStates={section.sectionStates}
        sectionId={section.id}
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
  defaultRefreshPolicy,
}: { 
  sections: Section[]; 
  state: Record<string, unknown>;
  onAction: (action: Action) => void;
  onStateChange: (key: string, value: unknown) => void;
  defaultRefreshPolicy?: RefreshPolicy;
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
            defaultRefreshPolicy={defaultRefreshPolicy}
          />
          {section.layoutHints?.dividerBelow && <hr className="sdui-divider" />}
        </div>
      ))}
    </>
  );
}
