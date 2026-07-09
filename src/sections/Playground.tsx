import { useState, useCallback, useRef, useEffect } from 'react'
import { useScrollReveal } from '../hooks/useScrollReveal'
import '../hooks/useScrollReveal.css'
import { SchemaReference } from '../components/SchemaReference'
import { PlatformCode } from '../components/PlatformCode'
import { useJsonCursorContext } from '../hooks/useJsonCursorContext'
import './Playground.css'

const DEFAULT_JSON = `{
  "data": {
    "id": "playground",
    "schemaVersion": "1.0",
    "defaultRefreshPolicy": { "type": "static" },
    "sections": [
      {
        "id": "game-panel__type-AtomicComposite",
        "type": "AtomicComposite",
        "refreshPolicy": [{ "type": "static" }],
        "surface": {
          "margin": {
            "top": "token:nba.spacing.lg",
            "bottom": "token:nba.spacing.lg",
            "start": "token:nba.spacing.lg",
            "end": "token:nba.spacing.lg"
          },
          "background": "token:nba.bg.secondary",
          "cornerRadius": "token:nba.radius.md",
          "shadow": { "color": "#00000014", "radius": 6, "offsetX": 0, "offsetY": 2 }
        },
        "data": {
          "ui": {
            "type": "Container",
            "direction": "column",
            "crossAlignment": "stretch",
            "padding": {
              "top": "token:nba.spacing.lg",
              "bottom": "token:nba.spacing.lg",
              "start": "token:nba.spacing.lg",
              "end": "token:nba.spacing.lg"
            },
            "widthMode": "fill",
            "actions": [
              { "trigger": "onActivate", "type": "navigate", "targetUri": "nba://game/0022400123" }
            ],
            "children": [
              {
                "type": "Container",
                "direction": "row",
                "alignment": "spaceBetween",
                "crossAlignment": "center",
                "widthMode": "fill",
                "children": [
                  {
                    "type": "Container",
                    "direction": "column",
                    "alignment": "center",
                    "crossAlignment": "center",
                    "children": [
                      {
                        "type": "Image",
                        "src": "https://cdn.nba.com/logos/nba/1610612747/primary/D/512x512/logo.png",
                        "fit": "contain",
                        "width": 48,
                        "height": 48
                      },
                      { "type": "Spacer", "height": "token:nba.spacing.sm" },
                      { "type": "Text", "content": "LAL", "variant": "titleMedium", "weight": "semiBold" },
                      { "type": "Text", "content": "108", "variant": "score", "weight": "bold", "bindRef": "awayTeam.score" }
                    ]
                  },
                  {
                    "type": "LiveClock",
                    "variant": "titleMedium",
                    "snapshotSeconds": 0,
                    "tickDirection": "down",
                    "format": "m:ss",
                    "bindRef": "clock"
                  },
                  {
                    "type": "Container",
                    "direction": "column",
                    "alignment": "center",
                    "crossAlignment": "center",
                    "children": [
                      {
                        "type": "Image",
                        "src": "https://cdn.nba.com/logos/nba/1610612738/primary/D/512x512/logo.png",
                        "fit": "contain",
                        "width": 48,
                        "height": 48
                      },
                      { "type": "Spacer", "height": "token:nba.spacing.sm" },
                      { "type": "Text", "content": "BOS", "variant": "titleMedium", "weight": "semiBold" },
                      { "type": "Text", "content": "112", "variant": "score", "weight": "bold", "bindRef": "homeTeam.score" }
                    ]
                  }
                ]
              },
              { "type": "Spacer", "height": "token:nba.spacing.md" },
              {
                "type": "Button",
                "label": "Watch Live",
                "variant": "primary",
                "actions": [
                  { "trigger": "onActivate", "type": "navigate", "targetUri": "nba://watch/0022400123" }
                ]
              }
            ]
          },
          "content": {
            "homeTeam": { "score": 112, "tricode": "BOS" },
            "awayTeam": { "score": 108, "tricode": "LAL" },
            "clock": { "snapshotSeconds": 154, "snapshotAt": "2026-06-09T19:09:04Z", "isRunning": true }
          }
        }
      }
    ]
  },
  "meta": { "degraded": false }
}`

