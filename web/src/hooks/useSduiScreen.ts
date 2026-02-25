import { useState, useEffect, useCallback } from 'react';
import type { SduiModels } from '@sdui/models';

interface UseSduiScreenOptions {
  screenType: 'scoreboard' | 'game-detail';
  gameId?: string;
  gameState?: 'pre' | 'live' | 'final';
  variant?: string;
}

interface UseSduiScreenResult {
  screen: SduiModels | null;
  loading: boolean;
  error: string | null;
  refetch: () => Promise<void>;
}

/**
 * Hook to fetch and manage an SDUI screen.
 * Fetches from the composition service through the proxy.
 */
export function useSduiScreen(options: UseSduiScreenOptions): UseSduiScreenResult {
  const { screenType, gameId, gameState = 'live', variant = 'A' } = options;

  const [screen, setScreen] = useState<SduiModels | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchScreen = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);

      const url = screenType === 'scoreboard'
        ? `/api/sdui/scoreboard?variant=${variant}`
        : `/api/sdui/game-detail/${gameId ?? ''}?gameState=${gameState}&variant=${variant}`;
      console.log('[SDUI] Fetching:', url);

      const response = await fetch(url, {
        headers: { 'X-Schema-Version': '1.0' },
      });

      if (!response.ok) {
        throw new Error(`Failed to fetch: ${response.status} ${response.statusText}`);
      }

      const traceId = response.headers.get('X-Trace-Id');
      const data: SduiModels = await response.json();
      console.log(
        '[SDUI] Received screen:', data.id,
        '| sections:', data.sections?.length,
        '| trace:', traceId ?? data.traceId,
      );

      setScreen(data);
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Unknown error';
      console.error('[SDUI] Error:', message);
      setError(message);
    } finally {
      setLoading(false);
    }
  }, [screenType, gameId, gameState, variant]);

  useEffect(() => {
    fetchScreen();
  }, [fetchScreen]);

  return { screen, loading, error, refetch: fetchScreen };
}
