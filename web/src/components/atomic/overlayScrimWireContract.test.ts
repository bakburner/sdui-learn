import { describe, it, expect } from 'vitest';

/**
 * Mirrors `AtomicCompositeBuilderFeedModulesTest#assertOverlayImageStacksHaveReadableTextBacking`:
 * for Image-based OverlayContainer, any Text in an overlay must sit above a solid fill or
 * a gradient whose end stop references overlay.scrim (or hex scrim in fixtures).
 */
type Json = Record<string, unknown> | unknown[] | string | number | boolean | null;

const SCRIM_END_HINTS = ['overlay.scrim', 'token:color.overlay.scrim', '#000000', '#000000CC'];

function walk(node: Json | undefined, visit: (o: Record<string, unknown>) => void): void {
  if (node == null) return;
  if (Array.isArray(node)) {
    for (const c of node) walk(c as Json, visit);
    return;
  }
  if (typeof node === 'object') {
    const o = node as Record<string, unknown>;
    visit(o);
    for (const v of Object.values(o)) walk(v as Json, visit);
  }
}

function collectOverlayContainers(node: Json | undefined, out: Record<string, unknown>[]): void {
  walk(node, o => {
    if (o.type === 'OverlayContainer') out.push(o);
  });
}

function collectType(
  node: Json | undefined,
  type: string,
  out: Record<string, unknown>[],
): void {
  walk(node, o => {
    if (o.type === type) out.push(o);
  });
}

/** Stack order: root … leaf (text). */
function findPathToText(
  current: Json,
  target: Record<string, unknown>,
  path: Record<string, unknown>[],
): boolean {
  if (current === null || current === undefined) return false;
  if (Array.isArray(current)) {
    for (const c of current) {
      if (findPathToText(c as Json, target, path)) {
        return true;
      }
    }
    return false;
  }
  if (typeof current === 'object') {
    const o = current as Record<string, unknown>;
    path.push(o);
    if (o === target) {
      return true;
    }
    for (const v of Object.values(o)) {
      if (findPathToText(v as Json, target, path)) {
        return true;
      }
    }
    path.pop();
  }
  return false;
}

function nodeProvidesTextContrast(n: Record<string, unknown>): boolean {
  const bg = n.background;
  if (bg == null) return false;
  if (typeof bg === 'string') {
    return bg.length > 0;
  }
  if (typeof bg === 'object' && bg != null) {
    const b = bg as { colors?: unknown[] };
    const colors = b.colors;
    if (Array.isArray(colors) && colors.length >= 2) {
      const last = String(colors[colors.length - 1] ?? '');
      if (last.includes('overlay.scrim')) return true;
      if (SCRIM_END_HINTS.some(h => last.includes(h))) return true;
    }
  }
  return false;
}

function textHasContrastAncestor(overlayRoot: Json, textNode: Record<string, unknown>): boolean {
  const path: Record<string, unknown>[] = [];
  if (!findPathToText(overlayRoot, textNode, path) || path.length < 2) {
    return false;
  }
  // path ends with `textNode`; any ancestor (excluding the text) may provide scrim.
  for (let i = 0; i < path.length - 1; i++) {
    if (nodeProvidesTextContrast(path[i])) {
      return true;
    }
  }
  return false;
}

function assertImageOverlayTextHasScrimOrSolid(ui: Json | undefined): void {
  const ocs: Record<string, unknown>[] = [];
  collectOverlayContainers(ui, ocs);
  for (const oc of ocs) {
    if ((oc.base as { type?: string } | undefined)?.type !== 'Image') {
      continue;
    }
    const layers = oc.overlays;
    if (!Array.isArray(layers)) continue;
    for (const layer of layers) {
      const el = (layer as { element?: unknown }).element;
      const texts: Record<string, unknown>[] = [];
      collectType(el as Json, 'Text', texts);
      for (const t of texts) {
        expect(textHasContrastAncestor(el as Json, t)).toBe(true);
      }
    }
  }
}

describe('Overlay scrim — wire contract (static shapes)', () => {
  it('passes when copy Text is a descendant of the same overlay element that carries the scrim', () => {
    const ui = {
      type: 'ScrollContainer',
      children: [
        {
          type: 'OverlayContainer',
          base: { type: 'Image', src: 'https://example.com/h.jpg' },
          overlays: [
            {
              alignment: 'bottomStart',
              element: {
                type: 'Container',
                fillWidth: true,
                background: { colors: ['#00000000', '#000000CC'], direction: 'vertical' },
                children: [
                  { type: 'Text', content: 'Title', variant: 'bodyMedium' },
                ],
              },
            },
          ],
        },
      ],
    };
    expect(() => assertImageOverlayTextHasScrimOrSolid(ui)).not.toThrow();
  });

  it('passes for media overlay using token:color.overlay.scrim end stop', () => {
    const ui = {
      type: 'OverlayContainer',
      base: { type: 'Image', src: 'https://example.com/m.jpg' },
      overlays: [
        {
          alignment: 'bottomStart',
          element: {
            type: 'Container',
            direction: 'column',
            children: [
              { type: 'Text', content: 'Head', variant: 'titleMedium' },
            ],
            background: {
              colors: ['#00000000', 'token:color.overlay.scrim'],
              direction: 'vertical',
            },
          },
        },
      ],
    };
    expect(() => assertImageOverlayTextHasScrimOrSolid(ui)).not.toThrow();
  });

  it('fails when text sits in overlay with no scrim or solid under it', () => {
    const ui = {
      type: 'OverlayContainer',
      base: { type: 'Image', src: 'https://example.com/bad.jpg' },
      overlays: [
        {
          alignment: 'bottomStart',
          element: { type: 'Text', content: 'No backing', variant: 'bodyMedium' },
        },
      ],
    };
    expect(() => assertImageOverlayTextHasScrimOrSolid(ui)).toThrow();
  });
});
