import React from 'react';
import type { Section, Action, Data, RefreshPolicy } from '@sdui/models';
import { ScoreboardHeader } from './sections/ScoreboardHeader';
import { StatLine } from './sections/StatLine';
import { HeroPanel } from './sections/HeroPanel';
import { ContentRail } from './sections/ContentRail';
import { TabGroup } from './sections/TabGroup';
import { PromoBanner } from './sections/PromoBanner';
import { GamePanel } from './sections/GamePanel';
import { Row } from './sections/Row';
import { BoxscoreTable } from './sections/BoxscoreTable';
import { Form } from './sections/Form';
import { AdSlot } from './sections/AdSlot';
import { SeasonLeadersTable } from './sections/SeasonLeadersTable';
import { FollowingRail } from './sections/FollowingRail';
import { FeaturedGamePanel } from './sections/FeaturedGamePanel';
import { SectionHeader } from './sections/SectionHeader';
import { VideoCarousel } from './sections/VideoCarousel';
import { NbaTvSchedule } from './sections/NbaTvSchedule';
import { SubscribeBanner } from './sections/SubscribeBanner';
import { SubscribeHero } from './sections/SubscribeHero';
import { ErrorState } from './sections/ErrorState';
import { LiveSectionWrapper } from './LiveSectionWrapper';

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
      
    case 'ContentRail':
      return <ContentRail {...commonProps} />;
      
    case 'TabGroup':
      return <TabGroup {...commonProps} />;
      
    case 'PromoBanner':
      return <PromoBanner {...commonProps} />;

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

    case 'FollowingRail':
      return <FollowingRail {...commonProps} />;

    case 'FeaturedGamePanel':
      return <FeaturedGamePanel {...commonProps} />;

    case 'SectionHeader':
      return <SectionHeader {...commonProps} />;
      
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

    case 'ErrorState':
      return <ErrorState {...commonProps} />;
      
    default:
      // Ignore unknown section types (forward compatibility)
      console.debug(`[SectionRouter] Ignoring unknown section type: ${section.type}`);
      return null;
  }
}

/**
 * Routes sections to their appropriate renderers based on type.
 * Wraps sections in LiveSectionWrapper to handle refresh policies and data bindings.
 */
export function SectionRouter({ 
  section, 
  state, 
  onAction, 
  onStateChange,
  defaultRefreshPolicy,
}: SectionRouterProps): React.ReactElement | null {
  // Check if this section needs live updates
  const needsLiveWrapper = Boolean(
    section.refreshPolicy?.type && section.refreshPolicy.type !== 'static' ||
    section.dataBindings?.bindings?.length ||
    defaultRefreshPolicy?.type && defaultRefreshPolicy.type !== 'static'
  );

  if (needsLiveWrapper) {
    return (
      <LiveSectionWrapper
        section={section}
        state={state}
        onAction={onAction}
        onStateChange={onStateChange}
        defaultRefreshPolicy={defaultRefreshPolicy}
      >
        {(liveData: Data | undefined) => {
          // Create a section with live data substituted
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
  }

  // No live updates needed, render directly
  return (
    <SectionRenderer
      section={section}
      state={state}
      onAction={onAction}
      onStateChange={onStateChange}
    />
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
        <SectionRouter
          key={section.id}
          section={section}
          state={state}
          onAction={onAction}
          onStateChange={onStateChange}
          defaultRefreshPolicy={defaultRefreshPolicy}
        />
      ))}
    </>
  );
}