export function Playground() {
  const [jsonInput, setJsonInput] = useState(DEFAULT_JSON)
  const [parseError, setParseError] = useState<string | null>(null)
  const [parsedData, setParsedData] = useState(() => {
    try { return JSON.parse(DEFAULT_JSON) } catch { return null }
  })
  const [showReference, setShowReference] = useState(false)
  const [showPlatform, setShowPlatform] = useState(false)
  const [isFullscreen, setIsFullscreen] = useState(false)
  const [showOnboarding, setShowOnboarding] = useState(true)
  const [editorMode, setEditorMode] = useState<'edit' | 'diff' | 'network'>('edit')
  const [baseJson] = useState(DEFAULT_JSON)
  const [selectedElement, setSelectedElement] = useState<string | null>(null)
  const [highlightLines, setHighlightLines] = useState<{ primary: { start: number; end: number }; secondary?: { start: number; end: number } } | null>(null)
  const [validationExpanded, setValidationExpanded] = useState(false)
  const [deviceFrame, setDeviceFrame] = useState<'phone' | 'tablet' | 'tv' | 'desktop' | 'watch'>('phone')
  const [isAuthed, setIsAuthed] = useState(() => sessionStorage.getItem('sdui-auth') === 'true')
  const [copied, setCopied] = useState(false)

  const scrollToElement = useCallback((el: Record<string, any>) => {
    const ta = textareaRef.current
    if (!ta) return
    const text = ta.value
    const typeStr = `"type": "${el.type}"`
    const distinguisher = el.content || el.src || el.label || el.bindRef
    let searchFrom = 0
    let bestStart = -1
    let bestEnd = -1

    while (true) {
      const idx = text.indexOf(typeStr, searchFrom)
      if (idx === -1) break

      // Find the enclosing { for this "type" occurrence
      let braceStart = text.lastIndexOf('{', idx)
      let depth = 1
      let braceEnd = braceStart + 1
      while (braceEnd < text.length && depth > 0) {
        if (text[braceEnd] === '{') depth++
        if (text[braceEnd] === '}') depth--
        braceEnd++
      }

      // Check distinguisher within this specific block only
      const block = text.slice(braceStart, braceEnd)
      if (!distinguisher || block.includes(`"${String(distinguisher)}"`)) {
        bestStart = braceStart
        bestEnd = braceEnd
        break
      }
      searchFrom = idx + 1
    }
    if (bestStart === -1) return

    const lines = text.split('\n')
    const primaryStart = text.slice(0, bestStart).split('\n').length - 1
    const primaryEnd = text.slice(0, bestEnd).split('\n').length - 1

    // Find the parent container block (one level up)
    let parentStart = -1
    let parentEnd = -1
    if (bestStart > 0) {
      // Search backwards for the parent opening brace
      let pStart = bestStart - 1
      while (pStart >= 0 && text[pStart] !== '{') pStart--
      if (pStart >= 0) {
        let depth = 1
        let pEnd = pStart + 1
        while (pEnd < text.length && depth > 0) {
          if (text[pEnd] === '{') depth++
          if (text[pEnd] === '}') depth--
          pEnd++
        }
        // Only use as secondary if it's actually larger than the primary
        if (pStart < bestStart && pEnd > bestEnd) {
          parentStart = text.slice(0, pStart).split('\n').length - 1
          parentEnd = text.slice(0, pEnd).split('\n').length - 1
        }
      }
    }

    setHighlightLines({
      primary: { start: primaryStart, end: primaryEnd },
      secondary: parentStart >= 0 ? { start: parentStart, end: parentEnd } : undefined,
    })

    // Scroll to center on the primary element
    const lineHeight = ta.scrollHeight / lines.length
    const targetScroll = Math.max(0, primaryStart * lineHeight - ta.clientHeight / 3)
    ta.scrollTop = targetScroll

    const overlay = ta.parentElement?.querySelector('.editor-highlight-overlay') as HTMLElement | null
    if (overlay) overlay.scrollTop = targetScroll

    setTimeout(() => setHighlightLines(null), 3000)
  }, [jsonInput])

  useEffect(() => {
    if (!isFullscreen) return
    const handleEsc = (e: KeyboardEvent) => {
      if (e.key === 'Escape') setIsFullscreen(false)
    }
    document.addEventListener('keydown', handleEsc)
    document.body.style.overflow = 'hidden'
    return () => {
      document.removeEventListener('keydown', handleEsc)
      document.body.style.overflow = ''
    }
  }, [isFullscreen])

  useEffect(() => {
    const open = () => setIsFullscreen(true)
    window.addEventListener('launch-editor', open)
    return () => window.removeEventListener('launch-editor', open)
  }, [])
  const [inspectedElement, setInspectedElement] = useState<Record<string, any> | null>(null)
  const { context: cursorContext, handleCursorChange } = useJsonCursorContext()
  const textareaRef = useRef<HTMLTextAreaElement>(null)
  const header = useScrollReveal<HTMLDivElement>()

  const handleChange = useCallback((value: string) => {
    setJsonInput(value)
    try {
      const parsed = JSON.parse(value)
      setParsedData(parsed)
      setParseError(null)
    } catch (e) {
      setParseError((e as Error).message)
    }
  }, [])

  const handleReset = () => {
    setJsonInput(DEFAULT_JSON)
    setParsedData(JSON.parse(DEFAULT_JSON))
    setParseError(null)
  }

  const handleFormat = () => {
    try {
      const parsed = JSON.parse(jsonInput)
      const formatted = JSON.stringify(parsed, null, 2)
      setJsonInput(formatted)
    } catch {
      // already has a parse error shown
    }
  }

  const handleCopyJson = () => {
    navigator.clipboard.writeText(jsonInput).then(() => {
      setCopied(true)
      setTimeout(() => setCopied(false), 2000)
    })
  }

  const handleInsertSnippet = (snippet: string) => {
    setJsonInput(snippet)
    try {
      setParsedData(JSON.parse(snippet))
      setParseError(null)
    } catch (e) {
      setParseError((e as Error).message)
    }
  }

  const handleEditorCursor = () => {
    const ta = textareaRef.current
    if (!ta) return
    const { elementType, propertyName } = handleCursorChange(jsonInput, ta.selectionStart)
    if (elementType && propertyName) {
      setSelectedElement(elementType)
      if (!showReference) setShowReference(true)
    }
  }

  const handleInsertAtCursor = useCallback((elementSnippet: string) => {
    const ta = textareaRef.current
    if (!ta) return
    const text = ta.value

    // Strategy: parse the JSON, find the nearest children array, and append to it.
    // Then re-serialize. This guarantees valid JSON output.
    try {
      const data = JSON.parse(text)
      const childrenArr = findNearestChildrenArray(data)
      if (childrenArr) {
        const parsed = JSON.parse(elementSnippet)
        childrenArr.push(parsed)
        const newJson = JSON.stringify(data, null, 2)
        handleChange(newJson)

        // Scroll to the new element
        setTimeout(() => {
          if (ta) {
            const snippetType = parsed.type || ''
            const lastIdx = newJson.lastIndexOf(`"type": "${snippetType}"`)
            if (lastIdx !== -1) {
              const line = newJson.slice(0, lastIdx).split('\n').length - 1
              const lines = newJson.split('\n')
              const lineHeight = ta.scrollHeight / lines.length
              ta.scrollTop = Math.max(0, line * lineHeight - ta.clientHeight / 3)
              setHighlightLines({ primary: { start: line, end: Math.min(line + 3, lines.length - 1) } })
              setTimeout(() => setHighlightLines(null), 2000)
            }
          }
        }, 50)
      }
    } catch {
      // If JSON is currently broken, can't insert safely
    }

    ta.focus()
  }, [handleChange])

  const [validationMarkers, setValidationMarkers] = useState<ValidationMarker[]>([])

  useEffect(() => {
    const timer = setTimeout(() => {
      setValidationMarkers(validateSchema(jsonInput))
    }, 500)
    return () => clearTimeout(timer)
  }, [jsonInput])

  const handlePropertyChange = useCallback((elementType: string, property: string, value: string) => {
    const ta = textareaRef.current
    let targetElement = inspectedElement?.type === elementType ? inspectedElement : null

    if (!targetElement) {
      // Try to find element at cursor position, or fall back to first match
      try {
        const data = JSON.parse(jsonInput)
        if (ta && cursorContext.elementType === elementType) {
          targetElement = findElementAtCursor(data, jsonInput, ta.selectionStart, elementType)
        }
        if (!targetElement) {
          targetElement = findElementByType(data, elementType)
        }
      } catch {
        return
      }
    }
    if (!targetElement) return

    const updated = { ...targetElement, [property]: value }
    setInspectedElement(updated)

    try {
      const data = JSON.parse(jsonInput)
      patchElement(data, targetElement, property, value)
      const newJson = JSON.stringify(data, null, 2)
      setJsonInput(newJson)
      setParsedData(data)
      setParseError(null)

      // Scroll to the changed property in the editor
      if (ta) {
        const propStr = `"${property}"`
        const idx = newJson.indexOf(propStr)
        if (idx !== -1) {
          const line = newJson.slice(0, idx).split('\n').length - 1
          const lines = newJson.split('\n')
          const lineHeight = ta.scrollHeight / lines.length
          const targetScroll = Math.max(0, line * lineHeight - ta.clientHeight / 3)
          ta.scrollTop = targetScroll
          setHighlightLines({ primary: { start: line, end: line } })
          setTimeout(() => setHighlightLines(null), 1500)
        }
      }
    } catch {
      // ignore if JSON is broken
    }
  }, [inspectedElement, jsonInput, cursorContext])

  return (
    <section id="playground">
      <div ref={header.ref} className={`reveal ${header.isVisible ? 'visible' : ''}`}>
        <div className="section-label">Interactive</div>
        <h2 className="section-title">Playground</h2>
        <p className="section-subtitle full-width">
          Build and preview SDUI layouts in real time. Edit JSON, see it render live, inspect element properties, browse the full schema reference, and view platform-specific rendering code for React, SwiftUI, and Compose.
        </p>
        <div className="open-playground-cta">
          <button className="open-playground-btn" onClick={() => setIsFullscreen(true)}>
            Launch Playground
          </button>
        </div>
      </div>

      {isFullscreen && (
        <div className="playground-fullscreen">
          <div className="fullscreen-header">
            <div className="fullscreen-header-left">
              <div className="playground-examples">
                <button className="example-btn example-btn-new" onClick={() => handleInsertSnippet(EXAMPLE_BLANK)}>+ New</button>
                <span className="examples-label">Samples:</span>
                <button className="example-btn" onClick={() => handleInsertSnippet(EXAMPLE_GAME_CARD)}>Game Card</button>
                <button className="example-btn" onClick={() => handleInsertSnippet(EXAMPLE_NEWS_CARD)}>News Card</button>
                <button className="example-btn" onClick={() => handleInsertSnippet(EXAMPLE_PROMO)}>Promo Banner</button>
              </div>
            </div>
            <span className="fullscreen-brand">SDUI Playground</span>
            <div className="fullscreen-header-right">
              <button
                className={`reference-toggle ${showReference ? 'active' : ''}`}
                onClick={() => { setShowReference(!showReference); if (!showReference) setShowPlatform(false) }}
              >
                {showReference ? '✕ Schema' : '📖 Schema'}
              </button>
              <button
                className={`reference-toggle ${showPlatform ? 'active' : ''}`}
                onClick={() => { setShowPlatform(!showPlatform); if (!showPlatform) setShowReference(false) }}
              >
                {showPlatform ? '✕ Platform' : '💻 Platform'}
              </button>
              <button
                className="fullscreen-toggle"
                onClick={() => setIsFullscreen(false)}
              >
                ✕ Exit
              </button>
            </div>
          </div>

          {isAuthed && <PromptBar onGenerate={handleInsertSnippet} />}
          {!isAuthed && <LoginBar onAuth={() => { setIsAuthed(true); sessionStorage.setItem('sdui-auth', 'true') }} />}

          {showOnboarding && (
            <div className="playground-onboarding">
              <div className="onboarding-content">
                <h3 className="onboarding-title">SDUI Playground</h3>
                <p className="onboarding-desc">
                  This is a live SDUI response editor. The JSON on the left is what the server sends; the preview on the right is what the client renders natively.
                </p>
                <div className="onboarding-hints">
                  <div className="onboarding-hint"><span className="onboarding-hint-icon">✏️</span> Edit JSON directly or double-click text in the preview to change it</div>
                  <div className="onboarding-hint"><span className="onboarding-hint-icon">💬</span> Use the prompt bar to generate layouts by describing what you want</div>
                  <div className="onboarding-hint"><span className="onboarding-hint-icon">📖</span> Open Schema to browse elements — click value chips to apply them</div>
                  <div className="onboarding-hint"><span className="onboarding-hint-icon">➕</span> Use + buttons in Schema to insert new elements into your layout</div>
                </div>
                <button className="onboarding-dismiss" onClick={() => setShowOnboarding(false)}>Got it</button>
              </div>
            </div>
          )}

          <div className={`playground-layout ${showReference || showPlatform ? 'with-reference' : ''}`}>
            <div className="playground-container">
              <div className="playground-editor">
                <div className="editor-toolbar">
                  <div className="editor-mode-tabs">
                    <button className={`mode-tab ${editorMode === 'edit' ? 'active' : ''}`} onClick={() => setEditorMode('edit')}>Editor</button>
                    <button className={`mode-tab ${editorMode === 'diff' ? 'active' : ''}`} onClick={() => setEditorMode('diff')}>Diff</button>
                    <button className={`mode-tab ${editorMode === 'network' ? 'active' : ''}`} onClick={() => setEditorMode('network')}>Network</button>
                  </div>
                  <div className="toolbar-actions">
                    <button className="toolbar-btn" onClick={handleCopyJson}>{copied ? '✓ Copied' : 'Copy'}</button>
                    <button className="toolbar-btn" onClick={handleFormat}>Format</button>
                    <button className="toolbar-btn" onClick={handleReset}>Reset</button>
                  </div>
                </div>
                {editorMode === 'edit' && (
                  <>
                    <div className="editor-wrapper">
                      {highlightLines && (
                        <div className="editor-highlight-overlay" aria-hidden="true">
                          {jsonInput.split('\n').map((_, i) => {
                            const isPrimary = i >= highlightLines.primary.start && i <= highlightLines.primary.end
                            const isSecondary = highlightLines.secondary && i >= highlightLines.secondary.start && i <= highlightLines.secondary.end && !isPrimary
                            return (
                              <div
                                key={i}
                                className={`highlight-line ${isPrimary ? 'active' : ''} ${isSecondary ? 'secondary' : ''}`}
                              />
                            )
                          })}
                        </div>
                      )}
                      {validationMarkers.length > 0 && (
                        <div className="editor-validation-gutter" aria-hidden="true">
                          {jsonInput.split('\n').map((_, i) => {
                            const marker = validationMarkers.find(m => m.line === i)
                            return (
                              <div key={i} className="gutter-line">
                                {marker && (
                                  <span
                                    className={`gutter-marker ${marker.severity}`}
                                    title={marker.message}
                                  >
                                    {marker.severity === 'error' ? '●' : '◐'}
                                  </span>
                                )}
                              </div>
                            )
                          })}
                        </div>
                      )}
                      <textarea
                        ref={textareaRef}
                        className={`editor-textarea ${validationMarkers.length > 0 ? 'with-gutter' : ''}`}
                        value={jsonInput}
                        onChange={(e) => handleChange(e.target.value)}
                        onClick={handleEditorCursor}
                        onKeyUp={handleEditorCursor}
                        spellCheck={false}
                        onScroll={(e) => {
                          const target = e.target as HTMLElement
                          const overlay = target.parentElement?.querySelector('.editor-highlight-overlay') as HTMLElement | null
                          if (overlay) overlay.scrollTop = target.scrollTop
                          const gutter = target.parentElement?.querySelector('.editor-validation-gutter') as HTMLElement | null
                          if (gutter) gutter.scrollTop = target.scrollTop
                        }}
                      />
                    </div>
                    {validationMarkers.length > 0 && !parseError && (
                      <div className={`editor-validation-summary ${validationExpanded ? 'expanded' : ''}`}>
                        <button
                          className="validation-summary-toggle"
                          onClick={() => setValidationExpanded(!validationExpanded)}
                        >
                          <span className="validation-icon">◐</span>
                          <span className="validation-count">{validationMarkers.length} schema {validationMarkers.length === 1 ? 'issue' : 'issues'}</span>
                          <span className="validation-chevron">{validationExpanded ? '▾' : '▸'}</span>
                        </button>
                        {validationExpanded && (
                          <ul className="validation-list">
                            {validationMarkers.map((m, i) => (
                              <li
                                key={i}
                                className={`validation-item ${m.severity}`}
                                onClick={() => {
                                  const ta = textareaRef.current
                                  if (!ta) return
                                  const lines = ta.value.split('\n')
                                  let charPos = 0
                                  for (let l = 0; l < m.line && l < lines.length; l++) {
                                    charPos += lines[l].length + 1
                                  }
                                  ta.focus()
                                  ta.setSelectionRange(charPos, charPos + (lines[m.line]?.length || 0))
                                  const lineHeight = ta.scrollHeight / lines.length
                                  const targetScroll = Math.max(0, m.line * lineHeight - ta.clientHeight / 3)
                                  ta.scrollTop = targetScroll
                                  setHighlightLines({ primary: { start: m.line, end: m.line } })
                                  setTimeout(() => setHighlightLines(null), 2000)
                                }}
                              >
                                <span className="validation-item-marker">{m.severity === 'error' ? '●' : '◐'}</span>
                                <span className="validation-item-message">{m.message}</span>
                                <span className="validation-item-line">line {m.line + 1}</span>
                              </li>
                            ))}
                          </ul>
                        )}
                      </div>
                    )}
                    {parseError && (
                      <div className="editor-error">
                        <span className="error-icon">✕</span>
                        {parseError}
                      </div>
                    )}
                  </>
                )}
                {editorMode === 'diff' && (
                  <DiffView baseJson={baseJson} currentJson={jsonInput} />
                )}
                {editorMode === 'network' && (
                  <NetworkView json={jsonInput} />
                )}
              </div>

              <div className="playground-preview">
                <div className="preview-toolbar">
                  <span className="toolbar-title">Preview</span>
                  <div className="device-toggle">
                    <button className={`device-btn ${deviceFrame === 'phone' ? 'active' : ''}`} onClick={() => setDeviceFrame('phone')} title="Phone">
                      <svg width="12" height="18" viewBox="0 0 12 18" fill="none" stroke="currentColor" strokeWidth="1.5"><rect x="1" y="1" width="10" height="16" rx="2"/><line x1="5" y1="14" x2="7" y2="14"/></svg>
                    </button>
                    <button className={`device-btn ${deviceFrame === 'tablet' ? 'active' : ''}`} onClick={() => setDeviceFrame('tablet')} title="Tablet">
                      <svg width="16" height="18" viewBox="0 0 16 18" fill="none" stroke="currentColor" strokeWidth="1.5"><rect x="1" y="1" width="14" height="16" rx="2"/><line x1="7" y1="14" x2="9" y2="14"/></svg>
                    </button>
                    <button className={`device-btn ${deviceFrame === 'tv' ? 'active' : ''}`} onClick={() => setDeviceFrame('tv')} title="Connected TV">
                      <svg width="18" height="16" viewBox="0 0 18 16" fill="none" stroke="currentColor" strokeWidth="1.5"><rect x="1" y="1" width="16" height="11" rx="1.5"/><line x1="6" y1="14" x2="12" y2="14"/><line x1="9" y1="12" x2="9" y2="14"/></svg>
                    </button>
                    <button className={`device-btn ${deviceFrame === 'desktop' ? 'active' : ''}`} onClick={() => setDeviceFrame('desktop')} title="Desktop / Web">
                      <svg width="18" height="16" viewBox="0 0 18 16" fill="none" stroke="currentColor" strokeWidth="1.5"><rect x="1" y="1" width="16" height="11" rx="1.5"/><path d="M4 14h10M7 12v2M11 12v2"/></svg>
                    </button>
                    <button className={`device-btn ${deviceFrame === 'watch' ? 'active' : ''}`} onClick={() => setDeviceFrame('watch')} title="Watch">
                      <svg width="14" height="18" viewBox="0 0 14 18" fill="none" stroke="currentColor" strokeWidth="1.5"><rect x="3" y="4" width="8" height="10" rx="3"/><path d="M5 4V2h4v2M5 14v2h4v-2"/></svg>
                    </button>
                  </div>
                  {selectedElement && (
                    <span className="selected-indicator">
                      Inspecting: <strong>{selectedElement}</strong>
                    </span>
                  )}
                </div>
                <div className={`preview-frame device-${deviceFrame}`}>
                  <div className="preview-content">
                    {parsedData ? (
                      <SduiRenderer data={parsedData} selectedEl={inspectedElement} onSelectElement={(el) => {
                        setSelectedElement(el.type)
                        setInspectedElement(el)
                        setEditorMode('edit')
                        setTimeout(() => scrollToElement(el), 50)
                      }} onTextEdit={(el, newText) => {
                        const prop = el.content !== undefined ? 'content' : 'text'
                        try {
                          const data = JSON.parse(jsonInput)
                          patchElement(data, el, prop, newText)
                          const newJson = JSON.stringify(data, null, 2)
                          setJsonInput(newJson)
                          setParsedData(data)
                          setParseError(null)
                        } catch { /* ignore */ }
                      }} />
                    ) : (
                      <div className="preview-empty">Fix the JSON to see a preview</div>
                    )}
                  </div>
                </div>
              </div>
            </div>

            {showReference && (
              <SchemaReference
                selectedElement={selectedElement}
                focusedProperty={cursorContext.propertyName}
                onInsertSnippet={handleInsertSnippet}
                onInsertAtCursor={handleInsertAtCursor}
                onPropertyChange={handlePropertyChange}
                onSelectType={(type) => {
                  const typeStr = `"type": "${type}"`
                  const idx = jsonInput.indexOf(typeStr)
                  if (idx === -1) return
                  setSelectedElement(type)
                  scrollToElement({ type })
                }}
              />
            )}
            {showPlatform && (
              <div className="platform-panel">
                <PlatformCode selectedElement={selectedElement} />
              </div>
            )}
          </div>

        </div>
      )}
    </section>
  )
}


