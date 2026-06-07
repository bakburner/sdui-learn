import React from 'react';
import type { SectionProps } from '../SectionRouter';
import { AtomicRouter } from '../atomic';

/**
 * VideoPlayerStub — reserved SDK integration point for the video player.
 *
 * Today the visible surface is the pre-SDK placeholder composed by the
 * server as an atomic tree under `section.data.ui`; this renderer is a
 * thin walker over that tree via `AtomicRouter`. Once the video SDK
 * lands it mounts here using the SDK inputs at the top of `section.data`
 * (`playerType`, `contentId`, `autoplay`, `capabilities`,
 * `displayConfig`) and the atomic tree becomes the SDK's loading / error
 * placeholder.
 *
 * Outer chrome comes from `section.surface` via `SectionContainer`.
 */
export function VideoPlayerStub({
  section,
  state,
  onAction,
  onStateChange,
}: SectionProps): React.ReactElement | null {
  const ui = section.data?.ui;
  if (!ui) {
    console.debug(
      `[VideoPlayerStub] section ${section.id} has no data.ui atomic tree`,
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
