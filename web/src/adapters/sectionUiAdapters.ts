import type {
  Action,
  BoxscoreColumnDefinition,
  BoxscorePlayerRow,
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

export interface GamePanelUiModel {
  awayTricode: string;
  homeTricode: string;
  awayScore: string;
  homeScore: string;
  awayLogoUrl?: string;
  homeLogoUrl?: string;
  awayName?: string;
  homeName?: string;
  awayRecord?: string;
  homeRecord?: string;
  statusText: string;
  broadcaster?: string;
  gameDateEt?: string;
  primaryAction?: Action;
  variant: string;
  badgeText?: string;
  visualLabel?: string;
  backgroundImageUrl?: string;
  visualState?: string;
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

export interface HeroPanelUiModel {
  id: string;
  headline: string;
  subhead?: string;
  thumbnailUrl?: string;
  fallbackThumbnailUrl?: string;
  contentType?: string;
  duration?: string;
  action?: Action;
}

export function mapHeroPanel(section: Section): HeroPanelUiModel | null {
  const data = section.data as Record<string, unknown> | undefined;
  if (!data?.headline) return null;
  return {
    id: (data.id as string) ?? section.id,
    headline: data.headline as string,
    subhead: data.subhead as string | undefined,
    thumbnailUrl: data.thumbnailUrl as string | undefined,
    fallbackThumbnailUrl: data.fallbackThumbnailUrl as string | undefined,
    contentType: data.contentType as string | undefined,
    duration: data.duration as string | undefined,
    action: data.action as Action | undefined,
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

// ── GamePanel ──────────────────────────────────────────────────────

export function mapGamePanel(section: Section): GamePanelUiModel | null {
  const data = section.data as Data | undefined;
  if (!data?.homeTeam || !data?.awayTeam) return null;
  const raw = data as Record<string, unknown>;
  const actions = raw.actions as Action[] | undefined;
  const primaryAction = actions?.find((action) => action.type === 'navigate') ?? actions?.[0];
  const statusText =
    data.gameStatusText ||
    (data.gameStatus === 1 ? ((raw.gameTimeEt as string | undefined) || 'Pregame') : data.gameStatus === 2 ? 'Live' : 'Final');

  const gameStatus = data.gameStatus;
  const visualState = gameStatus === 2 ? 'LIVE' : gameStatus === 1 ? 'PRE' : 'FINAL';

  return {
    awayTricode: data.awayTeam.teamTricode,
    homeTricode: data.homeTeam.teamTricode,
    awayScore: String(data.awayTeam.score ?? '-'),
    homeScore: String(data.homeTeam.score ?? '-'),
    awayLogoUrl: data.awayTeam.logoUrl,
    homeLogoUrl: data.homeTeam.logoUrl,
    awayName: data.awayTeam.teamName || data.awayTeam.teamCity,
    homeName: data.homeTeam.teamName || data.homeTeam.teamCity,
    awayRecord: (raw.awayTeam as Record<string, unknown>)?.record as string | undefined,
    homeRecord: (raw.homeTeam as Record<string, unknown>)?.record as string | undefined,
    statusText,
    broadcaster: raw.broadcaster as string | undefined,
    gameDateEt: raw.gameDateEt as string | undefined,
    primaryAction,
    variant: (raw.variant as string | undefined) || 'standard',
    badgeText: raw.badgeText as string | undefined,
    visualLabel: raw.visualLabel as string | undefined,
    backgroundImageUrl: raw.backgroundImageUrl as string | undefined,
    visualState,
  };
}
