import { useState, useEffect, useCallback } from 'react';
import type React from 'react';
import type { SduiModels } from '@sdui/models';
import { fetchSduiScreen } from '../runtime/fetchSduiScreen';

interface UseSduiScreenOptions {
  /** Server endpoint path, e.g. "/v1/sdui/scoreboard". Null skips the fetch. */
  endpoint: string | null;
  /** Experiment assignments from Amplitude (experimentId → variant). */
  experiments?: Record<string, string>;
}

interface UseSduiScreenResult {
  screen: SduiModels | null;
  loading: boolean;
  error: string | null;
  refetch: () => Promise<void>;
  /** Direct setter for surgical section-level updates (e.g. action-triggered refresh). */
  setScreen: React.Dispatch<React.SetStateAction<SduiModels | null>>;
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
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchScreen = useCallback(async () => {
    if (!endpoint) return;
    try {
      setLoading(true);
      setError(null);

      const { screen: data, url, method, traceId } = await fetchSduiScreen({ endpoint, experiments });
      console.log(`[SDUI] Fetching (${method}):`, url);
      console.log(
        '[SDUI] Received screen:', data.id,
        '| sections:', data.sections?.length,
        '| trace:', data.traceId ?? traceId,
      );

      setScreen(data);
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Unknown error';
      console.error('[SDUI] Error:', message);
      setError(message);
    } finally {
      setLoading(false);
    }
  }, [endpoint, JSON.stringify(experiments)]);

  useEffect(() => {
    fetchScreen();
  }, [fetchScreen]);

  return { screen, loading, error, refetch: fetchScreen, setScreen };
}
