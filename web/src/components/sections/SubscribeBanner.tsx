import React from 'react';
import type { SectionProps } from '../SectionRouter';
import { AtomicRouter } from '../atomic';
import type { AtomicCompositeData } from '../atomic';

/**
 * SubscribeBanner — reserved SDK integration point for inline
 * subscription upsell.
 *
 * The entire visible surface is expressed as an atomic tree under
 * `section.data.ui`; this renderer is a thin walker over that tree via
 * `AtomicRouter`, identical in behaviour to an AtomicComposite section.
 *
 * Outer chrome (margin, radius, gradient background, inner padding) comes
 * from `section.surface` via `SectionContainer` — this renderer only
 * walks the inner atomic tree.
 *
 * `section.data.ctaAction` is the pre-SDK fallback action; once the IAP
 * SDK lands it will take over the CTA button's tap, reading product
 * identifiers from `section.data.tiers`.
 */
export function SubscribeBanner({
  section,
  state,
  onAction,
  onStateChange,
}: SectionProps): React.ReactElement | null {
  const data = section.data as unknown as AtomicCompositeData | undefined;
  if (!data?.ui) {
    console.debug(
      `[SubscribeBanner] section ${section.id} has no data.ui atomic tree`,
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
