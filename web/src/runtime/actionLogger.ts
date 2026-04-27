/**
 * Debug-only logger for the SDUI action pipeline.
 *
 * The action system fires many beacons per session — taps, onVisible
 * impressions, mutates, refreshes — and `fireAndForget` in particular
 * has no on-screen side effect, which makes it impossible to verify
 * visually during local testing without instrumentation.
 *
 * This helper is gated to development builds via Vite's
 * `import.meta.env.DEV`, so logs ride `npm run dev` and disappear from
 * the production bundle. Hosts can flip {@link setActionLoggerEnabled}
 * to capture telemetry from a built bundle when dogfooding.
 *
 * Mirrors iOS's `os.Logger.debug(...)` calls in `ActionDispatcher` and
 * Android's `SduiActionLogger`.
 */

const TAG = '[SDUI/Action]';

let enabled: boolean = (() => {
  try {
    type ImportMeta = { env?: { DEV?: boolean } };
    const meta = import.meta as ImportMeta;
    return Boolean(meta.env?.DEV);
  } catch {
    return false;
  }
})();

/** Manually enable/disable verbose action logging at runtime. */
export function setActionLoggerEnabled(value: boolean): void {
  enabled = value;
}

/** Whether verbose action logging is currently emitting. */
export function isActionLoggerEnabled(): boolean {
  return enabled;
}

/** Log a debug-level entry from the action pipeline. */
export function actionLog(label: string, ...rest: unknown[]): void {
  if (enabled) console.log(`${TAG} ${label}`, ...rest);
}

/** Log a warning-level entry from the action pipeline. */
export function actionWarn(label: string, ...rest: unknown[]): void {
  if (enabled) console.warn(`${TAG} ${label}`, ...rest);
}

/** Log an error-level entry from the action pipeline. */
export function actionError(label: string, ...rest: unknown[]): void {
  if (enabled) console.error(`${TAG} ${label}`, ...rest);
}
