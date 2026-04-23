import React from 'react';
import type { AtomicProps } from './AtomicRouter';
import { AtomicRouter } from './AtomicRouter';
import type { Shadow, Badge } from './AtomicElement';
import { resolveBackgroundCSS } from '../../utils/background';
import { accessibilityProps } from '../../utils/accessibility';
import {
  resolveContainerVariant,
  axisAllowsOverride,
  logVariantOverrideBlocked,
  supportsBackdropFilter,
} from '../../utils/ContainerVariantResolver';
import { useColorTokenResolver } from '../../utils/ColorTokenResolver';

const badgePositionMap: Record<string, React.CSSProperties> = {
  topStart:    { position: 'absolute', top: 4, left: 4 },
  topEnd:      { position: 'absolute', top: 4, right: 4 },
  bottomStart: { position: 'absolute', bottom: 4, left: 4 },
  bottomEnd:   { position: 'absolute', bottom: 4, right: 4 },
};

/**
 * AtomicContainer — renders a flex row or column with gap, padding,
 * background color, optional gradient, flex children, and responsive breakpoint.
 *
 * Flex: When a child has a non-null `flex` value, it receives proportional
 * flex-grow along the main axis (like CSS flex-grow). Children without flex
 * size to content.
 *
 * Breakpoint: When set and direction is "row", the container flips to column
 * below the breakpoint width using a CSS media query. This replaces the old
 * Row section type with a purely atomic, server-composed primitive.
 */