// ── Prompt Bar ─────────────────────────────────
function PromptBar({ onGenerate }: { onGenerate: (json: string) => void }) {
  const [prompt, setPrompt] = useState('')
  const [isGenerating, setIsGenerating] = useState(false)

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    if (!prompt.trim()) return
    setIsGenerating(true)
    setTimeout(() => {
      const result = generateFromPrompt(prompt)
      onGenerate(result)
      setIsGenerating(false)
      setPrompt('')
    }, 600)
  }

  return (
    <form className="prompt-bar" onSubmit={handleSubmit}>
      <span className="prompt-icon">✦</span>
      <input
        className="prompt-input"
        type="text"
        value={prompt}
        onChange={(e) => setPrompt(e.target.value)}
        placeholder="Describe what you want to build... (e.g. &quot;player stats card&quot; or &quot;promo with countdown&quot;)"
        disabled={isGenerating}
      />
      <button className="prompt-submit" type="submit" disabled={isGenerating || !prompt.trim()}>
        {isGenerating ? 'Building...' : 'Generate'}
      </button>
    </form>
  )
}

function LoginBar({ onAuth }: { onAuth: () => void }) {
  const [value, setValue] = useState('')
  const [error, setError] = useState(false)

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    if (value === 'nbasdui') {
      onAuth()
    } else {
      setError(true)
      setTimeout(() => setError(false), 2000)
    }
  }

  return (
    <form className="prompt-bar login-bar" onSubmit={handleSubmit}>
      <span className="prompt-icon">🔒</span>
      <input
        className={`prompt-input ${error ? 'input-error' : ''}`}
        type="password"
        value={value}
        onChange={(e) => setValue(e.target.value)}
        placeholder="Enter password to unlock AI generation..."
      />
      <button className="prompt-submit" type="submit" disabled={!value.trim()}>
        Unlock
      </button>
    </form>
  )
}

function generateFromPrompt(prompt: string): string {
  const p = prompt.toLowerCase()

  // ── Extract contextual values from prompt ──
  const teams = extractTeams(p)
  const player = extractPlayer(p)
  const scores = extractScores(p)
  const time = extractTime(p)
  const colors = getTeamColors(teams[0])

  // ── Detect all intents (supports multi-section generation) ──
  const intents: string[] = []
  if (p.includes('stats') || p.includes('player') || p.includes('leader')) intents.push('stats')
  if (p.includes('countdown') || p.includes('timer') || p.includes('clock') || p.includes('starts in')) intents.push('countdown')
  if (p.includes('schedule') || p.includes('list') || p.includes('upcoming') || p.includes('tonight') || p.includes('today')) intents.push('schedule')
  if (p.includes('promo') || p.includes('banner') || p.includes('subscribe') || p.includes('league pass') || p.includes('offer')) intents.push('promo')
  if (p.includes('score') || p.includes('live') || p.includes('scoreboard') || p.includes('playing now')) intents.push('score')
  if (p.includes('news') || p.includes('headline') || p.includes('article') || p.includes('breaking') || p.includes('trade')) intents.push('news')
  if (p.includes('standing') || p.includes('rank') || p.includes('record') || p.includes('conference')) intents.push('standings')
  if (p.includes('highlight') || p.includes('video') || p.includes('replay') || p.includes('clip')) intents.push('highlights')
  if (p.includes('notification') || p.includes('alert') || p.includes('remind')) intents.push('notification')
  if (p.includes('comparison') || p.includes('compare') || p.includes('vs') || p.includes('versus') || p.includes('matchup')) intents.push('matchup')

  if (intents.length === 0) intents.push('custom')

  // ── Build sections for each detected intent ──
  const sections = intents.map(intent => buildSection(intent, { teams, player, scores, time, colors, prompt }))

  return JSON.stringify({ screenId: "generated", sections }, null, 2)
}

