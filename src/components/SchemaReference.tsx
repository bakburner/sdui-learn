import { useState, useEffect, useRef } from 'react'
import './SchemaReference.css'

interface SchemaReferenceProps {
  selectedElement: string | null
  inspectedElement?: Record<string, any> | null
  focusedProperty?: string | null
  onInsertSnippet: (snippet: string) => void
  onInsertAtCursor?: (snippet: string) => void
  onPropertyChange?: (elementType: string, property: string, value: string) => void
  onSelectType?: (elementType: string) => void
}

export function SchemaReference({ selectedElement, inspectedElement, focusedProperty, onInsertSnippet, onInsertAtCursor, onPropertyChange, onSelectType }: SchemaReferenceProps) {
  const [activeTab, setActiveTab] = useState<'elements' | 'tokens' | 'actions'>('elements')
  const [expandedElement, setExpandedElement] = useState<string | null>(selectedElement)

  useEffect(() => {
    if (selectedElement) {
      setExpandedElement(selectedElement)
      setActiveTab('elements')
    }
  }, [selectedElement])

  return (
    <div className="schema-reference">
      <div className="reference-header">
        <h4 className="reference-title">Schema Reference</h4>
        <div className="reference-tabs">
          <button
            className={`ref-tab ${activeTab === 'elements' ? 'active' : ''}`}
            onClick={() => setActiveTab('elements')}
          >
            Elements
          </button>
          <button
            className={`ref-tab ${activeTab === 'tokens' ? 'active' : ''}`}
            onClick={() => setActiveTab('tokens')}
          >
            Tokens
          </button>
          <button
            className={`ref-tab ${activeTab === 'actions' ? 'active' : ''}`}
            onClick={() => setActiveTab('actions')}
          >
            Actions
          </button>
        </div>
      </div>

      <div className="reference-body">
        {activeTab === 'elements' && (
          <ElementsTab
            expanded={expandedElement}
            focusedProperty={focusedProperty}
            onExpand={(type) => {
              setExpandedElement(type)
              if (type && onSelectType) onSelectType(type)
            }}
            onInsert={onInsertSnippet}
            onInsertAtCursor={onInsertAtCursor}
            onValueClick={onPropertyChange}
          />
        )}
        {activeTab === 'tokens' && <TokensTab />}
        {activeTab === 'actions' && <ActionsTab />}
      </div>
    </div>
  )
}

function ElementsTab({
  expanded,
  focusedProperty,
  onExpand,
  onInsert,
  onInsertAtCursor,
  onValueClick,
}: {
  expanded: string | null
  focusedProperty?: string | null
  onExpand: (id: string | null) => void
  onInsert: (snippet: string) => void
  onInsertAtCursor?: (snippet: string) => void
  onValueClick?: (elementType: string, property: string, value: string) => void
}) {
  const highlightRef = useRef<HTMLTableRowElement>(null)

  useEffect(() => {
    if (highlightRef.current) {
      highlightRef.current.scrollIntoView({ behavior: 'smooth', block: 'nearest' })
    }
  }, [focusedProperty, expanded])

  return (
    <div className="ref-list">
      {ELEMENT_DOCS.map((el) => (
        <div key={el.type} className={`ref-item ${expanded === el.type ? 'expanded' : ''}`}>
          <div className="ref-item-header-row">
            <button
              className="ref-item-header"
              onClick={() => onExpand(expanded === el.type ? null : el.type)}
            >
              <span className="ref-item-type">{el.type}</span>
              <span className="ref-item-desc">{el.shortDesc}</span>
              <span className="ref-chevron">{expanded === el.type ? '▾' : '›'}</span>
            </button>
            {onInsertAtCursor && (
              <button
                className="ref-insert-cursor-btn"
                title={`Insert ${el.type} at cursor`}
                onClick={(e) => {
                  e.stopPropagation()
                  onInsertAtCursor(STARTER_SNIPPETS[el.type] || `{ "type": "${el.type}" }`)
                }}
              >
                +
              </button>
            )}
          </div>
          {expanded === el.type && (
            <div className="ref-item-body">
              <p className="ref-item-detail">{el.detail}</p>
              <table className="ref-props-table">
                <thead>
                  <tr>
                    <th>Property</th>
                    <th>Type</th>
                    <th>Values / Description</th>
                  </tr>
                </thead>
                <tbody>
                  {el.props.map((p) => {
                    const isHighlighted = focusedProperty === p.name
                    return (
                      <tr
                        key={p.name}
                        ref={isHighlighted ? highlightRef : undefined}
                        className={`${p.required ? 'required-row' : ''} ${isHighlighted ? 'highlighted-row' : ''}`}
                      >
                        <td>
                          <code className="prop-code">{p.name}</code>
                          {p.required && <span className="req-badge">req</span>}
                        </td>
                        <td className="type-cell">{p.type}</td>
                        <td>
                          {p.values ? (
                            <div className="value-chips">
                              {p.values.map((v) => (
                                <code
                                  key={v}
                                  className={`value-chip clickable`}
                                  onClick={() => onValueClick?.(el.type, p.name, v)}
                                >
                                  {v}
                                </code>
                              ))}
                            </div>
                          ) : p.name === 'content' || p.name === 'label' ? (
                            <EditablePropertyInput
                              elementType={el.type}
                              property={p.name}
                              placeholder={p.desc || ''}
                              currentValue={inspectedElement?.type === el.type ? inspectedElement?.[p.name] : undefined}
                              onCommit={onValueClick}
                            />
                          ) : (
                            <span className="desc-text">{p.desc}</span>
                          )}
                        </td>
                      </tr>
                    )
                  })}
                </tbody>
              </table>
              <div className="ref-item-actions">
                {el.snippet && (
                  <button className="insert-btn" onClick={() => onInsert(el.snippet!)}>
                    Replace with Example →
                  </button>
                )}
              </div>
            </div>
          )}
        </div>
      ))}
    </div>
  )
}

