import type { DataBinding, Section } from '@sdui/models';

const MISS_THRESHOLD = 3;
const consecutiveMissCounts = new Map<string, number>();

/**
 * Clear all miss counters for a section. Call when a section is removed
 * from the screen to prevent memory leaks.
 */
export function resetBindingCounters(sectionId: string): void {
  for (const key of consecutiveMissCounts.keys()) {
    if (key.startsWith(sectionId + ':')) {
      consecutiveMissCounts.delete(key);
    }
  }
}

/**
 * Apply data bindings from an incoming message to section data.
 * 
 * Bindings use JSONPath-like syntax:
 * - sourcePath: Path in incoming message (e.g., '$.homeTeam.score')
 * - targetPath: Dot-path to section property (e.g., 'homeTeam.score')
 */
export function applyDataBindings<T extends Record<string, unknown>>(
  sectionData: T,
  bindings: DataBinding | undefined,
  incomingMessage: Record<string, unknown>,
  stringTable?: Record<string, string>,
  sectionId?: string,
  traceId?: string
): T {
  if (!bindings?.bindings?.length) {
    return sectionData;
  }

  const traceTag = traceId ? ` trace=${traceId}` : '';
  let updated: T = sectionData;

  for (const binding of bindings.bindings) {
    try {
      const missKey = sectionId ? `${sectionId}:${binding.sourcePath}` : undefined;
      const value = getValueByPath(incomingMessage, binding.sourcePath);
      if (value !== undefined) {
        // Source resolved successfully — reset miss counter
        if (missKey) {
          consecutiveMissCounts.delete(missKey);
        }
        const boundValue = applyTransform(binding.transform, value, incomingMessage);
        if (boundValue === UNKNOWN_TRANSFORM) {
          // Server declared a transform this client doesn't recognize. Skip
          // the write rather than downgrading to the raw value, which would
          // silently drop a server-declared semantic.
          console.warn(
            `[DataBinding${traceTag}] Unknown transform '${binding.transform}' for ${binding.targetPath}; skipping write to avoid downgrading server semantics.`
          );
          continue;
        }
        // Check if there's a stringKey for this target path
        const stringKey = bindings.stringKeys?.[binding.targetPath];
        if (stringKey && stringTable) {
          const resolved = stringTable[stringKey];
          if (resolved !== undefined) {
            updated = setValueByPathImmutable(updated, binding.targetPath, resolved);
            console.log(`[DataBinding${traceTag}] ${binding.sourcePath} -> ${binding.targetPath}: stringKey '${stringKey}' resolved to '${resolved}'`);
          } else {
            // Fall back to raw value when stringKey not found in table
            updated = setValueByPathImmutable(updated, binding.targetPath, boundValue);
            console.warn(`[DataBinding${traceTag}] stringKey '${stringKey}' not found in stringTable for ${binding.targetPath}, using raw value:`, boundValue);
          }
        } else {
          updated = setValueByPathImmutable(updated, binding.targetPath, boundValue);
          console.log(`[DataBinding${traceTag}] ${binding.sourcePath} -> ${binding.targetPath}:`, boundValue);
        }
      } else {
        // Source path missing — track consecutive misses
        if (missKey) {
          const count = (consecutiveMissCounts.get(missKey) ?? 0) + 1;
          consecutiveMissCounts.set(missKey, count);
          if (count >= MISS_THRESHOLD) {
            console.warn(
              `[DataBinding${traceTag}] Binding path missing for ${count} consecutive cycles:`,
              `sectionId=${sectionId}, sourcePath=${binding.sourcePath}`
            );
            // TODO: emit binding_path_missing analytics event
          }
        }
      }
    } catch (err) {
      // Track miss on exception as well
      if (sectionId) {
        const missKey = `${sectionId}:${binding.sourcePath}`;
        const count = (consecutiveMissCounts.get(missKey) ?? 0) + 1;
        consecutiveMissCounts.set(missKey, count);
      }
      console.warn(`[DataBinding${traceTag}] Failed to apply binding:`, binding, err);
    }
  }

  return updated;
}

/**
 * Sentinel returned from {@link applyTransform} when the server declared a
 * transform value this client build does not recognize. Callers must skip
 * the write rather than fall back to the untransformed value, which would
 * silently drop server-declared semantics.
 */
const UNKNOWN_TRANSFORM = Symbol('UNKNOWN_TRANSFORM');

function applyTransform(
  transform: string | undefined,
  value: unknown,
  root: Record<string, unknown>
): unknown {
  switch (transform) {
    case 'liveClockSnapshot':
      return normalizeLiveClockSnapshot(value, root);
    case undefined:
      return value;
    default:
      return UNKNOWN_TRANSFORM;
  }
}

function normalizeLiveClockSnapshot(
  value: unknown,
  root: Record<string, unknown>
): { snapshotSeconds: number; snapshotAt: string; isRunning: boolean } {
  const objectValue = isRecord(value) ? value : undefined;
  const rawSeconds = objectValue?.snapshotSeconds ?? objectValue?.seconds ?? objectValue?.remainingSeconds ?? value;
  const snapshotSeconds = parseClockSeconds(rawSeconds) ?? 0;
  const snapshotAt = asString(objectValue?.snapshotAt ?? objectValue?.snapshotAtIso ?? root.snapshotAt) ?? new Date().toISOString();
  const runningValue = objectValue?.isRunning ?? objectValue?.clockRunning ?? objectValue?.gameClockRunning
    ?? root.isRunning ?? root.clockRunning ?? root.gameClockRunning;
  return {
    snapshotSeconds,
    snapshotAt,
    isRunning: asBoolean(runningValue) ?? false,
  };
}

