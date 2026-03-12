import React from 'react';
import type { SectionStates } from '@sdui/models';

interface SectionSkeletonProps {
  sectionStates?: SectionStates;
}

export function SectionSkeleton({ sectionStates }: SectionSkeletonProps): React.ReactElement {
  const skeleton = sectionStates?.loading?.skeleton ?? 'shimmer';
  const minHeight = sectionStates?.loading?.minHeightDp ?? 80;

  if (skeleton === 'none') {
    return <></>;
  }

  if (skeleton === 'spinner') {
    return (
      <div style={{ ...styles.container, minHeight }}>
        <div style={styles.spinner} />
      </div>
    );
  }

  if (skeleton === 'placeholder') {
    return (
      <div style={{ ...styles.container, minHeight }}>
        <div style={styles.placeholderLine} />
        <div style={{ ...styles.placeholderLine, width: '60%' }} />
        <div style={{ ...styles.placeholderLine, width: '40%' }} />
      </div>
    );
  }

  // Default: shimmer
  return (
    <div style={{ ...styles.container, minHeight }}>
      <div style={styles.shimmerBlock} />
      <div style={{ ...styles.shimmerBlock, width: '75%', height: 12 }} />
      <div style={{ ...styles.shimmerBlock, width: '50%', height: 12 }} />
    </div>
  );
}

const styles: Record<string, React.CSSProperties> = {
  container: {
    display: 'flex',
    flexDirection: 'column',
    gap: 8,
    padding: 16,
    borderRadius: 8,
    backgroundColor: 'rgba(255, 255, 255, 0.03)',
  },
  shimmerBlock: {
    height: 16,
    width: '100%',
    borderRadius: 4,
    background: 'linear-gradient(90deg, #1a1a2e 25%, #2a2a4e 50%, #1a1a2e 75%)',
    backgroundSize: '200% 100%',
    animation: 'shimmer 1.5s ease-in-out infinite',
  },
  spinner: {
    width: 24,
    height: 24,
    border: '2px solid #333',
    borderTopColor: '#ff6b6b',
    borderRadius: '50%',
    animation: 'spin 1s linear infinite',
    alignSelf: 'center',
  },
  placeholderLine: {
    height: 14,
    width: '100%',
    borderRadius: 4,
    backgroundColor: '#1a1a2e',
  },
};
