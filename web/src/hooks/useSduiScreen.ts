import { useState, useEffect, useCallback, useRef } from 'react';
import type React from 'react';
import type { SduiModels, Section } from '@sdui/models';
import { fetchSduiScreen } from '../runtime/fetchSduiScreen';

interface UseSduiScreenOptions {
  /** Server endpoint path, e.g. "/v1/sdui/screen/scoreboard". Null skips the fetch. */
  endpoint: string | null;
  /** Experiment assignments from Amplitude (experimentId → variant). */
  experiments?: Record<string, string>;
}

interface UseSduiScreenResult {
  screen: SduiModels | null;
  /** Last successful screen; kept after fetch failures for shell navigation escape. */
  shellScreen: SduiModels | null;
  loading: boolean;
  error: string | null;
  /** True when the server signals the client must update to continue. */
  upgradeRequired: boolean;
  refetch: () => Promise<void>;
  /** Direct setter for surgical section-level updates (e.g. action-triggered refresh). */
  setScreen: React.Dispatch<React.SetStateAction<SduiModels | null>>;
  /**
   * Screen-channel entry point: fetches a full screen from the given endpoint
   * with user-supplied params, validates response.id against the current screen,
   * and applies a strict full replacement on match. Drops with a warning on
   * id mismatch (server contract violation). Resets the screen-level poll timer
   * and stores userParams for replay on subsequent pull-to-refresh / poll ticks.
   */
  replaceCurrentScreen: (endpoint: string, userParams?: Record<string, string>) => Promise<void>;
  /** Merge a replacement section into the current screen by section ID. */
  onSectionReplace: (section: Section) => void;
  /** Remove a section from the current screen by section ID (used when a
   *  section endpoint returns 404 — the section is gone from the feed). */
  onSectionGone: (sectionId: string) => void;
}

/**
 * Hook to fetch and manage an SDUI screen.
 *
 * Sends the full request envelope as bracket-notation query params (D1).
 * Falls back to POST if the query string exceeds 8192 chars.
 */
export function useSduiScreen(options: UseSduiScreenOptions): UseSduiScreenResult {
  const { endpoint, experiments = {} } = options;

  const [screen, setScreen] = useState<SduiModels | null>(null);
  const [shellScreen, setShellScreen] = useState<SduiModels | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [upgradeRequired, setUpgradeRequired] = useState(false);
  const screenIntervalRef = useRef<number | null>(null);
  const userParamsRef = useRef<Record<string, string>>({});
  const screenRef = useRef<SduiModels | null>(null);

  // Keep screenRef in sync for use in callbacks that shouldn't re-create on
  // every screen update (avoids stale closures in replaceCurrentScreen).
  useEffect(() => { screenRef.current = screen; }, [screen]);

  const resetPollTimer = useCallback(() => {
    if (screenIntervalRef.current !== null) {
      clearInterval(screenIntervalRef.current);
      screenIntervalRef.current = null;
    }
  }, []);

  const applyScreen = useCallback((data: SduiModels) => {
    setShellScreen(data);
    setScreen(data);
  }, []);

  const fetchScreen = useCallback(async () => {
    if (!endpoint) return;
    try {
      setLoading(true);
      setError(null);

      const { screen: data, url, method, traceId, versionMismatch } = await fetchSduiScreen({
        endpoint,
        experiments,
        userParams: userParamsRef.current,
      });
      console.log(`[SDUI] Fetching (${method}):`, url);
      console.log(
        '[SDUI] Received screen:', data.id,
        '| sections:', data.sections?.length,
        '| trace:', data.traceId ?? traceId,
      );

      if (versionMismatch === 'upgrade-required') {
        setUpgradeRequired(true);
      }

      applyScreen(data);
      resetPollTimer();
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Unknown error';
      console.error('[SDUI] Error:', message);
      setError(message);
    } finally {
      setLoading(false);
    }
  }, [endpoint, JSON.stringify(experiments), applyScreen, resetPollTimer]);

  useEffect(() => {
    setScreen(null);
    setError(null);
    setUpgradeRequired(false);
    userParamsRef.current = {};
    if (endpoint) {
      setLoading(true);
    }
  }, [endpoint]);

  useEffect(() => {
    fetchScreen();
  }, [fetchScreen]);

  useEffect(() => {
    if (screenIntervalRef.current !== null) {
      clearInterval(screenIntervalRef.current);
      screenIntervalRef.current = null;
    }

    const defaultPolicy = screen?.defaultRefreshPolicy;
    if (!defaultPolicy || defaultPolicy.type !== 'poll' || !defaultPolicy.intervalMs) return;

    const intervalMs = defaultPolicy.intervalMs;
    screenIntervalRef.current = window.setInterval(() => {
      fetchScreen();
    }, intervalMs);

    return () => {
      if (screenIntervalRef.current !== null) {
        clearInterval(screenIntervalRef.current);
        screenIntervalRef.current = null;
      }
    };
  }, [screen?.defaultRefreshPolicy, fetchScreen]);

  /**
   * Screen-channel entry point for action-driven refreshes (parameterized
   * re-composition). Strict same-id full-replace contract: if the response id
   * matches the current screen, the screen is fully replaced. On mismatch the
   * response is dropped with a warning (server contract violation).
   *
   * Stores userParams so subsequent pull-to-refresh and poll ticks carry the
   * user's current parameterization. Resets the screen-level poll timer to
   * avoid a double-fetch one tick later.
   */
  const replaceCurrentScreen = useCallback(async (
    refreshEndpoint: string,
    userParams?: Record<string, string>,
  ): Promise<void> => {
    try {
      const params = userParams ?? {};
      const { screen: data, url, method } = await fetchSduiScreen({
        endpoint: refreshEndpoint,
        experiments,
        userParams: params,
      });

      const currentId = screenRef.current?.id;
      if (currentId && data.id !== currentId) {
        console.warn(
          `[SDUI] replaceCurrentScreen: response id "${data.id}" does not match ` +
          `current screen id "${currentId}" — dropping response (contract violation).`,
        );
        return;
      }

      console.log(
        `[SDUI] replaceCurrentScreen: full replace id=${data.id} (${method}) url=${url}`,
      );

      userParamsRef.current = params;
      applyScreen(data);
      resetPollTimer();
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Unknown error';
      console.error('[SDUI] replaceCurrentScreen failed:', message);
    }
  }, [JSON.stringify(experiments), applyScreen, resetPollTimer]);

  const handleSectionReplace = useCallback((newSection: Section) => {
    setScreen((current) => {
      if (!current) return current;
      const idx = current.sections.findIndex((s) => s.id === newSection.id);
      const sections = [...current.sections];
      if (idx >= 0) {
        sections[idx] = newSection;
      } else {
        sections.push(newSection);
      }
      return { ...current, sections };
    });
  }, []);

  const handleSectionGone = useCallback((sectionId: string) => {
    setScreen((current) => {
      if (!current) return current;
      const idx = current.sections.findIndex((s) => s.id === sectionId);
      if (idx < 0) return current;
      const sections = current.sections.filter((s) => s.id !== sectionId);
      return { ...current, sections };
    });
  }, []);

  return {
    screen,
    shellScreen,
    loading,
    error,
    upgradeRequired,
    refetch: fetchScreen,
    setScreen,
    replaceCurrentScreen,
    onSectionReplace: handleSectionReplace,
    onSectionGone: handleSectionGone,
  };
}