// ── Team database ──
const TEAM_DB: Record<string, { abbr: string; full: string; city: string; teamId: string; primary: string; secondary: string }> = {
  lakers: { abbr: "LAL", full: "Los Angeles Lakers", city: "Los Angeles", teamId: "1610612747", primary: "#552583", secondary: "#FDB927" },
  lal: { abbr: "LAL", full: "Los Angeles Lakers", city: "Los Angeles", teamId: "1610612747", primary: "#552583", secondary: "#FDB927" },
  celtics: { abbr: "BOS", full: "Boston Celtics", city: "Boston", teamId: "1610612738", primary: "#007A33", secondary: "#BA9653" },
  bos: { abbr: "BOS", full: "Boston Celtics", city: "Boston", teamId: "1610612738", primary: "#007A33", secondary: "#BA9653" },
  warriors: { abbr: "GSW", full: "Golden State Warriors", city: "Golden State", teamId: "1610612744", primary: "#1D428A", secondary: "#FFC72C" },
  gsw: { abbr: "GSW", full: "Golden State Warriors", city: "Golden State", teamId: "1610612744", primary: "#1D428A", secondary: "#FFC72C" },
  heat: { abbr: "MIA", full: "Miami Heat", city: "Miami", teamId: "1610612748", primary: "#98002E", secondary: "#F9A01B" },
  mia: { abbr: "MIA", full: "Miami Heat", city: "Miami", teamId: "1610612748", primary: "#98002E", secondary: "#F9A01B" },
  nuggets: { abbr: "DEN", full: "Denver Nuggets", city: "Denver", teamId: "1610612743", primary: "#0E2240", secondary: "#FEC524" },
  den: { abbr: "DEN", full: "Denver Nuggets", city: "Denver", teamId: "1610612743", primary: "#0E2240", secondary: "#FEC524" },
  suns: { abbr: "PHX", full: "Phoenix Suns", city: "Phoenix", teamId: "1610612756", primary: "#1D1160", secondary: "#E56020" },
  phx: { abbr: "PHX", full: "Phoenix Suns", city: "Phoenix", teamId: "1610612756", primary: "#1D1160", secondary: "#E56020" },
  bucks: { abbr: "MIL", full: "Milwaukee Bucks", city: "Milwaukee", teamId: "1610612749", primary: "#00471B", secondary: "#EEE1C6" },
  mil: { abbr: "MIL", full: "Milwaukee Bucks", city: "Milwaukee", teamId: "1610612749", primary: "#00471B", secondary: "#EEE1C6" },
  sixers: { abbr: "PHI", full: "Philadelphia 76ers", city: "Philadelphia", teamId: "1610612755", primary: "#006BB6", secondary: "#ED174C" },
  phi: { abbr: "PHI", full: "Philadelphia 76ers", city: "Philadelphia", teamId: "1610612755", primary: "#006BB6", secondary: "#ED174C" },
  knicks: { abbr: "NYK", full: "New York Knicks", city: "New York", teamId: "1610612752", primary: "#006BB6", secondary: "#F58426" },
  nyk: { abbr: "NYK", full: "New York Knicks", city: "New York", teamId: "1610612752", primary: "#006BB6", secondary: "#F58426" },
  bulls: { abbr: "CHI", full: "Chicago Bulls", city: "Chicago", teamId: "1610612741", primary: "#CE1141", secondary: "#000000" },
  chi: { abbr: "CHI", full: "Chicago Bulls", city: "Chicago", teamId: "1610612741", primary: "#CE1141", secondary: "#000000" },
  nets: { abbr: "BKN", full: "Brooklyn Nets", city: "Brooklyn", teamId: "1610612751", primary: "#000000", secondary: "#FFFFFF" },
  bkn: { abbr: "BKN", full: "Brooklyn Nets", city: "Brooklyn", teamId: "1610612751", primary: "#000000", secondary: "#FFFFFF" },
  raptors: { abbr: "TOR", full: "Toronto Raptors", city: "Toronto", teamId: "1610612761", primary: "#CE1141", secondary: "#000000" },
  tor: { abbr: "TOR", full: "Toronto Raptors", city: "Toronto", teamId: "1610612761", primary: "#CE1141", secondary: "#000000" },
  mavs: { abbr: "DAL", full: "Dallas Mavericks", city: "Dallas", teamId: "1610612742", primary: "#00538C", secondary: "#002B5E" },
  mavericks: { abbr: "DAL", full: "Dallas Mavericks", city: "Dallas", teamId: "1610612742", primary: "#00538C", secondary: "#002B5E" },
  dal: { abbr: "DAL", full: "Dallas Mavericks", city: "Dallas", teamId: "1610612742", primary: "#00538C", secondary: "#002B5E" },
  thunder: { abbr: "OKC", full: "Oklahoma City Thunder", city: "Oklahoma City", teamId: "1610612760", primary: "#007AC1", secondary: "#EF6100" },
  okc: { abbr: "OKC", full: "Oklahoma City Thunder", city: "Oklahoma City", teamId: "1610612760", primary: "#007AC1", secondary: "#EF6100" },
  clippers: { abbr: "LAC", full: "LA Clippers", city: "Los Angeles", teamId: "1610612746", primary: "#C8102E", secondary: "#1D428A" },
  lac: { abbr: "LAC", full: "LA Clippers", city: "Los Angeles", teamId: "1610612746", primary: "#C8102E", secondary: "#1D428A" },
  wolves: { abbr: "MIN", full: "Minnesota Timberwolves", city: "Minnesota", teamId: "1610612750", primary: "#0C2340", secondary: "#236192" },
  timberwolves: { abbr: "MIN", full: "Minnesota Timberwolves", city: "Minnesota", teamId: "1610612750", primary: "#0C2340", secondary: "#236192" },
  min: { abbr: "MIN", full: "Minnesota Timberwolves", city: "Minnesota", teamId: "1610612750", primary: "#0C2340", secondary: "#236192" },
}

const PLAYER_DB: Record<string, { name: string; number: string; pos: string; team: string; playerId: string; ppg: string; rpg: string; apg: string }> = {
  lebron: { name: "LeBron James", number: "23", pos: "Forward", team: "Lakers", playerId: "2544", ppg: "25.7", rpg: "7.3", apg: "8.3" },
  curry: { name: "Stephen Curry", number: "30", pos: "Guard", team: "Warriors", playerId: "201939", ppg: "26.4", rpg: "4.5", apg: "5.1" },
  steph: { name: "Stephen Curry", number: "30", pos: "Guard", team: "Warriors", playerId: "201939", ppg: "26.4", rpg: "4.5", apg: "5.1" },
  giannis: { name: "Giannis Antetokounmpo", number: "34", pos: "Forward", team: "Bucks", playerId: "203507", ppg: "30.4", rpg: "11.5", apg: "5.8" },
  jokic: { name: "Nikola Jokić", number: "15", pos: "Center", team: "Nuggets", playerId: "203999", ppg: "26.4", rpg: "12.4", apg: "9.0" },
  luka: { name: "Luka Dončić", number: "77", pos: "Guard", team: "Mavericks", playerId: "1629029", ppg: "33.9", rpg: "9.2", apg: "9.8" },
  doncic: { name: "Luka Dončić", number: "77", pos: "Guard", team: "Mavericks", playerId: "1629029", ppg: "33.9", rpg: "9.2", apg: "9.8" },
  tatum: { name: "Jayson Tatum", number: "0", pos: "Forward", team: "Celtics", playerId: "1628369", ppg: "26.9", rpg: "8.1", apg: "4.9" },
  sga: { name: "Shai Gilgeous-Alexander", number: "2", pos: "Guard", team: "Thunder", playerId: "1628983", ppg: "30.1", rpg: "5.5", apg: "6.2" },
  shai: { name: "Shai Gilgeous-Alexander", number: "2", pos: "Guard", team: "Thunder", playerId: "1628983", ppg: "30.1", rpg: "5.5", apg: "6.2" },
  embiid: { name: "Joel Embiid", number: "21", pos: "Center", team: "76ers", playerId: "203954", ppg: "33.1", rpg: "10.2", apg: "4.2" },
  ant: { name: "Anthony Edwards", number: "5", pos: "Guard", team: "Timberwolves", playerId: "1630162", ppg: "25.9", rpg: "5.4", apg: "5.1" },
  edwards: { name: "Anthony Edwards", number: "5", pos: "Guard", team: "Timberwolves", playerId: "1630162", ppg: "25.9", rpg: "5.4", apg: "5.1" },
}

function extractTeams(p: string): Array<{ abbr: string; full: string; city: string; teamId: string; primary: string; secondary: string }> {
  const found: typeof TEAM_DB[string][] = []
  for (const [key, team] of Object.entries(TEAM_DB)) {
    if (p.includes(key) && !found.some(t => t.abbr === team.abbr)) {
      found.push(team)
    }
  }
  if (found.length === 0) {
    found.push(TEAM_DB.lakers, TEAM_DB.celtics)
  }
  return found
}

function extractPlayer(p: string): typeof PLAYER_DB[string] {
  for (const [key, player] of Object.entries(PLAYER_DB)) {
    if (p.includes(key)) return player
  }
  return PLAYER_DB.lebron
}

function extractScores(p: string): [number, number] {
  const scoreMatch = p.match(/(\d{2,3})\s*[-–to]+\s*(\d{2,3})/)
  if (scoreMatch) return [parseInt(scoreMatch[1]), parseInt(scoreMatch[2])]
  return [108, 112]
}

function extractTime(p: string): string {
  const timeMatch = p.match(/(\d{1,2}:\d{2}\s*(pm|am)?)/i)
  if (timeMatch) return timeMatch[1].toUpperCase()
  return "7:30 PM"
}

function getTeamColors(team?: { primary: string; secondary: string }): { primary: string; secondary: string } {
  return team ?? { primary: "#1D428A", secondary: "#FBCD44" }
}

interface PromptContext {
  teams: ReturnType<typeof extractTeams>
  player: ReturnType<typeof extractPlayer>
  scores: [number, number]
  time: string
  colors: { primary: string; secondary: string }
  prompt: string
}

