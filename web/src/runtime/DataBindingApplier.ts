import type { DataBinding, Section } from '@sdui/models';

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
  stringTable?: Record<string, string>
): T {
  if (!bindings?.bindings?.length) {
    return sectionData;
  }

  // Clone to avoid mutation
  const updated = JSON.parse(JSON.stringify(sectionData)) as T;

  for (const binding of bindings.bindings) {
    try {
      const value = getValueByPath(incomingMessage, binding.sourcePath);
      if (value !== undefined) {
        // Check if there's a stringKey for this target path
        const stringKey = bindings.stringKeys?.[binding.targetPath];
        if (stringKey && stringTable) {
          const resolved = stringTable[stringKey];
          if (resolved !== undefined) {
            setValueByPath(updated, binding.targetPath, resolved);
            console.log(`[DataBinding] ${binding.sourcePath} -> ${binding.targetPath}: stringKey '${stringKey}' resolved to '${resolved}'`);
          } else {
            // Fall back to raw value when stringKey not found in table
            setValueByPath(updated, binding.targetPath, value);
            console.warn(`[DataBinding] stringKey '${stringKey}' not found in stringTable for ${binding.targetPath}, using raw value:`, value);
          }
        } else {
          setValueByPath(updated, binding.targetPath, value);
          console.log(`[DataBinding] ${binding.sourcePath} -> ${binding.targetPath}:`, value);
        }
      }
    } catch (err) {
      console.warn(`[DataBinding] Failed to apply binding:`, binding, err);
    }
  }

  return updated;
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
 * Set value in object using dot-path syntax.
 */
function setValueByPath(obj: Record<string, unknown>, path: string, value: unknown): void {
  const segments = path.split('.');
  let current = obj;

  for (let i = 0; i < segments.length - 1; i++) {
    const segment = segments[i];
    
    // Handle array indexing
    const arrayMatch = segment.match(/^(\w+)\[(\d+)\]$/);
    if (arrayMatch) {
      const [, key, index] = arrayMatch;
      if (!current[key]) {
        current[key] = [];
      }
      const arr = current[key] as unknown[];
      const idx = parseInt(index, 10);
      if (!arr[idx]) {
        arr[idx] = {};
      }
      current = arr[idx] as Record<string, unknown>;
    } else {
      if (!current[segment]) {
        current[segment] = {};
      }
      current = current[segment] as Record<string, unknown>;
    }
  }

  const lastSegment = segments[segments.length - 1];
  const arrayMatch = lastSegment.match(/^(\w+)\[(\d+)\]$/);
  if (arrayMatch) {
    const [, key, index] = arrayMatch;
    if (!current[key]) {
      current[key] = [];
    }
    (current[key] as unknown[])[parseInt(index, 10)] = value;
  } else {
    current[lastSegment] = value;
  }
}

/**
 * Check if a section has active data bindings.
 */
export function hasDataBindings(section: Section): boolean {
  return Boolean(section.dataBinding?.bindings?.length);
}
