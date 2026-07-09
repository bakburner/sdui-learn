import { useState } from 'react'
import { useScrollReveal } from '../hooks/useScrollReveal'
import '../hooks/useScrollReveal.css'
import './DeepDive.css'

const TABS = [
  'Action System',
  'Data Binding & Refresh',
  'Token Architecture',
  'Platform & Envelope',
] as const

const ACTION_TYPES = [
  { name: 'navigate', desc: 'route / deeplink' },
  { name: 'fireAndForget', desc: 'analytics beacon' },
  { name: 'mutate', desc: 'update screen state' },
  { name: 'refresh', desc: 'force fetch' },
  { name: 'request', desc: 'server-managed write (favorite / dismiss)' },
  { name: 'purchase', desc: 'IAP' },
  { name: 'dismiss', desc: 'close overlay' },
  { name: 'flashMessage', desc: 'show transient message' },
]

const TRIGGERS = [
  { name: 'onActivate', desc: 'tap / click / enter' },
  { name: 'onLongPress', desc: '' },
  { name: 'onVisible', desc: '' },
  { name: 'onSwipe', desc: '' },
  { name: 'onFocus', desc: '' },
  { name: 'onBlur', desc: '' },
  { name: 'onSubmit', desc: '' },
]

const FAILURE_POLICIES = [
  { name: 'halt', desc: 'Show error, stop action chain' },
  { name: 'continue', desc: 'Log error, proceed to next action' },
  { name: 'silent', desc: 'Swallow error entirely' },
]

const REFRESH_TYPES = [
  { name: 'static', desc: 'No refresh — editorial content', icon: 'lock' },
  { name: 'poll', desc: 'Interval HTTP fetch — slower-changing stats', icon: 'clock' },
  { name: 'sse', desc: 'Realtime channel via Ably — live scores', icon: 'bolt' },
]

const INLINE_PRIMITIVES = [
  'padding', 'cornerRadius', 'shadow', 'gap', 'opacity', 'border', 'background',
]

const TOKEN_FAMILIES = [
  'grey', 'blue', 'red', 'green', 'orange', 'yellow',
]

const SEMANTIC_ALIASES = [
  'primary', 'secondary', 'feedback', 'team colors',
]

const ENVELOPE_FIELDS = [
  { key: 'platform.name', value: 'android | ios | web' },
  { key: 'deviceClass', value: 'phone | tablet | tv' },
  { key: 'schemaVersion', value: '"2.4"' },
  { key: 'experiments', value: '{ "new-scoreboard": "variant-b" }' },
  { key: 'capabilities', value: '["sse", "onFocus"]' },
  { key: 'correlationId', value: '"req-abc123"' },
]

const SHARED_ACROSS = [
  'Schema definitions',
  'Codegen',
  'Upstream data pipeline',
  'Section-type semantics',
  'Action / binding structure',
]

const DIFFERS_PER_PLATFORM = [
  'Which sections are composed and in what order',
  'Information density',
  'Action URIs (targetUri for native deeplinks, webUrl for web)',
  'Image dimensions',
  'Interaction triggers (touch vs D-pad vs hover)',
]

const ACTION_EXAMPLE = `{
  "trigger": "onActivate",
  "actions": [
    { "type": "fireAndForget", "event": "card_tap" },
    { "type": "navigate", "targetUri": "nba://game/123" }
  ]
}`

const BINDING_EXAMPLE = `{
  "refreshPolicy": [{ "type": "sse", "channel": "{gameId}:linescore" }],
  "dataBinding": {
    "bindings": [
      { "sourcePath": "$.homeTeam.score", "targetPath": "homeScore" },
      { "sourcePath": "$.awayTeam.score", "targetPath": "awayScore" },
      { "sourcePath": "$.gameStatusText", "targetPath": "statusText" }
    ]
  }
}`

