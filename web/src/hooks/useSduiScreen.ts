import { useState, useEffect, useCallback } from 'react';
import type React from 'react';
import type { SduiModels } from '@sdui/models';
import { RequestEnvelopeBuilder } from '../request/RequestEnvelopeBuilder';

interface UseSduiScreenOptions {
  /** Server endpoint path, e.g. "/sdui/scoreboard" or "/sdui/game-detail/0042300102?gameState=live" */
  endpoint: string;
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
    try {
      setLoading(true);
      setError(null);

      const builder = new RequestEnvelopeBuilder().experiments(experiments);
      const traceId = RequestEnvelopeBuilder.generateTraceId();

      let response: Response;

      if (builder.exceedsGetThreshold()) {
        // POST fallback for oversized query strings
        const url = `/api${endpoint}`;
        console.log('[SDUI] Fetching (POST fallback):', url);
        response = await fetch(url, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'X-Trace-Id': traceId,
          },
          body: JSON.stringify(builder.buildJsonBody()),
        });
      } else {
        // Standard GET with bracket-notation query params
        const qs = builder.buildQueryString();
        const separator = endpoint.includes('?') ? '&' : '?';
        const url = `/api${endpoint}${separator}${qs}`;
        console.log('[SDUI] Fetching:', url);
        response = await fetch(url, {
          headers: {
            'X-Trace-Id': traceId,
          },
        });
      }

      if (!response.ok) {
        throw new Error(`Failed to fetch: ${response.status} ${response.statusText}`);
      }

      const responseTraceId = response.headers.get('X-Trace-Id');
      const data: SduiModels = await response.json();
      console.log(
        '[SDUI] Received screen:', data.id,
        '| sections:', data.sections?.length,
        '| trace:', responseTraceId ?? data.traceId,
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