function buildSection(intent: string, ctx: PromptContext) {
  const { teams, player, scores, time, colors } = ctx
  const [home, away] = [teams[0], teams[1] ?? teams[0]]

  switch (intent) {
    case 'stats':
      return {
        sectionId: "player-stats",
        sectionType: "AtomicComposite",
        content: {
          type: "Container", direction: "column", gap: "md", padding: "lg",
          background: { color: "#191C23" }, cornerRadius: "lg",
          children: [
            { type: "Container", direction: "row", gap: "md", crossAxisAlignment: "center", children: [
              { type: "Image", url: `https://cdn.nba.com/headshots/nba/latest/260x190/${player.playerId}.png`, width: 64, height: 64, cornerRadius: "full" },
              { type: "Container", direction: "column", children: [
                { type: "Text", text: player.name, variant: "titleMedium" },
                { type: "Text", text: `#${player.number} · ${player.pos} · ${player.team}`, variant: "bodySmall", color: "#8E9196" }
              ]}
            ]},
            { type: "Divider" },
            { type: "Container", direction: "row", mainAxisAlignment: "spaceAround", children: [
              { type: "Container", direction: "column", crossAxisAlignment: "center", children: [
                { type: "Text", text: player.ppg, variant: "headlineMedium" },
                { type: "Text", text: "PPG", variant: "labelSmall", color: "#8E9196" }
              ]},
              { type: "Container", direction: "column", crossAxisAlignment: "center", children: [
                { type: "Text", text: player.rpg, variant: "headlineMedium" },
                { type: "Text", text: "RPG", variant: "labelSmall", color: "#8E9196" }
              ]},
              { type: "Container", direction: "column", crossAxisAlignment: "center", children: [
                { type: "Text", text: player.apg, variant: "headlineMedium" },
                { type: "Text", text: "APG", variant: "labelSmall", color: "#8E9196" }
              ]}
            ]}
          ]
        }
      }

    case 'countdown':
      return {
        sectionId: "countdown",
        sectionType: "AtomicComposite",
        content: {
          type: "Container", direction: "column", gap: "lg", padding: "xl",
          background: { color: colors.primary }, cornerRadius: "xl", crossAxisAlignment: "center",
          children: [
            { type: "Text", text: "GAME STARTS IN", variant: "labelLarge", color: colors.secondary },
            { type: "LiveClock", snapshotSeconds: 5400, isRunning: true, tickDirection: "down", format: "h:mm:ss" },
            { type: "Text", text: `${home.abbr} vs ${away.abbr}`, variant: "headlineSmall" },
            { type: "Text", text: `${time} · Tip-Off`, variant: "bodySmall", color: "#8E9196" },
            { type: "Button", label: "Set Reminder", variant: "primary", actions: [{ actionType: "navigate", destination: "nba://reminders" }] }
          ]
        }
      }

    case 'schedule':
      return {
        sectionId: "schedule",
        sectionType: "AtomicComposite",
        content: {
          type: "Container", direction: "column", gap: "sm", padding: "lg",
          background: { color: "#191C23" }, cornerRadius: "lg",
          children: [
            { type: "Text", text: "TODAY'S GAMES", variant: "labelLarge" },
            ...[
              `${home.abbr} vs ${away.abbr} · ${time} · TNT`,
              "GSW vs MIA · 8:00 PM · ESPN",
              "DEN vs PHX · 10:00 PM · NBA TV"
            ].map(game => ({
              type: "Container", direction: "row", mainAxisAlignment: "spaceBetween", crossAxisAlignment: "center",
              padding: "md", background: { color: "#2B2F37" }, cornerRadius: "md",
              children: [
                { type: "Text", text: game, variant: "bodyMedium" },
                { type: "Button", label: "Watch", variant: "text" }
              ]
            }))
          ]
        }
      }

    case 'promo':
      return {
        sectionId: "promo",
        sectionType: "AtomicComposite",
        content: {
          type: "Container", direction: "column", gap: "md", padding: "xl",
          background: { color: colors.primary }, cornerRadius: "xl",
          children: [
            { type: "Text", text: "NBA LEAGUE PASS", variant: "labelLarge", color: colors.secondary },
            { type: "Text", text: "Watch Every Game", variant: "headlineLarge" },
            { type: "Text", text: "Stream live and on-demand games all season. Start your free trial.", variant: "bodyMedium", color: "#B8C4D9" },
            { type: "Container", direction: "row", gap: "md", children: [
              { type: "Button", label: "Start Free Trial", variant: "primary", actions: [{ actionType: "navigate", destination: "nba://subscribe" }] },
              { type: "Button", label: "Learn More", variant: "secondary" }
            ]}
          ]
        }
      }

    case 'score':
      return {
        sectionId: "live-score",
        sectionType: "AtomicComposite",
        content: {
          type: "Container", direction: "column", gap: "md", padding: "lg",
          background: { color: "#191C23" }, cornerRadius: "lg",
          children: [
            { type: "Container", direction: "row", mainAxisAlignment: "spaceBetween", crossAxisAlignment: "center", children: [
              { type: "Text", text: "LIVE", variant: "labelSmall", color: "#C8102E" },
              { type: "LiveClock", snapshotSeconds: 423, isRunning: true, tickDirection: "down", format: "mm:ss" }
            ]},
            { type: "Container", direction: "row", mainAxisAlignment: "spaceBetween", children: [
              { type: "Container", direction: "column", crossAxisAlignment: "center", children: [
                { type: "Image", url: `https://cdn.nba.com/logos/nba/${home.teamId}/primary/L/logo.svg`, width: 48, height: 48 },
                { type: "Text", text: home.abbr, variant: "labelMedium" },
                { type: "Text", text: String(scores[0]), variant: "headlineLarge" }
              ]},
              { type: "Text", text: "Q4", variant: "titleMedium", color: "#8E9196" },
              { type: "Container", direction: "column", crossAxisAlignment: "center", children: [
                { type: "Image", url: `https://cdn.nba.com/logos/nba/${away.teamId}/primary/L/logo.svg`, width: 48, height: 48 },
                { type: "Text", text: away.abbr, variant: "labelMedium" },
                { type: "Text", text: String(scores[1]), variant: "headlineLarge" }
              ]}
            ]},
            { type: "Button", label: "Watch Live", variant: "primary", actions: [{ actionType: "navigate", destination: "nba://watch/live" }] }
          ]
        }
      }

    case 'news':
      return {
        sectionId: "news",
        sectionType: "AtomicComposite",
        content: {
          type: "Container", direction: "column", gap: "md", padding: "lg",
          background: { color: "#191C23" }, cornerRadius: "lg",
          children: [
            { type: "Text", text: "BREAKING NEWS", variant: "labelLarge", color: "#C8102E" },
            { type: "Text", text: `${home.full} Acquire All-Star in Blockbuster Trade`, variant: "headlineSmall" },
            { type: "Text", text: `The ${home.full} have completed a trade sending two first-round picks for a three-time All-Star, sources confirm.`, variant: "bodyMedium", color: "#8E9196" },
            { type: "Container", direction: "row", gap: "md", children: [
              { type: "Button", label: "Full Story", variant: "primary", actions: [{ actionType: "navigate", destination: "nba://news/trade-123" }] },
              { type: "Button", label: "Share", variant: "text", actions: [{ actionType: "share", title: `${home.abbr} Trade News`, url: "https://nba.com/news/trade-123" }] }
            ]}
          ]
        }
      }

    case 'standings':
      return {
        sectionId: "standings",
        sectionType: "AtomicComposite",
        content: {
          type: "Container", direction: "column", gap: "sm", padding: "lg",
          background: { color: "#191C23" }, cornerRadius: "lg",
          children: [
            { type: "Text", text: "WESTERN CONFERENCE", variant: "labelLarge", color: colors.secondary },
            ...[
              { rank: 1, team: "OKC", record: "57-25" },
              { rank: 2, team: "DEN", record: "54-28" },
              { rank: 3, team: home.abbr, record: "52-30" },
              { rank: 4, team: "MIN", record: "49-33" },
              { rank: 5, team: "DAL", record: "47-35" },
            ].map(row => ({
              type: "Container", direction: "row", gap: "md", crossAxisAlignment: "center",
              padding: "sm", background: row.team === home.abbr ? { color: "#2B2F37" } : undefined,
              children: [
                { type: "Text", text: String(row.rank), variant: "labelMedium", color: "#8E9196" },
                { type: "Text", text: row.team, variant: "bodyMedium" },
                { type: "Text", text: row.record, variant: "bodySmall", color: "#8E9196" }
              ]
            }))
          ]
        }
      }

    case 'highlights':
      return {
        sectionId: "highlights",
        sectionType: "AtomicComposite",
        content: {
          type: "Container", direction: "column", gap: "md", padding: "lg",
          background: { color: "#191C23" }, cornerRadius: "lg",
          children: [
            { type: "Text", text: "TOP HIGHLIGHTS", variant: "labelLarge", color: "#FBCD44" },
            { type: "Container", direction: "row", gap: "md", children: [
              { type: "Container", direction: "column", gap: "xs", children: [
                { type: "Image", url: `https://cdn.nba.com/logos/nba/${home.teamId}/primary/L/logo.svg`, width: 120, height: 80, cornerRadius: "md" },
                { type: "Text", text: `${player.name} 40-PT Performance`, variant: "bodySmall" }
              ]},
              { type: "Container", direction: "column", gap: "xs", children: [
                { type: "Image", url: `https://cdn.nba.com/logos/nba/${away.teamId}/primary/L/logo.svg`, width: 120, height: 80, cornerRadius: "md" },
                { type: "Text", text: "Game Recap: Final Highlights", variant: "bodySmall" }
              ]}
            ]},
            { type: "Button", label: "See All Highlights", variant: "text", actions: [{ actionType: "navigate", destination: "nba://video/highlights" }] }
          ]
        }
      }

    case 'notification':
      return {
        sectionId: "notification",
        sectionType: "AtomicComposite",
        content: {
          type: "Container", direction: "row", gap: "md", padding: "md",
          background: { color: "#2B2F37" }, cornerRadius: "lg", crossAxisAlignment: "center",
          children: [
            { type: "Container", direction: "column", gap: "none", children: [
              { type: "Text", text: "🔔", variant: "headlineSmall" }
            ]},
            { type: "Container", direction: "column", gap: "xs", children: [
              { type: "Text", text: `${home.abbr} vs ${away.abbr} starting soon`, variant: "bodyMedium" },
              { type: "Text", text: `Tip-off at ${time}`, variant: "bodySmall", color: "#8E9196" }
            ]},
            { type: "Button", label: "Watch", variant: "primary", actions: [{ actionType: "navigate", destination: "nba://watch/live" }] }
          ]
        }
      }

    case 'matchup':
      return {
        sectionId: "matchup",
        sectionType: "AtomicComposite",
        content: {
          type: "Container", direction: "column", gap: "lg", padding: "lg",
          background: { color: "#191C23" }, cornerRadius: "lg",
          children: [
            { type: "Text", text: "MATCHUP", variant: "labelLarge", color: "#FBCD44" },
            { type: "Container", direction: "row", mainAxisAlignment: "spaceAround", crossAxisAlignment: "center", children: [
              { type: "Container", direction: "column", crossAxisAlignment: "center", gap: "sm", children: [
                { type: "Image", url: `https://cdn.nba.com/logos/nba/${home.teamId}/primary/L/logo.svg`, width: 64, height: 64 },
                { type: "Text", text: home.abbr, variant: "titleMedium" },
                { type: "Text", text: "52-30", variant: "bodySmall", color: "#8E9196" }
              ]},
              { type: "Text", text: "VS", variant: "headlineMedium", color: "#8E9196" },
              { type: "Container", direction: "column", crossAxisAlignment: "center", gap: "sm", children: [
                { type: "Image", url: `https://cdn.nba.com/logos/nba/${away.teamId}/primary/L/logo.svg`, width: 64, height: 64 },
                { type: "Text", text: away.abbr, variant: "titleMedium" },
                { type: "Text", text: "54-28", variant: "bodySmall", color: "#8E9196" }
              ]}
            ]},
            { type: "Divider" },
            { type: "Container", direction: "row", mainAxisAlignment: "spaceAround", children: [
              { type: "Container", direction: "column", crossAxisAlignment: "center", children: [
                { type: "Text", text: "112.4", variant: "titleMedium" },
                { type: "Text", text: "PPG", variant: "labelSmall", color: "#8E9196" }
              ]},
              { type: "Container", direction: "column", crossAxisAlignment: "center", children: [
                { type: "Text", text: "vs", variant: "bodySmall", color: "#8E9196" }
              ]},
              { type: "Container", direction: "column", crossAxisAlignment: "center", children: [
                { type: "Text", text: "115.8", variant: "titleMedium" },
                { type: "Text", text: "PPG", variant: "labelSmall", color: "#8E9196" }
              ]}
            ]},
            { type: "Button", label: "Full Preview", variant: "primary", actions: [{ actionType: "navigate", destination: "nba://matchup" }] }
          ]
        }
      }

    default:
      return {
        sectionId: "custom",
        sectionType: "AtomicComposite",
        content: {
          type: "Container", direction: "column", gap: "md", padding: "lg",
          background: { color: "#191C23" }, cornerRadius: "lg",
          children: [
            { type: "Text", text: ctx.prompt.slice(0, 40).toUpperCase(), variant: "labelLarge", color: "#FBCD44" },
            { type: "Text", text: "Custom Component", variant: "headlineSmall" },
            { type: "Text", text: "Try prompts like: \"Curry stats with countdown timer\" or \"Lakers vs Celtics live score 108-112\" or \"tonight's schedule with league pass promo\"", variant: "bodyMedium", color: "#8E9196" },
            { type: "Button", label: "Get Started", variant: "primary" }
          ]
        }
      }
  }
}

// ── Diff View ──────────────────────────────────
function DiffView({ baseJson, currentJson }: { baseJson: string; currentJson: string }) {
  const baseLines = baseJson.split('\n')
  const currentLines = currentJson.split('\n')
  const maxLen = Math.max(baseLines.length, currentLines.length)
  const diffLines: { type: 'same' | 'added' | 'removed' | 'changed'; line: string; lineNum: number }[] = []

  for (let i = 0; i < maxLen; i++) {
    const baseLine = baseLines[i] ?? ''
    const currLine = currentLines[i] ?? ''
    if (i >= baseLines.length) {
      diffLines.push({ type: 'added', line: currLine, lineNum: i + 1 })
    } else if (i >= currentLines.length) {
      diffLines.push({ type: 'removed', line: baseLine, lineNum: i + 1 })
    } else if (baseLine !== currLine) {
      diffLines.push({ type: 'changed', line: currLine, lineNum: i + 1 })
    } else {
      diffLines.push({ type: 'same', line: currLine, lineNum: i + 1 })
    }
  }

  const hasChanges = diffLines.some(d => d.type !== 'same')

  return (
    <div className="diff-view">
      {!hasChanges && (
        <div className="diff-empty">No changes from the original. Edit the JSON to see a diff.</div>
      )}
      <pre className="diff-content">
        {diffLines.map((d, i) => (
          <div key={i} className={`diff-line diff-${d.type}`}>
            <span className="diff-gutter">{d.lineNum}</span>
            <span className="diff-marker">{d.type === 'added' ? '+' : d.type === 'removed' ? '-' : d.type === 'changed' ? '~' : ' '}</span>
            <span className="diff-text">{d.line}</span>
          </div>
        ))}
      </pre>
    </div>
  )
}