export function DeepDive() {
  const [activeTab, setActiveTab] = useState(0)
  const header = useScrollReveal<HTMLDivElement>()
  const content = useScrollReveal<HTMLDivElement>({ threshold: 0.05 })

  return (
    <section id="deep-dive">
      <div ref={header.ref} className={`reveal ${header.isVisible ? 'visible' : ''}`}>
        <div className="section-label">Technical Deep Dive</div>
        <h2 className="section-title">Under the Hood</h2>
        <p className="section-subtitle">
          Explore the core systems that power SDUI — actions, real-time data, design tokens, and platform composition.
        </p>
      </div>

      <div ref={content.ref} className={`dd-container reveal ${content.isVisible ? 'visible' : ''}`}>
        <div className="dd-tabs" role="tablist">
          {TABS.map((tab, i) => (
            <button
              key={tab}
              role="tab"
              aria-selected={activeTab === i}
              className={`dd-tab ${activeTab === i ? 'dd-tab--active' : ''}`}
              onClick={() => setActiveTab(i)}
            >
              {tab}
            </button>
          ))}
        </div>

        <div className="dd-panel" role="tabpanel">
          {activeTab === 0 && <ActionSystemPanel />}
          {activeTab === 1 && <DataBindingPanel />}
          {activeTab === 2 && <TokenArchitecturePanel />}
          {activeTab === 3 && <PlatformEnvelopePanel />}
        </div>
      </div>
    </section>
  )
}

function ActionSystemPanel() {
  return (
    <div className="dd-action-panel">
      <h4 className="dd-panel-heading">Action Types</h4>
      <p className="dd-panel-desc">
        Every interactive element declares a list of actions to fire in sequence when triggered.
      </p>
      <div className="dd-chips">
        {ACTION_TYPES.map((a) => (
          <div key={a.name} className="dd-chip">
            <span className="dd-chip-name">{a.name}</span>
            <span className="dd-chip-desc">{a.desc}</span>
          </div>
        ))}
      </div>

      <h4 className="dd-panel-heading">Triggers</h4>
      <div className="dd-trigger-row">
        {TRIGGERS.map((t) => (
          <span key={t.name} className="dd-trigger-badge">
            {t.name}
            {t.desc && <span className="dd-trigger-hint">{t.desc}</span>}
          </span>
        ))}
      </div>

      <h4 className="dd-panel-heading">Precedence</h4>
      <p className="dd-panel-desc">
        Nested / subsection &gt; section &gt; screen-default
      </p>

      <h4 className="dd-panel-heading">Failure Policies</h4>
      <div className="dd-policy-row">
        {FAILURE_POLICIES.map((p) => (
          <div key={p.name} className="dd-policy">
            <span className="dd-policy-name">{p.name}</span>
            <span className="dd-policy-desc">{p.desc}</span>
          </div>
        ))}
      </div>

      <h4 className="dd-panel-heading">Example</h4>
      <pre className="dd-code">{ACTION_EXAMPLE}</pre>
    </div>
  )
}

function DataBindingPanel() {
  return (
    <div className="dd-binding-panel">
      <h4 className="dd-panel-heading">Refresh Types</h4>
      <p className="dd-panel-desc">
        Different sections on the same screen can have different refresh policies.
      </p>
      <div className="dd-refresh-row">
        {REFRESH_TYPES.map((r) => (
          <div key={r.name} className="dd-refresh-card">
            <div className="dd-refresh-icon">
              <RefreshIcon type={r.icon} />
            </div>
            <span className="dd-refresh-name">{r.name}</span>
            <span className="dd-refresh-desc">{r.desc}</span>
          </div>
        ))}
      </div>

      <h4 className="dd-panel-heading">Field-Level Binding</h4>
      <p className="dd-panel-desc">
        The server declares JSONPath mappings (sourcePath to targetPath). The client patches individual fields without re-fetching the entire section.
      </p>

      <h4 className="dd-panel-heading">Example</h4>
      <pre className="dd-code">{BINDING_EXAMPLE}</pre>
    </div>
  )
}

