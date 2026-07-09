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
  const [highlightLines, setHighlightLines] = useState<{ start: number; end: number } | null>(null)

  const scrollToElement = useCallback((el: Record<string, any>) => {
    const ta = textareaRef.current
    if (!ta) return
    const text = ta.value
    // Find the element's "type": "X" in the JSON
    const typeStr = `"type": "${el.type}"`
    let searchFrom = 0
    let matchIndex = -1
    const distinguisher = el.content || el.src || el.label || el.bindRef
    while (true) {
      const idx = text.indexOf(typeStr, searchFrom)
      if (idx === -1) break
      matchIndex = idx
      if (!distinguisher) break
      const nearby = text.slice(idx, idx + 300)
      if (nearby.includes(String(distinguisher))) break
      searchFrom = idx + 1
    }
    if (matchIndex === -1) return

    // Find the opening brace of this element
    let braceStart = text.lastIndexOf('{', matchIndex)
    // Find the matching closing brace
    let depth = 1
    let braceEnd = braceStart + 1
    while (braceEnd < text.length && depth > 0) {
      if (text[braceEnd] === '{') depth++
      if (text[braceEnd] === '}') depth--
      braceEnd++
    }

    // Calculate line numbers for highlight
    const lines = text.split('\n')
    const startLine = text.slice(0, braceStart).split('\n').length - 1
    const endLine = text.slice(0, braceEnd).split('\n').length - 1
    setHighlightLines({ start: startLine, end: endLine })

    // Scroll: use character position to calculate proportional scroll
    const lineHeight = ta.scrollHeight / lines.length
    const targetScroll = Math.max(0, startLine * lineHeight - ta.clientHeight / 3)
    ta.scrollTop = targetScroll

    // Sync highlight overlay
    const overlay = ta.parentElement?.querySelector('.editor-highlight-overlay') as HTMLElement | null
    if (overlay) overlay.scrollTop = targetScroll

    // Clear highlight after a delay
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

  const handleInsertSnippet = (snippet: string) => {
    setJsonInput(snippet)
    try {
      setParsedData(JSON.parse(snippet))
      setParseError(null)
    } catch (e) {
      setParseError((e as Error).message)
    }
  }

  const handleReorder = (parentChildren: any[], fromIndex: number, toIndex: number) => {
    // Swap in place (parentChildren is a live ref into parsedData)
    const item = parentChildren[fromIndex]
    parentChildren.splice(fromIndex, 1)
    parentChildren.splice(toIndex, 0, item)
    // Re-serialize from the mutated tree and set fresh state
    const updated = JSON.stringify(parsedData, null, 2)
    setJsonInput(updated)
    setParsedData(JSON.parse(updated))
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

  const handlePropertyChange = useCallback((elementType: string, property: string, value: string) => {
    if (!inspectedElement || inspectedElement.type !== elementType) return
    const updated = { ...inspectedElement, [property]: value }
    setInspectedElement(updated)

    // Update the JSON string by finding and patching the element in the parsed data
    try {
      const data = JSON.parse(jsonInput)
      patchElement(data, inspectedElement, property, value)
      const newJson = JSON.stringify(data, null, 2)
      setJsonInput(newJson)
      setParsedData(data)
      setParseError(null)
    } catch {
      // fallback: ignore if JSON is broken
    }
  }, [inspectedElement, jsonInput])

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
                <button className="example-btn" onClick={() => handleInsertSnippet(EXAMPLE_SCORE)}>Game Score</button>
                <button className="example-btn" onClick={() => handleInsertSnippet(EXAMPLE_RAIL)}>Content Rail</button>
                <button className="example-btn" onClick={() => handleInsertSnippet(EXAMPLE_PROMO)}>Promo Banner</button>
                <button className="example-btn" onClick={() => handleInsertSnippet(EXAMPLE_CONDITIONAL)}>Conditional</button>
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

          <PromptBar onGenerate={handleInsertSnippet} />

          {showOnboarding && (
            <div className="playground-onboarding">
              <div className="onboarding-content">
                <h3 className="onboarding-title">SDUI Playground</h3>
                <p className="onboarding-desc">
                  This is a live SDUI response editor. The JSON on the left is what the server sends; the preview on the right is what the client renders natively.
                </p>
                <div className="onboarding-hints">
                  <div className="onboarding-hint"><span className="onboarding-hint-icon">✏️</span> Edit the JSON to see the preview update in real-time</div>
                  <div className="onboarding-hint"><span className="onboarding-hint-icon">💬</span> Use the prompt bar to generate sections by describing what you want</div>
                  <div className="onboarding-hint"><span className="onboarding-hint-icon">📖</span> Open Schema to browse element types and their properties</div>
                  <div className="onboarding-hint"><span className="onboarding-hint-icon">🎯</span> Click any element in the preview to inspect it</div>
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
                    <button className="toolbar-btn" onClick={handleFormat}>Format</button>
                    <button className="toolbar-btn" onClick={handleReset}>Reset</button>
                  </div>
                </div>
                {editorMode === 'edit' && (
                  <>
                    <div className="editor-wrapper">
                      {highlightLines && (
                        <div className="editor-highlight-overlay" aria-hidden="true">
                          {jsonInput.split('\n').map((_, i) => (
                            <div
                              key={i}
                              className={`highlight-line ${i >= highlightLines.start && i <= highlightLines.end ? 'active' : ''}`}
                            />
                          ))}
                        </div>
                      )}
                      <textarea
                        ref={textareaRef}
                        className="editor-textarea"
                        value={jsonInput}
                        onChange={(e) => handleChange(e.target.value)}
                        onClick={handleEditorCursor}
                        onKeyUp={handleEditorCursor}
                        spellCheck={false}
                        onScroll={(e) => {
                          const overlay = (e.target as HTMLElement).previousElementSibling as HTMLElement | null
                          if (overlay) overlay.scrollTop = (e.target as HTMLElement).scrollTop
                        }}
                      />
                    </div>
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
                  {selectedElement && (
                    <span className="selected-indicator">
                      Inspecting: <strong>{selectedElement}</strong>
                    </span>
                  )}
                </div>
                <div className="preview-frame">
                  <div className="preview-content">
                    {parsedData ? (
                      <SduiRenderer data={parsedData} onReorder={handleReorder} onSelectElement={(el) => {
                        setSelectedElement(el.type)
                        setInspectedElement(el)
                        setEditorMode('edit')
                        setTimeout(() => scrollToElement(el), 50)
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

          {selectedElement && inspectedElement && (
            <PropertyInspector elementType={selectedElement} elementData={inspectedElement} onFocus={() => scrollToElement(inspectedElement)} />
          )}
        </div>
      )}
    </section>
  )
}

function PropertyInspector({ elementType, elementData, onFocus }: { elementType: string; elementData: Record<string, any>; onFocus?: () => void }) {
  const schema = ELEMENT_SCHEMAS[elementType]
  if (!schema) return null

  const SPACING_MAP: Record<string, string> = { xs: '4px', sm: '8px', md: '12px', lg: '16px', xl: '24px', xxl: '32px' }

  return (
    <div className="property-inspector">
      <div className="inspector-header" onClick={onFocus} style={{ cursor: onFocus ? 'pointer' : undefined }}>
        <span className="inspector-type">{elementType}</span>
        <span className="inspector-hint">{onFocus ? 'Click to locate in editor' : 'Current values for this instance'}</span>
      </div>
      <div className="inspector-grid">
        {schema.properties.map((prop) => {
          const currentValue = elementData[prop.name]
          const isSet = currentValue !== undefined
          const displayValue = isSet
            ? (typeof currentValue === 'object' ? JSON.stringify(currentValue) : String(currentValue))
            : null

          return (
            <div key={prop.name} className={`inspector-prop ${isSet ? 'has-value' : 'unset'}`}>
              <div className="prop-name-row">
                <span className="prop-name">{prop.name}</span>
                {prop.required && <span className="prop-required">required</span>}
              </div>
              <div className="prop-current">
                {isSet ? (
                  <>
                    <span className="prop-current-value">{displayValue}</span>
                    {prop.values && SPACING_MAP[currentValue] && (
                      <span className="prop-resolved">= {SPACING_MAP[currentValue]}</span>
                    )}
                  </>
                ) : (
                  <span className="prop-not-set">not set</span>
                )}
              </div>
              {prop.values && (
                <div className="prop-values">
                  {prop.values.map((v) => (
                    <span key={v} className={`prop-value ${v === String(currentValue) ? 'active' : ''}`}>{v}</span>
                  ))}
                </div>
              )}
              {!prop.values && <span className="prop-desc">{prop.description}</span>}
            </div>
          )
        })}
      </div>
    </div>
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
function SduiRenderer({ data, onSelectElement, onReorder }: { data: any; onSelectElement: (el: any) => void; onReorder?: (parent: any, fromIndex: number, toIndex: number) => void }) {
  const sections = data?.data?.sections || data?.sections
  if (!sections) return <div className="preview-empty">Add a "sections" array</div>

  return (
    <div className="sdui-screen">
      {sections.map((section: any, i: number) => {
        const ui = section.data?.ui || section.content
        return (
          <div key={section.id || section.sectionId || i} className="sdui-section">
            {ui && <AtomicElement element={ui} onSelect={onSelectElement} onReorder={onReorder} parentChildren={null} index={0} />}
          </div>
        )
      })}
    </div>
  )
}

function AtomicElement({ element, onSelect, onReorder, parentChildren, index }: { element: any; onSelect: (el: any) => void; onReorder?: (parent: any, fromIndex: number, toIndex: number) => void; parentChildren: any[] | null; index: number }) {
  if (!element || !element.type) return null

  const handleClick = (e: React.MouseEvent) => {
    e.stopPropagation()
    onSelect(element)
  }

  const canMoveUp = parentChildren && index > 0
  const canMoveDown = parentChildren && index < parentChildren.length - 1

  const reorderButtons = parentChildren && onReorder ? (
    <div className="reorder-buttons" onClick={(e) => e.stopPropagation()}>
      {canMoveUp && <button className="reorder-btn" onClick={() => onReorder(parentChildren, index, index - 1)}>↑</button>}
      {canMoveDown && <button className="reorder-btn" onClick={() => onReorder(parentChildren, index, index + 1)}>↓</button>}
    </div>
  ) : null

  switch (element.type) {
    case 'Container':
      return (
        <div
          className="atomic-container atomic-hoverable"
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
          {reorderButtons}
          {element.children?.map((child: any, i: number) => (
            <AtomicElement key={i} element={child} onSelect={onSelect} onReorder={onReorder} parentChildren={element.children} index={i} />
          ))}
        </div>
      )
    case 'Text':
      return (
        <span
          className={`atomic-text variant-${element.variant || 'bodyMedium'} atomic-hoverable atomic-reorderable`}
          style={{ color: element.color, fontWeight: element.weight === 'bold' ? 700 : element.weight === 'semiBold' ? 600 : undefined }}
          onClick={handleClick}
        >
          {reorderButtons}
          {element.content || element.text}
        </span>
      )
    case 'Image':
      return (
        <div
          className="atomic-image atomic-hoverable atomic-reorderable"
          style={{ width: element.width, height: element.height }}
          onClick={handleClick}
        >
          {reorderButtons}
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
          className={`atomic-button button-${element.variant || 'primary'} atomic-hoverable atomic-reorderable`}
          onClick={handleClick}
        >
          {reorderButtons}
          {element.label}
        </button>
      )
    case 'Spacer':
      return (
        <div className="atomic-hoverable atomic-reorderable" style={{ height: tokenToPixels(element.height) || tokenToPixels(element.width) || element.size || 8 }} onClick={handleClick}>
          {reorderButtons}
        </div>
      )
    case 'Divider':
      return (
        <div className="atomic-divider atomic-hoverable atomic-reorderable" onClick={handleClick}>
          {reorderButtons}
        </div>
      )
    case 'LiveClock':
      return (
        <span className={`atomic-text variant-${element.variant || 'bodyMedium'} atomic-hoverable atomic-reorderable`} onClick={handleClick}>
          {reorderButtons}
          {formatClock(element.snapshotSeconds, element.format)}
        </span>
      )
    case 'Conditional':
      return (
        <div className="atomic-conditional atomic-hoverable atomic-reorderable" onClick={handleClick}>
          {reorderButtons}
          <span className="conditional-badge">IF</span>
          <AtomicElement element={element.then} onSelect={onSelect} onReorder={onReorder} parentChildren={null} index={0} />
        </div>
      )
    case 'ScrollContainer':
      return (
        <div className="atomic-container atomic-hoverable" onClick={handleClick} style={{ flexDirection: 'row', overflowX: 'auto', gap: gapValue(element.gap) }}>
          {reorderButtons}
          {element.children?.map((child: any, i: number) => (
            <AtomicElement key={i} element={child} onSelect={onSelect} onReorder={onReorder} parentChildren={element.children} index={i} />
          ))}
        </div>
      )
    case 'OverlayContainer':
      return (
        <div className="atomic-container atomic-hoverable" onClick={handleClick} style={{ position: 'relative' }}>
          {reorderButtons}
          {element.base && <AtomicElement element={element.base} onSelect={onSelect} onReorder={onReorder} parentChildren={null} index={0} />}
          {element.overlays?.map((child: any, i: number) => (
            <AtomicElement key={i} element={child} onSelect={onSelect} onReorder={onReorder} parentChildren={element.overlays} index={i} />
          ))}
        </div>
      )
    default:
      return (
        <div className="atomic-unknown atomic-hoverable atomic-reorderable" onClick={handleClick}>
          {reorderButtons}
          Unknown: {element.type}
        </div>
      )
  }
}

// Walk the parsed data tree to find the element by reference and patch a property
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
    'token:nba.spacing.xs': 4, 'token:nba.spacing.sm': 8, 'token:nba.spacing.md': 12,
    'token:nba.spacing.lg': 16, 'token:nba.spacing.xl': 24, 'token:nba.spacing.xxl': 32,
    xs: 4, sm: 8, md: 12, lg: 16, xl: 24, xxl: 32,
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
    'token:nba.radius.sm': '4px', 'token:nba.radius.md': '8px', 'token:nba.radius.lg': '12px',
    'token:nba.radius.xl': '16px', 'token:nba.radius.full': '9999px',
    sm: '4px', md: '6px', lg: '8px', xl: '12px', full: '9999px',
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
      { name: 'gap', type: 'spacing token', description: 'Space between children', values: ['xs', 'sm', 'md', 'lg', 'xl', 'xxl'] },
      { name: 'padding', type: 'spacing token', description: 'Inner padding on all sides', values: ['xs', 'sm', 'md', 'lg', 'xl', 'xxl'] },
      { name: 'mainAxisAlignment', type: 'string', description: 'Justify content along main axis', values: ['start', 'end', 'center', 'spaceBetween', 'spaceAround'] },
      { name: 'crossAxisAlignment', type: 'string', description: 'Align items along cross axis', values: ['start', 'end', 'center', 'stretch', 'baseline'] },
      { name: 'background', type: 'object', description: 'Background color or gradient ({ color } or { type, colors, angle })' },
      { name: 'cornerRadius', type: 'radius token', description: 'Border radius', values: ['sm', 'md', 'lg', 'xl', 'full'] },
      { name: 'widthMode', type: 'string', description: 'How width is calculated', values: ['hug', 'fill', 'fixed'] },
      { name: 'heightMode', type: 'string', description: 'How height is calculated', values: ['hug', 'fill', 'fixed'] },
      { name: 'flex', type: 'number', description: 'Flex grow weight relative to siblings' },
      { name: 'shadows', type: 'array', description: 'Box shadows array ({ type, color, radius, offsetX, offsetY })' },
      { name: 'children', type: 'element[]', description: 'Child atomic elements (max 20)', required: true },
    ],
  },
  Text: {
    description: 'Styled text element mapped to the design system\'s typography scale.',
    properties: [
      { name: 'text', type: 'string', description: 'The text content to display', required: true },
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
      { name: 'url', type: 'string', description: 'Image URL (remote)', required: true },
      { name: 'width', type: 'number', description: 'Display width in logical pixels' },
      { name: 'height', type: 'number', description: 'Display height in logical pixels' },
      { name: 'alt', type: 'string', description: 'Accessibility description' },
      { name: 'contentScale', type: 'string', description: 'How image fills its bounds', values: ['fit', 'fill', 'crop', 'none'] },
      { name: 'cornerRadius', type: 'radius token', description: 'Clip the image with rounded corners', values: ['sm', 'md', 'lg', 'xl', 'full'] },
    ],
  },
  Button: {
    description: 'Interactive button with label and actions array.',
    properties: [
      { name: 'label', type: 'string', description: 'Button text', required: true },
      { name: 'variant', type: 'string', description: 'Visual style', required: true, values: ['primary', 'secondary', 'text', 'destructive'] },
      { name: 'actions', type: 'action[]', description: 'Actions to fire on tap (navigate, analytics, refresh, etc.)' },
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
      { name: 'isRunning', type: 'boolean', description: 'Whether the clock is ticking', required: true },
      { name: 'tickDirection', type: 'string', description: 'Count up or down', values: ['up', 'down'] },
      { name: 'stopAtSeconds', type: 'number', description: 'Stop ticking at this value' },
      { name: 'format', type: 'string', description: 'Display format', values: ['mm:ss', 'h:mm:ss', 'ss.t'] },
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

// ── Example presets ──────────────────────────────

const EXAMPLE_SCORE = `{
  "screenId": "game",
  "sections": [
    {
      "sectionId": "score-1",
      "sectionType": "AtomicComposite",
      "content": {
        "type": "Container",
        "direction": "column",
        "gap": "md",
        "padding": "lg",
        "background": { "color": "#191C23" },
        "cornerRadius": "lg",
        "children": [
          { "type": "Text", "text": "FINAL", "variant": "labelSmall", "color": "#8E9196" },
          {
            "type": "Container",
            "direction": "row",
            "mainAxisAlignment": "spaceBetween",
            "children": [
              {
                "type": "Container",
                "direction": "row",
                "gap": "lg",
                "crossAxisAlignment": "center",
                "children": [
                  { "type": "Text", "text": "LAL", "variant": "titleMedium" },
                  { "type": "Text", "text": "108", "variant": "headlineMedium" }
                ]
              },
              {
                "type": "Container",
                "direction": "row",
                "gap": "sm",
                "crossAxisAlignment": "center",
                "children": [
                  { "type": "Text", "text": "BOS", "variant": "titleMedium" },
                  { "type": "Text", "text": "112", "variant": "headlineMedium" }
                ]
              }
            ]
          },
          { "type": "Divider" },
          { "type": "Text", "text": "L. James: 32 PTS, 8 REB, 7 AST", "variant": "bodySmall", "color": "#8E9196" }
        ]
      }
    }
  ]
}`

const EXAMPLE_RAIL = `{
  "screenId": "feed",
  "sections": [
    {
      "sectionId": "rail-1",
      "sectionType": "AtomicComposite",
      "content": {
        "type": "Container",
        "direction": "column",
        "gap": "md",
        "padding": "lg",
        "children": [
          {
            "type": "Container",
            "direction": "row",
            "mainAxisAlignment": "spaceBetween",
            "crossAxisAlignment": "center",
            "children": [
              { "type": "Text", "text": "TRENDING NOW", "variant": "labelLarge" },
              {
                "type": "Button",
                "label": "See All",
                "variant": "text",
                "actions": [{ "actionType": "navigate", "destination": "nba://trending" }]
              }
            ]
          },
          {
            "type": "Container",
            "direction": "row",
            "gap": "md",
            "children": [
              {
                "type": "Container",
                "direction": "column",
                "gap": "sm",
                "padding": "md",
                "background": { "color": "#2B2F37" },
                "cornerRadius": "md",
                "children": [
                  { "type": "Text", "text": "Top Play", "variant": "labelSmall", "color": "#FBCD44" },
                  { "type": "Text", "text": "LeBron's clutch three", "variant": "titleSmall" }
                ]
              },
              {
                "type": "Container",
                "direction": "column",
                "gap": "sm",
                "padding": "md",
                "background": { "color": "#2B2F37" },
                "cornerRadius": "md",
                "children": [
                  { "type": "Text", "text": "Highlight", "variant": "labelSmall", "color": "#69ABF3" },
                  { "type": "Text", "text": "Curry's 50-pt game", "variant": "titleSmall" }
                ]
              }
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
        "padding": "xl",
        "background": { "color": "#1D428A" },
        "cornerRadius": "xl",
        "children": [
          { "type": "Text", "text": "NBA LEAGUE PASS", "variant": "labelLarge", "color": "#FBCD44" },
          { "type": "Text", "text": "Watch Every Game", "variant": "headlineLarge" },
          { "type": "Text", "text": "Stream live and on-demand games all season long. Start your free trial today.", "variant": "bodyMedium", "color": "#B8C4D9" },
          {
            "type": "Container",
            "direction": "row",
            "gap": "md",
            "children": [
              {
                "type": "Button",
                "label": "Start Free Trial",
                "variant": "primary",
                "actions": [{ "actionType": "navigate", "destination": "nba://subscribe" }]
              },
              {
                "type": "Button",
                "label": "Learn More",
                "variant": "secondary",
                "actions": [{ "actionType": "navigate", "destination": "nba://league-pass" }]
              }
            ]
          }
        ]
      }
    }
  ]
}`

const EXAMPLE_CONDITIONAL = `{
  "screenId": "personalized",
  "sections": [
    {
      "sectionId": "welcome-1",
      "sectionType": "AtomicComposite",
      "content": {
        "type": "Container",
        "direction": "column",
        "gap": "lg",
        "padding": "lg",
        "background": { "color": "#191C23" },
        "cornerRadius": "lg",
        "children": [
          {
            "type": "Conditional",
            "condition": {
              "field": "user.isSubscribed",
              "operator": "equals",
              "value": true
            },
            "then": {
              "type": "Container",
              "direction": "column",
              "gap": "sm",
              "children": [
                { "type": "Text", "text": "LEAGUE PASS", "variant": "labelSmall", "color": "#30D158" },
                { "type": "Text", "text": "Your Games Tonight", "variant": "headlineSmall" }
              ]
            },
            "else": {
              "type": "Container",
              "direction": "column",
              "gap": "md",
              "children": [
                { "type": "Text", "text": "UPGRADE", "variant": "labelSmall", "color": "#FBCD44" },
                { "type": "Text", "text": "Don't Miss a Moment", "variant": "headlineSmall" },
                { "type": "Text", "text": "Get access to every out-of-market game.", "variant": "bodySmall", "color": "#8E9196" },
                { "type": "Button", "label": "Subscribe", "variant": "primary" }
              ]
            }
          }
        ]
      }
    }
  ]
}`
