import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { fetchSduiScreen } from './fetchSduiScreen';

/**
 * Contract tests asserting that parameterized refresh produces the *same*
 * encoded URL shape as a normal screen fetch — i.e. that refresh routes
 * through the canonical envelope transport rather than any bespoke URL
 * builder.
 *
 * The systemic regression these tests guard against:
 * Web's `ActionHandler.handleRefresh` used to assemble URLs by hand from
 * `URLSearchParams` and skip the envelope, the GET/POST length fallback,
 * and `X-Trace-Id` propagation. All of those invariants live in
 * `fetchSduiScreen`, so the only safe design is to route refresh through
 * the same primitive.
 */

interface CapturedRequest {
  url: string;
  method: string;
  headers: Record<string, string>;
  body: string | undefined;
}

const EMPTY_SCREEN = { id: 'stats-leaders', schemaVersion: '1.0', sections: [] };

function installFetchStub(captured: CapturedRequest[]): void {
  vi.stubGlobal(
    'fetch',
    vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
      captured.push({
        url: typeof input === 'string' ? input : input.toString(),
        method: init?.method ?? 'GET',
        headers: normalizeHeaders(init?.headers),
        body: typeof init?.body === 'string' ? init.body : undefined,
      });
      return new Response(JSON.stringify(EMPTY_SCREEN), {
        status: 200,
        headers: { 'Content-Type': 'application/json' },
      });
    }),
  );
}

function normalizeHeaders(headers: HeadersInit | undefined): Record<string, string> {
  if (!headers) return {};
  if (headers instanceof Headers) {
    const out: Record<string, string> = {};
    headers.forEach((v, k) => {
      out[k.toLowerCase()] = v;
    });
    return out;
  }
  if (Array.isArray(headers)) {
    return Object.fromEntries(headers.map(([k, v]) => [k.toLowerCase(), v]));
  }
  return Object.fromEntries(Object.entries(headers).map(([k, v]) => [k.toLowerCase(), String(v)]));
}

/**
 * Deterministic form-factor stub. jsdom does not implement `matchMedia`, so
 * `currentFormFactor()` falls back to `'web.wide'`. Tests still pin the
 * stub explicitly so a future jsdom upgrade or test runner doesn't flip
 * the asserted form factor underneath us.
 */
function stubMatchMedia(matches: boolean): void {
  vi.stubGlobal(
    'matchMedia',
    vi.fn((query: string) => ({
      matches,
      media: query,
      onchange: null,
      addListener: () => {},
      removeListener: () => {},
      addEventListener: () => {},
      removeEventListener: () => {},
      dispatchEvent: () => false,
    })),
  );
}

describe('fetchSduiScreen — parameterized refresh transport', () => {
  let captured: CapturedRequest[];

  beforeEach(() => {
    captured = [];
    installFetchStub(captured);
    stubMatchMedia(true); // (min-width: 768px) → web.wide
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('routes /v1/sdui/* through the /api proxy and percent-encodes user params', async () => {
    await fetchSduiScreen({
      endpoint: '/v1/sdui/refresh/stats-leaders',
      userParams: {
        perMode: 'Totals',
        season: '2025-26',
        seasonType: 'Regular Season',
      },
      traceId: 'trace-parent',
    });

    expect(captured).toHaveLength(1);
    const req = captured[0];
    expect(req.method).toBe('GET');
    expect(req.url.startsWith('/api/v1/sdui/refresh/stats-leaders?')).toBe(true);

    const query = req.url.split('?')[1];
    expect(query).toContain('perMode=Totals');
    expect(query).toContain('season=2025-26');
    expect(query).toContain('seasonType=Regular%20Season');
    expect(query).toContain('locale=en');

    expect(req.headers['x-trace-id']).toBe('trace-parent');
    expect(req.headers['x-platform']).toBe('web');
    expect(req.headers['x-resolved-country']).toBe('US');
    expect(req.headers['x-resolved-market-cohort']).toBe('MARKET_UNKNOWN');
  });

  it('sorts user params deterministically by key', async () => {
    await fetchSduiScreen({
      endpoint: '/v1/sdui/screen/refresh/x',
      userParams: { zKey: 'z', aKey: 'a', mKey: 'm' },
    });

    const query = captured[0].url.split('?')[1];
    const a = query.indexOf('aKey=a');
    const m = query.indexOf('mKey=m');
    const z = query.indexOf('zKey=z');
    expect(a).toBeGreaterThanOrEqual(0);
    expect(m).toBeGreaterThan(a);
    expect(z).toBeGreaterThan(m);
  });

  it('refresh and screen fetch produce the same encoded shape modulo user params', async () => {
    await fetchSduiScreen({ endpoint: '/v1/sdui/screen/scoreboard' });
    const screenQuery = captured[0].url.split('?')[1];

    captured.length = 0;
    await fetchSduiScreen({ endpoint: '/v1/sdui/screen/scoreboard', userParams: { k: 'v' } });
    const refreshQuery = captured[0].url.split('?')[1];

    expect(refreshQuery.startsWith('k=v&')).toBe(true);
    expect(refreshQuery.slice('k=v&'.length)).toBe(screenQuery);
  });
});