// ── Network Simulation View ────────────────────
function NetworkView({ json }: { json: string }) {
  const [step, setStep] = useState(0)

  const steps = [
    { label: 'Request', method: 'GET', url: '/api/v1/screens/home', status: null, duration: null },
    { label: 'DNS Lookup', method: null, url: 'cdn.nba.com → 104.18.22.1', status: null, duration: '12ms' },
    { label: 'TLS Handshake', method: null, url: 'TLS 1.3 established', status: null, duration: '24ms' },
    { label: 'Response', method: null, url: '200 OK', status: 200, duration: '89ms' },
    { label: 'Parse & Render', method: null, url: 'AtomicRouter → elements', status: null, duration: '6ms' },
  ]

  useEffect(() => {
    if (step >= steps.length) return
    const timer = setTimeout(() => setStep(s => s + 1), 600)
    return () => clearTimeout(timer)
  })

  const restart = () => setStep(0)

  let jsonSize = 0
  try { jsonSize = new Blob([json]).size } catch { jsonSize = json.length }

  return (
    <div className="network-view">
      <div className="network-header">
        <span className="network-title">Request Lifecycle</span>
        <button className="toolbar-btn" onClick={restart}>Replay</button>
      </div>
      <div className="network-timeline">
        {steps.map((s, i) => (
          <div key={i} className={`network-step ${i < step ? 'complete' : ''} ${i === step ? 'active' : ''}`}>
            <div className="network-step-dot" />
            <div className="network-step-info">
              <span className="network-step-label">{s.label}</span>
              {s.method && <code className="network-method">{s.method} {s.url}</code>}
              {!s.method && <span className="network-detail">{s.url}</span>}
              {s.duration && <span className="network-duration">{s.duration}</span>}
              {s.status && <span className="network-status">{s.status}</span>}
            </div>
          </div>
        ))}
      </div>
      <div className="network-summary">
        <div className="network-stat">
          <span className="stat-label">Payload Size</span>
          <span className="stat-value">{(jsonSize / 1024).toFixed(1)} KB</span>
        </div>
        <div className="network-stat">
          <span className="stat-label">Total Time</span>
          <span className="stat-value">131ms</span>
        </div>
        <div className="network-stat">
          <span className="stat-label">Sections</span>
          <span className="stat-value">{(() => { try { return JSON.parse(json)?.sections?.length ?? 0 } catch { return '?' } })()}</span>
        </div>
        <div className="network-stat">
          <span className="stat-label">Cache</span>
          <span className="stat-value">HIT (CDN Edge)</span>
        </div>
      </div>
      <div className="network-refresh">
        <span className="network-refresh-title">Refresh Policy</span>
        <div className="network-refresh-info">
          <code className="refresh-code">{`{ "type": "interval", "seconds": 30, "onEvent": "gameStateChange" }`}</code>
          <span className="refresh-desc">Client re-fetches every 30s or when a game state push arrives — no manual refresh needed.</span>
        </div>
      </div>
    </div>
  )
}

// Lightweight SDUI renderer for the playground
function SduiRenderer({ data, onSelectElement, selectedEl, onTextEdit }: { data: any; onSelectElement: (el: any) => void; selectedEl: any; onTextEdit?: (el: any, newText: string) => void }) {
  const sections = data?.data?.sections || data?.sections
  if (!sections) return <div className="preview-empty">Add a "sections" array</div>

  return (
    <div className="sdui-screen">
      {sections.map((section: any, i: number) => {
        const ui = section.data?.ui || section.content
        return (
          <div key={section.id || section.sectionId || i} className="sdui-section">
            {ui && <AtomicElement element={ui} onSelect={onSelectElement} selectedEl={selectedEl} onTextEdit={onTextEdit} />}
          </div>
        )
      })}
    </div>
  )
}

function EditableText({ element, isSelected, onClick, onTextEdit }: { element: any; isSelected: boolean; onClick: (e: React.MouseEvent) => void; onTextEdit?: (el: any, newText: string) => void }) {
  const [editing, setEditing] = useState(false)
  const [editValue, setEditValue] = useState('')
  const inputRef = useRef<HTMLInputElement>(null)

  const startEdit = () => {
    setEditValue(element.content || element.text || '')
    setEditing(true)
    setTimeout(() => inputRef.current?.select(), 0)
  }

  const commitEdit = () => {
    setEditing(false)
    const newText = editValue.trim()
    if (newText && newText !== (element.content || element.text)) {
      onTextEdit?.(element, newText)
    }
  }

  if (editing) {
    return (
      <input
        ref={inputRef}
        className={`atomic-text-input variant-${element.variant || 'bodyMedium'}`}
        value={editValue}
        onChange={(e) => setEditValue(e.target.value)}
        onBlur={commitEdit}
        onKeyDown={(e) => {
          if (e.key === 'Enter') commitEdit()
          if (e.key === 'Escape') setEditing(false)
        }}
        style={{ color: element.color, fontWeight: element.weight === 'bold' ? 700 : element.weight === 'semiBold' ? 600 : undefined }}
      />
    )
  }

  return (
    <span
      className={`atomic-text variant-${element.variant || 'bodyMedium'} atomic-hoverable ${isSelected ? 'atomic-selected' : ''}`}
      style={{ color: element.color, fontWeight: element.weight === 'bold' ? 700 : element.weight === 'semiBold' ? 600 : undefined }}
      onClick={onClick}
      onDoubleClick={(e) => { e.stopPropagation(); startEdit() }}
      title="Double-click to edit text"
    >
      {element.content || element.text}
    </span>
  )
}

function AtomicElement({ element, onSelect, selectedEl, onTextEdit }: { element: any; onSelect: (el: any) => void; selectedEl: any; onTextEdit?: (el: any, newText: string) => void }) {
  if (!element || !element.type) return null
  const isSelected = element === selectedEl

  const handleClick = (e: React.MouseEvent) => {
    e.stopPropagation()
    onSelect(element)
  }

  switch (element.type) {
    case 'Container':
      return (
        <div
          className={`atomic-container atomic-hoverable ${isSelected ? 'atomic-selected' : ''}`}
          onClick={handleClick}
          style={{
            flexDirection: element.direction === 'row' ? 'row' : 'column',
            gap: gapValue(element.gap),
            padding: paddingValue(element.padding),
            background: element.background?.color || (typeof element.background === 'string' ? undefined : undefined),
            borderRadius: radiusValue(element.cornerRadius),
            justifyContent: alignValue(element.alignment || element.mainAxisAlignment),
            alignItems: alignValue(element.crossAlignment || element.crossAxisAlignment),
            width: element.widthMode === 'fill' ? '100%' : undefined,
          }}
        >
          {element.children?.map((child: any, i: number) => (
            <AtomicElement key={i} element={child} onSelect={onSelect} selectedEl={selectedEl} onTextEdit={onTextEdit} />
          ))}
        </div>
      )
    case 'Text':
      return (
        <EditableText
          element={element}
          isSelected={isSelected}
          onClick={handleClick}
          onTextEdit={onTextEdit}
        />
      )
    case 'Image':
      return (
        <div
          className={`atomic-image atomic-hoverable ${isSelected ? 'atomic-selected' : ''}`}
          style={{ width: element.width, height: element.height }}
          onClick={handleClick}
        >
          <img
            src={element.src || element.url}
            alt={element.accessibility?.label || element.alt || ''}
            style={{ width: '100%', height: '100%', objectFit: (element.fit as any) || 'contain' }}
            onError={(e) => {
              (e.target as HTMLImageElement).style.display = 'none'
              const parent = (e.target as HTMLImageElement).parentElement
              if (parent) parent.style.background = 'var(--surface-raised)'
            }}
          />
        </div>
      )
    case 'Button':
      return (
        <button
          className={`atomic-button button-${element.variant || 'primary'} atomic-hoverable ${isSelected ? 'atomic-selected' : ''}`}
          onClick={handleClick}
        >
          {element.label}
        </button>
      )
    case 'Spacer':
      return (
        <div className={`atomic-hoverable ${isSelected ? 'atomic-selected' : ''}`} style={{ height: tokenToPixels(element.height) || tokenToPixels(element.width) || element.size || 8 }} onClick={handleClick} />
      )
    case 'Divider':
      return <div className={`atomic-divider atomic-hoverable ${isSelected ? 'atomic-selected' : ''}`} onClick={handleClick} />
    case 'LiveClock':
      return (
        <span className={`atomic-text variant-${element.variant || 'bodyMedium'} atomic-hoverable ${isSelected ? 'atomic-selected' : ''}`} onClick={handleClick}>
          {formatClock(element.snapshotSeconds, element.format)}
        </span>
      )
    case 'Conditional':
      return (
        <div className={`atomic-conditional atomic-hoverable ${isSelected ? 'atomic-selected' : ''}`} onClick={handleClick}>
          <span className="conditional-badge">IF</span>
          <AtomicElement element={element.then} onSelect={onSelect} selectedEl={selectedEl} onTextEdit={onTextEdit} />
        </div>
      )
    case 'ScrollContainer':
      return (
        <div className={`atomic-container atomic-hoverable ${isSelected ? 'atomic-selected' : ''}`} onClick={handleClick} style={{ flexDirection: 'row', overflowX: 'auto', gap: gapValue(element.gap) }}>
          {element.children?.map((child: any, i: number) => (
            <AtomicElement key={i} element={child} onSelect={onSelect} selectedEl={selectedEl} onTextEdit={onTextEdit} />
          ))}
        </div>
      )
    case 'OverlayContainer':
      return (
        <div className={`atomic-container atomic-hoverable ${isSelected ? 'atomic-selected' : ''}`} onClick={handleClick} style={{ position: 'relative' }}>
          {element.base && <AtomicElement element={element.base} onSelect={onSelect} selectedEl={selectedEl} onTextEdit={onTextEdit} />}
          {element.overlays?.map((child: any, i: number) => (
            <AtomicElement key={i} element={child} onSelect={onSelect} selectedEl={selectedEl} onTextEdit={onTextEdit} />
          ))}
        </div>
      )
    default:
      return (
        <div className={`atomic-unknown atomic-hoverable ${isSelected ? 'atomic-selected' : ''}`} onClick={handleClick}>
          Unknown: {element.type}
        </div>
      )
  }
}

