import { describe, it, expect } from 'vitest';
import { readFileSync, readdirSync } from 'node:fs';
import { join, resolve } from 'node:path';
import type { Screen, Section } from '@sdui/models';

/*
 * Schema / fixture drift sentinel.
 *
 * The fixtures in schema/examples/ are the shared wire contract between
 * the server composers and every client. The directory contains two
 * kinds of files:
 *
 *   - Full SDUI screens (discriminated by presence of `schemaVersion`
 *     and `sections`) — e.g. `for-you.json`, `game-detail-live.json`.
 *   - Sub-payloads referenced by a screen's refreshPolicy or composed
 *     into a screen at runtime — e.g. `stream-info.json` (stream list),
 *     `video-highlights.json` (highlight reel data),
 *     `schedule-2024-25.json` (schedule dropdown options),
 *     `action-failure-semantics.json` (documentation-style action examples).
 *
 * This test asserts the bare minimum the web client needs to trust
 * about each group:
 *
 *   1. Every file is syntactically valid JSON.
 *   2. Every file round-trips through JSON.stringify / JSON.parse
 *      without data loss.
 *   3. Files shaped like full screens declare `id`, `schemaVersion`,
 *      and `sections`, and every section in them declares `id` and
 *      `type` (what the SectionRouter assumes).
 *   4. At least one screen-shaped fixture exists (guards against the
 *      directory accidentally going all-sub-payload).
 *
 * TypeScript's structural types don't enforce shape at runtime, so this
 * is intentionally a smoke test — the strict-decode drift tests live on
 * Android (Jackson) and iOS (quicktype Codable). This file exists so
 * that a fixture malformed in a commit fails in CI before it reaches
 * any client.
 */

const FIXTURES_DIR = resolve(__dirname, '../../../schema/examples');

type Fixture = { name: string; raw: string; parsed: unknown };

function loadFixtures(): Fixture[] {
  const files = readdirSync(FIXTURES_DIR).filter((f) => f.endsWith('.json'));
  return files.map((name) => {
    const raw = readFileSync(join(FIXTURES_DIR, name), 'utf-8');
    return { name, raw, parsed: JSON.parse(raw) };
  });
}

function isScreenShaped(value: unknown): value is Partial<Screen> {
  if (value === null || typeof value !== 'object') return false;
  const v = value as Record<string, unknown>;
  return 'schemaVersion' in v && Array.isArray(v.sections);
}

describe('schema/examples round-trip', () => {
  const fixtures = loadFixtures();

  it('discovers fixtures', () => {
    expect(fixtures.length).toBeGreaterThan(0);
  });

  it('contains at least one screen-shaped fixture', () => {
    const screens = fixtures.filter((f) => isScreenShaped(f.parsed));
    expect(screens.length).toBeGreaterThan(0);
  });

  for (const { name, raw, parsed } of fixtures) {
    describe(name, () => {
      it('round-trips through JSON.parse/stringify', () => {
        const restringified = JSON.stringify(parsed);
        const reparsed = JSON.parse(restringified);
        expect(reparsed).toEqual(parsed);
      });

      if (isScreenShaped(parsed)) {
        it('declares top-level screen fields', () => {
          expect(typeof parsed.id).toBe('string');
          expect(typeof parsed.schemaVersion).toBe('string');
          expect(Array.isArray(parsed.sections)).toBe(true);
        });

        it('every section declares id and type', () => {
          const sections: Section[] = (parsed.sections ?? []) as Section[];
          for (const section of sections) {
            expect(typeof section.id).toBe('string');
            expect(section.id.length).toBeGreaterThan(0);
            expect(typeof section.type).toBe('string');
            expect(section.type.length).toBeGreaterThan(0);
          }
        });
      } else {
        it('is a sub-payload (not a full screen)', () => {
          expect(raw.length).toBeGreaterThan(0);
        });
      }
    });
  }
});