function TokenArchitecturePanel() {
  return (
    <div className="dd-token-panel">
      <h4 className="dd-panel-heading">Three-Layer Token Stack</h4>
      <p className="dd-panel-desc">
        Tokens are resolved at render time through three layers of abstraction.
      </p>

      <div className="dd-token-layers">
        <div className="dd-token-layer dd-layer-1">
          <div className="dd-layer-badge">Layer 1</div>
          <span className="dd-layer-title">Inline Primitives</span>
          <p className="dd-layer-desc">
            All sizes use semantic tokens (nba.spacing.md, nba.radius.lg) not raw pixels.
          </p>
          <div className="dd-inline-tokens">
            {INLINE_PRIMITIVES.map((t) => (
              <span key={t} className="dd-inline-token">{t}</span>
            ))}
          </div>
        </div>

        <div className="dd-layer-arrow">&#8595;</div>

        <div className="dd-token-layer dd-layer-2">
          <div className="dd-layer-badge">Layer 2</div>
          <span className="dd-layer-title">Variants</span>
          <p className="dd-layer-desc">
            Named presets that carry platform-native treatments. A <code>hero</code> container becomes Liquid Glass on iOS, Material elevation on Android, CSS backdrop-blur on web. Variants express what inline properties cannot (materials, press animations, OS-adaptive surfaces).
          </p>
        </div>

        <div className="dd-layer-arrow">&#8595;</div>

        <div className="dd-token-layer dd-layer-3">
          <div className="dd-layer-badge">Layer 3</div>
          <span className="dd-layer-title">Color Tokens</span>
          <p className="dd-layer-desc">
            <code>token:nba.color.*</code> / <code>token:nba.label.*</code> references resolved to light/dark hex at render time.
          </p>
          <div className="dd-color-families">
            <span className="dd-color-family-label">Families:</span>
            <div className="dd-color-chips">
              {TOKEN_FAMILIES.map((f) => (
                <span key={f} className={`dd-color-chip dd-color-${f}`}>{f}</span>
              ))}
            </div>
            <span className="dd-color-family-label">Semantic:</span>
            <div className="dd-color-chips">
              {SEMANTIC_ALIASES.map((s) => (
                <span key={s} className="dd-color-chip dd-color-semantic">{s}</span>
              ))}
            </div>
          </div>
        </div>
      </div>

      <h4 className="dd-panel-heading">Decision Rule</h4>
      <div className="dd-decision-rule">
        <div className="dd-decision-step">
          <span className="dd-decision-q">Can inline express it?</span>
          <span className="dd-decision-a">&#8594; Inline primitive</span>
        </div>
        <div className="dd-decision-step">
          <span className="dd-decision-q">Need platform API / interaction?</span>
          <span className="dd-decision-a">&#8594; Variant</span>
        </div>
        <div className="dd-decision-step">
          <span className="dd-decision-q">Need light/dark adaptation?</span>
          <span className="dd-decision-a">&#8594; Color token</span>
        </div>
      </div>
    </div>
  )
}

function PlatformEnvelopePanel() {
  return (
    <div className="dd-platform-panel">
      <h4 className="dd-panel-heading">Request Envelope</h4>
      <p className="dd-panel-desc">
        The client sends platform context on every request so the composition service can tailor the response.
      </p>
      <div className="dd-envelope-fields">
        {ENVELOPE_FIELDS.map((f) => (
          <div key={f.key} className="dd-envelope-field">
            <span className="dd-envelope-key">{f.key}</span>
            <span className="dd-envelope-val">{f.value}</span>
          </div>
        ))}
      </div>

      <h4 className="dd-panel-heading">Shared Across Platforms</h4>
      <ul className="dd-list dd-list-shared">
        {SHARED_ACROSS.map((item) => (
          <li key={item}>{item}</li>
        ))}
      </ul>

      <h4 className="dd-panel-heading">Differs Per Platform Family</h4>
      <ul className="dd-list dd-list-differs">
        {DIFFERS_PER_PLATFORM.map((item) => (
          <li key={item}>{item}</li>
        ))}
      </ul>

      <p className="dd-panel-note">
        The composition service routes requests to per-platform composers that assemble the final response.
      </p>
    </div>
  )
}

function RefreshIcon({ type }: { type: string }) {
  switch (type) {
    case 'lock':
      return (
        <svg width="20" height="20" viewBox="0 0 20 20" fill="none" stroke="currentColor" strokeWidth="1.5">
          <rect x="4" y="9" width="12" height="8" rx="2" />
          <path d="M7 9V6a3 3 0 016 0v3" />
        </svg>
      )
    case 'clock':
      return (
        <svg width="20" height="20" viewBox="0 0 20 20" fill="none" stroke="currentColor" strokeWidth="1.5">
          <circle cx="10" cy="10" r="7" />
          <polyline points="10 6 10 10 13 12" />
        </svg>
      )
    case 'bolt':
      return (
        <svg width="20" height="20" viewBox="0 0 20 20" fill="none" stroke="currentColor" strokeWidth="1.5">
          <polygon points="11 2 5 11 10 11 9 18 15 9 10 9" />
        </svg>
      )
    default:
      return null
  }
}
