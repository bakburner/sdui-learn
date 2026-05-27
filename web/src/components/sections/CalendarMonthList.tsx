import React, { useEffect, useMemo, useRef } from 'react';
import { Temporal } from '@js-temporal/polyfill';
import type { SectionProps } from '../SectionRouter';
import { mapCalendarMonthList } from '../../adapters/sectionUiAdapters';
import { accessibilityProps } from '../../utils/accessibility';
import { useColorTokenResolver, usePrefersColorScheme } from '../../utils/ColorTokenResolver';
import { currentFormFactor, resolveLayoutScalar } from '../../utils/LayoutTokenResolver';

const COLOR_LABEL_PRIMARY = 'token:nba.label.primary';
const COLOR_LABEL_SECONDARY = 'token:nba.label.secondary';
const COLOR_LABEL_INTERACTIVE = 'token:nba.label.interactive';
const COLOR_LABEL_SELECTION = 'token:nba.label.selection';
const COLOR_BG_SELECTION = 'token:nba.bg.selection';
const COLOR_DIVIDER_SUBTLE = 'token:nba.divider.subtle';
const COLOR_FILL_ACCENT = 'token:nba.fill.accent';
const SPACING_2XS = 'token:nba.spacing.2xs';
const SPACING_XS = 'token:nba.spacing.xs';
const SPACING_SM = 'token:nba.spacing.sm';
const SPACING_MD = 'token:nba.spacing.md';
const SPACING_LG = 'token:nba.spacing.lg';

function toJsDate(date: Temporal.PlainDate): Date {
  return new Date(Date.UTC(date.year, date.month - 1, date.day));
}

function plainDate(isoDate: string): Temporal.PlainDate {
  return Temporal.PlainDate.from(isoDate);
}

function firstDayOfWeek(): number {
  try {
    const locale = new Intl.Locale(navigator?.language ?? 'en-US');
    const weekInfo = (locale as { weekInfo?: { firstDay: number }; getWeekInfo?: () => { firstDay: number } }).weekInfo
      ?? (locale as { getWeekInfo?: () => { firstDay: number } }).getWeekInfo?.();
    if (weekInfo && typeof weekInfo.firstDay === 'number') {
      return weekInfo.firstDay;
    }
  } catch {
    // Fallback below for environments that do not support weekInfo.
  }
  return 7;
}

function dayColumn(date: Temporal.PlainDate, firstDay: number): number {
  return ((date.dayOfWeek - firstDay) % 7 + 7) % 7;
}

function monthStart(date: Temporal.PlainDate): Temporal.PlainDate {
  return date.with({ day: 1 });
}

function monthKey(date: Temporal.PlainDate): string {
  return `${date.year}-${String(date.month).padStart(2, '0')}`;
}

function monthLabel(date: Temporal.PlainDate, locale: string): string {
  return new Intl.DateTimeFormat(locale, { month: 'long', year: 'numeric' }).format(toJsDate(date));
}

function weekdayHeaders(firstDay: number, locale: string): string[] {
  const base = Temporal.PlainDate.from('2024-01-01');
  const headers: string[] = [];
  const formatter = new Intl.DateTimeFormat(locale, { weekday: 'narrow' });
  for (let i = 0; i < 7; i++) {
    const isoDay = ((firstDay - 1 + i) % 7) + 1;
    const offset = (isoDay - 1 + 7) % 7;
    headers.push(formatter.format(toJsDate(base.add({ days: offset }))));
  }
  return headers;
}

