import React, { useContext, memo, useMemo } from 'react';
import type { Action } from '@sdui/models';
import type { AtomicProps } from './AtomicRouter';
import { AtomicBox, AtomicBoxBadge } from './AtomicBox';
import { accessibilityProps } from '../../utils/accessibility';
import { resolveImageVariant } from '../../utils/ImageVariantResolver';
import { currentFormFactor } from '../../utils/LayoutTokenResolver';
import { CompositeContentContext, resolveBindRefString } from '../../utils/BindRefResolver';
import { areAtomicPropsEqual } from './areAtomicPropsEqual';
import { getActivateActions } from './getActivateActions';

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
function AtomicImageInner({ element, onAction }: AtomicProps): React.ReactElement {
  const variantSpec = resolveImageVariant(element.variant, currentFormFactor());

  const resolvedObjectFit: React.CSSProperties['objectFit'] =
    fitToObjectFit[element.fit ?? ''] ?? variantSpec?.objectFit ?? 'contain';

  const arFromVariant =
    variantSpec?.aspectRatio != null ? Number(variantSpec.aspectRatio) : undefined;
  const aspectRatioForDims =
    element.aspectRatio ??
    (arFromVariant != null && !Number.isNaN(arFromVariant) && arFromVariant > 0
      ? arFromVariant
      : undefined);

  const intrinsicSize = useMemo((): { w: number; h: number } | undefined => {
    const w = element.width;
    const h = element.height;
    const ar = aspectRatioForDims;
    if (w != null && h != null) {
      return { w, h };
    }
    if (w != null && ar != null && ar > 0) {
      return { w, h: Math.max(1, Math.round(w / ar)) };
    }
    if (h != null && ar != null && ar > 0) {
      return { w: Math.max(1, Math.round(h * ar)), h };
    }
    return undefined;
  }, [element.width, element.height, aspectRatioForDims]);

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
        const actions = getActivateActions(element.actions as Action[] | undefined);
        if (actions.length > 0) onAction(actions);
      }
    : undefined;

  const fallbackUrl = element.placeholder;
  const handleError = (e: React.SyntheticEvent<HTMLImageElement>) => {
    const img = e.currentTarget;
    if (fallbackUrl && img.src !== fallbackUrl) {
      img.src = fallbackUrl;
    }
  };

  // Resolve `src` from `bindRef` when present, falling back to the
  // inline `src`. Lets composers rebind image URLs in flight without
  // touching the ui tree.
  const compositeContent = useContext(CompositeContentContext);
  const resolvedSrc = resolveBindRefString(element.bindRef, compositeContent) ?? element.src;

  const a11y = element.accessibility;
  const altText = a11y?.hidden ? '' : (a11y?.label ?? element.alt ?? '');

  const img = (
    <img
      src={resolvedSrc}
      alt={altText}
      style={{ ...imgStyle, ...(hasActions ? { cursor: 'pointer' } : {}) }}
      onClick={handleClick}
      onError={handleError}
      loading="lazy"
      {...(intrinsicSize
        ? { width: intrinsicSize.w, height: intrinsicSize.h }
        : {})}
      {...accessibilityProps(a11y)}
    />
  );

  return (
    <AtomicBox element={element}>
      {img}
      {element.badge && <AtomicBoxBadge badge={element.badge} onAction={onAction as (a: unknown) => void} />}
    </AtomicBox>
  );
}

export const AtomicImage = memo(AtomicImageInner, areAtomicPropsEqual);
