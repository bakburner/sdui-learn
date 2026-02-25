import type { Action, ContentCardData, Data, Section, StatLineData, TabData, TeamData } from '@sdui/models';

export interface ScoreboardHeaderUiModel {
  awayTeam?: TeamData;
  homeTeam?: TeamData;
  statusText: string;
  isLive: boolean;
  periodLabel?: string;
}

export interface StatLineUiModel {
  title?: string;
  stats: StatLineData[];
}

export interface ContentRailUiModel {
  title?: string;
  cards: ContentCardData[];
}

export interface TabGroupUiModel {
  tabs: Array<{
    id: string;
    label: string;
    stateValue: string;
    isActive: boolean;
  }>;
  activeSections: Section[];
  stateKey: string;
}

export interface PromoBannerUiModel {
  title?: string;
  headline?: string;
  subhead?: string;
  imageUrl?: string;
  backgroundImageUrl?: string;
  primaryAction?: Action;
}

export interface GameCardUiModel {
  awayTricode: string;
  homeTricode: string;
  awayScore: string;
  homeScore: string;
  awayLogoUrl?: string;
  homeLogoUrl?: string;
  statusText: string;
  primaryAction?: Action;
}

export function mapScoreboardHeader(section: Section): ScoreboardHeaderUiModel | null {
  const data = section.data as Data | undefined;
  if (!data) return null;
  const isLive = data.gameStatus === 2;
  return {
    awayTeam: data.awayTeam,
    homeTeam: data.homeTeam,
    statusText: data.gameStatusText || 'TBD',
    isLive,
    periodLabel: isLive && data.period && data.period > 0 ? `Q${data.period}` : undefined,
  };
}

export function mapStatLine(section: Section): StatLineUiModel | null {
  const data = section.data as Data | undefined;
  if (!data?.stats?.length) return null;
  return {
    title: data.title,
    stats: data.stats,
  };
}

export function mapContentRail(section: Section): ContentRailUiModel | null {
  const data = section.data as Data | undefined;
  if (!data?.cards?.length) return null;
  return {
    title: data.title,
    cards: data.cards,
  };
}

export function mapTabGroup(section: Section, state: Record<string, unknown>): TabGroupUiModel | null {
  const data = section.data as Data | undefined;
  if (!data?.tabs?.length) return null;

  const stateKey = data.stateKey || `${section.id}_activeTab`;
  const activeValue = (state[stateKey] as string) || data.defaultTab || data.tabs[0]?.id || '';

  const tabs = (data.tabs as TabData[]).map((tab) => {
    const value = tab.stateValue || tab.id;
    return {
      id: tab.id,
      label: tab.label,
      stateValue: value,
      isActive: tab.id === activeValue || value === activeValue,
    };
  });

  return {
    tabs,
    activeSections: data.tabContents?.[activeValue] || [],
    stateKey,
  };
}

export function mapPromoBanner(section: Section): PromoBannerUiModel | null {
  const data = section.data as Record<string, unknown> | undefined;
  if (!data) return null;
  const actions = data.actions as Action[] | undefined;
  const singleAction = data.action as Action | undefined;
  return {
    title: data.title as string | undefined,
    headline: data.headline as string | undefined,
    subhead: (data.subhead as string | undefined) || (data.description as string | undefined),
    imageUrl: data.imageUrl as string | undefined,
    backgroundImageUrl: data.backgroundImageUrl as string | undefined,
    primaryAction: actions?.[0] || singleAction,
  };
}

export function mapGameCard(section: Section): GameCardUiModel | null {
  const data = section.data as Data | undefined;
  if (!data?.homeTeam || !data?.awayTeam) return null;
  const raw = data as Record<string, unknown>;
  const actions = raw.actions as Action[] | undefined;
  const primaryAction = actions?.find((action) => action.type === 'navigate') ?? actions?.[0];
  const statusText =
    data.gameStatusText ||
    (data.gameStatus === 1 ? ((raw.gameTimeEt as string | undefined) || 'Pregame') : data.gameStatus === 2 ? 'Live' : 'Final');

  return {
    awayTricode: data.awayTeam.teamTricode,
    homeTricode: data.homeTeam.teamTricode,
    awayScore: String(data.awayTeam.score ?? '-'),
    homeScore: String(data.homeTeam.score ?? '-'),
    awayLogoUrl: data.awayTeam.logoUrl,
    homeLogoUrl: data.homeTeam.logoUrl,
    statusText,
    primaryAction,
  };
}
