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

import { currentFormFactor } from '../utils/LayoutTokenResolver';

const MAX_QUERY_LENGTH = 8192;

interface Platform {
  name: string;
  appVersion?: string;
  osVersion?: string;
  deviceClass: string;
  capabilities: {
    sse: boolean;
    onFocus?: boolean;
  };
  /**
   * Form factor for layout-token resolution. Sent on every request so the
   * server can route to a form-factor-aware composer (Phase 3 of the SDUI
   * implementation plan). Matches the iOS / Android wire field name and
   * ordering for byte parity.
   */
  formFactor: string;
}

interface Device {
  deviceId?: string;
  zipCode?: string;
  countryCode?: string;
  region?: string;
}

export class RequestEnvelopeBuilder {
  private _locale = 'en';
  private _schemaVersion = '1.0';
  private _gameState: string | undefined;
  private _platform: Platform = {
    name: 'web',
    deviceClass: 'web',
    capabilities: { sse: true },
    formFactor: currentFormFactor(),
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

  gameState(state: string | undefined): this {
    this._gameState = state;
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

  sseCapable(capable: boolean): this {
    this._platform.capabilities.sse = capable;
    return this;
  }

  formFactor(value: string): this {
    this._platform.formFactor = value;
    return this;
  }

  countryCode(code: string): this {
    this._device.countryCode = code;
    return this;
  }

  region(region: string): this {
    this._device.region = region;
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
    if (this._gameState) {
      params.push(['gameState', this._gameState]);
    }

    // Platform (nested)
    params.push(['platform[name]', this._platform.name]);
    if (this._platform.appVersion) {
      params.push(['platform[appVersion]', this._platform.appVersion]);
    }
    if (this._platform.osVersion) {
      params.push(['platform[osVersion]', this._platform.osVersion]);
    }
    params.push(['platform[deviceClass]', this._platform.deviceClass]);
    params.push(['platform[capabilities][sse]', String(this._platform.capabilities.sse)]);
    if (this._platform.capabilities.onFocus) {
      params.push(['platform[capabilities][onFocus]', 'true']);
    }
    params.push(['platform[formFactor]', this._platform.formFactor]);

    // Device (nested, all optional)
    if (this._device.deviceId) params.push(['device[deviceId]', this._device.deviceId]);
    if (this._device.zipCode) params.push(['device[zipCode]', this._device.zipCode]);
    if (this._device.countryCode) params.push(['device[countryCode]', this._device.countryCode]);
    if (this._device.region) params.push(['device[region]', this._device.region]);

    // Experiments (nested map)
    for (const [id, variant] of Object.entries(this._experiments)) {
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
    const body: Record<string, unknown> = {
      locale: this._locale,
      schemaVersion: this._schemaVersion,
      platform: this._platform,
    };
    if (this._gameState) body.gameState = this._gameState;
    if (Object.keys(this._device).length > 0) body.device = this._device;
    if (Object.keys(this._experiments).length > 0) body.experiments = this._experiments;
    return body;
  }

  /**
   * Generate a trace ID for this request.
   */
  static generateTraceId(): string {
    return `trace-${crypto.randomUUID().substring(0, 8)}`;
  }
}
