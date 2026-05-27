import React, { useMemo, useRef, useEffect, useCallback } from 'react';
import { Temporal } from '@js-temporal/polyfill';
import type { SectionProps } from '../SectionRouter';
import { mapCalendarStrip } from '../../adapters/sectionUiAdapters';
import { accessibilityProps } from '../../utils/accessibility';
import { useColorTokenResolver, usePrefersColorScheme } from '../../utils/ColorTokenResolver';
import { currentFormFactor, resolveLayoutScalar } from '../../utils/LayoutTokenResolver';

// ── Token references (§3.6 — no hex literals, no raw px for tokenizable values) ──

const COLOR_LABEL_PRIMARY = 'token:nba.label.primary';
const COLOR_LABEL_SECONDARY = 'token:nba.label.secondary';
const COLOR_LABEL_INTERACTIVE = 'token:nba.label.interactive';
const COLOR_LABEL_SELECTION = 'token:nba.label.selection';
const COLOR_BG_SELECTION = 'token:nba.bg.selection';
const COLOR_DIVIDER_SUBTLE = 'token:nba.divider.subtle';
const SPACING_XS = 'token:nba.spacing.xs';
const SPACING_SM = 'token:nba.spacing.sm';
const SPACING_MD = 'token:nba.spacing.md';

// ── Temporal helpers ──

function plainDate(iso: string): Temporal.PlainDate {
  return Temporal.PlainDate.from(iso);
}

function isoString(date: Temporal.PlainDate): string {
  return date.toString();
}

/**
 * Locale-aware first day of week (1=Mon … 7=Sun per Intl convention).
 * Falls back to Sunday (7) when `Intl.Locale.prototype.weekInfo` is unavailable.
 */
function firstDayOfWeek(): number {
  try {
    const locale = new Intl.Locale(navigator?.language ?? 'en-US');
    const weekInfo = (locale as { weekInfo?: { firstDay: number }; getWeekInfo?: () => { firstDay: number } }).weekInfo
      ?? (locale as { getWeekInfo?: () => { firstDay: number } }).getWeekInfo?.();
    if (weekInfo && typeof weekInfo.firstDay === 'number') {
      return weekInfo.firstDay;
    }
  } catch {
    // Safari < 17.4, Firefox < 120 — fall through to default
  }
  return 7; // Sunday
}

/**
 * Temporal.PlainDate.dayOfWeek is 1=Mon … 7=Sun (ISO 8601).
 * Shift so the configured first-day-of-week maps to column 0.
 */
// @internal exported for unit tests
export function dayColumn(date: Temporal.PlainDate, firstDay: number): number {
  return ((date.dayOfWeek - firstDay) % 7 + 7) % 7;
}

// @internal exported for unit tests
export function startOfWeek(date: Temporal.PlainDate, firstDay: number): Temporal.PlainDate {
  const col = dayColumn(date, firstDay);
  return date.subtract({ days: col });
}

export interface WeekGroup {
  weekStart: Temporal.PlainDate;
  days: Temporal.PlainDate[];
}

// @internal exported for unit tests
export function generateWeeks(
  minDate: Temporal.PlainDate,
  maxDate: Temporal.PlainDate,
  firstDay: number,
): WeekGroup[] {
  const weeks: WeekGroup[] = [];
  let cursor = startOfWeek(minDate, firstDay);

  while (Temporal.PlainDate.compare(cursor, maxDate) <= 0) {
    const days: Temporal.PlainDate[] = [];
    for (let d = 0; d < 7; d++) {
      const day = cursor.add({ days: d });
      if (
        Temporal.PlainDate.compare(day, minDate) >= 0 &&
        Temporal.PlainDate.compare(day, maxDate) <= 0
      ) {
        days.push(day);
      }
    }
    if (days.length > 0) {
      weeks.push({ weekStart: cursor, days });
    }
    cursor = cursor.add({ days: 7 });
  }

  return weeks;
}

// ── Display formatters ──

function weekdayHeaders(firstDay: number, locale: string): string[] {
  const base = Temporal.PlainDate.from('2024-01-01'); // Monday
  const headers: string[] = [];
  const fmt = new Intl.DateTimeFormat(locale, { weekday: 'narrow' });
  for (let i = 0; i < 7; i++) {
    const isoDay = ((firstDay - 1 + i) % 7) + 1; // 1-based ISO day
    const offset = (isoDay - 1 + 7) % 7; // days after Monday
    const date = base.add({ days: offset });
    headers.push(fmt.format(new Date(date.year, date.month - 1, date.day)));
  }
  return headers;
}

function monthYearLabel(date: Temporal.PlainDate, locale: string): string {
  const fmt = new Intl.DateTimeFormat(locale, { month: 'long', year: 'numeric' });
  return fmt.format(new Date(date.year, date.month - 1, date.day));
}

// ── Cell state ──

type CellState = 'default' | 'selected' | 'defaultDate' | 'defaultDateSelected';

