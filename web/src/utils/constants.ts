/**
 * Prefix for all SDUI server paths.
 *
 * Used for URI → endpoint resolution and by ActionHandler to prefix
 * relative API requests through the Vite dev proxy.
 */
export const SDUI_PATH_PREFIX = '/v1/sdui/';

/**
 * Dev proxy prefix prepended to relative API calls so Vite forwards
 * them to the Spring Boot backend.
 */
export const API_PROXY_PREFIX = '/api';
