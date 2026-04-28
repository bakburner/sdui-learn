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
import {
  currentFormFactor,
  resolveLayoutScalar,
  resolveSpacingPx,
  type FormFactor,
} from '../../utils/LayoutTokenResolver';
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
  const ff = currentFormFactor();
  const variantSpec = resolveContainerVariant(element.variant, ff);
  const variantName = variantSpec ? (element.variant as string) : undefined;

  const style: React.CSSProperties = {};

  // margin (outer spacing) — start/end map to LTR left/right; tokens
  // resolved per form factor so semantic spacing (e.g. `token:spacing.md`)
  // produces a concrete pixel value here, not a CSS-invalid string.
  if (element.margin) {
    const m = resolveSpacingPx(element.margin, ff);
    if (element.margin.top != null)    style.marginTop    = m.top;
    if (element.margin.end != null)    style.marginRight  = m.right;
    if (element.margin.bottom != null) style.marginBottom = m.bottom;
    if (element.margin.start != null)  style.marginLeft   = m.left;
  }

  // padding (inner spacing, inside bg/corner clip)
  if (element.padding) {
    const p = resolveSpacingPx(element.padding, ff);
    if (element.padding.top != null)    style.paddingTop    = p.top;
    if (element.padding.end != null)    style.paddingRight  = p.right;
    if (element.padding.bottom != null) style.paddingBottom = p.bottom;
    if (element.padding.start != null)  style.paddingLeft   = p.left;
  }

  // width / height / fillWidth — token strings resolve to pixels; numbers
  // pass through; non-token strings (constraint primitives like `fill`,
  // `wrap`) flow through to CSS for now (constraint primitive support is
  // a follow-up; see implementation plan Phase 2).
  if (element.width != null) {
    style.width = resolveLayoutSize(element.width, ff);
    style.flexShrink = 0;
  } else if (element.fillWidth || variantSpec?.fillWidth) {
    style.width = '100%';
  }
  if (element.height != null) {
    style.height = resolveLayoutSize(element.height, ff);
  }

  // corner radius — per-corner wins when any corner is non-zero; else
  // inline `cornerRadius`; else variant default. Tokens resolved per
  // form factor; variant `cornerRadius` is already a number.
  const inlineCornerPx = element.cornerRadius != null
    ? resolveLayoutScalar(element.cornerRadius, ff)
    : undefined;
  const radiiFallback = inlineCornerPx ?? variantSpec?.cornerRadius ?? 0;
  const radii = element.cornerRadii;
  const radiiPx = radii ? {
    topStart:    radii.topStart    != null ? resolveLayoutScalar(radii.topStart,    ff) : undefined,
    topEnd:      radii.topEnd      != null ? resolveLayoutScalar(radii.topEnd,      ff) : undefined,
    bottomStart: radii.bottomStart != null ? resolveLayoutScalar(radii.bottomStart, ff) : undefined,
    bottomEnd:   radii.bottomEnd   != null ? resolveLayoutScalar(radii.bottomEnd,   ff) : undefined,
  } : undefined;
  const hasAnyRadii = radiiPx != null && (
    (radiiPx.topStart ?? 0) !== 0 || (radiiPx.topEnd ?? 0) !== 0 ||
    (radiiPx.bottomStart ?? 0) !== 0 || (radiiPx.bottomEnd ?? 0) !== 0
  );
  if (hasAnyRadii && radiiPx) {
    style.borderTopLeftRadius     = radiiPx.topStart    ?? radiiFallback;
    style.borderTopRightRadius    = radiiPx.topEnd      ?? radiiFallback;
    style.borderBottomRightRadius = radiiPx.bottomEnd   ?? radiiFallback;
    style.borderBottomLeftRadius  = radiiPx.bottomStart ?? radiiFallback;
    style.overflow = 'hidden';
  } else if (inlineCornerPx != null) {
    style.borderRadius = inlineCornerPx;
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

/**
 * Resolve a width / height-style scalar that may be a number, a `token:…`
 * reference, or a constraint primitive (`fill`, `wrap`, `flex`). Tokens
 * resolve to pixels through the form-factor-aware resolver. Numbers and
 * non-token strings pass through unchanged so existing constraint
 * primitives keep their current CSS value (full constraint primitive
 * support is a follow-up; tracked in implementation plan Phase 2).
 */
function resolveLayoutSize(
  value: number | string,
  formFactor: FormFactor,
): number | string {
  if (typeof value === 'number') return value;
  if (value.startsWith('token:')) return resolveLayoutScalar(value, formFactor);
  return value;
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
