import React from 'react';
import type { Section, Action } from '@sdui/models';

interface VideoPlayerStubProps {
  section: Section;
  onAction: (action: Action) => void;
}

/**
 * Stub renderer for VideoPlayer sections.
 * Displays a placeholder showing playerType + contentId.
 * Will be replaced with actual video SDK integration.
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
        backgroundColor: '#1A1F2E',
        borderRadius: 8,
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
      <div style={{ fontSize: 12, color: '#888' }}>
        {playerType} • {contentId}
      </div>
    </div>
  );
}