function findNearestChildrenArray(obj: any): any[] | null {
  if (!obj || typeof obj !== 'object') return null
  if (Array.isArray(obj)) {
    for (const item of obj) {
      const found = findNearestChildrenArray(item)
      if (found) return found
    }
    return null
  }
  if (obj.children && Array.isArray(obj.children)) return obj.children
  for (const val of Object.values(obj)) {
    if (typeof val === 'object' && val !== null) {
      const found = findNearestChildrenArray(val)
      if (found) return found
    }
  }
  return null
}

function findElementAtCursor(obj: any, json: string, cursorPos: number, type: string): any {
  // Find which element object the cursor is inside, then match it in the parsed tree
  let braceStart = cursorPos
  let depth = 0
  while (braceStart >= 0) {
    if (json[braceStart] === '}') depth++
    if (json[braceStart] === '{') {
      if (depth === 0) break
      depth--
    }
    braceStart--
  }
  if (braceStart < 0) return null

  // Extract the block and check if it has the expected type
  let endDepth = 1
  let braceEnd = braceStart + 1
  while (braceEnd < json.length && endDepth > 0) {
    if (json[braceEnd] === '{') endDepth++
    if (json[braceEnd] === '}') endDepth--
    braceEnd++
  }
  const block = json.slice(braceStart, braceEnd)
  const typeMatch = block.match(/"type"\s*:\s*"([^"]+)"/)
  if (!typeMatch || typeMatch[1] !== type) return null

  // Parse the block to get a reference object, then find matching element in tree
  try {
    const parsed = JSON.parse(block)
    return findMatchingElement(obj, parsed)
  } catch {
    return null
  }
}

function findMatchingElement(tree: any, target: any): any {
  if (!tree || typeof tree !== 'object') return null
  if (Array.isArray(tree)) {
    for (const item of tree) {
      const found = findMatchingElement(item, target)
      if (found) return found
    }
    return null
  }
  if (tree.type === target.type && shallowMatch(tree, target)) return tree
  for (const val of Object.values(tree)) {
    if (typeof val === 'object' && val !== null) {
      const found = findMatchingElement(val, target)
      if (found) return found
    }
  }
  return null
}

function findElementByType(obj: any, type: string): any {
  if (!obj || typeof obj !== 'object') return null
  if (Array.isArray(obj)) {
    for (const item of obj) {
      const found = findElementByType(item, type)
      if (found) return found
    }
    return null
  }
  if (obj.type === type) return obj
  for (const val of Object.values(obj)) {
    if (typeof val === 'object' && val !== null) {
      const found = findElementByType(val, type)
      if (found) return found
    }
  }
  return null
}

function patchElement(obj: any, target: any, property: string, value: string): boolean {
  if (!obj || typeof obj !== 'object') return false
  if (obj === target || (obj.type === target.type && shallowMatch(obj, target))) {
    obj[property] = value
    return true
  }
  for (const key of Object.keys(obj)) {
    if (typeof obj[key] === 'object') {
      if (patchElement(obj[key], target, property, value)) return true
    }
  }
  return false
}

function shallowMatch(a: any, b: any): boolean {
  if (a.type !== b.type) return false
  const keys = Object.keys(b).filter(k => k !== 'children' && k !== 'then' && k !== 'else' && k !== 'background' && k !== 'foreground')
  return keys.every(k => {
    if (typeof b[k] === 'object') return true
    return a[k] === b[k]
  })
}

function formatClock(seconds: number | undefined, format: string | undefined): string {
  if (seconds == null) return '0:00'
  const m = Math.floor(seconds / 60)
  const s = seconds % 60
  if (format === 'mm:ss') return `${m}:${s.toString().padStart(2, '0')}`
  return `${m}:${s.toString().padStart(2, '0')}`
}

function tokenToPixels(token: string | number | undefined): number | undefined {
  if (token == null) return undefined
  if (typeof token === 'number') return token
  const map: Record<string, number> = {
    'token:nba.spacing.xs': 2, 'token:nba.spacing.sm': 4, 'token:nba.spacing.md': 12,
    'token:nba.spacing.lg': 16, 'token:nba.spacing.xl': 32, 'token:nba.spacing.2xl': 40,
    xs: 2, sm: 4, md: 12, lg: 16, xl: 32, '2xl': 40,
  }
  return map[token]
}

function gapValue(token: string | undefined): string {
  if (!token) return '0'
  const px = tokenToPixels(token)
  return px ? `${px}px` : '0'
}

function paddingValue(padding: any): string | undefined {
  if (!padding) return undefined
  if (typeof padding === 'string') return gapValue(padding)
  if (typeof padding === 'object') {
    const t = tokenToPixels(padding.top) || 0
    const r = tokenToPixels(padding.end) || 0
    const b = tokenToPixels(padding.bottom) || 0
    const l = tokenToPixels(padding.start) || 0
    return `${t}px ${r}px ${b}px ${l}px`
  }
  return undefined
}

function radiusValue(token: string | undefined): string {
  if (!token) return '0'
  const map: Record<string, string> = {
    'token:nba.radius.xs': '2px', 'token:nba.radius.sm': '4px', 'token:nba.radius.md': '12px',
    'token:nba.radius.lg': '16px', 'token:nba.radius.xl': '24px', 'token:nba.radius.2xl': '32px',
    'token:nba.radius.full': '9999px',
    xs: '2px', sm: '4px', md: '12px', lg: '16px', xl: '24px', '2xl': '32px', full: '9999px',
  }
  return map[token] || '0'
}

function alignValue(alignment: string | undefined): string {
  const map: Record<string, string> = {
    start: 'flex-start',
    end: 'flex-end',
    center: 'center',
    spaceBetween: 'space-between',
    spaceAround: 'space-around',
  }
  return alignment ? (map[alignment] || 'flex-start') : 'flex-start'
}

// ── Element Schema Definitions ──────────────────

interface PropDef {
  name: string
  type: string
  description: string
  required?: boolean
  values?: string[]
}

interface ElementSchema {
  description: string
  properties: PropDef[]
}

const ELEMENT_SCHEMAS: Record<string, ElementSchema> = {
  Container: {
    description: 'Flex layout container — the primary building block for server-composed layouts.',
    properties: [
      { name: 'direction', type: 'string', description: 'Flex axis direction', required: true, values: ['row', 'column'] },
      { name: 'gap', type: 'spacing token', description: 'Space between children', values: ['xs', 'sm', 'md', 'lg', 'xl', '2xl'] },
      { name: 'padding', type: 'Spacing', description: '{ top, bottom, start, end } with token values' },
      { name: 'alignment', type: 'string', description: 'Justify content along main axis', values: ['start', 'end', 'center', 'spaceBetween', 'spaceAround'] },
      { name: 'crossAlignment', type: 'string', description: 'Align items along cross axis', values: ['start', 'end', 'center', 'stretch', 'baseline'] },
      { name: 'backgrounds', type: 'array', description: 'Array of ColorToken, Gradient, or Image backgrounds' },
      { name: 'cornerRadius', type: 'radius token', description: 'Border radius', values: ['xs', 'sm', 'md', 'lg', 'xl', '2xl', 'full'] },
      { name: 'widthMode', type: 'string', description: 'How width is calculated', values: ['hug', 'fill', 'fixed'] },
      { name: 'heightMode', type: 'string', description: 'How height is calculated', values: ['hug', 'fill', 'fixed'] },
      { name: 'flex', type: 'number', description: 'Flex grow weight relative to siblings' },
      { name: 'shadow', type: 'object', description: '{ color, radius, offsetX, offsetY }' },
      { name: 'children', type: 'element[]', description: 'Child atomic elements (max 20)', required: true },
    ],
  },
  Text: {
    description: 'Styled text element mapped to the design system\'s typography scale.',
    properties: [
      { name: 'content', type: 'string', description: 'The text content to display', required: true },
      { name: 'variant', type: 'string', description: 'Typography variant from the type scale', required: true, values: ['displayLarge', 'displayMedium', 'displaySmall', 'headlineLarge', 'headlineMedium', 'headlineSmall', 'titleLarge', 'titleMedium', 'titleSmall', 'bodyLarge', 'bodyMedium', 'bodySmall', 'labelLarge', 'labelMedium', 'labelSmall', 'score'] },
      { name: 'color', type: 'string', description: 'Override text color (hex or CSS color)' },
      { name: 'weight', type: 'number', description: 'Override font weight (100–900)' },
      { name: 'maxLines', type: 'number', description: 'Maximum lines before truncation with ellipsis' },
      { name: 'textAlign', type: 'string', description: 'Text alignment', values: ['start', 'center', 'end'] },
    ],
  },
  Image: {
    description: 'Remote image with dimensions and content scaling.',
    properties: [
      { name: 'src', type: 'string', description: 'Image URL (remote)', required: true },
      { name: 'width', type: 'number', description: 'Display width in logical pixels' },
      { name: 'height', type: 'number', description: 'Display height in logical pixels' },
      { name: 'accessibility', type: 'object', description: '{ label: "description" }' },
      { name: 'fit', type: 'string', description: 'How image fills its bounds', values: ['cover', 'contain', 'fill', 'none'] },
      { name: 'cornerRadius', type: 'radius token', description: 'Clip the image with rounded corners', values: ['xs', 'sm', 'md', 'lg', 'xl', '2xl', 'full'] },
    ],
  },
  Button: {
    description: 'Interactive button with label and actions array.',
    properties: [
      { name: 'label', type: 'string', description: 'Button text', required: true },
      { name: 'variant', type: 'string', description: 'Visual style', required: true, values: ['primary', 'secondary', 'tertiary', 'text'] },
      { name: 'actions', type: 'action[]', description: 'Actions to fire on tap (navigate, fireAndForget, refresh, etc.)' },
      { name: 'disabled', type: 'boolean', description: 'Whether the button is non-interactive' },
      { name: 'icon', type: 'string', description: 'Optional icon identifier shown before label' },
    ],
  },
  Spacer: {
    description: 'Fixed-size empty space between elements.',
    properties: [
      { name: 'size', type: 'number', description: 'Height/width in logical pixels (based on parent direction)' },
    ],
  },
  Divider: {
    description: 'Horizontal or vertical line separator.',
    properties: [
      { name: 'direction', type: 'string', description: 'Orientation', values: ['horizontal', 'vertical'] },
      { name: 'color', type: 'string', description: 'Override divider color (defaults to theme divider)' },
      { name: 'thickness', type: 'number', description: 'Line thickness in pixels (default: 1)' },
    ],
  },
  ScrollContainer: {
    description: 'Scrollable region for content that overflows.',
    properties: [
      { name: 'direction', type: 'string', description: 'Scroll direction', required: true, values: ['horizontal', 'vertical'] },
      { name: 'paging', type: 'boolean', description: 'Enable paged/snapping scroll behavior' },
      { name: 'showIndicator', type: 'boolean', description: 'Show scroll position indicator' },
      { name: 'children', type: 'element[]', description: 'Child elements inside the scroll region', required: true },
    ],
  },
  LiveClock: {
    description: 'Client-side ticking clock driven by server-provided snapshot.',
    properties: [
      { name: 'snapshotSeconds', type: 'number', description: 'Clock value at snapshot time', required: true },
      { name: 'snapshotAt', type: 'string', description: 'ISO timestamp of when snapshot was taken' },
      { name: 'isRunning', type: 'boolean', description: 'Whether the clock is ticking' },
      { name: 'tickDirection', type: 'string', description: 'Count up or down', values: ['up', 'down'] },
      { name: 'stopAtSeconds', type: 'number', description: 'Stop ticking at this value' },
      { name: 'format', type: 'string', description: 'Display format', values: ['m:ss', 'mm:ss', 'h:mm:ss', 'ss.t'] },
    ],
  },
  Conditional: {
    description: 'State-driven if/else branching for conditional display.',
    properties: [
      { name: 'condition', type: 'object', description: 'Condition to evaluate ({ field, operator, value })', required: true },
      { name: 'then', type: 'element', description: 'Element tree to render when condition is true', required: true },
      { name: 'else', type: 'element', description: 'Element tree to render when condition is false' },
    ],
  },
}

