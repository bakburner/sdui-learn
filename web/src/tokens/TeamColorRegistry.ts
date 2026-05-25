/**
 * TeamColorRegistry — resolves `nba.team.*` tokens locally using the bundled
 * team section from schema/color-tokens.json. No network calls are made.
 */

import teamData from '../../../schema/color-tokens.json';
import { resolveColorToken, type ColorScheme } from '../utils/ColorTokenResolver';

type TeamSection = typeof teamData.team;
type Palettes = TeamSection['palettes'];
type Modes = TeamSection['modes'];
type Semantic = TeamSection['semantic'];

type PaletteRoles = Record<string, string | undefined>;
type ModeValue = string | { ref: string } | { value: string };
type ModeMap = Record<string, ModeValue>;

const palettes: Palettes = teamData.team.palettes;
const modes: Modes = teamData.team.modes;
const semantic: Semantic = teamData.team.semantic;

const knownTeamIds = new Set(Object.keys(palettes));

function isRefValue(v: ModeValue): v is { ref: string } {
  return typeof v === 'object' && 'ref' in v;
}

function isLiteralValue(v: ModeValue): v is { value: string } {
  return typeof v === 'object' && 'value' in v;
}

/**
 * Resolve a team color token to a concrete hex string.
 *
 * @param token  Full token name, e.g. "nba.team.bg"
 * @param teamId Three-letter team abbreviation, e.g. "atl"
 * @param theme  Current color scheme — "light" or "dark"
 * @returns Hex color string, or null if unresolvable.
 */
export function resolveTeamColor(
  token: string,
  teamId: string,
  theme: ColorScheme,
): string | null {
  const semanticEntry = (semantic as Record<string, Record<string, string>>)[token];
  if (!semanticEntry) {
    logMissing(token, teamId);
    return null;
  }

  if (!knownTeamIds.has(teamId)) {
    logMissing(token, teamId);
    return null;
  }

  const modeName = resolveModeName(semanticEntry, theme);
  if (!modeName) {
    logMissing(token, teamId);
    return null;
  }

  const modeMap = (modes as Record<string, ModeMap>)[modeName];
  if (!modeMap) {
    logMissing(token, teamId);
    return null;
  }

  const modeValue: ModeValue | undefined =
    modeMap[teamId] ?? modeMap['_default'];

  if (modeValue === undefined) {
    logMissing(token, teamId);
    return null;
  }

  return resolveModeValue(modeValue, teamId, theme);
}

function resolveModeName(
  semanticEntry: Record<string, string>,
  theme: ColorScheme,
): string | undefined {
  if ('mode' in semanticEntry) {
    return semanticEntry.mode;
  }
  return semanticEntry[theme];
}

function resolveModeValue(
  modeValue: ModeValue,
  teamId: string,
  theme: ColorScheme,
): string | null {
  if (typeof modeValue === 'string') {
    const palette = (palettes as Record<string, PaletteRoles>)[teamId];
    const hex = palette?.[modeValue];
    if (!hex) {
      logMissing(`palette.${modeValue}`, teamId);
      return null;
    }
    return hex;
  }

  if (isLiteralValue(modeValue)) {
    return modeValue.value;
  }

  if (isRefValue(modeValue)) {
    const resolved = resolveColorToken(`token:${modeValue.ref}`, theme);
    return resolved ?? null;
  }

  return null;
}

function logMissing(token: string, teamId: string): void {
  if (typeof console !== 'undefined') {
    console.warn('token_resolver_missing', { token, teamId });
  }
}
