import { describe, expect, it, vi, beforeEach } from 'vitest';
import { resolveTeamColor } from './TeamColorRegistry';

describe('TeamColorRegistry', () => {
  beforeEach(() => {
    vi.restoreAllMocks();
  });

  it('resolves nba.team.bg for atl → primary (#C8102E)', () => {
    const result = resolveTeamColor('nba.team.bg', 'atl', 'light');
    expect(result).toBe('#C8102E');
  });

  it('resolves nba.team.bg for bkn → secondary override (#707271)', () => {
    const result = resolveTeamColor('nba.team.bg', 'bkn', 'light');
    expect(result).toBe('#707271');
  });

  it('resolves nba.team.bg for sas → tertiary override (#4A4A4A)', () => {
    const result = resolveTeamColor('nba.team.bg', 'sas', 'dark');
    expect(result).toBe('#4A4A4A');
  });

  it('resolves nba.team.accent for sac in dark mode → literal hex (#BEC9CF)', () => {
    const result = resolveTeamColor('nba.team.accent', 'sac', 'dark');
    expect(result).toBe('#BEC9CF');
  });

  it('resolves nba.team.accent-label for ind in dark mode via nba.color.primary.10', () => {
    const result = resolveTeamColor('nba.team.accent-label', 'ind', 'dark');
    // nba.color.primary.10 → nba.color.grey.10 → #191C23
    expect(result).toBe('#191C23');
  });

  it('returns null and logs warning for unknown team', () => {
    const warnSpy = vi.spyOn(console, 'warn').mockImplementation(() => {});
    const result = resolveTeamColor('nba.team.bg', 'zzz', 'light');
    expect(result).toBeNull();
    expect(warnSpy).toHaveBeenCalledWith('token_resolver_missing', expect.objectContaining({ teamId: 'zzz' }));
  });

  it('makes NO network/fetch calls during resolution', () => {
    const fetchSpy = vi.spyOn(globalThis, 'fetch').mockImplementation(() => {
      throw new Error('Network call detected — resolution must be local-only');
    });

    resolveTeamColor('nba.team.bg', 'atl', 'light');
    resolveTeamColor('nba.team.accent', 'sac', 'dark');
    resolveTeamColor('nba.team.accent-label', 'ind', 'dark');
    resolveTeamColor('nba.team.bg', 'bkn', 'dark');

    expect(fetchSpy).not.toHaveBeenCalled();
  });
});
