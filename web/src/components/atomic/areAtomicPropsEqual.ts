import type { AtomicProps } from './AtomicRouter';

export function areAtomicPropsEqual(prev: AtomicProps, next: AtomicProps): boolean {
  return (
    prev.element === next.element &&
    prev.state === next.state &&
    prev.onAction === next.onAction &&
    prev.depth === next.depth &&
    prev.onStateChange === next.onStateChange &&
    prev.sectionSlotDepth === next.sectionSlotDepth
  );
}
