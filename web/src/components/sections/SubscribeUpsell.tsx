import React from 'react';
import type { SectionProps } from '../SectionRouter';
import { AtomicRouter } from '../atomic';

/**
 * SubscribeUpsell — reserved SDK integration point for the subscription
 * upsell surface (both inline-banner and full-screen-hero layouts; the
 * visual variant is expressed by the inner `data.ui` atomic tree and by
 * the section's outer `surface`, not by component identity).
 *
 * The entire visible surface is expressed as an atomic tree under
 * `section.data.ui`; this renderer is a thin walker over that tree via
 * `AtomicRouter`, identical in behaviour to an AtomicComposite section.
 *
 * Outer chrome (margin, radius, gradient background, inner padding) comes
 * from `section.surface` via `SectionContainer` — this renderer only
 * walks the inner atomic tree.
 *
 * `section.data.ctaAction` is the optional pre-SDK fallback action;
 * `section.data.tiers` carries IAP product identifiers reserved for the
 * future IAP SDK. Neither is read by this renderer today.
 */
export function SubscribeUpsell({
  section,
  state,
  onAction,
  onStateChange,
}: SectionProps): React.ReactElement | null {
  const ui = section.data?.ui;
  if (!ui) {
    console.debug(
      `[SubscribeUpsell] section ${section.id} has no data.ui atomic tree`,
    );
    return null;
  }
  return (
    <AtomicRouter
      element={ui}
      state={state}
      onAction={onAction}
      onStateChange={onStateChange}
    />
  );
}
