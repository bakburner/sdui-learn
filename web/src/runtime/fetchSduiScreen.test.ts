import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { fetchSduiScreen } from './fetchSduiScreen';
import { RequestEnvelopeBuilder } from '../request/RequestEnvelopeBuilder';

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

  it('routes /sdui/* through the /api proxy and percent-encodes user params', async () => {
    await fetchSduiScreen({
      endpoint: '/sdui/refresh/stats-leaders',
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
    expect(req.url.startsWith('/api/sdui/refresh/stats-leaders?')).toBe(true);

    const query = req.url.split('?')[1];
    expect(query).toContain('perMode=Totals');
    expect(query).toContain('season=2025-26');
    expect(query).toContain('seasonType=Regular%20Season');
    expect(query).toContain('platform%5Bname%5D=web');
    expect(query).toContain('locale=en');

    expect(req.headers['x-trace-id']).toBe('trace-parent');
  });

  it('sorts user params deterministically by key', async () => {
    await fetchSduiScreen({
      endpoint: '/sdui/refresh/x',
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
    await fetchSduiScreen({ endpoint: '/sdui/scoreboard' });
    const screenQuery = captured[0].url.split('?')[1];

    captured.length = 0;
    await fetchSduiScreen({ endpoint: '/sdui/scoreboard', userParams: { k: 'v' } });
    const refreshQuery = captured[0].url.split('?')[1];

    expect(refreshQuery.startsWith('k=v&')).toBe(true);
    expect(refreshQuery.slice('k=v&'.length)).toBe(screenQuery);
  });
});

describe('fetchSduiScreen — formFactor in request envelope', () => {
  let captured: CapturedRequest[];

  beforeEach(() => {
    captured = [];
    installFetchStub(captured);
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('emits platform[formFactor]=web.wide on GET when viewport is wide', async () => {
    stubMatchMedia(true); // (min-width: 768px) matches → web.wide
    await fetchSduiScreen({ endpoint: '/sdui/scoreboard' });

    const req = captured[0];
    expect(req.method).toBe('GET');

    const query = req.url.split('?')[1] ?? '';
    expect(query).toContain('platform%5BformFactor%5D=web.wide');

    // formFactor must follow the capabilities block — same byte ordering
    // as iOS / Android so the CDN cache key collides across platforms.
    const sse = query.indexOf('platform%5Bcapabilities%5D%5Bsse%5D=');
    const ff = query.indexOf('platform%5BformFactor%5D=');
    expect(sse).toBeGreaterThanOrEqual(0);
    expect(ff).toBeGreaterThan(sse);
  });

  it('emits platform[formFactor]=web.narrow on GET when viewport is narrow', async () => {
    stubMatchMedia(false); // (min-width: 768px) does not match → web.narrow
    await fetchSduiScreen({ endpoint: '/sdui/scoreboard' });

    const query = captured[0].url.split('?')[1] ?? '';
    expect(query).toContain('platform%5BformFactor%5D=web.narrow');
  });

  it('includes platform.formFactor in JSON body on POST fallback', async () => {
    stubMatchMedia(true); // web.wide

    // Force POST by stuffing experiments past the 8192-char threshold.
    const bigExperiments: Record<string, string> = {};
    for (let i = 0; i < 600; i++) {
      bigExperiments[`exp_${i.toString().padStart(4, '0')}`] = 'variant_xyz_aaaaaaaaaaaaa';
    }

    await fetchSduiScreen({ endpoint: '/sdui/scoreboard', experiments: bigExperiments });
    const req = captured[0];
    expect(req.method).toBe('POST');
    expect(req.body).toBeDefined();

    const body = JSON.parse(req.body!) as { platform?: { formFactor?: string } };
    expect(body.platform?.formFactor).toBe('web.wide');
  });

  it('builder.formFactor() override wins over the matchMedia-derived default', () => {
    stubMatchMedia(true); // would default to web.wide
    const qs = new RequestEnvelopeBuilder().formFactor('tablet').buildQueryString();
    expect(qs).toContain('platform%5BformFactor%5D=tablet');
    expect(qs).not.toContain('platform%5BformFactor%5D=web.wide');
  });
});
