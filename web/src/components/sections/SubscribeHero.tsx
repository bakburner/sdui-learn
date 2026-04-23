import React from 'react';
import type { SectionProps } from '../SectionRouter';
import { AtomicRouter } from '../atomic';
import type { AtomicCompositeData } from '../atomic';

/**
 * SubscribeHero — reserved SDK integration point for the full-screen
 * subscription upsell.
 *
 * The entire visible surface (logo, title, subtitle, feature list, tier
 * cards, CTAs) is expressed as an atomic tree under `section.data.ui`;
 * this renderer is a thin walker over that tree via `AtomicRouter`,
 * identical in behaviour to an AtomicComposite section.
 *
 * Outer chrome (margin, radius, gradient background, inner padding) comes
 * from `section.surface` via `SectionContainer` — this renderer only
 * walks the inner atomic tree. See AGENTS.md §15.3.
 *
 * `section.data.tiers` carries IAP product identifiers reserved for the
 * future IAP SDK; the renderer reads nothing from it today.
 */
export function SubscribeHero({
  section,
  state,
  onAction,
  onStateChange,
}: SectionProps): React.ReactElement | null {
  const data = section.data as unknown as AtomicCompositeData | undefined;
  if (!data?.ui) {
    console.debug(
      `[SubscribeHero] section ${section.id} has no data.ui atomic tree`,
    );
    return null;
  }
  return (
    <AtomicRouter
      element={data.ui}
      state={state}
      onAction={onAction}
      onStateChange={onStateChange}
    />
  );
}