function cellState(
  dayIso: string,
  selectedIso: string,
  defaultDateIso: string,
): CellState {
  const isSelected = dayIso === selectedIso;
  const isDefault = dayIso === defaultDateIso;
  if (isSelected && isDefault) return 'defaultDateSelected';
  if (isSelected) return 'selected';
  if (isDefault) return 'defaultDate';
  return 'default';
}

// ── Component ──

export function CalendarStrip({
  section,
  state,
  onAction,
  onStateChange,
}: SectionProps): React.ReactElement | null {
  const model = mapCalendarStrip(section);
  const scheme = usePrefersColorScheme();
  const resolveColor = useColorTokenResolver();
  const formFactor = currentFormFactor();
  const scrollRef = useRef<HTMLDivElement>(null);
  const weekRefs = useRef<Map<string, HTMLDivElement>>(new Map());
  const monthLabelRef = useRef<HTMLElement>(null);
  const locale = typeof navigator !== 'undefined' ? navigator.language : 'en-US';

  const firstDay = useMemo(() => firstDayOfWeek(), []);

  const tokenStyles = useMemo(() => {
    const spXs = resolveLayoutScalar(SPACING_XS, formFactor, scheme);
    const spSm = resolveLayoutScalar(SPACING_SM, formFactor, scheme);
    const spMd = resolveLayoutScalar(SPACING_MD, formFactor, scheme);

    return {
      spXs,
      spSm,
      spMd,
      colorPrimary: resolveColor(COLOR_LABEL_PRIMARY) ?? 'var(--text-primary)',
      colorSecondary: resolveColor(COLOR_LABEL_SECONDARY) ?? 'var(--text-secondary)',
      colorInteractive: resolveColor(COLOR_LABEL_INTERACTIVE) ?? 'var(--nba-blue)',
      colorSelection: resolveColor(COLOR_LABEL_SELECTION) ?? '#fff',
      bgSelection: resolveColor(COLOR_BG_SELECTION) ?? 'var(--text-primary)',
      dividerSubtle: resolveColor(COLOR_DIVIDER_SUBTLE) ?? 'var(--divider)',
    };
  }, [resolveColor, formFactor, scheme]);

  const headers = useMemo(() => weekdayHeaders(firstDay, locale), [firstDay, locale]);

  const { weeks, defaultWindow } = useMemo(() => {
    if (!model) return { weeks: [], defaultWindow: { minDate: '2026-01-01', maxDate: '2026-12-31' } };
    const fallbackMin = plainDate(model.selectedDate).subtract({ months: 3 });
    const fallbackMax = plainDate(model.selectedDate).add({ months: 3 });
    const min = model.minDate ? plainDate(model.minDate) : fallbackMin;
    const max = model.maxDate ? plainDate(model.maxDate) : fallbackMax;
    return {
      weeks: generateWeeks(min, max, firstDay),
      defaultWindow: { minDate: isoString(min), maxDate: isoString(max) },
    };
  }, [model?.minDate, model?.maxDate, model?.selectedDate, firstDay]);

  const selectedIso = model
    ? ((state[model.stateKey] as string | undefined) ?? model.selectedDate)
    : '';
  const defaultDateIso = model?.defaultDate ?? '';

  // Scroll to the selected date's week on mount & when selectedIso changes
  useEffect(() => {
    if (!model || weeks.length === 0) return;
    const targetDate = plainDate(selectedIso);
    const targetWeekStart = startOfWeek(targetDate, firstDay);
    const key = isoString(targetWeekStart);
    const weekEl = weekRefs.current.get(key);
    if (weekEl && scrollRef.current) {
      weekEl.scrollIntoView({ inline: 'center', block: 'nearest', behavior: 'auto' });
    }
  }, [selectedIso, weeks.length]);

  // IntersectionObserver → update month/year label for the centered week
  useEffect(() => {
    const scrollEl = scrollRef.current;
    if (!scrollEl || weeks.length === 0) return;

    const observer = new IntersectionObserver(
      (entries) => {
        let best: { ratio: number; weekStartIso: string } | null = null;
        for (const entry of entries) {
          const iso = (entry.target as HTMLElement).dataset.weekstart;
          if (iso && entry.intersectionRatio > (best?.ratio ?? 0)) {
            best = { ratio: entry.intersectionRatio, weekStartIso: iso };
          }
        }
        if (best && monthLabelRef.current) {
          const ws = plainDate(best.weekStartIso);
          const mid = ws.add({ days: 3 });
          monthLabelRef.current.textContent = monthYearLabel(mid, locale);
        }
      },
      { root: scrollEl, threshold: [0, 0.25, 0.5, 0.75, 1.0] },
    );

    weekRefs.current.forEach((el) => observer.observe(el));

    return () => observer.disconnect();
  }, [weeks, locale]);

  const handleDateClick = useCallback(
    (dayIso: string) => {
      if (!model) return;
      onStateChange(model.stateKey, dayIso);
      onAction(model.onDateSelected);
    },
    [model, onStateChange, onAction],
  );

  const setWeekRef = useCallback((key: string, el: HTMLDivElement | null) => {
    if (el) {
      weekRefs.current.set(key, el);
    } else {
      weekRefs.current.delete(key);
    }
  }, []);

  if (!model) {
    return null;
  }

  const initialMonthLabel = monthYearLabel(plainDate(selectedIso), locale);
  const monthLabelClickable = Boolean(model.expandedAction);
  const handleMonthLabelClick = (): void => {
    if (model.expandedAction) {
      onAction(model.expandedAction);
    }
  };

  const cellSize = 40;
  const cellGap = tokenStyles.spXs;

  return (
    <div
      style={styles.root}
      {...accessibilityProps(section.accessibility)}
    >
      {monthLabelClickable ? (
        <button
          ref={(el) => {
            monthLabelRef.current = el;
          }}
          type="button"
          onClick={handleMonthLabelClick}
          style={{
            ...styles.monthLabel,
            ...styles.monthLabelButton,
            padding: `${tokenStyles.spSm}px ${tokenStyles.spMd}px`,
            color: tokenStyles.colorPrimary,
          }}
        >
          {initialMonthLabel}
        </button>
      ) : (
        <div
          ref={(el) => {
            monthLabelRef.current = el;
          }}
          style={{
            ...styles.monthLabel,
            padding: `${tokenStyles.spSm}px ${tokenStyles.spMd}px`,
            color: tokenStyles.colorPrimary,
          }}
        >
          {initialMonthLabel}
        </div>
      )}

      {/* Weekday headers */}
      <div
        style={{
          ...styles.weekdayRow,
          gap: cellGap,
          padding: `0 ${tokenStyles.spMd}px`,
        }}
      >
        {headers.map((h, i) => (
          <div
            key={i}
            style={{
              ...styles.weekdayCell,
              width: cellSize,
              color: tokenStyles.colorSecondary,
            }}
          >
            {h}
          </div>
        ))}
      </div>

      {/* Horizontal scroll row of date cells */}
      <div
        ref={scrollRef}
        style={{
          ...styles.scrollContainer,
          gap: tokenStyles.spMd,
          padding: `${tokenStyles.spSm}px ${tokenStyles.spMd}px`,
        }}
      >
        {weeks.map((week) => {
          const weekKey = isoString(week.weekStart);
          return (
            <div
              key={weekKey}
              ref={(el) => setWeekRef(weekKey, el)}
              data-weekstart={weekKey}
              style={{
                ...styles.weekGroup,
                gap: cellGap,
              }}
            >
              {week.days.map((day) => {
                const dayIso = isoString(day);
                const cs = cellState(dayIso, selectedIso, defaultDateIso);
                const isSelected = cs === 'selected' || cs === 'defaultDateSelected';
                const isDefault = cs === 'defaultDate' || cs === 'defaultDateSelected';

                const bgColor = isSelected
                  ? (isDefault ? tokenStyles.colorInteractive : tokenStyles.bgSelection)
                  : 'transparent';
                const textColor = isSelected
                  ? tokenStyles.colorSelection
                  : (isDefault ? tokenStyles.colorInteractive : tokenStyles.colorPrimary);
                const borderColor = isDefault && !isSelected
                  ? tokenStyles.colorInteractive
                  : 'transparent';

                return (
                  <button
                    key={dayIso}
                    type="button"
                    aria-pressed={isSelected}
                    onClick={() => handleDateClick(dayIso)}
                    style={{
                      ...styles.dateCell,
                      width: cellSize,
                      height: cellSize,
                      backgroundColor: bgColor,
                      color: textColor,
                      borderColor,
                      borderWidth: isDefault && !isSelected ? 2 : 0,
                      borderStyle: 'solid',
                      borderRadius: cellSize / 2,
                    }}
                  >
                    {day.day}
                  </button>
                );
              })}
            </div>
          );
        })}
      </div>
    </div>
  );
}

