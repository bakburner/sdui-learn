import type {
  Action,
  BoxscoreColumnDefinition,
  Data,
  FormField,
  PlayerRow,
  Section,
  TabData,
} from '@sdui/models';

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

export interface GamePanelDisplayConfig {
  logoSize: number;
  cardHeight?: number;
  cornerRadius: number;
  elevation: number;
  scoreTextStyle: 'compact' | 'prominent';
  background?: any;
  liveBackground?: any;
  badgeColor?: string;
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
  displayConfig: GamePanelDisplayConfig;
  badgeText?: string;
  visualLabel?: string;
  visualState?: string;
  variant?: string;
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
  players: PlayerRow[];
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
    players: (data.players as PlayerRow[]) || [],
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
    displayConfig: parseDisplayConfig(raw.displayConfig),
    badgeText: raw.badgeText as string | undefined,
    visualLabel: raw.visualLabel as string | undefined,
    visualState,
    variant: raw.variant as string | undefined,
  };
}

function parseDisplayConfig(raw: any): GamePanelDisplayConfig {
  if (!raw || typeof raw !== 'object') {
    return { logoSize: 32, cornerRadius: 12, elevation: 0, scoreTextStyle: 'compact' };
  }
  return {
    logoSize: raw.logoSize ?? 32,
    cardHeight: raw.cardHeight ?? undefined,
    cornerRadius: raw.cornerRadius ?? 12,
    elevation: raw.elevation ?? 0,
    scoreTextStyle: raw.scoreTextStyle ?? 'compact',
    background: raw.background,
    liveBackground: raw.liveBackground,
    badgeColor: raw.badgeColor,
  };
}
