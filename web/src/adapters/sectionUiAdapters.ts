import type {
  Action,
  BoxscoreColumnDefinition,
  BoxscorePlayerRow,
  ContentCardData,
  Data,
  FormField,
  Section,
  StatLineData,
  TabData,
  TeamData,
} from '@sdui/models';

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

// ── BoxscoreTable ──────────────────────────────────────────────────

export interface BoxscoreTableUiModel {
  teamTricode: string;
  teamName: string;
  teamColor?: string;
  teamLogoUrl?: string;
  columns: BoxscoreColumnDefinition[];
  players: BoxscorePlayerRow[];
  teamTotals?: Record<string, unknown>;
  sortColumn?: string;
  sortDirection: 'asc' | 'desc';
  sortStateKey?: string;
  sortDirectionStateKey?: string;
  emptyMessage?: string;
}

export function mapBoxscoreTable(section: Section, state: Record<string, unknown>): BoxscoreTableUiModel | null {
  const data = section.data as Data | undefined;
  if (!data) return null;

  const sortStateKey = data.sortStateKey as string | undefined;
  const sortDirectionStateKey = data.sortDirectionStateKey as string | undefined;
  const sortColumn = sortStateKey ? (state[sortStateKey] as string | undefined) : undefined;
  const sortDirection = (sortDirectionStateKey ? (state[sortDirectionStateKey] as string) : 'desc') as 'asc' | 'desc';

  return {
    teamTricode: (data.teamTricode as string) || '',
    teamName: (data.teamName as string) || '',
    teamColor: data.teamColor as string | undefined,
    teamLogoUrl: data.teamLogoUrl as string | undefined,
    columns: (data.columns as BoxscoreColumnDefinition[]) || [],
    players: (data.players as BoxscorePlayerRow[]) || [],
    teamTotals: data.teamTotals as Record<string, unknown> | undefined,
    sortColumn,
    sortDirection,
    sortStateKey,
    sortDirectionStateKey,
    emptyMessage: data.emptyMessage as string | undefined,
  };
}

// ── Form ───────────────────────────────────────────────────────────

export interface FormUiModel {
  fields: FormField[];
  submitAction?: Action;
  submitLabel?: string;
  layout: string;
}

export function mapForm(section: Section, _state: Record<string, unknown>): FormUiModel | null {
  const data = section.data as Data | undefined;
  if (!data?.fields) return null;
  return {
    fields: data.fields as FormField[],
    submitAction: data.submitAction as Action | undefined,
    submitLabel: data.submitLabel as string | undefined,
    layout: (data.layout as string) || 'vertical',
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
