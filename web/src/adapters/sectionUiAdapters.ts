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

