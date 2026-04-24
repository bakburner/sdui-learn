import React from 'react';
import type { Action } from '@sdui/models';
import type { AtomicProps } from './AtomicRouter';
import { AtomicBox, AtomicBoxBadge } from './AtomicBox';
import { resolveImageVariant } from '../../utils/ImageVariantResolver';

const DEFAULT_FALLBACK = 'https://cdn.nba.com/manage/2025/04/nba-247-logoman-yt-thumbnail__1_.png';

const fitToObjectFit: Record<string, React.CSSProperties['objectFit']> = {
  cover: 'cover',
  contain: 'contain',
  fill: 'fill',
  none: 'none',
};

/**
 * AtomicImage — renders an <img> with object-fit and aspect-ratio. The
 * unified box model (margin/padding/bg/cornerRadius/shadow/border/badge)
 * is applied by AtomicBox; this renderer only owns the <img> itself and
 * the variant-derived object-fit / aspect-ratio semantics.
 *
 * The <img>'s `width` fills its box (100%) so sizing props on the
 * element (width / height / fillWidth / variant.fillWidth) are honored
 * by AtomicBox and the image stretches to that frame; `aspect-ratio`
 * and `object-fit` stay on the <img> to preserve the intended framing.
 */
export function AtomicImage({ element, onAction }: AtomicProps): React.ReactElement {
  const variantSpec = resolveImageVariant(element.variant);

  const resolvedObjectFit: React.CSSProperties['objectFit'] =
    fitToObjectFit[element.fit ?? ''] ?? variantSpec?.objectFit ?? 'contain';

  const aspectRatioStyle: React.CSSProperties =
    element.aspectRatio != null
      ? { aspectRatio: String(element.aspectRatio) }
      : variantSpec?.aspectRatio != null
        ? { aspectRatio: String(variantSpec.aspectRatio) }
        : {};

  const imgStyle: React.CSSProperties = {
    display: 'block',
    width: '100%',
    ...(element.height != null ? { height: '100%' } : {}),
    objectFit: resolvedObjectFit,
    ...aspectRatioStyle,
  };

  const hasActions = element.actions && element.actions.length > 0;
  const handleClick = hasActions
    ? () => {
        const action = element.actions![0] as unknown as Action;
        onAction(action);
      }
    : undefined;

  const fallbackUrl = element.placeholder || DEFAULT_FALLBACK;
  const handleError = (e: React.SyntheticEvent<HTMLImageElement>) => {
    const img = e.currentTarget;
    if (fallbackUrl && img.src !== fallbackUrl) {
      img.src = fallbackUrl;
    }
  };

  const img = (
    <img
      src={element.src}
      alt={element.accessibility?.label ?? element.alt ?? ''}
      style={{ ...imgStyle, ...(hasActions ? { cursor: 'pointer' } : {}) }}
      onClick={handleClick}
      onError={handleError}
      {...(element.accessibility?.hidden ? { 'aria-hidden': true } : {})}
    />
  );

  return (
    <AtomicBox element={element}>
      {img}
      {element.badge && <AtomicBoxBadge badge={element.badge} onAction={onAction as (a: unknown) => void} />}
    </AtomicBox>
  );
}
