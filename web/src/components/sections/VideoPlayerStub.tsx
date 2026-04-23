import React from 'react';
import type { Section, Action } from '@sdui/models';

interface VideoPlayerStubProps {
  section: Section;
  onAction: (action: Action) => void;
}

/**
 * Stub renderer for VideoPlayer sections. Will be replaced with the
 * video SDK in a later phase; until then renders a placeholder play
 * icon. Outer chrome (background, corner radius) comes from
 * `section.display` via `SectionContainer`. The renderer only owns
 * the 16:9 content frame and placeholder glyph. See AGENTS.md
 * §15.1(2) and §15.3.
 */
export function VideoPlayerStub({ section }: VideoPlayerStubProps): React.ReactElement {
  const data = section.data as Record<string, unknown> | undefined;
  const playerType = (data?.playerType as string) ?? 'unknown';
  const contentId = (data?.contentId as string) ?? 'unknown';

  return (
    <div
      style={{
        width: '100%',
        aspectRatio: '16 / 9',
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        color: 'white',
      }}
    >
      <svg width="48" height="48" viewBox="0 0 24 24" fill="white">
        <path d="M8 5v14l11-7z" />
      </svg>
      <div style={{ marginTop: 8, fontSize: 18, fontWeight: 600 }}>Video Player</div>
      <div style={{ fontSize: 12, color: 'rgba(255,255,255,0.6)' }}>
        {playerType} • {contentId}
      </div>
    </div>
  );
}
