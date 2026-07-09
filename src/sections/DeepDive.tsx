import { useState } from 'react'
import { useScrollReveal } from '../hooks/useScrollReveal'
import '../hooks/useScrollReveal.css'
import './DeepDive.css'

const TABS = [
  'Action System',
  'Data Binding & Refresh',
  'Token Architecture',
] as const

const ACTION_TYPES = [
  { name: 'navigate', desc: 'change route (push / replace / external / modal / fullscreen)' },
  { name: 'fireAndForget', desc: 'emit a Beacon (analytics event) — no UI side effect' },
  { name: 'mutate', desc: 'apply set / toggle / increment / append to Screen state' },
  { name: 'refresh', desc: 're-fetch a section or full screen, optionally with Param bindings' },
  { name: 'request', desc: 'server-managed write (POST / PUT / DELETE) with optional bodyBindings' },
  { name: 'purchase', desc: 'IAP via productRef' },
  { name: 'dismiss', desc: 'close the current presented host (modal / sheet / overlay)' },
  { name: 'flashMessage', desc: 'show a transient message' },
]

const TRIGGERS = [
  { name: 'onActivate', desc: 'tap / click / keyboard Enter / TV select (preferred)' },
  { name: 'onTap', desc: 'deprecated alias for onActivate' },
  { name: 'onLongPress', desc: 'Android / iOS only' },
  { name: 'onVisible', desc: 'routes through Impression policy for dedup' },
  { name: 'onSwipe', desc: 'ScrollContainer-level' },
  { name: 'onFocus', desc: 'focusable primitives' },
  { name: 'onBlur', desc: 'focusable primitives' },
  { name: 'onSubmit', desc: 'form-context only' },
]

const FAILURE_POLICIES = [
  { name: 'halt', desc: 'Show error (failure feedback), stop sequence. Default for navigate / request / purchase' },
  { name: 'continue', desc: 'Log error, proceed to next action. Default for mutate / refresh' },
  { name: 'silent', desc: 'Swallow error entirely. Default for fireAndForget / dismiss / flashMessage' },
]

const REFRESH_TYPES = [
  { name: 'static', desc: 'No refresh — editorial content', icon: 'lock' },
  { name: 'poll', desc: 'Interval HTTP fetch via sectionEndpoint — slower-changing stats', icon: 'clock' },
  { name: 'sse / ably', desc: 'Realtime push channel — live scores. Both feed the same Data-binding pipeline', icon: 'bolt' },
]

const INLINE_PRIMITIVES = [
  'padding', 'cornerRadius', 'background.color', 'gap', 'opacity', 'border', 'shadows',
]

const TOKEN_FAMILIES = [
  'grey', 'blue', 'red', 'green', 'orange', 'yellow',
]

const SEMANTIC_ALIASES = [
  'primary', 'secondary', 'feedback', 'team colors',
]


const ACTION_EXAMPLE = `{
  "actions": [
    { "trigger": "onActivate", "type": "fireAndForget", "event": "card_tap" },
    { "trigger": "onActivate", "type": "navigate", "targetUri": "nba://game/0022400999" }
  ]
}`

const BINDING_EXAMPLE = `{
  "refreshPolicy": [{ "type": "sse", "channel": "{gameId}:linescore" }],
  "data": {
    "ui": {
      "type": "Text",
      "content": "94",
      "variant": "score",
      "bindRef": "homeTeam.score"
    },
    "content": {
      "homeTeam": { "score": 94, "tricode": "BOS" },
      "awayTeam": { "score": 89, "tricode": "LAL" },
      "clock": { "snapshotSeconds": 272, "isRunning": false }
    }
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

      <h4 className="dd-panel-heading">Data Binding (DataBindingPath)</h4>
      <p className="dd-panel-desc">
        Declarative mapping from a path in an incoming live-data payload to a path inside a section's <code>data</code>. The binding runtime walks these and patches sections with structural sharing so unrelated subtrees keep their object identity. Leaf elements use <code>bindRef</code> to resolve content from <code>data.content[bindRef]</code> at render time.
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
          <span className="dd-layer-title">Inline Style Primitives</span>
          <p className="dd-layer-desc">
            Directly-expressible properties on any element. Used for fine-grained intent that doesn't yet warrant a named variant. Values reference <code>token:nba.*</code> scalars (e.g. <code>nba.spacing.md</code>) — never raw pixels.
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
            Predefined visual treatments that map to a native idiom (<code>ContainerVariant</code>, <code>ButtonVariant</code>, <code>TextVariant</code>). Express semantic visual intent; realize differently per OS tier (Liquid Glass iOS 26+, Material 3 expressive on Android 14+, CSS filter on Web). New variant values must clear a strict governance bar.
          </p>
        </div>

        <div className="dd-layer-arrow">&#8595;</div>

        <div className="dd-token-layer dd-layer-3">
          <div className="dd-layer-badge">Layer 3</div>
          <span className="dd-layer-title">Color Tokens</span>
          <p className="dd-layer-desc">
            Named color references in <code>color-tokens.json</code>. Resolved per scheme (light / dark / contrast) and may have OS-tier-specific values.
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

      <h4 className="dd-panel-heading">Override Matrix (Precedence)</h4>
      <div className="dd-decision-rule">
        <div className="dd-decision-step">
          <span className="dd-decision-q">style token</span>
          <span className="dd-decision-a">&lt; variant</span>
        </div>
        <div className="dd-decision-step">
          <span className="dd-decision-q">variant</span>
          <span className="dd-decision-a">&lt; inline override</span>
        </div>
        <div className="dd-decision-step">
          <span className="dd-decision-q">Inline overrides win.</span>
          <span className="dd-decision-a">Use for one-off intent that doesn't warrant a variant</span>
        </div>
      </div>
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
