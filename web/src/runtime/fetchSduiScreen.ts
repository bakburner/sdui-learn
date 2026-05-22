import type { SduiModels, Section } from '@sdui/models';
import { RequestEnvelopeBuilder } from '../request/RequestEnvelopeBuilder';
import { SDUI_PATH_PREFIX, API_PROXY_PREFIX } from '../utils/constants';

export interface FetchSduiScreenOptions {
  /** Server-relative path, e.g. `/v1/sdui/screen/scoreboard`. */
  endpoint: string;
  /** Experiment assignments from Amplitude (experimentId → variant). */
  experiments?: Record<string, string>;
  /**
   * Optional user-supplied filter params (e.g. Form submit bindings like
   * `season=2025-26`). Always travel in the URL query string regardless of
   * GET vs POST so the server reads them through the same `@RequestParam`
   * path on either side. They participate in the GET/POST length decision.
   */
  userParams?: Record<string, string>;
  /**
   * Optional trace ID to reuse from a parent fetch (e.g. parameterized
   * refresh inheriting its screen's trace). Falls back to a fresh ID
   * when absent.
   */
  traceId?: string;
  /** Optional bearer token forwarded as the Authorization header. */
  authToken?: string;
}

export interface FetchSduiScreenResult {
  screen: SduiModels;
  /** Trace ID actually used on the wire (echoed by the server when present). */
  traceId: string;
  /** Final URL hit, useful for debugging and contract tests. */
  url: string;
  /** HTTP method used. */
  method: 'GET' | 'POST';
  /**
   * When the server signals that the client's schema version is below the
   * minimum supported, this will be `'upgrade-required'`. Callers should
   * display a platform-appropriate update prompt.
   */
  versionMismatch?: string;
}

export interface FetchSduiSectionOptions {
  /** Server-relative SDUI path, e.g. '/v1/sdui/section/stats-api:game-123::AtomicComposite::scoreboard'. */
  endpoint: string;
  /** Experiment assignments from Amplitude (experimentId → variant). */
  experiments?: Record<string, string>;
  /**
   * Optional trace ID to reuse from a parent fetch. Falls back to a fresh ID
   * when absent.
   */
  traceId?: string;
  /** Optional bearer token forwarded as the Authorization header. */
  authToken?: string;
}

/**
 * Single SDUI fetch primitive shared by initial loads (`useSduiScreen`) and
 * action-driven parameterized refreshes (`ActionHandler.handleRefresh`).
 *
 * Transport contract — applied identically for both call sites:
 *  - The request envelope (platform, locale, device, experiments) is built
 *    via `RequestEnvelopeBuilder` and serialized as bracket-notation query
 *    params (GET) or a JSON body of the same shape (POST).
 *  - GET is the default; POST is used only when the envelope query string
 *    exceeds 8192 chars (`exceedsGetThreshold`).
 *  - User-supplied filter params (refresh `paramBindings`) ride the URL
 *    query string regardless of HTTP method, RFC-3986 percent-encoded.
 *  - `X-Trace-Id` is sent on every request and reused from the parent
 *    screen's trace when supplied, so paginated/refreshed flows correlate.
 */
export async function fetchSduiScreen(
  options: FetchSduiScreenOptions,
): Promise<FetchSduiScreenResult> {
  const { endpoint, experiments = {}, userParams = {}, traceId: parentTraceId, authToken } = options;

  const builder = new RequestEnvelopeBuilder().experiments(experiments);
  const traceId = parentTraceId ?? RequestEnvelopeBuilder.generateTraceId();
  const requestId = crypto.randomUUID();

  const userQuery = encodeUserParams(userParams);
  const apiPath = endpoint.startsWith(SDUI_PATH_PREFIX) ? `${API_PROXY_PREFIX}${endpoint}` : endpoint;

  let response: Response;
  let url: string;
  let method: 'GET' | 'POST';

  // Threshold includes both halves so large userParams trigger POST too.
  const envelopeQuery = builder.buildQueryString();
  const combinedLength = envelopeQuery.length + (userQuery ? userQuery.length + 1 : 0);

  if (combinedLength > 8192) {
    method = 'POST';
    url = userQuery ? `${apiPath}?${userQuery}` : apiPath;
    response = await fetch(url, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'X-Trace-Id': traceId,
        'X-Request-Id': requestId,
        ...(builder.getDeviceId() ? { 'X-Device-Id': builder.getDeviceId()! } : {}),
        'X-Platform': builder.getPlatformName(),
        ...(builder.getAppVersion() ? { 'X-App-Version': builder.getAppVersion()! } : {}),
        ...(builder.getOsVersion() ? { 'X-OS-Version': builder.getOsVersion()! } : {}),
        // TODO(edge): placeholder — edge worker will set these from client IP
        'X-Resolved-Country': 'US',
        'X-Resolved-Market-Cohort': 'MARKET_UNKNOWN',
        ...(authToken ? { 'Authorization': `Bearer ${authToken}` } : {}),
      },
      body: JSON.stringify(builder.buildJsonBody()),
    });
  } else {
    method = 'GET';
    const combined = userQuery ? `${userQuery}&${envelopeQuery}` : envelopeQuery;
    const separator = apiPath.includes('?') ? '&' : '?';
    url = `${apiPath}${separator}${combined}`;
    response = await fetch(url, {
      headers: {
        'X-Trace-Id': traceId,
        'X-Request-Id': requestId,
        ...(builder.getDeviceId() ? { 'X-Device-Id': builder.getDeviceId()! } : {}),
        'X-Platform': builder.getPlatformName(),
        ...(builder.getAppVersion() ? { 'X-App-Version': builder.getAppVersion()! } : {}),
        ...(builder.getOsVersion() ? { 'X-OS-Version': builder.getOsVersion()! } : {}),
        // TODO(edge): placeholder — edge worker will set these from client IP
        'X-Resolved-Country': 'US',
        'X-Resolved-Market-Cohort': 'MARKET_UNKNOWN',
        ...(authToken ? { 'Authorization': `Bearer ${authToken}` } : {}),
      },
    });
  }

  if (!response.ok) {
    throw new Error(`Failed to fetch: ${response.status} ${response.statusText}`);
  }

  const versionMismatch = response.headers.get('X-Schema-Version-Mismatch') ?? undefined;
  if (versionMismatch) {
    console.warn(`[SDUI] Schema version mismatch: ${versionMismatch}. Client update required.`);
  }

  const screen: SduiModels = await response.json();
  return { screen, traceId, url, method, versionMismatch };
}

