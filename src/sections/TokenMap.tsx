import { useState } from 'react'
import { useScrollReveal, useStaggerReveal } from '../hooks/useScrollReveal'
import '../hooks/useScrollReveal.css'
import './TokenMap.css'

const EXAMPLE_JSON = `{
  "type": "Container",
  "direction": "column",
  "gap": "md",
  "padding": "lg",
  "cornerRadius": "lg",
  "background": { "color": "surface-raised" },
  "children": [
    {
      "type": "Text",
      "text": "Game Day",
      "variant": "titleLarge",
      "color": "text-primary"
    },
    {
      "type": "Text",
      "text": "LAL vs BOS — 7:30 PM ET",
      "variant": "bodyMedium",
      "color": "text-secondary"
    }
  ]
}`

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
  { name: 'md', px: 8 },
  { name: 'lg', px: 12 },
  { name: 'xl', px: 16 },
  { name: 'full', px: 9999 },
]

const TYPOGRAPHY_TOKENS = [
  { variant: 'titleLarge', size: '24px', weight: 700, family: 'Headline' },
  { variant: 'titleMedium', size: '20px', weight: 600, family: 'Headline' },
  { variant: 'bodyLarge', size: '16px', weight: 400, family: 'Body' },
  { variant: 'bodyMedium', size: '14px', weight: 400, family: 'Body' },
  { variant: 'bodySmall', size: '12px', weight: 400, family: 'Body' },
  { variant: 'labelMedium', size: '12px', weight: 600, family: 'Body' },
]

const COLOR_TOKENS = [
  { name: 'surface', value: 'var(--surface)', hex: '#141518' },
  { name: 'surface-raised', value: 'var(--surface-raised)', hex: '#1E2025' },
  { name: 'text-primary', value: 'var(--text-primary)', hex: '#F5F5F5' },
  { name: 'text-secondary', value: 'var(--text-secondary)', hex: '#8E9196' },
  { name: 'text-tertiary', value: 'var(--text-tertiary)', hex: '#5A5D63' },
  { name: 'nba-tint', value: 'var(--nba-tint)', hex: '#F4C752' },
  { name: 'divider', value: 'var(--divider)', hex: '#2B2F37' },
]

type ViewportMode = 'phone' | 'tablet' | 'desktop'

