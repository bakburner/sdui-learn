import { useState, useEffect, useRef } from 'react'
import './SchemaReference.css'

interface SchemaReferenceProps {
  selectedElement: string | null
  focusedProperty?: string | null
  onInsertSnippet: (snippet: string) => void
  onPropertyChange?: (elementType: string, property: string, value: string) => void
  onSelectType?: (elementType: string) => void
}

export function SchemaReference({ selectedElement, focusedProperty, onInsertSnippet, onPropertyChange, onSelectType }: SchemaReferenceProps) {
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
  onValueClick,
}: {
  expanded: string | null
  focusedProperty?: string | null
  onExpand: (id: string | null) => void
  onInsert: (snippet: string) => void
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
          <button
            className="ref-item-header"
            onClick={() => onExpand(expanded === el.type ? null : el.type)}
          >
            <span className="ref-item-type">{el.type}</span>
            <span className="ref-item-desc">{el.shortDesc}</span>
            <span className="ref-chevron">{expanded === el.type ? '−' : '+'}</span>
          </button>
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
                          ) : (
                            <span className="desc-text">{p.desc}</span>
                          )}
                        </td>
                      </tr>
                    )
                  })}
                </tbody>
              </table>
              {el.snippet && (
                <button className="insert-btn" onClick={() => onInsert(el.snippet!)}>
                  Insert Example →
                </button>
              )}
            </div>
          )}
        </div>
      ))}
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
      { name: 'gap', type: 'token', values: ['xs', 'sm', 'md', 'lg', 'xl', 'xxl'] },
      { name: 'padding', type: 'token', values: ['xs', 'sm', 'md', 'lg', 'xl', 'xxl'] },
      { name: 'mainAxisAlignment', type: 'string', values: ['start', 'end', 'center', 'spaceBetween', 'spaceAround'] },
      { name: 'crossAxisAlignment', type: 'string', values: ['start', 'end', 'center', 'stretch', 'baseline'] },
      { name: 'background', type: 'object', desc: '{ color } or { type: "gradient", colors, angle }' },
      { name: 'cornerRadius', type: 'token', values: ['sm', 'md', 'lg', 'xl', 'full'] },
      { name: 'widthMode', type: 'string', values: ['hug', 'fill', 'fixed'] },
      { name: 'heightMode', type: 'string', values: ['hug', 'fill', 'fixed'] },
      { name: 'flex', type: 'number', desc: 'Flex grow weight (e.g. 1, 2)' },
      { name: 'shadows', type: 'array', desc: '[{ type, color, radius, offsetX, offsetY }]' },
      { name: 'children', type: 'element[]', required: true, desc: 'Child elements (max 20)' },
    ],
  },
  {
    type: 'Text',
    shortDesc: 'Styled typography',
    detail: 'Renders text using the platform\'s native typography scale. Variant maps to the design system — each platform interprets it with its native fonts.',
    props: [
      { name: 'text', type: 'string', required: true, desc: 'Content to display' },
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
      { name: 'url', type: 'string', required: true, desc: 'Image URL' },
      { name: 'width', type: 'number', desc: 'Display width in logical pixels' },
      { name: 'height', type: 'number', desc: 'Display height in logical pixels' },
      { name: 'alt', type: 'string', desc: 'Accessibility text' },
      { name: 'contentScale', type: 'string', values: ['fit', 'fill', 'crop', 'none'] },
      { name: 'cornerRadius', type: 'token', values: ['sm', 'md', 'lg', 'xl', 'full'] },
    ],
  },
  {
    type: 'Button',
    shortDesc: 'Interactive CTA',
    detail: 'A tappable button that fires an actions array on press. Supports primary, secondary, text, and destructive visual variants.',
    props: [
      { name: 'label', type: 'string', required: true, desc: 'Button text' },
      { name: 'variant', type: 'string', required: true, values: ['primary', 'secondary', 'text', 'destructive'] },
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
    detail: 'Foreground content over a background (image, gradient, scrim). Used for hero cards, video thumbnails with labels.',
    props: [
      { name: 'background', type: 'element', required: true, desc: 'Background layer (usually Image)' },
      { name: 'foreground', type: 'element', required: true, desc: 'Content on top' },
      { name: 'scrim', type: 'boolean', desc: 'Dark overlay for readability' },
      { name: 'cornerRadius', type: 'token', values: ['sm', 'md', 'lg', 'xl'] },
    ],
  },
]