export function CalendarMonthList({
  section,
  state,
  onAction,
  onStateChange,
}: SectionProps): React.ReactElement | null {
  const model = mapCalendarMonthList(section);
  const formFactor = currentFormFactor();
  const scheme = usePrefersColorScheme();
  const resolveColor = useColorTokenResolver();
  const locale = typeof navigator !== 'undefined' ? navigator.language : 'en-US';
  const firstDay = useMemo(() => firstDayOfWeek(), []);
  const monthRefs = useRef<Map<string, HTMLDivElement>>(new Map());

  const tokenStyles = useMemo(() => {
    const sp2Xs = resolveLayoutScalar(SPACING_2XS, formFactor, scheme);
    const spXs = resolveLayoutScalar(SPACING_XS, formFactor, scheme);
    const spSm = resolveLayoutScalar(SPACING_SM, formFactor, scheme);
    const spMd = resolveLayoutScalar(SPACING_MD, formFactor, scheme);
    const spLg = resolveLayoutScalar(SPACING_LG, formFactor, scheme);
    return {
      sp2Xs,
      spXs,
      spSm,
      spMd,
      spLg,
      colorPrimary: resolveColor(COLOR_LABEL_PRIMARY) ?? 'var(--text-primary)',
      colorSecondary: resolveColor(COLOR_LABEL_SECONDARY) ?? 'var(--text-secondary)',
      colorInteractive: resolveColor(COLOR_LABEL_INTERACTIVE) ?? 'var(--nba-blue)',
      colorSelection: resolveColor(COLOR_LABEL_SELECTION) ?? '#ffffff',
      bgSelection: resolveColor(COLOR_BG_SELECTION) ?? 'var(--text-primary)',
      dividerSubtle: resolveColor(COLOR_DIVIDER_SUBTLE) ?? 'var(--divider)',
      fillAccent: resolveColor(COLOR_FILL_ACCENT) ?? 'var(--nba-blue)',
    };
  }, [resolveColor, formFactor, scheme]);

  const headers = useMemo(() => weekdayHeaders(firstDay, locale), [firstDay, locale]);

  const calendarWindow = useMemo(() => {
    if (!model) return null;
    try {
      const selected = plainDate(model.selectedDate);
      const min = monthStart(model.minDate ? plainDate(model.minDate) : selected);
      const max = monthStart(model.maxDate ? plainDate(model.maxDate) : selected);
      if (Temporal.PlainDate.compare(min, max) > 0) {
        return null;
      }
      return { min, max };
    } catch {
      return null;
    }
  }, [model]);

  const months = useMemo(() => {
    if (!calendarWindow) return [];
    const list: Temporal.PlainDate[] = [];
    let cursor = calendarWindow.min;
    while (Temporal.PlainDate.compare(cursor, calendarWindow.max) <= 0) {
      list.push(cursor);
      cursor = cursor.add({ months: 1 });
    }
    return list;
  }, [calendarWindow]);

  const selectedIso = model ? ((state[model.stateKey] as string | undefined) ?? model.selectedDate) : '';
  const defaultDateIso = model?.defaultDate ?? '';

  useEffect(() => {
    if (!model || months.length === 0) return;
    try {
      const selectedMonth = monthKey(monthStart(plainDate(selectedIso)));
      monthRefs.current.get(selectedMonth)?.scrollIntoView({ block: 'start', behavior: 'auto' });
    } catch {
      // Ignore invalid runtime state; model defaults still render.
    }
  }, [model, months, selectedIso]);

  if (!model || months.length === 0) {
    return null;
  }

  const handleDateClick = (isoDate: string): void => {
    onStateChange(model.stateKey, isoDate);
    onAction(model.onDateSelected);
  };

  const scrollToDefaultMonth = (): void => {
    const key = monthKey(monthStart(plainDate(model.defaultDate)));
    monthRefs.current.get(key)?.scrollIntoView({ block: 'start', behavior: 'smooth' });
  };

  return (
    <div style={styles.root} {...accessibilityProps(section.accessibility)}>
      <div
        style={{
          ...styles.scrollContainer,
          padding: tokenStyles.spMd,
          gap: tokenStyles.spLg,
        }}
      >
        <div
          style={{
            ...styles.weekdayHeader,
            top: 0,
            paddingBottom: tokenStyles.spSm,
            borderBottom: `1px solid ${tokenStyles.dividerSubtle}`,
            backgroundColor: 'var(--surface)',
          }}
        >
          {headers.map((header, index) => (
            <div key={index} style={{ ...styles.weekdayCell, color: tokenStyles.colorSecondary }}>
              {header}
            </div>
          ))}
        </div>

        {months.map((monthDate) => {
          const key = monthKey(monthDate);
          const firstOfMonth = monthDate;
          const dayCount = firstOfMonth.daysInMonth;
          const leadingBlanks = dayColumn(firstOfMonth, firstDay);
          const cells = Array.from({ length: leadingBlanks + dayCount }, (_, idx) => idx - leadingBlanks + 1);

          return (
            <section
              key={key}
              ref={(el) => {
                if (el) {
                  monthRefs.current.set(key, el);
                } else {
                  monthRefs.current.delete(key);
                }
              }}
              style={styles.monthSection}
            >
              <h3
                style={{
                  ...styles.monthHeader,
                  top: 34,
                  padding: `${tokenStyles.spSm}px 0`,
                  color: tokenStyles.colorPrimary,
                  backgroundColor: 'var(--surface)',
                }}
              >
                {monthLabel(firstOfMonth, locale)}
              </h3>
              <div
                style={{
                  ...styles.monthGrid,
                  gap: tokenStyles.spXs,
                }}
              >
                {cells.map((day) => {
                  if (day < 1) {
                    return <div key={`blank-${key}-${day}`} />;
                  }

                  const date = firstOfMonth.with({ day });
                  const isoDate = date.toString();
                  const metadata = model.dateMetadata[isoDate];
                  const hasGames = (metadata?.gameCount ?? 0) > 0;
                  const hasTeamGame = Boolean(metadata?.hasTeamGame);
                  const isSelected = isoDate === selectedIso;
                  const isToday = isoDate === defaultDateIso;

                  return (
                    <button
                      key={isoDate}
                      type="button"
                      aria-pressed={isSelected}
                      onClick={() => handleDateClick(isoDate)}
                      style={{
                        ...styles.dayCell,
                        padding: tokenStyles.sp2Xs,
                        borderColor: isToday && !isSelected ? tokenStyles.colorInteractive : 'transparent',
                        backgroundColor: isSelected ? tokenStyles.bgSelection : 'transparent',
                        color: isSelected ? tokenStyles.colorSelection : tokenStyles.colorPrimary,
                      }}
                    >
                      <span>{day}</span>
                      {hasGames ? (
                        <span
                          style={{
                            ...styles.dot,
                            marginTop: tokenStyles.sp2Xs,
                            backgroundColor: hasTeamGame ? tokenStyles.fillAccent : tokenStyles.colorSecondary,
                          }}
                        />
                      ) : (
                        <span style={styles.dotSpacer} />
                      )}
                    </button>
                  );
                })}
              </div>
            </section>
          );
        })}
      </div>

      <button
        type="button"
        onClick={scrollToDefaultMonth}
        style={{
          ...styles.todayButton,
          right: tokenStyles.spMd,
          bottom: tokenStyles.spMd,
          padding: `${tokenStyles.spXs}px ${tokenStyles.spSm}px`,
          borderColor: tokenStyles.dividerSubtle,
          color: tokenStyles.colorPrimary,
          backgroundColor: 'var(--surface)',
        }}
      >
        Today
      </button>
    </div>
  );
}