// ── Static styles (no tokenizable values hardcoded) ──

const styles: Record<string, React.CSSProperties> = {
  root: {
    width: '100%',
    overflow: 'hidden',
  },
  monthLabel: {
    fontWeight: 700,
    fontSize: 'inherit',
    cursor: 'default',
    userSelect: 'none',
  },
  monthLabelButton: {
    background: 'none',
    border: 0,
    textAlign: 'left',
    cursor: 'pointer',
    fontFamily: 'inherit',
  },
  weekdayRow: {
    display: 'flex',
    justifyContent: 'center',
  },
  weekdayCell: {
    textAlign: 'center',
    fontSize: 11,
    fontWeight: 600,
    textTransform: 'uppercase' as const,
    userSelect: 'none',
    flexShrink: 0,
  },
  scrollContainer: {
    display: 'flex',
    overflowX: 'auto',
    scrollSnapType: 'x mandatory' as React.CSSProperties['scrollSnapType'],
    scrollbarWidth: 'none',
    WebkitOverflowScrolling: 'touch',
  },
  weekGroup: {
    display: 'flex',
    flexShrink: 0,
    scrollSnapAlign: 'center',
  },
  dateCell: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    padding: 0,
    background: 'none',
    fontFamily: 'inherit',
    fontWeight: 600,
    fontSize: 14,
    cursor: 'pointer',
    flexShrink: 0,
    transition: 'background-color 0.15s ease, color 0.15s ease',
    outline: 'none',
  },
};