const SPACING_TOKENS = [
  { name: 'xs', px: 4 },
  { name: 'sm', px: 8 },
  { name: 'md', px: 12 },
  { name: 'lg', px: 16 },
  { name: 'xl', px: 24 },
  { name: 'xxl', px: 32 },
]

const RADIUS_TOKENS = [
  { name: 'sm', px: 4 },
  { name: 'md', px: 6 },
  { name: 'lg', px: 8 },
  { name: 'xl', px: 12 },
  { name: 'full', px: 9999 },
]

const TYPO_TOKENS = [
  { variant: 'displayLarge', size: 48, family: 'display', familyName: 'Knockout', weight: 395, preview: 'DISPLAY LARGE' },
  { variant: 'headlineLarge', size: 32, family: 'headline', familyName: 'Action NBA', weight: 360, preview: 'HEADLINE LARGE' },
  { variant: 'headlineMedium', size: 28, family: 'headline', familyName: 'Action NBA', weight: 360, preview: 'HEADLINE MEDIUM' },
  { variant: 'titleLarge', size: 20, family: 'body', familyName: 'Roboto', weight: 500, preview: 'Title Large' },
  { variant: 'titleMedium', size: 16, family: 'body', familyName: 'Roboto', weight: 500, preview: 'Title Medium' },
  { variant: 'bodyLarge', size: 16, family: 'body', familyName: 'Roboto', weight: 400, preview: 'Body large text for reading' },
  { variant: 'bodyMedium', size: 14, family: 'body', familyName: 'Roboto', weight: 400, preview: 'Body medium text' },
  { variant: 'bodySmall', size: 12, family: 'body', familyName: 'Roboto', weight: 400, preview: 'Body small text' },
  { variant: 'labelLarge', size: 14, family: 'label', familyName: 'Roboto', weight: 400, preview: 'LABEL LARGE' },
  { variant: 'labelSmall', size: 11, family: 'label', familyName: 'Roboto', weight: 400, preview: 'LABEL SMALL' },
  { variant: 'score', size: 20, family: 'headline', familyName: 'Knockout', weight: 360, preview: '108' },
]

const ACTION_DOCS = [
  {
    type: 'navigate',
    description: 'Navigate to an internal screen via URI scheme',
    fields: [
      { name: 'destination', type: 'string', desc: 'URI (e.g. "nba://game-detail/123")' },
    ],
    example: '{ "actionType": "navigate", "destination": "nba://game-detail/0022400123" }',
  },
  {
    type: 'refresh',
    description: 'Re-fetch the current screen or a specific section',
    fields: [
      { name: 'target', type: 'string', desc: '"screen" or sectionId' },
    ],
    example: '{ "actionType": "refresh", "target": "screen" }',
  },
  {
    type: 'deeplink',
    description: 'Open platform URL with in-app fallback',
    fields: [
      { name: 'url', type: 'string', desc: 'Universal/deep link URL' },
      { name: 'fallback', type: 'string', desc: 'Internal URI if deep link fails' },
    ],
    example: '{ "actionType": "deeplink", "url": "https://nba.com/game/123", "fallback": "nba://game-detail/123" }',
  },
  {
    type: 'web',
    description: 'Open URL in browser or modal webview',
    fields: [
      { name: 'url', type: 'string', desc: 'Web URL to open' },
      { name: 'presentation', type: 'string', desc: '"browser", "modal", or "sheet"' },
    ],
    example: '{ "actionType": "web", "url": "https://leaguepass.nba.com", "presentation": "modal" }',
  },
  {
    type: 'analytics',
    description: 'Fire-and-forget event tracking',
    fields: [
      { name: 'event', type: 'string', desc: 'Event name' },
      { name: 'properties', type: 'object', desc: 'Key-value event properties' },
    ],
    example: '{ "actionType": "analytics", "event": "cta_tap", "properties": { "source": "promo" } }',
  },
  {
    type: 'share',
    description: 'Trigger native share sheet',
    fields: [
      { name: 'title', type: 'string', desc: 'Share title/subject' },
      { name: 'url', type: 'string', desc: 'URL to share' },
    ],
    example: '{ "actionType": "share", "title": "Check this out!", "url": "https://nba.com/game/123" }',
  },
]
