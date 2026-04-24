import React, { useContext, useEffect, useRef, useState } from 'react';
import type { AtomicProps } from './AtomicRouter';
import { AtomicBox } from './AtomicBox';
import { accessibilityProps } from '../../utils/accessibility';
import { useColorTokenResolver } from '../../utils/ColorTokenResolver';
import { CompositeContentContext, resolveBindRefObject } from '../../utils/BindRefResolver';

const clockVariantStyles: Record<string, React.CSSProperties> = {
  displayLarge:   { fontSize: 57, fontWeight: 800, lineHeight: '0.85em', fontFamily: 'var(--font-headline)' },
  displayMedium:  { fontSize: 45, fontWeight: 800, lineHeight: '0.85em', fontFamily: 'var(--font-headline)' },
  displaySmall:   { fontSize: 36, fontWeight: 700, lineHeight: '0.9em',  fontFamily: 'var(--font-headline)' },
  headlineLarge:  { fontSize: 32, fontWeight: 700, lineHeight: '1.1em',  fontFamily: 'var(--font-headline)' },
  headlineMedium: { fontSize: 28, fontWeight: 700, lineHeight: '1.1em',  fontFamily: 'var(--font-headline)' },
  headlineSmall:  { fontSize: 24, fontWeight: 700, lineHeight: '1.15em', fontFamily: 'var(--font-headline)' },
  titleLarge:     { fontSize: 22, fontWeight: 500, lineHeight: '28px',   fontFamily: 'var(--font-body)' },
  titleMedium:    { fontSize: 16, fontWeight: 500, lineHeight: '24px',   fontFamily: 'var(--font-body)' },
  titleSmall:     { fontSize: 14, fontWeight: 500, lineHeight: '20px',   fontFamily: 'var(--font-body)' },
  bodyLarge:      { fontSize: 16, fontWeight: 400, lineHeight: '24px',   fontFamily: 'var(--font-body)' },
  bodyMedium:     { fontSize: 14, fontWeight: 400, lineHeight: '20px',   fontFamily: 'var(--font-body)' },
  bodySmall:      { fontSize: 12, fontWeight: 400, lineHeight: '16px',   fontFamily: 'var(--font-body)' },
  labelLarge:     { fontSize: 14, fontWeight: 500, lineHeight: '20px',   fontFamily: 'var(--font-body)' },
  labelMedium:    { fontSize: 12, fontWeight: 500, lineHeight: '16px',   fontFamily: 'var(--font-body)' },
  labelSmall:     { fontSize: 11, fontWeight: 500, lineHeight: '16px',   fontFamily: 'var(--font-body)' },
  score:          { fontSize: 28, fontWeight: 800, lineHeight: '1em',    fontFamily: 'var(--font-headline)' },
};

function formatSeconds(total: number, format: string): string {
  const clamped = Math.max(0, Math.floor(total));
  const h = Math.floor(clamped / 3600);
  const m = Math.floor((clamped % 3600) / 60);
  const s = clamped % 60;
  const pad2 = (n: number) => n.toString().padStart(2, '0');
  switch (format) {
    case 'h:mm:ss': return `${h}:${pad2(m)}:${s.toString().padStart(2, '0')}`;
    case 'mm:ss':   return `${pad2(m)}:${pad2(s)}`;
    case 'm:ss':
    default:        return `${m}:${pad2(s)}`;
  }
}

/**
 * AtomicLiveClock — renders a ticking clock whose anchor is the server
 * snapshot (`snapshotSeconds` at `snapshotAt`, plus `isRunning`). When
 * the clock is running, `requestAnimationFrame` drives a local
 * interpolation loop; when the document is hidden the RAF loop is
 * cancelled so a backgrounded tab stops doing visible work.
 *
 * Typography and accessibility flow through the same box-model and
 * variant resolution used by `AtomicText` — the clock is a text-shaped
 * leaf from a layout perspective.
 */
export function AtomicLiveClock({ element }: AtomicProps): React.ReactElement {
  const resolveColor = useColorTokenResolver();
  // Resolve the `(snapshotSeconds, snapshotAt, isRunning)` tuple. When
  // `bindRef` is set it points at an object inside the enclosing
  // composite's `data.content` with those three keys — that lets the
  // server push a single `{clock: {...}}` snapshot on every tick
  // instead of threading three independent binding paths.
  const compositeContent = useContext(CompositeContentContext);
  const bound = resolveBindRefObject(element.bindRef, compositeContent);
  const snapshotSeconds = typeof bound?.snapshotSeconds === 'number'
    ? bound.snapshotSeconds
    : (element.snapshotSeconds ?? 0);
  // `snapshotAt` arrives as a `Date` on the typed element and as a
  // raw string when pulled from the untyped `data.content` blob via
  // bindRef. Normalize both into a parsed epoch-ms.
  const snapshotAtRaw = (typeof bound?.snapshotAt === 'string' ? bound.snapshotAt : undefined)
    ?? (element.snapshotAt instanceof Date ? element.snapshotAt.toISOString() : element.snapshotAt as unknown as string | undefined);
  const snapshotAtMs = snapshotAtRaw ? Date.parse(snapshotAtRaw) : Number.NaN;
  const isRunning = typeof bound?.isRunning === 'boolean'
    ? bound.isRunning
    : element.isRunning === true;
  const directionDown = (element.tickDirection ?? 'down') === 'down';
  const stopAt = element.stopAtSeconds;
  const format = element.format ?? 'm:ss';

  const [, setFrame] = useState(0);
  const rafRef = useRef<number | null>(null);

  useEffect(() => {
    if (!isRunning) return;
    let cancelled = false;
    const tick = () => {
      if (cancelled) return;
      setFrame((f) => (f + 1) % 1_000_000);
      rafRef.current = window.requestAnimationFrame(tick);
    };
    const onVisibility = () => {
      if (document.hidden) {
        if (rafRef.current != null) window.cancelAnimationFrame(rafRef.current);
        rafRef.current = null;
      } else if (rafRef.current == null) {
        tick();
      }
    };
    tick();
    document.addEventListener('visibilitychange', onVisibility);
    return () => {
      cancelled = true;
      if (rafRef.current != null) window.cancelAnimationFrame(rafRef.current);
      rafRef.current = null;
      document.removeEventListener('visibilitychange', onVisibility);
    };
  }, [isRunning, snapshotAtRaw, snapshotSeconds]);

  let displayedSeconds: number;
  if (isRunning && Number.isFinite(snapshotAtMs)) {
    const elapsedSec = Math.max(0, (Date.now() - snapshotAtMs) / 1000);
    displayedSeconds = directionDown ? snapshotSeconds - elapsedSec : snapshotSeconds + elapsedSec;
  } else {
    displayedSeconds = snapshotSeconds;
  }
  if (stopAt != null) {
    displayedSeconds = directionDown
      ? Math.max(displayedSeconds, stopAt)
      : Math.min(displayedSeconds, stopAt);
  } else if (directionDown) {
    displayedSeconds = Math.max(displayedSeconds, 0);
  }

  const variantKey = element.variant ?? 'score';
  const baseStyle = clockVariantStyles[variantKey] ?? clockVariantStyles.score;
  const resolvedColor = resolveColor(element.color);
  const style: React.CSSProperties = {
    ...baseStyle,
    fontVariantNumeric: 'tabular-nums',
    ...(resolvedColor ? { color: resolvedColor } : {}),
  };

  return (
    <AtomicBox element={element}>
      <span style={style} {...accessibilityProps(element.accessibility)}>
        {formatSeconds(displayedSeconds, format)}
      </span>
    </AtomicBox>
  );
}
