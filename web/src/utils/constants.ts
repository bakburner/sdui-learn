/**
 * Universal client-side fallback image.
 *
 * Used as the last-resort `onError` replacement for any `<img>` tag in
 * section renderers.  The server MAY still supply a per-section
 * `fallbackThumbnailUrl` override, but this constant is the final
 * safety-net so callers never need to guard against `undefined`.
 */
export const DEFAULT_FALLBACK_IMAGE =
  'https://cdn.nba.com/manage/2025/04/nba-247-logoman-yt-thumbnail__1_.png';

/**
 * Prefix for all SDUI server paths.
 *
 * Used for URI → endpoint resolution and by ActionHandler to prefix
 * relative API requests through the Vite dev proxy.
 */
export const SDUI_PATH_PREFIX = '/sdui/';

/**
 * Dev proxy prefix prepended to relative API calls so Vite forwards
 * them to the Spring Boot backend.
 */
export const API_PROXY_PREFIX = '/api';
