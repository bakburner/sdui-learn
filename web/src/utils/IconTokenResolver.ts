/**
 * Resolves cross-platform SDUI icon tokens (e.g. `sdui:play`) to the
 * web-native asset. On the web the resolved value is a
 * Material-icon ligature name suitable for rendering with the Google
 * Material Icons font (e.g. `<span class="material-icons">home</span>`).
 *
 * The token vocabulary lives in `schema/icon-tokens.json`; unknown
 * tokens fall back to `sdui:warning`. Non-token strings pass through
 * unchanged so renderers can keep accepting raw Material ligature
 * names for back-compat while the server migrates to tokens.
 */

const TOKEN_PREFIX = 'sdui:';
const FALLBACK_TOKEN = 'sdui:warning';

/**
 * Snapshot of `schema/icon-tokens.json` — the `web` column keyed by
 * token. Kept inline so the bundle carries no fetch dependency;
 * regenerate whenever the canonical icon list changes.
 */
const TOKENS: Readonly<Record<string, string>> = {
  'sdui:play':        'play_arrow',
  'sdui:pause':       'pause',
  'sdui:back':        'arrow_back',
  'sdui:forward':     'arrow_forward',
  'sdui:settings':    'settings',
  'sdui:expand':      'expand_more',
  'sdui:collapse':    'expand_less',
  'sdui:check':       'check',
  'sdui:warning':     'warning',
  'sdui:live':        'sensors',
  'sdui:person':      'account_circle',
  'sdui:close':       'close',
  'sdui:search':      'search',
  'sdui:share':       'share',
  'sdui:favorite':    'favorite_border',
  'sdui:favorited':   'favorite',
  'sdui:fullscreen':  'fullscreen',
  'sdui:pip':         'picture_in_picture',
  'sdui:cast':        'cast',
  'sdui:info':        'info',
  'sdui:calendar':    'calendar_today',
  'sdui:refresh':     'refresh',
  'sdui:home':        'home',
  'sdui:basketball':  'sports_basketball',
  'sdui:video':       'play_circle',
  'sdui:leaderboard': 'leaderboard',
  'sdui:grid':        'widgets',
};

export const IconTokenResolver = {
  /**
   * Convert a server icon string into a Material-icon ligature name.
   *
   * Returns the Material name when `token` is a recognised
   * `sdui:` token; the fallback token's name when the token is
   * unknown; the input unchanged when it is not an `sdui:` token at
   * all; `undefined` when `token` is null/undefined/empty.
   */
  resolve(token: string | null | undefined): string | undefined {
    if (!token) return undefined;
    if (!token.startsWith(TOKEN_PREFIX)) return token;

    const resolved = TOKENS[token];
    if (resolved) return resolved;

    if (typeof console !== 'undefined') {
      console.warn(
        `[IconTokenResolver] unknown icon token ${token}; falling back to ${FALLBACK_TOKEN}`,
      );
    }
    return TOKENS[FALLBACK_TOKEN];
  },
};
