import { useCallback, useState } from 'react'

export interface JsonCursorContext {
  elementType: string | null
  propertyName: string | null
}

/**
 * Given a JSON string and a cursor position, determines which element type
 * and property the cursor is within. Walks backward from the cursor to find
 * the nearest "type": "..." and the property key on the current line.
 */
export function parseJsonContext(json: string, cursorPos: number): JsonCursorContext {
  // Find the property name on the current line (cursor's line)
  const beforeCursor = json.slice(0, cursorPos)
  const currentLineStart = beforeCursor.lastIndexOf('\n') + 1
  const currentLine = json.slice(currentLineStart, json.indexOf('\n', cursorPos) === -1 ? json.length : json.indexOf('\n', cursorPos))

  // Extract property name from current line: "propertyName": ...
  const propMatch = currentLine.match(/"([a-zA-Z]+)"\s*:/)
  const propertyName = propMatch ? propMatch[1] : null

  // Walk backward from cursor to find the nearest "type": "ElementType"
  // We need to find which element block we're inside
  const elementType = findEnclosingElementType(json, cursorPos)

  return { elementType, propertyName }
}

function findEnclosingElementType(json: string, pos: number): string | null {
  // Strategy: find all "type": "..." occurrences before the cursor position,
  // then figure out which one's block we're still inside.
  // Simple heuristic: walk backwards tracking brace depth.

  let depth = 0
  let i = pos - 1

  // First, figure out our current object depth by scanning forward from pos
  // to find if we hit } before { — if so we need to adjust
  // Actually simpler: scan backward, tracking braces, until we find "type":

  while (i >= 0) {
    const ch = json[i]
    if (ch === '}') depth++
    if (ch === '{') {
      if (depth === 0) {
        // We've exited to a parent object. Check if this object has "type":
        const typeInBlock = findTypeInObject(json, i)
        if (typeInBlock) return typeInBlock
      } else {
        depth--
      }
    }
    i--
  }

  return null
}

function findTypeInObject(json: string, openBracePos: number): string | null {
  // From the opening brace, look for "type": "..." within the immediate level
  let depth = 0
  let i = openBracePos + 1

  while (i < json.length) {
    const ch = json[i]
    if (ch === '{' || ch === '[') depth++
    if (ch === '}' || ch === ']') {
      if (depth === 0) break
      depth--
    }
    if (depth === 0) {
      // Check for "type": pattern
      const remaining = json.slice(i, i + 30)
      const typeMatch = remaining.match(/^"type"\s*:\s*"([A-Za-z]+)"/)
      if (typeMatch) return typeMatch[1]
    }
    i++
  }

  return null
}

export function useJsonCursorContext() {
  const [context, setContext] = useState<JsonCursorContext>({ elementType: null, propertyName: null })

  const handleCursorChange = useCallback((json: string, cursorPos: number): JsonCursorContext => {
    const newContext = parseJsonContext(json, cursorPos)
    setContext((prev) => {
      if (prev.elementType === newContext.elementType && prev.propertyName === newContext.propertyName) {
        return prev
      }
      return newContext
    })
    return newContext
  }, [])

  return { context, handleCursorChange }
}
