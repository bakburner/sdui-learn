import React from 'react';
import type { AtomicElement, Shadow, Badge } from '@sdui/models';
import { resolveBackgroundCSS, type Background } from '../../utils/background';
import { useColorTokenResolver } from '../../utils/ColorTokenResolver';
import {
  resolveContainerVariant,
  axisAllowsOverride,
  logVariantOverrideBlocked,
  supportsBackdropFilter,
  type ContainerVariantSpec,
} from '../../utils/ContainerVariantResolver';
import { AtomicRouter } from './AtomicRouter';

/**
 * AtomicBox — the single site for every AtomicElement's CSS box model on the
 * web client.
 *
 * Every primitive renderer (Container, Text, Image, Button, Divider,
 * DisplayGrid, ScrollContainer) wraps its content in AtomicBox so margin
 * / padding / background / cornerRadius / shadow / border / opacity /
 * width / height / variant resolution live in exactly one place rather
 * than being re-implemented per primitive. Layout-only primitives
 * (Spacer, Conditional, SectionSlot) bypass it — the chosen child or
 * hosted section carries the box model instead.
 *
 * The rendering order is fixed so every primitive produces the same
 * box-model shape on the wire:
 *
 *     margin box  (outer)
 *       └─ shadow + background + border + cornerRadius + backdrop-filter
 *            └─ padding
 *                 └─ content (inner)
 *
 * Rules:
 *   - `margin` lives on the outer wrapper so sibling-to-sibling spacing is
 *     untouched by the element's own background / corner clip.
 *   - `padding` lives inside the background / corner-clip, so the bg
 *     extends to the padded frame (CSS `box-sizing: content-box`-like
 *     intent; the element's declared `width` / `height` controls the
 *     outer padded frame, i.e. border-box sizing).
 *   - `variant` is resolved once here and merged with inline props per
 *     the `overrideMatrix` in the variant spec.
 *
 * Most primitives hand `layoutStyle` in so their flex / display semantics
 * are applied to the same DOM node that carries the box model — a single
 * wrapper div per element, not two.
 */
export interface AtomicBoxProps {
  element: AtomicElement;
  children: React.ReactNode;
  /**
   * Extra CSS merged onto the same wrapper div. Used by layout-owning
   * primitives (Container, ScrollContainer, DisplayGrid) to set
   * `display: flex` / `overflow: auto` / flex direction / gap / alignment
   * on the box div itself. Intentionally merged *after* the box-model
   * CSS so callers can still override things like `overflow` when they
   * legitimately need to (e.g. ScrollContainer).
   */
  layoutStyle?: React.CSSProperties;
  /** Extra CSS properties applied with highest precedence. Rare — used when
   *  a primitive needs to force a visual behaviour that the generic box
   *  model cannot express cleanly. Prefer `layoutStyle` when possible. */
  styleOverrides?: React.CSSProperties;
  /** Optional className for responsive-media-query scoping (AtomicContainer
   *  uses this for its `breakpoint` row→column flip). */
  className?: string;
  /** Render `<button>` instead of `<div>` when the primitive is a button.
   *  Defaults to `div`. */
  as?: 'div' | 'button';
  /** `disabled` for the button case. */
  disabled?: boolean;
  /** `onClick` handler forwarded to the wrapper element. */
  onClick?: React.MouseEventHandler;
  /** `aria-label` forwarded to the wrapper element. */
  ariaLabel?: string;
  /** `role` forwarded to the wrapper element. */
  role?: string;
  /** `style` forwarded children-only props merged via JSX spread. */
  extraProps?: Record<string, unknown>;
}

export function AtomicBox(props: AtomicBoxProps): React.ReactElement {
  const {
    element,
    children,
    layoutStyle,
    styleOverrides,
    className,
    as = 'div',
    disabled,
    onClick,
    ariaLabel,
    role,
    extraProps,
  } = props;

  const resolveColor = useColorTokenResolver();
  const style = buildBoxStyle(element, resolveColor, layoutStyle, styleOverrides);

  if (as === 'button') {
    return (
      <button
        style={style}
        className={className}
        disabled={disabled}
        onClick={onClick}
        aria-label={ariaLabel}
        {...(extraProps as Record<string, unknown>)}
      >
        {children}
      </button>
    );
  }

  return (
    <div
      style={style}
      className={className}
      onClick={onClick}
      aria-label={ariaLabel}
      role={role}
      {...(extraProps as Record<string, unknown>)}
    >
      {children}
    </div>
  );
}

/** Absolute-positions a `<Badge>` overlay against a box — used by
 *  AtomicContainer / AtomicImage. The parent must already have
 *  `position: relative` on it, which `buildBoxStyle` sets automatically
 *  when `element.badge` is present. */
export function AtomicBoxBadge(props: {
  badge: Badge;
  onAction: (action: unknown) => void;
}): React.ReactElement | null {
  const { badge, onAction } = props;
  if (!badge.element) return null;
  const pos = badgePositionMap[badge.alignment ?? 'topEnd'] ?? badgePositionMap.topEnd;
  return (
    <div style={pos}>
      <AtomicRouter
        element={badge.element}
        state={{}}
        onAction={onAction as (action: import('@sdui/models').Action) => void}
      />
    </div>
  );
}