export async function fetchSduiSection(options: FetchSduiSectionOptions): Promise<Section> {
  const { endpoint, experiments = {}, traceId: parentTraceId, authToken } = options;

  const builder = new RequestEnvelopeBuilder().experiments(experiments);
  const traceId = parentTraceId ?? RequestEnvelopeBuilder.generateTraceId();
  const requestId = crypto.randomUUID();
  const apiPath = endpoint.startsWith(SDUI_PATH_PREFIX) ? `${API_PROXY_PREFIX}${endpoint}` : endpoint;
  const envelopeQuery = builder.buildQueryString();

  let response: Response;

  if (envelopeQuery.length > 8192) {
    response = await fetch(apiPath, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'X-Trace-Id': traceId,
        'X-Request-Id': requestId,
        ...(builder.getDeviceId() ? { 'X-Device-Id': builder.getDeviceId()! } : {}),
        'X-Platform': builder.getPlatformName(),
        ...(builder.getAppVersion() ? { 'X-App-Version': builder.getAppVersion()! } : {}),
        ...(builder.getOsVersion() ? { 'X-OS-Version': builder.getOsVersion()! } : {}),
        // TODO(edge): placeholder — edge worker will set these from client IP
        'X-Resolved-Country': 'US',
        'X-Resolved-Market-Cohort': 'MARKET_UNKNOWN',
        ...(authToken ? { 'Authorization': `Bearer ${authToken}` } : {}),
      },
      body: JSON.stringify(builder.buildJsonBody()),
    });
  } else {
    const separator = apiPath.includes('?') ? '&' : '?';
    const url = `${apiPath}${separator}${envelopeQuery}`;
    response = await fetch(url, {
      headers: {
        'X-Trace-Id': traceId,
        'X-Request-Id': requestId,
        ...(builder.getDeviceId() ? { 'X-Device-Id': builder.getDeviceId()! } : {}),
        'X-Platform': builder.getPlatformName(),
        ...(builder.getAppVersion() ? { 'X-App-Version': builder.getAppVersion()! } : {}),
        ...(builder.getOsVersion() ? { 'X-OS-Version': builder.getOsVersion()! } : {}),
        // TODO(edge): placeholder — edge worker will set these from client IP
        'X-Resolved-Country': 'US',
        'X-Resolved-Market-Cohort': 'MARKET_UNKNOWN',
        ...(authToken ? { 'Authorization': `Bearer ${authToken}` } : {}),
      },
    });
  }

  if (response.status === 404) {
    throw new SectionNotFoundError(`Section not found: ${endpoint}`);
  }
  if (!response.ok) {
    throw new Error(`Failed to fetch section: ${response.status} ${response.statusText}`);
  }

  const versionMismatch = response.headers.get('X-Schema-Version-Mismatch');
  if (versionMismatch === 'upgrade-required') {
    console.warn(`[SDUI] Schema version mismatch on section fetch: upgrade-required`);
    throw new SchemaVersionMismatchError('Client schema version is no longer supported. Please update the app.');
  }

  const section: Section = await response.json();
  return section;
}

export class SectionNotFoundError extends Error {
  constructor(message: string) {
    super(message);
    this.name = 'SectionNotFoundError';
  }
}

export class SchemaVersionMismatchError extends Error {
  constructor(message: string) {
    super(message);
    this.name = 'SchemaVersionMismatchError';
  }
}

/**
 * Percent-encode user params with the same rule the envelope uses, sorted
 * by key so the encoded URL is deterministic across calls (matters for
 * parity tests and CDN cache keys).
 */
function encodeUserParams(params: Record<string, string>): string {
  const entries = Object.entries(params).filter(([, v]) => v !== '' && v !== undefined && v !== null);
  if (entries.length === 0) return '';
  entries.sort(([a], [b]) => (a < b ? -1 : a > b ? 1 : 0));
  return entries
    .map(([k, v]) => `${encodeURIComponent(k)}=${encodeURIComponent(String(v))}`)
    .join('&');
}
