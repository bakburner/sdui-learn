import type { AtomicElement } from '@sdui/models';

export { AtomicRouter } from './AtomicRouter';
export type { AtomicProps } from './AtomicRouter';
export type { AtomicElement } from '@sdui/models';

/**
 * Narrow view over a section's `data` payload for the family of sections that
 * deliver their visible surface as an atomic tree under `data.ui` —
 * AtomicComposite itself and the "reserved SDK" sections (SubscribeBanner,
 * SubscribeHero, VideoPlayer). Fields the renderer does not consume directly
 * are ignored here but remain available under the full generated `Data` union.
 */
export type AtomicCompositeData = {
  ui?: AtomicElement;
  content?: Record<string, unknown>;
};
