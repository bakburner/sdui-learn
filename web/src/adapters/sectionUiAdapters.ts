import type {
  Action,
  BoxscoreColumnDefinition,
  DateMetadatum,
  FormField,
  PlayerRow,
  Section,
  SectionData,
  TabData,
} from '@sdui/models';

// ── CalendarStrip ──────────────────────────────────────────────────

export interface CalendarStripUiModel {
  stateKey: string;
  selectedDate: string;
  defaultDate: string;
  minDate: string | undefined;
  maxDate: string | undefined;
  expandedAction: Action | undefined;
  onDateSelected: Action;
}

export function mapCalendarStrip(section: Section): CalendarStripUiModel | null {
  const data = section.data as SectionData | undefined;
  if (!data) return null;

  const stateKey = data.stateKey as string | undefined;
  const selectedDate = data.selectedDate as string | undefined;
  const defaultDate = data.defaultDate as string | undefined;
  const expandedActionRaw = data.expandedAction as unknown;
  const onDateSelectedRaw = data.onDateSelected as unknown;

  if (!stateKey || !selectedDate || !defaultDate) return null;

  if (
    onDateSelectedRaw == null ||
    Array.isArray(onDateSelectedRaw) ||
    typeof onDateSelectedRaw !== 'object'
  ) {
    return null;
  }

  if (
    expandedActionRaw != null &&
    (Array.isArray(expandedActionRaw) || typeof expandedActionRaw !== 'object')
  ) {
    return null;
  }

  return {
    stateKey,
    selectedDate,
    defaultDate,
    minDate: data.minDate as string | undefined,
    maxDate: data.maxDate as string | undefined,
    expandedAction: expandedActionRaw as Action | undefined,
    onDateSelected: onDateSelectedRaw as Action,
  };
}

export interface CalendarMonthListUiModel {
  stateKey: string;
  selectedDate: string;
  defaultDate: string;
  minDate: string | undefined;
  maxDate: string | undefined;
  dateMetadata: Record<string, DateMetadatum>;
  onDateSelected: Action;
}

export function mapCalendarMonthList(section: Section): CalendarMonthListUiModel | null {
  const data = section.data as SectionData | undefined;
  if (!data) return null;

  const stateKey = data.stateKey as string | undefined;
  const selectedDate = data.selectedDate as string | undefined;
  const defaultDate = data.defaultDate as string | undefined;
  const onDateSelectedRaw = data.onDateSelected as unknown;
  const dateMetadataRaw = data.dateMetadata as unknown;

  if (!stateKey || !selectedDate || !defaultDate) return null;

  if (
    onDateSelectedRaw == null ||
    Array.isArray(onDateSelectedRaw) ||
    typeof onDateSelectedRaw !== 'object'
  ) {
    return null;
  }

  if (
    dateMetadataRaw != null &&
    (Array.isArray(dateMetadataRaw) || typeof dateMetadataRaw !== 'object')
  ) {
    return null;
  }

  return {
    stateKey,
    selectedDate,
    defaultDate,
    minDate: data.minDate as string | undefined,
    maxDate: data.maxDate as string | undefined,
    dateMetadata: (dateMetadataRaw as Record<string, DateMetadatum> | undefined) ?? {},
    onDateSelected: onDateSelectedRaw as Action,
  };
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

export function mapTabGroup(section: Section, state: Record<string, unknown>): TabGroupUiModel | null {
  const data = section.data as SectionData | undefined;
  if (!data?.tabs?.length) return null;

  const stateKey = data.stateKey;
  if (!stateKey) return null;

  const tabs = data.tabs as TabData[];
  const fallbackTabValue = tabs[0]?.stateValue ?? tabs[0]?.id;
  const activeValue =
    (state[stateKey] as string | undefined) ?? data.defaultTab ?? fallbackTabValue;
  if (!activeValue) return null;

  const uiTabs = tabs.map((tab) => {
    const stateValue = tab.stateValue ?? tab.id;
    return {
      id: tab.id,
      label: tab.label,
      stateValue,
      isActive: stateValue === activeValue,
    };
  });

  return {
    tabs: uiTabs,
    activeSections: data.tabContents?.[activeValue] ?? [],
    stateKey,
  };
}

// ── BoxscoreTable ──────────────────────────────────────────────────

export interface BoxscoreTableUiModel {
  teamTricode: string;
  teamName: string;
  teamColor?: string;
  teamLogoUrl?: string;
  /** Server-provided image to try on load error before client initials/placeholder. */
  fallbackThumbnailUrl?: string;
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
  const data = section.data as SectionData | undefined;
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
    fallbackThumbnailUrl: data.fallbackThumbnailUrl as string | undefined,
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
  const data = section.data as SectionData | undefined;
  if (!data?.fields) return null;
  return {
    fields: data.fields as FormField[],
    submitAction: data.submitAction as Action | undefined,
    submitLabel: data.submitLabel as string | undefined,
    layout: (data.layout as string) || 'vertical',
  };
}

