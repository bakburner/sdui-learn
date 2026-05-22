import React, { useContext, useEffect, memo, useMemo } from 'react';
import type { Action } from '@sdui/models';
import { ActionTrigger } from '@sdui/models';
import type { AtomicProps } from './AtomicRouter';
import { AtomicBox, AtomicBoxBadge } from './AtomicBox';
import { accessibilityProps } from '../../utils/accessibility';
import { resolveImageVariant } from '../../utils/ImageVariantResolver';
import { currentFormFactor, resolveLayoutScalar as resolveLS } from '../../utils/LayoutTokenResolver';
import { CompositeContentContext, resolveBindRefString } from '../../utils/BindRefResolver';
import { useWireAssetBaseUrl } from '../../context/WireAssetBaseUrlContext';
import { resolveWireAssetUrl } from '../../utils/WireUrlResolver';
import { areAtomicPropsEqual } from './areAtomicPropsEqual';
import { longPressPointerProps } from './atomicActionHandlers';
import { logUnsupportedAtomicTriggers, selectActions } from './getActivateActions';

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
  useEffect(() => {
    logUnsupportedAtomicTriggers(element.actions as Action[] | undefined, element.id);
  }, [element.actions, element.id]);

  const variantSpec = resolveImageVariant(element.variant, currentFormFactor());

  const resolvedObjectFit: React.CSSProperties['objectFit'] =
    fitToObjectFit[element.fit ?? ''] ?? variantSpec?.objectFit ?? 'contain';

  const arFromVariant =
    variantSpec?.aspectRatio != null ? Number(variantSpec.aspectRatio) : undefined;
  const aspectRatioForDims: number | undefined = (() => {
    if (element.aspectRatio != null) {
      const n = Number(element.aspectRatio);
      return !Number.isNaN(n) && n > 0 ? n : undefined;
    }
    return arFromVariant != null && !Number.isNaN(arFromVariant) && arFromVariant > 0
      ? arFromVariant
      : undefined;
  })();

  const intrinsicSize = useMemo((): { w: number; h: number } | undefined => {
    const ff = currentFormFactor();
    const w = element.width != null ? resolveLS(element.width, ff) : undefined;
    const h = element.height != null ? resolveLS(element.height, ff) : undefined;
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
  const activateActions = selectActions(element.actions as Action[] | undefined, ActionTrigger.OnActivate);
  const longPressActions = selectActions(element.actions as Action[] | undefined, ActionTrigger.OnLongPress);
  const handleClick = hasActions
    ? () => {
        if (activateActions.length > 0) onAction(activateActions);
      }
    : undefined;
  const longPressHandlers = longPressActions.length > 0
    ? longPressPointerProps(() => onAction(longPressActions))
    : {};

  const wireBase = useWireAssetBaseUrl();

  // Resolve `src` from `bindRef` when present, falling back to the
  // inline `src`. Lets composers rebind image URLs in flight without
  // touching the ui tree.
  const compositeContent = useContext(CompositeContentContext);
  const rawSrc = resolveBindRefString(element.bindRef, compositeContent) ?? element.src;
  const resolvedSrc = resolveWireAssetUrl(rawSrc, wireBase);
  const fallbackUrl = resolveWireAssetUrl(element.placeholder, wireBase);

  const handleError = (e: React.SyntheticEvent<HTMLImageElement>) => {
    const img = e.currentTarget;
    if (fallbackUrl && img.src !== fallbackUrl) {
      img.src = fallbackUrl;
    }
  };

  const a11y = element.accessibility;
  const altText = a11y?.hidden ? '' : (a11y?.label ?? element.alt ?? '');

  const img = (
    <img
      src={resolvedSrc}
      alt={altText}
      style={{ ...imgStyle, ...(hasActions ? { cursor: 'pointer' } : {}) }}
      onClick={handleClick}
      {...longPressHandlers}
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