function EditablePropertyInput({ elementType, property, placeholder, currentValue, onCommit }: {
  elementType: string
  property: string
  placeholder: string
  currentValue?: string
  onCommit?: (elementType: string, property: string, value: string) => void
}) {
  const [value, setValue] = useState(currentValue || '')
  useEffect(() => {
    setValue(currentValue || '')
  }, [currentValue])
  const commit = () => {
    if (value.trim() && onCommit) {
      onCommit(elementType, property, value.trim())
    }
  }
  return (
    <div className="editable-prop-input">
      <input
        type="text"
        className="prop-text-input"
        placeholder={placeholder}
        value={value}
        onChange={(e) => setValue(e.target.value)}
        onBlur={commit}
        onKeyDown={(e) => { if (e.key === 'Enter') commit() }}
      />
    </div>
  )
}

function TokensTab() {
  return (
    <div className="ref-tokens">
      <div className="token-group">
        <h5 className="token-group-title">Spacing Tokens</h5>
        <p className="token-group-desc">Used for gap, padding, and margin properties.</p>
        <div className="token-grid">
          {SPACING_TOKENS.map((t) => (
            <div key={t.name} className="token-item">
              <div className="token-visual" style={{ width: t.px, height: 12, background: 'var(--nba-tint)' }} />
              <code className="token-name">{t.name}</code>
              <span className="token-value">{t.px}px</span>
            </div>
          ))}
        </div>
      </div>

      <div className="token-group">
        <h5 className="token-group-title">Corner Radius Tokens</h5>
        <p className="token-group-desc">Used for cornerRadius property.</p>
        <div className="token-grid">
          {RADIUS_TOKENS.map((t) => (
            <div key={t.name} className="token-item">
              <div
                className="token-visual-radius"
                style={{ borderRadius: t.px }}
              />
              <code className="token-name">{t.name}</code>
              <span className="token-value">{t.px}px</span>
            </div>
          ))}
        </div>
      </div>

      <div className="token-group">
        <h5 className="token-group-title">Typography Variants</h5>
        <p className="token-group-desc">Used for Text element's variant property.</p>
        <div className="typo-list">
          {TYPO_TOKENS.map((t) => (
            <div key={t.variant} className="typo-item">
              <span className={`typo-preview ${t.family}`} style={{ fontSize: t.size }}>
                {t.preview}
              </span>
              <div className="typo-meta">
                <code className="token-name">{t.variant}</code>
                <span className="token-value">{t.size}px · {t.familyName} · {t.weight}</span>
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  )
}

function ActionsTab() {
  return (
    <div className="ref-actions">
      {ACTION_DOCS.map((action) => (
        <div key={action.type} className="action-item">
          <div className="action-header">
            <code className="action-type">{action.type}</code>
            <span className="action-desc">{action.description}</span>
          </div>
          <div className="action-fields">
            {action.fields.map((f) => (
              <div key={f.name} className="action-field">
                <code className="field-name">{f.name}</code>
                <span className="field-type">{f.type}</span>
                <span className="field-desc">{f.desc}</span>
              </div>
            ))}
          </div>
          <pre className="action-example"><code>{action.example}</code></pre>
        </div>
      ))}
    </div>
  )
}

// ── Starter Snippets (minimal valid elements) ───

const STARTER_SNIPPETS: Record<string, string> = {
  Container: '{ "type": "Container", "direction": "column", "gap": "md", "children": [] }',
  Text: '{ "type": "Text", "content": "Hello", "variant": "bodyMedium" }',
  Image: '{ "type": "Image", "src": "https://cdn.nba.com/logos/nba/1610612747/primary/D/512x512/logo.png", "width": 48, "height": 48 }',
  Button: '{ "type": "Button", "label": "Tap Me", "variant": "primary" }',
  Spacer: '{ "type": "Spacer", "size": 16 }',
  Divider: '{ "type": "Divider" }',
  ScrollContainer: '{ "type": "ScrollContainer", "direction": "horizontal", "children": [] }',
  LiveClock: '{ "type": "LiveClock", "snapshotSeconds": 720, "isRunning": true, "tickDirection": "down", "format": "mm:ss" }',
  Conditional: '{ "type": "Conditional", "condition": { "field": "user.isSubscribed", "operator": "equals", "value": true }, "then": { "type": "Text", "content": "Subscribed", "variant": "bodyMedium" } }',
  DisplayGrid: '{ "type": "DisplayGrid", "columns": [{ "header": "Player", "width": "fill" }, { "header": "PTS", "width": "fixed" }], "rows": [["LeBron James", "25"], ["Stephen Curry", "30"]] }',
  OverlayContainer: '{ "type": "OverlayContainer", "base": { "type": "Image", "src": "https://cdn.nba.com/logos/nba/1610612747/primary/D/512x512/logo.png", "width": 200, "height": 120 }, "overlays": [{ "type": "Text", "content": "Overlay", "variant": "headlineSmall" }] }',
}

// ── Data ────────────────────────────────────────

interface PropDoc {
  name: string
  type: string
  required?: boolean
  values?: string[]
  desc?: string
}

interface ElementDoc {
  type: string
  shortDesc: string
  detail: string
  props: PropDoc[]
  snippet?: string
}

const ELEMENT_DOCS: ElementDoc[] = [
  {
    type: 'Container',
    shortDesc: 'Flex layout (row/column)',
    detail: 'The primary layout building block. Supports flex direction, alignment, gap, padding, backgrounds, shadows, and responsive behavior. Max 20 children, max depth 6.',
    props: [
      { name: 'direction', type: 'string', required: true, values: ['row', 'column'] },
      { name: 'gap', type: 'token', values: ['xs', 'sm', 'md', 'lg', 'xl', '2xl'] },
      { name: 'padding', type: 'Spacing', desc: '{ top, bottom, start, end } with token values' },
      { name: 'alignment', type: 'string', values: ['start', 'end', 'center', 'spaceBetween', 'spaceAround'] },
      { name: 'crossAlignment', type: 'string', values: ['start', 'end', 'center', 'stretch', 'baseline'] },
      { name: 'backgrounds', type: 'array', desc: 'Array of ColorToken, Gradient, or Image backgrounds' },
      { name: 'cornerRadius', type: 'token', values: ['xs', 'sm', 'md', 'lg', 'xl', '2xl', 'full'] },
      { name: 'widthMode', type: 'string', values: ['hug', 'fill', 'fixed'] },
      { name: 'heightMode', type: 'string', values: ['hug', 'fill', 'fixed'] },
      { name: 'flex', type: 'number', desc: 'Flex grow weight (e.g. 1, 2)' },
      { name: 'shadow', type: 'object', desc: '{ color, radius, offsetX, offsetY }' },
      { name: 'children', type: 'element[]', required: true, desc: 'Child elements (max 20)' },
    ],
  },
  {
    type: 'Text',
    shortDesc: 'Styled typography',
    detail: 'Renders text using the platform\'s native typography scale. Variant maps to the design system — each platform interprets it with its native fonts.',
    props: [
      { name: 'content', type: 'string', required: true, desc: 'Content to display' },
      { name: 'variant', type: 'string', required: true, values: ['displayLarge', 'displayMedium', 'displaySmall', 'headlineLarge', 'headlineMedium', 'headlineSmall', 'titleLarge', 'titleMedium', 'titleSmall', 'bodyLarge', 'bodyMedium', 'bodySmall', 'labelLarge', 'labelMedium', 'labelSmall', 'score'] },
      { name: 'color', type: 'string', desc: 'Hex color override (e.g. "#8E9196")' },
      { name: 'weight', type: 'number', desc: 'Font weight override (100–900)' },
      { name: 'maxLines', type: 'number', desc: 'Truncate with ellipsis after N lines' },
      { name: 'textAlign', type: 'string', values: ['start', 'center', 'end'] },
    ],
  },
  {
    type: 'Image',
    shortDesc: 'Remote image',
    detail: 'Loads and displays a remote image with specified dimensions. Falls back to empty space on load failure.',
    props: [
      { name: 'src', type: 'string', required: true, desc: 'Image URL' },
      { name: 'width', type: 'number', desc: 'Display width in logical pixels' },
      { name: 'height', type: 'number', desc: 'Display height in logical pixels' },
      { name: 'accessibility', type: 'object', desc: '{ label: "description" }' },
      { name: 'fit', type: 'string', values: ['cover', 'contain', 'fill', 'none'] },
      { name: 'cornerRadius', type: 'token', values: ['xs', 'sm', 'md', 'lg', 'xl', '2xl', 'full'] },
    ],
  },
  {
    type: 'Button',
    shortDesc: 'Interactive CTA',
    detail: 'A tappable button that fires an actions array on press. Supports primary, secondary, tertiary, and text visual variants.',
    props: [
      { name: 'label', type: 'string', required: true, desc: 'Button text' },
      { name: 'variant', type: 'string', required: true, values: ['primary', 'secondary', 'tertiary', 'text'] },
      { name: 'actions', type: 'action[]', desc: 'Array of action objects to fire' },
      { name: 'disabled', type: 'boolean', desc: 'Non-interactive state' },
      { name: 'icon', type: 'string', desc: 'Icon identifier before label' },
    ],
    snippet: `{
  "screenId": "button-demo",
  "sections": [{
    "sectionId": "buttons",
    "sectionType": "AtomicComposite",
    "content": {
      "type": "Container",
      "direction": "column",
      "gap": "md",
      "padding": "lg",
      "background": { "color": "#191C23" },
      "cornerRadius": "lg",
      "children": [
        { "type": "Text", "text": "BUTTON VARIANTS", "variant": "labelMedium", "color": "#8E9196" },
        { "type": "Button", "label": "Primary Action", "variant": "primary" },
        { "type": "Button", "label": "Secondary Action", "variant": "secondary" },
        { "type": "Button", "label": "Text Link", "variant": "text" },
        { "type": "Button", "label": "Delete", "variant": "destructive" }
      ]
    }
  }]
}`,
  },
  {
    type: 'Spacer',
    shortDesc: 'Fixed empty space',
    detail: 'Inserts a fixed-size gap. Direction-aware: in a column it adds height, in a row it adds width.',
    props: [
      { name: 'size', type: 'number', desc: 'Size in logical pixels (default: 8)' },
    ],
  },
  {
    type: 'Divider',
    shortDesc: 'Line separator',
    detail: 'Renders a thin line. Defaults to horizontal, themed color, 1px thick.',
    props: [
      { name: 'direction', type: 'string', values: ['horizontal', 'vertical'] },
      { name: 'color', type: 'string', desc: 'Override color (hex)' },
      { name: 'thickness', type: 'number', desc: 'Line thickness (default: 1)' },
    ],
  },
  {
    type: 'ScrollContainer',
    shortDesc: 'Scrollable region',
    detail: 'Wraps children in a scrollable container. Supports paging/snap for carousel-style UIs.',
    props: [
      { name: 'direction', type: 'string', required: true, values: ['horizontal', 'vertical'] },
      { name: 'paging', type: 'boolean', desc: 'Snap to items (carousel mode)' },
      { name: 'showIndicator', type: 'boolean', desc: 'Show scroll dots/bar' },
      { name: 'children', type: 'element[]', required: true, desc: 'Scrollable content' },
    ],
  },
  {
    type: 'LiveClock',
    shortDesc: 'Ticking game clock',
    detail: 'Client-side clock that ticks from a server-provided snapshot. No round trips needed — server sends current value and whether it\'s running.',
    props: [
      { name: 'snapshotSeconds', type: 'number', required: true, desc: 'Clock value at snapshot' },
      { name: 'snapshotAt', type: 'string', desc: 'ISO timestamp of snapshot' },
      { name: 'isRunning', type: 'boolean', required: true, desc: 'Whether clock is ticking' },
      { name: 'tickDirection', type: 'string', values: ['up', 'down'] },
      { name: 'stopAtSeconds', type: 'number', desc: 'Auto-stop value' },
      { name: 'format', type: 'string', values: ['mm:ss', 'h:mm:ss', 'ss.t'] },
    ],
  },
  {
    type: 'Conditional',
    shortDesc: 'If/else branching',
    detail: 'Evaluates a condition against client screen state and renders either the then or else element tree.',
    props: [
      { name: 'condition', type: 'object', required: true, desc: '{ field, operator, value }' },
      { name: 'then', type: 'element', required: true, desc: 'Render when true' },
      { name: 'else', type: 'element', desc: 'Render when false' },
    ],
  },
  {
    type: 'DisplayGrid',
    shortDesc: 'Non-interactive text grid',
    detail: 'Display-only grid of text values (stats, schedules). Zero interactivity — for sortable data, use a semantic section.',
    props: [
      { name: 'columns', type: 'column[]', required: true, desc: 'Column definitions with header and width' },
      { name: 'rows', type: 'string[][]', required: true, desc: 'Row data as string arrays' },
      { name: 'striped', type: 'boolean', desc: 'Alternate row backgrounds' },
    ],
  },
  {
    type: 'OverlayContainer',
    shortDesc: 'Stacked layers',
    detail: 'Content layered over a base element. Used for hero cards, video thumbnails with labels.',
    props: [
      { name: 'base', type: 'element', required: true, desc: 'Background layer (usually Image)' },
      { name: 'overlays', type: 'element[]', required: true, desc: 'Elements layered on top' },
      { name: 'scrim', type: 'boolean', desc: 'Dark overlay for readability' },
      { name: 'cornerRadius', type: 'token', values: ['xs', 'sm', 'md', 'lg', 'xl', '2xl'] },
    ],
  },
]

const SPACING_TOKENS = [
  { name: 'xs', px: 2 },
  { name: 'sm', px: 4 },
  { name: 'md', px: 12 },
  { name: 'lg', px: 16 },
  { name: 'xl', px: 32 },
  { name: '2xl', px: 40 },
]

const RADIUS_TOKENS = [
  { name: 'xs', px: 2 },
  { name: 'sm', px: 4 },
  { name: 'md', px: 12 },
  { name: 'lg', px: 16 },
  { name: 'xl', px: 24 },
  { name: '2xl', px: 32 },
  { name: 'full', px: 9999 },
]

const TYPO_TOKENS = [
  { variant: 'displayLarge', size: 57, family: 'display', familyName: 'Knockout', weight: 395, preview: 'DISPLAY LARGE' },
  { variant: 'displayMedium', size: 45, family: 'display', familyName: 'Knockout', weight: 395, preview: 'DISPLAY MEDIUM' },
  { variant: 'displaySmall', size: 36, family: 'display', familyName: 'Knockout', weight: 395, preview: 'DISPLAY SMALL' },
  { variant: 'headlineLarge', size: 32, family: 'headline', familyName: 'Knockout', weight: 360, preview: 'HEADLINE LARGE' },
  { variant: 'headlineMedium', size: 28, family: 'headline', familyName: 'Knockout', weight: 360, preview: 'HEADLINE MEDIUM' },
  { variant: 'headlineSmall', size: 24, family: 'headline', familyName: 'Knockout', weight: 360, preview: 'HEADLINE SMALL' },
  { variant: 'titleLarge', size: 22, family: 'body', familyName: 'Roboto', weight: 500, preview: 'Title Large' },
  { variant: 'titleMedium', size: 16, family: 'body', familyName: 'Roboto', weight: 500, preview: 'Title Medium' },
  { variant: 'titleSmall', size: 14, family: 'body', familyName: 'Roboto', weight: 500, preview: 'Title Small' },
  { variant: 'bodyLarge', size: 16, family: 'body', familyName: 'Roboto', weight: 400, preview: 'Body large text for reading' },
  { variant: 'bodyMedium', size: 14, family: 'body', familyName: 'Roboto', weight: 400, preview: 'Body medium text' },
  { variant: 'bodySmall', size: 12, family: 'body', familyName: 'Roboto', weight: 400, preview: 'Body small text' },
  { variant: 'labelLarge', size: 14, family: 'label', familyName: 'Roboto', weight: 500, preview: 'LABEL LARGE' },
  { variant: 'labelMedium', size: 12, family: 'label', familyName: 'Roboto', weight: 500, preview: 'LABEL MEDIUM' },
  { variant: 'labelSmall', size: 11, family: 'label', familyName: 'Roboto', weight: 500, preview: 'LABEL SMALL' },
  { variant: 'score', size: 14, family: 'display', familyName: 'Knockout', weight: 360, preview: '108' },
]

const ACTION_DOCS = [
  {
    type: 'navigate',
    description: 'Navigate to an internal screen or external URL',
    fields: [
      { name: 'type', type: '"navigate"', desc: 'Action type identifier' },
      { name: 'targetUri', type: 'string', desc: 'Internal URI (e.g. "nba://game/0022400123")' },
      { name: 'webUrl', type: 'string', desc: 'Fallback web URL for external navigation' },
      { name: 'presentation', type: 'string', desc: '"push", "modal", "sheet", or "external"' },
    ],
    example: '{ "type": "navigate", "targetUri": "nba://game/0022400123" }',
  },
  {
    type: 'fireAndForget',
    description: 'Fire-and-forget event (analytics, logging)',
    fields: [
      { name: 'type', type: '"fireAndForget"', desc: 'Action type identifier' },
      { name: 'event', type: 'string', desc: 'Event name to track' },
      { name: 'params', type: 'object', desc: 'Key-value event parameters' },
    ],
    example: '{ "type": "fireAndForget", "event": "cta_tap", "params": { "source": "promo" } }',
  },
  {
    type: 'mutate',
    description: 'Modify local screen state (toggle, update value)',
    fields: [
      { name: 'type', type: '"mutate"', desc: 'Action type identifier' },
      { name: 'field', type: 'string', desc: 'State field to modify' },
      { name: 'value', type: 'any', desc: 'New value to set' },
    ],
    example: '{ "type": "mutate", "field": "ui.expanded", "value": true }',
  },
  {
    type: 'refresh',
    description: 'Re-fetch the current screen or a specific section',
    fields: [
      { name: 'type', type: '"refresh"', desc: 'Action type identifier' },
      { name: 'target', type: 'string', desc: '"screen" or a specific sectionId' },
    ],
    example: '{ "type": "refresh", "target": "screen" }',
  },
  {
    type: 'dismiss',
    description: 'Close the current modal, sheet, or overlay',
    fields: [
      { name: 'type', type: '"dismiss"', desc: 'Action type identifier' },
    ],
    example: '{ "type": "dismiss" }',
  },
  {
    type: 'toast',
    description: 'Show a brief toast notification',
    fields: [
      { name: 'type', type: '"toast"', desc: 'Action type identifier' },
      { name: 'message', type: 'string', desc: 'Toast message to display' },
      { name: 'variant', type: 'string', desc: '"info", "success", or "error"' },
    ],
    example: '{ "type": "toast", "message": "Added to favorites", "variant": "success" }',
  },
]
