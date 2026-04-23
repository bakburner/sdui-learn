import React from 'react';
import type { Action } from '@sdui/models';
import type { AtomicProps } from './AtomicRouter';
import { AtomicRouter } from './AtomicRouter';
import type { Badge } from './AtomicElement';
import { accessibilityProps } from '../../utils/accessibility';
import { resolveImageVariant } from '../../utils/ImageVariantResolver';

const badgePositionMap: Record<string, React.CSSProperties> = {
  topStart:    { position: 'absolute', top: 4, left: 4 },
  topEnd:      { position: 'absolute', top: 4, right: 4 },
  bottomStart: { position: 'absolute', bottom: 4, left: 4 },
  bottomEnd:   { position: 'absolute', bottom: 4, right: 4 },
};

const DEFAULT_FALLBACK = 'https://cdn.nba.com/manage/2025/04/nba-247-logoman-yt-thumbnail__1_.png';

const fitToObjectFit: Record<string, React.CSSProperties['objectFit']> = {
  cover: 'cover',
  contain: 'contain',
  fill: 'fill',
  none: 'none',
};

/**
 * AtomicImage — renders an <img> with optional sizing, aspect ratio,
 * content scale, corner radius, and fallback on load error.
 */
export function AtomicImage({ element, onAction }: AtomicProps): React.ReactElement {
  const variantSpec = resolveImageVariant(element.variant);

  // object-fit: inline `fit` wins; otherwise variant; otherwise `contain` default.
  const resolvedObjectFit: React.CSSProperties['objectFit'] =
    fitToObjectFit[element.fit ?? ''] ?? variantSpec?.objectFit ?? 'contain';

  // width: inline `fillWidth` > inline `width` > variant `fillWidth`.
  const widthStyle: React.CSSProperties = element.fillWidth
    ? { width: '100%' }
    : element.width != null
      ? { width: element.width }
      : variantSpec?.fillWidth
        ? { width: '100%' }
        : {};

  // aspect-ratio: inline wins; else variant.
  const aspectRatioStyle: React.CSSProperties =
    element.aspectRatio != null
      ? { aspectRatio: String(element.aspectRatio) }
      : variantSpec?.aspectRatio != null
        ? { aspectRatio: String(variantSpec.aspectRatio) }
        : {};

  // corner radius: inline wins; else variant — but only when the variant's
  // `clip` flag is true (logo variant explicitly opts out of clipping).
  // Per-corner `cornerRadii` takes precedence when any corner value is
  // non-zero; corners omitted fall back to the resolved single value.
  const resolvedCornerRadius =
    element.cornerRadius != null
      ? element.cornerRadius
      : variantSpec && variantSpec.clip !== false
        ? variantSpec.cornerRadius
        : undefined;
  const variantClipAllowed = !variantSpec || variantSpec.clip !== false;
  const radii = element.cornerRadii;
  const hasAnyRadii = variantClipAllowed && radii != null && (
    (radii.topStart ?? 0) !== 0 || (radii.topEnd ?? 0) !== 0 ||
    (radii.bottomStart ?? 0) !== 0 || (radii.bottomEnd ?? 0) !== 0
  );
  let cornerRadiusStyle: React.CSSProperties;
  if (hasAnyRadii && radii) {
    const fallback = resolvedCornerRadius ?? 0;
    cornerRadiusStyle = {
      borderTopLeftRadius:     radii.topStart ?? fallback,
      borderTopRightRadius:    radii.topEnd ?? fallback,
      borderBottomRightRadius: radii.bottomEnd ?? fallback,
      borderBottomLeftRadius:  radii.bottomStart ?? fallback,
      overflow: 'hidden',
    };
  } else if (resolvedCornerRadius != null) {
    cornerRadiusStyle = { borderRadius: resolvedCornerRadius, overflow: 'hidden' };
  } else {
    cornerRadiusStyle = {};
  }

  const style: React.CSSProperties = {
    objectFit: resolvedObjectFit,
    ...widthStyle,
    ...(element.height != null ? { height: element.height } : {}),
    ...aspectRatioStyle,
    ...cornerRadiusStyle,
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
      style={{ ...style, ...(hasActions ? { cursor: 'pointer' } : {}), ...(element.badge ? { display: 'block' } : {}) }}
      onClick={handleClick}
      onError={handleError}
      {...(element.accessibility?.hidden ? { 'aria-hidden': true } : {})}
    />
  );

  const badge: Badge | undefined = element.badge;
  if (badge?.element) {
    return (
      <div style={{ position: 'relative', display: 'inline-block' }}>
        {img}
        <div style={badgePositionMap[badge.alignment ?? 'topEnd'] ?? badgePositionMap.topEnd}>
          <AtomicRouter element={badge.element} state={{}} onAction={onAction} />
        </div>
      </div>
    );
  }

  return img;
}
