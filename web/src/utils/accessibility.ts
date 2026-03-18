import type { AccessibilityProperties } from '@sdui/models';

/**
 * Converts server-provided {@link AccessibilityProperties} into a flat
 * record of ARIA / HTML attributes that can be spread onto a JSX element.
 *
 * Returns an empty object when `a11y` is undefined so callers can always spread.
 */
export function accessibilityProps(
  a11y: AccessibilityProperties | undefined,
): Record<string, string | number | boolean | undefined> {
  if (!a11y) return {};

  if (a11y.hidden) {
    return { 'aria-hidden': true, tabIndex: -1 };
  }

  const props: Record<string, string | number | boolean | undefined> = {};

  if (a11y.label) props['aria-label'] = a11y.label;

  if (a11y.role && a11y.role !== 'none') {
    props['role'] = a11y.role;
  }
  if (a11y.role === 'none') {
    props['role'] = 'presentation';
  }
  if (a11y.role === 'heading' && a11y.headingLevel) {
    props['role'] = 'heading';
    props['aria-level'] = a11y.headingLevel;
  }

  if (a11y.liveRegion && a11y.liveRegion !== 'off') {
    props['aria-live'] = a11y.liveRegion as string;
  }

  if (a11y.hint) props['aria-description'] = a11y.hint;

  return props;
}