export function TokenMap() {
  const [activeViewport, setActiveViewport] = useState<ViewportMode>('phone')
  const [hoveredSpacing, setHoveredSpacing] = useState<number | null>(null)

  const header = useScrollReveal<HTMLDivElement>()
  const splitReveal = useScrollReveal<HTMLDivElement>({ threshold: 0.05 })
  const explorerHeader = useScrollReveal<HTMLDivElement>()
  const { containerRef: spacingRef, visibleItems: spacingVisible } = useStaggerReveal(SPACING_TOKENS.length, 80)
  const { containerRef: radiusRef, visibleItems: radiusVisible } = useStaggerReveal(RADIUS_TOKENS.length, 80)
  const { containerRef: typographyRef, visibleItems: typographyVisible } = useStaggerReveal(TYPOGRAPHY_TOKENS.length, 80)
  const { containerRef: colorRef, visibleItems: colorVisible } = useStaggerReveal(COLOR_TOKENS.length, 60)
  const responsiveReveal = useScrollReveal<HTMLDivElement>()

  return (
    <section id="token-map">
      <div ref={header.ref} className={`reveal ${header.isVisible ? 'visible' : ''}`}>
        <div className="section-label">For Design</div>
        <h2 className="section-title">Design Token Map</h2>
        <p className="section-subtitle">
          See how abstract design tokens in SDUI JSON translate into real, rendered pixels — spacing, radius, typography, and color made visible.
        </p>
      </div>

      {/* Split View: JSON + Annotated Preview */}
      <div ref={splitReveal.ref} className={`token-split-view reveal-scale ${splitReveal.isVisible ? 'visible' : ''}`}>
        <div className="token-json-panel">
          <div className="token-panel-header">
            <span className="token-panel-label">SDUI JSON</span>
            <span className="token-panel-badge">Tokens highlighted</span>
          </div>
          <pre className="token-json-code">
            <code>
              <span className="json-punct">{'{'}</span>{'\n'}
              {'  '}<span className="json-key">"type"</span><span className="json-punct">:</span> <span className="json-string">"Container"</span><span className="json-punct">,</span>{'\n'}
              {'  '}<span className="json-key">"direction"</span><span className="json-punct">:</span> <span className="json-string">"column"</span><span className="json-punct">,</span>{'\n'}
              {'  '}<span className="json-key">"gap"</span><span className="json-punct">:</span> <span className="json-token token-spacing">"md"</span><span className="json-punct">,</span><span className="token-annotation">&#8592; 12px spacing</span>{'\n'}
              {'  '}<span className="json-key">"padding"</span><span className="json-punct">:</span> <span className="json-token token-spacing">"lg"</span><span className="json-punct">,</span><span className="token-annotation">&#8592; 16px inset</span>{'\n'}
              {'  '}<span className="json-key">"cornerRadius"</span><span className="json-punct">:</span> <span className="json-token token-radius">"lg"</span><span className="json-punct">,</span><span className="token-annotation">&#8592; 12px radius</span>{'\n'}
              {'  '}<span className="json-key">"background"</span><span className="json-punct">:</span> <span className="json-punct">{'{'}</span> <span className="json-key">"color"</span><span className="json-punct">:</span> <span className="json-token token-color">"surface-raised"</span> <span className="json-punct">{'}'}</span><span className="json-punct">,</span>{'\n'}
              {'  '}<span className="json-key">"children"</span><span className="json-punct">:</span> <span className="json-punct">[</span>{'\n'}
              {'    '}<span className="json-punct">{'{'}</span>{'\n'}
              {'      '}<span className="json-key">"type"</span><span className="json-punct">:</span> <span className="json-string">"Text"</span><span className="json-punct">,</span>{'\n'}
              {'      '}<span className="json-key">"text"</span><span className="json-punct">:</span> <span className="json-string">"Game Day"</span><span className="json-punct">,</span>{'\n'}
              {'      '}<span className="json-key">"variant"</span><span className="json-punct">:</span> <span className="json-token token-typography">"titleLarge"</span><span className="json-punct">,</span><span className="token-annotation">&#8592; 24px / 700</span>{'\n'}
              {'      '}<span className="json-key">"color"</span><span className="json-punct">:</span> <span className="json-token token-color">"text-primary"</span>{'\n'}
              {'    '}<span className="json-punct">{'}'}</span><span className="json-punct">,</span>{'\n'}
              {'    '}<span className="json-punct">{'{'}</span>{'\n'}
              {'      '}<span className="json-key">"type"</span><span className="json-punct">:</span> <span className="json-string">"Text"</span><span className="json-punct">,</span>{'\n'}
              {'      '}<span className="json-key">"text"</span><span className="json-punct">:</span> <span className="json-string">"LAL vs BOS"</span><span className="json-punct">,</span>{'\n'}
              {'      '}<span className="json-key">"variant"</span><span className="json-punct">:</span> <span className="json-token token-typography">"bodyMedium"</span><span className="json-punct">,</span><span className="token-annotation">&#8592; 14px / 400</span>{'\n'}
              {'      '}<span className="json-key">"color"</span><span className="json-punct">:</span> <span className="json-token token-color">"text-secondary"</span>{'\n'}
              {'    '}<span className="json-punct">{'}'}</span>{'\n'}
              {'  '}<span className="json-punct">]</span>{'\n'}
              <span className="json-punct">{'}'}</span>
            </code>
          </pre>
        </div>

        <div className="token-preview-panel">
          <div className="token-panel-header">
            <span className="token-panel-label">Rendered Output</span>
            <span className="token-panel-badge">Annotated</span>
          </div>
          <div className="token-preview-canvas">
            {/* Corner radius annotation */}
            <div className="annotation-corner">
              <svg className="corner-svg" viewBox="0 0 40 40" width="40" height="40">
                <path d="M 0 12 Q 0 0 12 0" fill="none" stroke="var(--nba-tint)" strokeWidth="2" strokeDasharray="3 2" />
              </svg>
              <span className="annotation-label corner-label">12px radius</span>
            </div>

            {/* The rendered card */}
            <div className="token-rendered-card">
              {/* Padding annotation */}
              <div className="annotation-padding-top">
                <div className="ruler-line ruler-horizontal"></div>
                <span className="annotation-label padding-label">padding: 16px</span>
              </div>
              <div className="annotation-padding-left">
                <div className="ruler-line ruler-vertical"></div>
                <span className="annotation-label padding-side-label">16px</span>
              </div>

              <div className="rendered-content">
                <span className="rendered-title">Game Day</span>
                {/* Gap annotation */}
                <div className="annotation-gap">
                  <div className="gap-indicator"></div>
                  <span className="annotation-label gap-label">gap: 12px</span>
                </div>
                <span className="rendered-subtitle">LAL vs BOS — 7:30 PM ET</span>
              </div>
            </div>

            {/* Typography annotations */}
            <div className="annotation-type-title">
              <span className="annotation-label type-label">titleLarge: 24px / weight 700</span>
            </div>
            <div className="annotation-type-body">
              <span className="annotation-label type-label">bodyMedium: 14px / weight 400</span>
            </div>
          </div>
        </div>
      </div>

      {/* Token Explorer Grid */}
      <div ref={explorerHeader.ref} className={`token-explorer reveal ${explorerHeader.isVisible ? 'visible' : ''}`}>
        <h3 className="explorer-title">Token Explorer</h3>
        <p className="explorer-subtitle">Hover tokens to see their effect. Each abstract name resolves to a concrete value the renderer uses.</p>
      </div>

      <div className="explorer-grid">
        {/* Spacing Tokens */}
        <div className="explorer-category" ref={spacingRef}>
          <h4 className="category-label">Spacing</h4>
          <div className="spacing-tokens">
            {SPACING_TOKENS.map((token, i) => (
              <div
                key={token.name}
                className={`spacing-token stagger-item ${spacingVisible.has(i) ? 'visible' : ''}`}
                onMouseEnter={() => setHoveredSpacing(i)}
                onMouseLeave={() => setHoveredSpacing(null)}
              >
                <div className="spacing-bar-container">
                  <div
                    className="spacing-bar"
                    style={{ width: `${token.px * 3}px` }}
                  ></div>
                </div>
                <div className="spacing-info">
                  <span className="spacing-name">{token.name}</span>
                  <span className="spacing-value">{token.px}px</span>
                </div>
                {hoveredSpacing === i && (
                  <div className="spacing-preview">
                    <div className="spacing-preview-box"></div>
                    <div className="spacing-preview-gap" style={{ height: `${token.px}px` }}></div>
                    <div className="spacing-preview-box"></div>
                  </div>
                )}
              </div>
            ))}
          </div>
        </div>

        {/* Radius Tokens */}
        <div className="explorer-category" ref={radiusRef}>
          <h4 className="category-label">Radius</h4>
          <div className="radius-tokens">
            {RADIUS_TOKENS.map((token, i) => (
              <div
                key={token.name}
                className={`radius-token stagger-item ${radiusVisible.has(i) ? 'visible' : ''}`}
              >
                <div
                  className="radius-square"
                  style={{ borderRadius: token.px === 9999 ? '50%' : `${token.px}px` }}
                ></div>
                <div className="radius-info">
                  <span className="radius-name">{token.name}</span>
                  <span className="radius-value">{token.px === 9999 ? '50%' : `${token.px}px`}</span>
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* Typography Tokens */}
        <div className="explorer-category explorer-category-wide" ref={typographyRef}>
          <h4 className="category-label">Typography</h4>
          <div className="typography-tokens">
            {TYPOGRAPHY_TOKENS.map((token, i) => (
              <div
                key={token.variant}
                className={`typography-token stagger-item ${typographyVisible.has(i) ? 'visible' : ''}`}
              >
                <span
                  className="typography-sample"
                  style={{ fontSize: token.size, fontWeight: token.weight }}
                >
                  {token.variant}
                </span>
                <div className="typography-meta">
                  <span className="typography-detail">{token.size} / {token.weight}</span>
                  <span className="typography-family">{token.family}</span>
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* Color Tokens */}
        <div className="explorer-category explorer-category-wide" ref={colorRef}>
          <h4 className="category-label">Color</h4>
          <div className="color-tokens">
            {COLOR_TOKENS.map((token, i) => (
              <div
                key={token.name}
                className={`color-token stagger-item ${colorVisible.has(i) ? 'visible' : ''}`}
              >
                <div
                  className="color-swatch"
                  style={{ background: token.value }}
                ></div>
                <div className="color-info">
                  <span className="color-name">{token.name}</span>
                  <span className="color-hex">{token.hex}</span>
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>

      {/* Responsive Preview */}
      <div ref={responsiveReveal.ref} className={`responsive-section reveal ${responsiveReveal.isVisible ? 'visible' : ''}`}>
        <h3 className="explorer-title">Responsive Preview</h3>
        <p className="explorer-subtitle">The same SDUI JSON rendered at different viewport widths. Token values adapt to screen context.</p>

        <div className="viewport-toggles">
          <button
            className={`viewport-btn ${activeViewport === 'phone' ? 'active' : ''}`}
            onClick={() => setActiveViewport('phone')}
          >
            <PhoneIcon />
            Phone
          </button>
          <button
            className={`viewport-btn ${activeViewport === 'tablet' ? 'active' : ''}`}
            onClick={() => setActiveViewport('tablet')}
          >
            <TabletIcon />
            Tablet
          </button>
          <button
            className={`viewport-btn ${activeViewport === 'desktop' ? 'active' : ''}`}
            onClick={() => setActiveViewport('desktop')}
          >
            <DesktopIcon />
            Desktop
          </button>
        </div>

        <div className="viewport-frames">
          <div className={`viewport-frame frame-phone ${activeViewport === 'phone' ? 'active' : ''}`}>
            <div className="frame-chrome">
              <span className="frame-label">320px</span>
            </div>
            <div className="frame-content">
              <ScoreboardPreview compact />
            </div>
          </div>
          <div className={`viewport-frame frame-tablet ${activeViewport === 'tablet' ? 'active' : ''}`}>
            <div className="frame-chrome">
              <span className="frame-label">768px</span>
            </div>
            <div className="frame-content">
              <ScoreboardPreview />
            </div>
          </div>
          <div className={`viewport-frame frame-desktop ${activeViewport === 'desktop' ? 'active' : ''}`}>
            <div className="frame-chrome">
              <span className="frame-label">1200px</span>
            </div>
            <div className="frame-content">
              <ScoreboardPreview expanded />
            </div>
          </div>
        </div>
      </div>
    </section>
  )
}

function ScoreboardPreview({ compact, expanded }: { compact?: boolean; expanded?: boolean }) {
  return (
    <div className={`scoreboard-preview ${compact ? 'compact' : ''} ${expanded ? 'expanded' : ''}`}>
      <div className="scoreboard-header">
        <span className="scoreboard-status">FINAL</span>
      </div>
      <div className="scoreboard-teams">
        <div className="scoreboard-team">
          <div className="team-logo-placeholder"></div>
          <span className="team-abbr">LAL</span>
          <span className="team-score">112</span>
        </div>
        <div className="scoreboard-team">
          <div className="team-logo-placeholder"></div>
          <span className="team-abbr">BOS</span>
          <span className="team-score">108</span>
        </div>
      </div>
      {expanded && (
        <div className="scoreboard-extra">
          <div className="quarter-scores">
            <span className="q-label">Q1</span>
            <span className="q-label">Q2</span>
            <span className="q-label">Q3</span>
            <span className="q-label">Q4</span>
          </div>
        </div>
      )}
    </div>
  )
}

function PhoneIcon() {
  return (
    <svg width="16" height="16" viewBox="0 0 16 16" fill="none" stroke="currentColor" strokeWidth="1.5">
      <rect x="4" y="1" width="8" height="14" rx="2" />
      <line x1="6.5" y1="12" x2="9.5" y2="12" />
    </svg>
  )
}

function TabletIcon() {
  return (
    <svg width="16" height="16" viewBox="0 0 16 16" fill="none" stroke="currentColor" strokeWidth="1.5">
      <rect x="2" y="2" width="12" height="12" rx="2" />
      <line x1="7" y1="12" x2="9" y2="12" />
    </svg>
  )
}

function DesktopIcon() {
  return (
    <svg width="16" height="16" viewBox="0 0 16 16" fill="none" stroke="currentColor" strokeWidth="1.5">
      <rect x="1" y="2" width="14" height="10" rx="1.5" />
      <line x1="5" y1="14" x2="11" y2="14" />
      <line x1="8" y1="12" x2="8" y2="14" />
    </svg>
  )
}