function parseClockSeconds(value: unknown): number | undefined {
  if (typeof value === 'number' && Number.isFinite(value)) {
    return Math.max(0, Math.floor(value));
  }
  if (typeof value !== 'string') {
    return undefined;
  }

  const trimmed = value.trim();
  const durationMatch = /^PT(?:(\d+(?:\.\d+)?)H)?(?:(\d+(?:\.\d+)?)M)?(?:(\d+(?:\.\d+)?)S)?$/i.exec(trimmed);
  if (durationMatch) {
    const hours = Number(durationMatch[1] ?? 0);
    const minutes = Number(durationMatch[2] ?? 0);
    const seconds = Number(durationMatch[3] ?? 0);
    return Math.max(0, Math.floor(hours * 3600 + minutes * 60 + seconds));
  }

  const clockMatch = /(?:^|\b)(\d{1,2}):([0-5]\d)(?:\.\d+)?(?:\b|$)/.exec(trimmed);
  if (clockMatch) {
    return Number(clockMatch[1]) * 60 + Number(clockMatch[2]);
  }

  return undefined;
}

function asBoolean(value: unknown): boolean | undefined {
  if (typeof value === 'boolean') return value;
  if (typeof value === 'number') return value !== 0;
  if (typeof value === 'string') {
    const normalized = value.trim().toLowerCase();
    if (['true', '1', 'yes', 'y'].includes(normalized)) return true;
    if (['false', '0', 'no', 'n'].includes(normalized)) return false;
  }
  return undefined;
}

function asString(value: unknown): string | undefined {
  return typeof value === 'string' && value.trim().length > 0 ? value : undefined;
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value);
}

/**
 * Get value from object using JSONPath-like syntax.
 * Supports: $.path.to.value or path.to.value
 */
function getValueByPath(obj: Record<string, unknown>, path: string): unknown {
  // Remove leading $. if present
  const cleanPath = path.startsWith('$.') ? path.slice(2) : path;
  
  const segments = cleanPath.split('.');
  let current: unknown = obj;

  for (const segment of segments) {
    if (current === null || current === undefined) {
      return undefined;
    }
    
    // Handle array indexing: items[0]
    const arrayMatch = segment.match(/^(\w+)\[(\d+)\]$/);
    if (arrayMatch) {
      const [, key, index] = arrayMatch;
      current = (current as Record<string, unknown>)[key];
      if (Array.isArray(current)) {
        current = current[parseInt(index, 10)];
      } else {
        return undefined;
      }
    } else {
      current = (current as Record<string, unknown>)[segment];
    }
  }

  return current;
}

/**
 * Immutable dot-path set that structural-shares all branches that do not
 * change so downstream memoization still sees stable references.
 */
function setValueByPathImmutable<T extends Record<string, unknown>>(
  root: T,
  path: string,
  value: unknown,
): T {
  const segments = path.split('.');
  if (segments.length === 0) {
    return root;
  }
  return updateAtPath(root, segments, 0, value) as T;
}

function updateAtPath(
  current: unknown,
  segments: string[],
  i: number,
  value: unknown,
): unknown {
  const seg = segments[i]!;
  const isLast = i === segments.length - 1;
  const parent: Record<string, unknown> = isRecord(current) ? current : {};
  const arrayMatch = seg.match(/^(\w+)\[(\d+)\]$/);

  if (arrayMatch) {
    const key = arrayMatch[1]!;
    const idx = parseInt(arrayMatch[2]!, 10);
    const oldArr: unknown = parent[key];
    const sourceArr: unknown[] = Array.isArray(oldArr) ? (oldArr as unknown[]) : [];
    const newArr: unknown[] = sourceArr.length ? [...sourceArr] : [];
    while (newArr.length <= idx) {
      newArr.push(undefined);
    }
    if (isLast) {
      if (sourceArr[idx] === value) {
        return isRecord(current) ? current : parent;
      }
      newArr[idx] = value;
      if (isRecord(current)) {
        return { ...current, [key]: newArr };
      }
      return { ...parent, [key]: newArr };
    }
    const newInner = updateAtPath(newArr[idx], segments, i + 1, value);
    if (newInner === newArr[idx] && isRecord(current) && (current as Record<string, unknown>)[key] === oldArr) {
      return current;
    }
    const withIdx = newArr;
    withIdx[idx] = newInner;
    if (isRecord(current)) {
      return { ...current, [key]: withIdx };
    }
    return { ...parent, [key]: withIdx };
  }

  if (isLast) {
    if (isRecord(current) && (current as Record<string, unknown>)[seg] === value) {
      return current;
    }
    if (isRecord(current)) {
      return { ...current, [seg]: value };
    }
    return { [seg]: value };
  }

  const child = parent[seg];
  const childForUpdate = isRecord(child) ? child : undefined;
  const newChild = updateAtPath(childForUpdate, segments, i + 1, value);
  if (newChild === child && isRecord(current)) {
    return current;
  }
  if (isRecord(current)) {
    return { ...current, [seg]: newChild };
  }
  return { [seg]: newChild };
}

/**
 * Check if a section has active data bindings.
 */
export function hasDataBindings(section: Section): boolean {
  return Boolean(section.dataBinding?.bindings?.length);
}
