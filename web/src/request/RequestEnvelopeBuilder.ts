/**
 * Builds the SDUI request envelope as bracket-notation query parameters.
 *
 * All composition context travels as query params per plan-request-transport.md D1/D3.
 * If the resulting query string exceeds MAX_QUERY_LENGTH, callers should switch to POST.
 *
 * @example
 * ```ts
 * const builder = new RequestEnvelopeBuilder()
 *   .locale('en')
 *   .experiment('gd_tab_order_v2', 'variant_b');
 *
 * const qs = builder.buildQueryString();
 * // "locale=en&schemaVersion=1.0&platform%5Bname%5D=web&..."
 * ```
 */

const MAX_QUERY_LENGTH = 8192;

interface Platform {
  name: string;
  appVersion?: string;
  osVersion?: string;
  deviceClass: string;
  // TODO(platform-tier): replace per-boolean capability flags with a single
  // server-defined platform tier string (e.g. "tier:full") to reduce CDN
  // cache-key fragmentation. Tier resolution at edge or in client.
  capabilities: {
    sse: boolean;
    onFocus?: boolean;
  };
}

interface Device {
  deviceId?: string;
}

export class RequestEnvelopeBuilder {
  private _locale = 'en';
  private _schemaVersion = '1.0';
  private _platform: Platform = {
    name: 'web',
    deviceClass: 'web',
    capabilities: { sse: true, onFocus: true },
  };
  private _device: Device = {};
  private _experiments: Record<string, string> = {};

  locale(locale: string): this {
    this._locale = locale;
    return this;
  }

  schemaVersion(version: string): this {
    this._schemaVersion = version;
    return this;
  }

  platformName(name: string): this {
    this._platform.name = name;
    return this;
  }

  appVersion(version: string): this {
    this._platform.appVersion = version;
    return this;
  }

  deviceClass(cls: string): this {
    this._platform.deviceClass = cls;
    return this;
  }

  osVersion(version: string): this {
    this._platform.osVersion = version;
    return this;
  }

  sseCapable(capable: boolean): this {
    this._platform.capabilities.sse = capable;
    return this;
  }

  onFocusCapable(capable: boolean): this {
    this._platform.capabilities.onFocus = capable;
    return this;
  }

  deviceId(id: string): this {
    this._device.deviceId = id;
    return this;
  }

  experiment(id: string, variant: string): this {
    this._experiments[id] = variant;
    return this;
  }

  experiments(map: Record<string, string>): this {
    Object.assign(this._experiments, map);
    return this;
  }

  /**
   * Build the query string in bracket notation.
   * Does NOT include the leading '?'.
   */
  buildQueryString(): string {
    const params: [string, string][] = [];

    // Top-level scalars
    params.push(['locale', this._locale]);
    params.push(['schemaVersion', this._schemaVersion]);

    // Platform (nested)
    params.push(['platform[deviceClass]', this._platform.deviceClass]);
    params.push(['platform[capabilities][sse]', String(this._platform.capabilities.sse)]);
    if (this._platform.capabilities.onFocus) {
      params.push(['platform[capabilities][onFocus]', 'true']);
    }

    // Device (nested, all optional)
    // deviceId travels as X-Device-Id header, not in the envelope query.

    // Experiments (nested map, sorted for deterministic CDN cache keys)
    for (const [id, variant] of Object.entries(this._experiments).sort(([a], [b]) => a < b ? -1 : a > b ? 1 : 0)) {
      params.push([`experiments[${id}]`, variant]);
    }

    return params
      .map(([key, value]) => `${encodeURIComponent(key)}=${encodeURIComponent(value)}`)
      .join('&');
  }

  /**
   * Whether the query string exceeds the safe GET threshold (8192 chars).
   */
  exceedsGetThreshold(): boolean {
    return this.buildQueryString().length > MAX_QUERY_LENGTH;
  }

  /**
   * Build the envelope as a JSON object for POST fallback.
   */
  buildJsonBody(): Record<string, unknown> {
    const platform = {
      deviceClass: this._platform.deviceClass,
      capabilities: this._platform.capabilities,
    };
    const body: Record<string, unknown> = {
      locale: this._locale,
      schemaVersion: this._schemaVersion,
      platform,
    };
    // deviceId travels as X-Device-Id header; strip it from the body.
    const { deviceId: _stripped, ...deviceWithoutId } = this._device;
    if (Object.keys(deviceWithoutId).length > 0) body.device = deviceWithoutId;
    if (Object.keys(this._experiments).length > 0) body.experiments = this._experiments;
    return body;
  }

  /**
   * Generate a trace ID for this request.
   */
  static generateTraceId(): string {
    return `trace-${crypto.randomUUID().substring(0, 8)}`;
  }

  /** Returns the deviceId for use as the X-Device-Id header value. */
  getDeviceId(): string | undefined {
    return this._device.deviceId;
  }

  getPlatformName(): string {
    return this._platform.name;
  }

  getAppVersion(): string | undefined {
    return this._platform.appVersion;
  }

  getOsVersion(): string | undefined {
    return this._platform.osVersion;
  }
}