const badgePositionMap: Record<string, React.CSSProperties> = {
  topStart:    { position: 'absolute', top: 4, left: 4 },
  topEnd:      { position: 'absolute', top: 4, right: 4 },
  bottomStart: { position: 'absolute', bottom: 4, left: 4 },
  bottomEnd:   { position: 'absolute', bottom: 4, right: 4 },
};

/** Assembles the CSS for the unified box model. Extracted from AtomicBox so
 *  unit tests and consumers that need the resolved style object (rare) can
 *  call it directly. */
export function buildBoxStyle(
  element: AtomicElement,
  resolveColor: (value: string | null | undefined) => string | undefined,
  layoutStyle?: React.CSSProperties,
  styleOverrides?: React.CSSProperties,
): React.CSSProperties {
  const variantSpec = resolveContainerVariant(element.variant);
  const variantName = variantSpec ? (element.variant as string) : undefined;

  const style: React.CSSProperties = {};

  // margin (outer spacing)
  if (element.margin) {
    const m = element.margin;
    if (m.top != null) style.marginTop = m.top;
    if (m.end != null) style.marginRight = m.end;
    if (m.bottom != null) style.marginBottom = m.bottom;
    if (m.start != null) style.marginLeft = m.start;
  }

  // padding (inner spacing, inside bg/corner clip)
  if (element.padding) {
    const p = element.padding;
    if (p.top != null) style.paddingTop = p.top;
    if (p.end != null) style.paddingRight = p.end;
    if (p.bottom != null) style.paddingBottom = p.bottom;
    if (p.start != null) style.paddingLeft = p.start;
  }

  // width / height / fillWidth
  if (element.width != null) {
    style.width = element.width;
    style.flexShrink = 0;
  } else if (element.fillWidth || variantSpec?.fillWidth) {
    style.width = '100%';
  }
  if (element.height != null) {
    style.height = element.height;
  }

  // corner radius — per-corner wins when any corner is non-zero; else
  // inline `cornerRadius`; else variant default.
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

  // background — inline wins on `allow`; variant wins on `lock`.
  if (element.background) {
    if (variantSpec && !axisAllowsOverride(variantSpec, 'background')) {
      logVariantOverrideBlocked(variantName!, 'background', element.background);
      if (variantSpec.backgroundCss) {
        style.background = variantSpec.backgroundCss;
      }
    } else {
      Object.assign(style, resolveBackgroundCSS(element.background as unknown as Background, resolveColor));
    }
  } else if (variantSpec?.backgroundCss) {
    style.background = variantSpec.backgroundCss;
  }

  // shadow — inline wins on `allow`; variant wins on `lock`.
  applyShadow(style, element.shadow, variantSpec, variantName, resolveColor);

  // border (variant-only today — no inline `border` prop).
  if (variantSpec?.border) {
    style.border = variantSpec.border;
  }

  // backdrop-filter — apply only when the browser supports the value.
  if (variantSpec?.backdropFilter && supportsBackdropFilter(variantSpec.backdropFilter)) {
    style.backdropFilter = variantSpec.backdropFilter;
    (style as React.CSSProperties & { WebkitBackdropFilter?: string })
      .WebkitBackdropFilter = variantSpec.backdropFilter;
  }

  // opacity
  if (element.opacity != null) {
    style.opacity = element.opacity;
  }

  // `badge` requires the box to establish a positioning context so the
  // absolute-positioned overlay anchors to this element.
  if (element.badge) {
    style.position = 'relative';
  }

  // Layout CSS (display/flex/direction/gap/alignment/overflow) is merged
  // in second so it can override box-model defaults if a primitive needs
  // to (e.g. ScrollContainer overriding `overflow`).
  if (layoutStyle) {
    Object.assign(style, layoutStyle);
  }

  // Caller-forced overrides apply last.
  if (styleOverrides) {
    Object.assign(style, styleOverrides);
  }

  return style;
}

function applyShadow(
  style: React.CSSProperties,
  inline: Shadow | null | undefined,
  variantSpec: ContainerVariantSpec | undefined,
  variantName: string | undefined,
  resolveColor: (value: string | null | undefined) => string | undefined,
): void {
  if (inline) {
    if (variantSpec && !axisAllowsOverride(variantSpec, 'shadow')) {
      logVariantOverrideBlocked(variantName!, 'shadow', inline);
      if (variantSpec.boxShadow) {
        style.boxShadow = variantSpec.boxShadow;
      }
      return;
    }
    const shadowColor = resolveColor(inline.color) ?? 'rgba(0,0,0,0.08)';
    style.boxShadow = `${inline.offsetX ?? 0}px ${inline.offsetY ?? 0}px ${inline.radius ?? 0}px ${shadowColor}`;
    return;
  }
  if (variantSpec?.boxShadow) {
    style.boxShadow = variantSpec.boxShadow;
  }
}