const styles: Record<string, React.CSSProperties> = {
  root: {
    position: 'relative',
    width: '100%',
    maxHeight: '75vh',
  },
  scrollContainer: {
    display: 'flex',
    flexDirection: 'column',
    overflowY: 'auto',
    maxHeight: '75vh',
  },
  weekdayHeader: {
    position: 'sticky',
    zIndex: 4,
    display: 'grid',
    gridTemplateColumns: 'repeat(7, minmax(0, 1fr))',
    textAlign: 'center',
  },
  weekdayCell: {
    fontSize: 11,
    fontWeight: 600,
    textTransform: 'uppercase',
    userSelect: 'none',
  },
  monthSection: {
    display: 'flex',
    flexDirection: 'column',
  },
  monthHeader: {
    position: 'sticky',
    zIndex: 3,
    margin: 0,
    fontSize: 16,
    fontWeight: 700,
  },
  monthGrid: {
    display: 'grid',
    gridTemplateColumns: 'repeat(7, minmax(0, 1fr))',
  },
  dayCell: {
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    justifyContent: 'center',
    minHeight: 40,
    borderRadius: 999,
    borderWidth: 1,
    borderStyle: 'solid',
    background: 'none',
    cursor: 'pointer',
    fontFamily: 'inherit',
    fontWeight: 600,
  },
  dot: {
    width: 6,
    height: 6,
    borderRadius: 999,
  },
  dotSpacer: {
    width: 6,
    height: 6,
    marginTop: 6,
  },
  todayButton: {
    position: 'absolute',
    borderWidth: 1,
    borderStyle: 'solid',
    borderRadius: 999,
    fontWeight: 600,
    cursor: 'pointer',
  },
};