// ── Schema Validation ────────────────────────────

interface ValidationMarker {
  line: number
  severity: 'error' | 'warning'
  message: string
}

const VALIDATION_SCHEMAS: Record<string, { required: string[]; enums: Record<string, string[]> }> = {
  Container: {
    required: ['direction', 'children'],
    enums: {
      direction: ['row', 'column'],
      gap: ['xs', 'sm', 'md', 'lg', 'xl', '2xl'],
      alignment: ['start', 'end', 'center', 'spaceBetween', 'spaceAround'],
      mainAxisAlignment: ['start', 'end', 'center', 'spaceBetween', 'spaceAround'],
      crossAlignment: ['start', 'end', 'center', 'stretch', 'baseline'],
      crossAxisAlignment: ['start', 'end', 'center', 'stretch', 'baseline'],
      cornerRadius: ['xs', 'sm', 'md', 'lg', 'xl', '2xl', 'full'],
      widthMode: ['hug', 'fill', 'fixed'],
      heightMode: ['hug', 'fill', 'fixed'],
    },
  },
  Text: {
    required: ['content|text', 'variant'],
    enums: {
      variant: ['displayLarge', 'displayMedium', 'displaySmall', 'headlineLarge', 'headlineMedium', 'headlineSmall', 'titleLarge', 'titleMedium', 'titleSmall', 'bodyLarge', 'bodyMedium', 'bodySmall', 'labelLarge', 'labelMedium', 'labelSmall', 'score'],
      textAlign: ['start', 'center', 'end'],
    },
  },
  Image: {
    required: ['src|url'],
    enums: {
      fit: ['cover', 'contain', 'fill', 'none'],
      cornerRadius: ['xs', 'sm', 'md', 'lg', 'xl', '2xl', 'full'],
    },
  },
  Button: {
    required: ['label', 'variant'],
    enums: {
      variant: ['primary', 'secondary', 'tertiary', 'text'],
    },
  },
  Spacer: { required: [], enums: {} },
  Divider: {
    required: [],
    enums: { direction: ['horizontal', 'vertical'] },
  },
  ScrollContainer: {
    required: ['direction', 'children'],
    enums: { direction: ['horizontal', 'vertical'] },
  },
  LiveClock: {
    required: ['snapshotSeconds'],
    enums: {
      tickDirection: ['up', 'down'],
      format: ['m:ss', 'mm:ss', 'h:mm:ss', 'ss.t'],
    },
  },
  Conditional: {
    required: ['condition', 'then'],
    enums: {},
  },
}

function validateSchema(json: string): ValidationMarker[] {
  let parsed: any
  try { parsed = JSON.parse(json) } catch { return [] }

  const markers: ValidationMarker[] = []
  const lines = json.split('\n')

  function findLineOfProperty(elementStartLine: number, propName: string): number {
    for (let i = elementStartLine; i < lines.length && i < elementStartLine + 30; i++) {
      if (lines[i].includes(`"${propName}"`)) return i
    }
    return elementStartLine
  }

  function findLineOfType(startSearchFrom: number, type: string): number {
    for (let i = startSearchFrom; i < lines.length; i++) {
      if (lines[i].includes(`"type"`) && lines[i].includes(`"${type}"`)) return i
    }
    return 0
  }

  let typeOccurrenceIndex = 0
  function walkElements(obj: any) {
    if (!obj || typeof obj !== 'object') return
    if (Array.isArray(obj)) {
      obj.forEach(item => walkElements(item))
      return
    }

    if (obj.type && typeof obj.type === 'string') {
      const schema = VALIDATION_SCHEMAS[obj.type]
      if (schema) {
        const typeLine = findLineOfType(typeOccurrenceIndex, obj.type)
        typeOccurrenceIndex = typeLine + 1

        // Check missing required fields (supports "a|b" alternatives)
        for (const req of schema.required) {
          const alternatives = req.split('|')
          const hasAny = alternatives.some(alt => obj[alt] !== undefined)
          if (!hasAny) {
            markers.push({
              line: typeLine,
              severity: 'error',
              message: `${obj.type}: missing required "${alternatives[0]}"`,
            })
          }
        }

        // Check invalid enum values
        for (const [prop, validValues] of Object.entries(schema.enums)) {
          if (obj[prop] !== undefined && typeof obj[prop] === 'string' && !validValues.includes(obj[prop])) {
            const propLine = findLineOfProperty(typeLine, prop)
            markers.push({
              line: propLine,
              severity: 'warning',
              message: `${obj.type}.${prop}: "${obj[prop]}" is not valid (expected: ${validValues.join(', ')})`,
            })
          }
        }
      }
    }

    // Recurse into all object values
    for (const val of Object.values(obj)) {
      if (typeof val === 'object' && val !== null) {
        walkElements(val)
      }
    }
  }

  walkElements(parsed)
  return markers
}

// ── Example presets ──────────────────────────────

const EXAMPLE_BLANK = `{
  "screenId": "my-screen",
  "sections": [
    {
      "sectionId": "section-1",
      "sectionType": "AtomicComposite",
      "content": {
        "type": "Container",
        "direction": "column",
        "gap": "md",
        "padding": { "top": "lg", "bottom": "lg", "start": "lg", "end": "lg" },
        "backgrounds": [{ "color": "#191C23" }],
        "cornerRadius": "lg",
        "children": [
          { "type": "Text", "content": "Your Title Here", "variant": "headlineSmall" },
          { "type": "Text", "content": "Start building by adding elements from the Schema panel.", "variant": "bodySmall", "color": "#8E9196" }
        ]
      }
    }
  ]
}`

const EXAMPLE_GAME_CARD = `{
  "screenId": "game",
  "sections": [
    {
      "sectionId": "game-card-1",
      "sectionType": "AtomicComposite",
      "content": {
        "type": "Container",
        "direction": "column",
        "gap": "md",
        "padding": { "top": "lg", "bottom": "lg", "start": "lg", "end": "lg" },
        "backgrounds": [{ "color": "#191C23" }],
        "cornerRadius": "lg",
        "children": [
          { "type": "Text", "content": "LIVE · Q4 2:34", "variant": "labelSmall", "color": "#C8102E" },
          {
            "type": "Container",
            "direction": "row",
            "alignment": "spaceBetween",
            "crossAlignment": "center",
            "children": [
              {
                "type": "Container",
                "direction": "column",
                "crossAlignment": "center",
                "gap": "sm",
                "children": [
                  { "type": "Image", "src": "https://cdn.nba.com/logos/nba/1610612747/primary/D/512x512/logo.png", "width": 48, "height": 48, "fit": "contain" },
                  { "type": "Text", "content": "LAL", "variant": "titleMedium" },
                  { "type": "Text", "content": "108", "variant": "headlineMedium" }
                ]
              },
              {
                "type": "LiveClock",
                "snapshotSeconds": 154,
                "isRunning": true,
                "tickDirection": "down",
                "format": "m:ss"
              },
              {
                "type": "Container",
                "direction": "column",
                "crossAlignment": "center",
                "gap": "sm",
                "children": [
                  { "type": "Image", "src": "https://cdn.nba.com/logos/nba/1610612738/primary/D/512x512/logo.png", "width": 48, "height": 48, "fit": "contain" },
                  { "type": "Text", "content": "BOS", "variant": "titleMedium" },
                  { "type": "Text", "content": "112", "variant": "headlineMedium" }
                ]
              }
            ]
          },
          { "type": "Divider" },
          { "type": "Button", "label": "Watch Live", "variant": "primary", "actions": [{ "type": "navigate", "targetUri": "nba://watch/0022400123" }] }
        ]
      }
    }
  ]
}`

const EXAMPLE_NEWS_CARD = `{
  "screenId": "news",
  "sections": [
    {
      "sectionId": "news-card-1",
      "sectionType": "AtomicComposite",
      "content": {
        "type": "Container",
        "direction": "column",
        "gap": "md",
        "padding": { "top": "lg", "bottom": "lg", "start": "lg", "end": "lg" },
        "backgrounds": [{ "color": "#191C23" }],
        "cornerRadius": "lg",
        "children": [
          { "type": "Text", "content": "BREAKING", "variant": "labelSmall", "color": "#C8102E" },
          { "type": "Text", "content": "Lakers Acquire All-Star in Blockbuster Trade", "variant": "headlineSmall" },
          { "type": "Text", "content": "The Los Angeles Lakers have completed a trade sending two first-round picks for a three-time All-Star, sources confirm.", "variant": "bodyMedium", "color": "#8E9196" },
          { "type": "Spacer", "size": 4 },
          {
            "type": "Container",
            "direction": "row",
            "gap": "md",
            "children": [
              { "type": "Button", "label": "Full Story", "variant": "primary", "actions": [{ "type": "navigate", "targetUri": "nba://news/trade-123" }] },
              { "type": "Button", "label": "Share", "variant": "text", "actions": [{ "type": "fireAndForget", "event": "share_tap", "params": { "article": "trade-123" } }] }
            ]
          }
        ]
      }
    }
  ]
}`

const EXAMPLE_PROMO = `{
  "screenId": "promo",
  "sections": [
    {
      "sectionId": "promo-1",
      "sectionType": "AtomicComposite",
      "content": {
        "type": "Container",
        "direction": "column",
        "gap": "md",
        "padding": { "top": "xl", "bottom": "xl", "start": "xl", "end": "xl" },
        "backgrounds": [{ "color": "#1D428A" }],
        "cornerRadius": "xl",
        "children": [
          { "type": "Text", "content": "NBA LEAGUE PASS", "variant": "labelLarge", "color": "#FBCD44" },
          { "type": "Text", "content": "Watch Every Game", "variant": "headlineLarge" },
          { "type": "Text", "content": "Stream live and on-demand games all season long. Start your free trial today.", "variant": "bodyMedium", "color": "#B8C4D9" },
          {
            "type": "Container",
            "direction": "row",
            "gap": "md",
            "children": [
              { "type": "Button", "label": "Start Free Trial", "variant": "primary", "actions": [{ "type": "navigate", "targetUri": "nba://subscribe" }] },
              { "type": "Button", "label": "Learn More", "variant": "secondary", "actions": [{ "type": "navigate", "targetUri": "nba://league-pass" }] }
            ]
          }
        ]
      }
    }
  ]
}`