export function AtomicContainer({ element, state, onAction, depth = 0, onStateChange, sectionSlotDepth }: AtomicProps): React.ReactElement {
  const resolveColor = useColorTokenResolver();
  const isRow = element.direction === 'row';
  const hasBreakpoint = isRow && element.breakpoint != null;

  // Unique class for scoped responsive CSS
  const className = hasBreakpoint ? `sdui-ac-${element.id ?? depth}` : undefined;

  const variantSpec = resolveContainerVariant(element.variant);
  const variantName = variantSpec ? (element.variant as string) : undefined;

  const style: React.CSSProperties = {
    display: 'flex',
    flexDirection: isRow ? 'row' : 'column',
    gap: element.gap,
  };

  // alignment → justify-content
  switch (element.alignment) {
    case 'center':       style.justifyContent = 'center'; break;
    case 'end':          style.justifyContent = 'flex-end'; break;
    case 'spaceBetween': style.justifyContent = 'space-between'; break;
    case 'spaceAround':  style.justifyContent = 'space-around'; break;
    case 'spaceEvenly':  style.justifyContent = 'space-evenly'; break;
    default:             style.justifyContent = 'flex-start'; break;
  }

  // crossAlignment → align-items
  switch (element.crossAlignment) {
    case 'center':  style.alignItems = 'center'; break;
    case 'end':     style.alignItems = 'flex-end'; break;
    case 'stretch': style.alignItems = 'stretch'; break;
    default:        style.alignItems = 'flex-start'; break;
  }

  if (element.fillWidth) {
    style.width = '100%';
  } else if (variantSpec?.fillWidth) {
    style.width = '100%';
  }

  // Explicit wire-level width / height win over fillWidth — a fixed-width
  // card in a horizontal rail needs a deterministic size so its children
  // don't stretch the container to their intrinsic width.
  if (element.width != null) {
    style.width = element.width;
    style.flexShrink = 0;
  }
  if (element.height != null) {
    style.height = element.height;
  }

  // padding
  if (element.padding) {
    const { top, end, bottom, start } = element.padding;
    style.padding = `${top}px ${end}px ${bottom}px ${start}px`;
  }

  // corner radius — `cornerRadii` (per-corner) wins over `cornerRadius` when
  // present. Any corner key omitted falls back to `cornerRadius` (or the
  // variant default, or 0). Used for content-card cards with rounded tops +
  // square bottoms so headline text does not collide with a bottom-corner
  // curve.
  const radiiFallback = element.cornerRadius ?? variantSpec?.cornerRadius ?? 0;
  const radii = element.cornerRadii;
  const hasAnyRadii = radii != null && (
    (radii.topStart ?? 0) !== 0 || (radii.topEnd ?? 0) !== 0 ||
    (radii.bottomStart ?? 0) !== 0 || (radii.bottomEnd ?? 0) !== 0
  );
  if (hasAnyRadii && radii) {
    style.borderTopLeftRadius     = radii.topStart ?? radiiFallback;
    style.borderTopRightRadius    = radii.topEnd ?? radiiFallback;
    style.borderBottomRightRadius = radii.bottomEnd ?? radiiFallback;
    style.borderBottomLeftRadius  = radii.bottomStart ?? radiiFallback;
    style.overflow = 'hidden';
  } else if (element.cornerRadius != null) {
    style.borderRadius = element.cornerRadius;
    style.overflow = 'hidden';
  } else if (variantSpec?.cornerRadius != null) {
    style.borderRadius = variantSpec.cornerRadius;
    style.overflow = 'hidden';
  }

  // background — inline wins on `allow`; variant wins on `lock` (inline ignored + logged).
  if (element.background) {
    if (variantSpec && !axisAllowsOverride(variantSpec, 'background')) {
      logVariantOverrideBlocked(variantName!, 'background', element.background);
      if (variantSpec.backgroundCss) {
        style.background = variantSpec.backgroundCss;
      }
    } else {
      Object.assign(style, resolveBackgroundCSS(element.background, resolveColor));
    }
  } else if (variantSpec?.backgroundCss) {
    style.background = variantSpec.backgroundCss;
  }

  // shadow — inline wins on `allow`; variant wins on `lock`.
  if (element.shadow) {
    if (variantSpec && !axisAllowsOverride(variantSpec, 'shadow')) {
      logVariantOverrideBlocked(variantName!, 'shadow', element.shadow);
      if (variantSpec.boxShadow) {
        style.boxShadow = variantSpec.boxShadow;
      }
    } else {
      const s: Shadow = element.shadow;
      const shadowColor = resolveColor(s.color) ?? 'rgba(0,0,0,0.08)';
      style.boxShadow = `${s.offsetX ?? 0}px ${s.offsetY ?? 0}px ${s.radius ?? 0}px ${shadowColor}`;
    }
  } else if (variantSpec?.boxShadow) {
    style.boxShadow = variantSpec.boxShadow;
  }

  // border (variant-only; there is no inline `border` prop today, so no conflict).
  if (variantSpec?.border) {
    style.border = variantSpec.border;
  }

  // backdrop-filter — apply only when the browser supports the value.
  if (variantSpec?.backdropFilter && supportsBackdropFilter(variantSpec.backdropFilter)) {
    style.backdropFilter = variantSpec.backdropFilter;
    (style as React.CSSProperties & { WebkitBackdropFilter?: string })
      .WebkitBackdropFilter = variantSpec.backdropFilter;
  }

  // badge requires relative positioning
  const badge: Badge | undefined = element.badge;
  if (badge) {
    style.position = 'relative';
  }

  return (
    <div className={className} style={style} {...accessibilityProps(element.accessibility)}>
      {element.children?.map((child, i) => {
        const childStyle: React.CSSProperties | undefined =
          child.flex != null && child.flex > 0
            ? { flex: `${child.flex} 1 0%`, minWidth: 0 }
            : undefined;

        return childStyle ? (
          <div key={child.id ?? i} style={childStyle}>
            <AtomicRouter element={child} state={state} onAction={onAction} depth={depth} onStateChange={onStateChange} sectionSlotDepth={sectionSlotDepth} />
          </div>
        ) : (
          <AtomicRouter key={child.id ?? i} element={child} state={state} onAction={onAction} depth={depth} onStateChange={onStateChange} sectionSlotDepth={sectionSlotDepth} />
        );
      })}

      {badge?.element && (
        <div style={badgePositionMap[badge.alignment ?? 'topEnd'] ?? badgePositionMap.topEnd}>
          <AtomicRouter element={badge.element} state={state} onAction={onAction} depth={depth} onStateChange={onStateChange} sectionSlotDepth={sectionSlotDepth} />
        </div>
      )}

      {/* Responsive breakpoint CSS — flip row→column below threshold */}
      {hasBreakpoint && className && (
        <style>{`
          @media (max-width: ${element.breakpoint}px) {
            .${className} {
              flex-direction: column !important;
            }
          }
        `}</style>
      )}
    </div>
  );
}
