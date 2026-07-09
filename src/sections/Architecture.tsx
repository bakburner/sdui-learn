import { useScrollReveal, useStaggerReveal } from '../hooks/useScrollReveal'
import '../hooks/useScrollReveal.css'
import './Architecture.css'

const SERVER_OWNED_AREAS = [
  {
    id: 'layout',
    title: 'Layout',
    description: 'Section roster, order, nesting, atomic trees',
    icon: 'grid',
  },
  {
    id: 'content',
    title: 'Content',
    description: 'Copy, labels, LocalizedString keys with English fallbacks',
    icon: 'text',
  },
  {
    id: 'assets',
    title: 'Assets',
    description: 'Image URLs, video IDs, sponsor marks',
    icon: 'image',
  },
  {
    id: 'style',
    title: 'Style',
    description: 'Style tokens (style-tokens.json), color tokens (color-tokens.json), variants, surfaces',
    icon: 'palette',
  },
  {
    id: 'refresh',
    title: 'Refresh Policy',
    description: 'Static / poll (sectionEndpoint) / SSE / Ably — section-level live-data control',
    icon: 'refresh',
  },
  {
    id: 'data-flow',
    title: 'Data Flow',
    description: 'First-paint data in section.data, live patches via DataBindingPath mappings and BindRef resolution',
    icon: 'flow',
  },
  {
    id: 'actions',
    title: 'Actions',
    description: 'Navigate, refresh, mutate, fire-and-forget, request, purchase, dismiss, flashMessage',
    icon: 'action',
  },
]

const SERVER_CONTROLS = [
  'Section composition & ordering',
  'Payload fields & defaults',
  'Refresh policies & bindings',
  'Action definitions & sequencing',
  'Experiment/variant composition',
]

const CLIENT_CONTROLS = [
  'Native component rendering',
  'Platform gesture/focus handling',
  'Networking primitives',
  'Nav/analytics SDK integration',
  'Platform accessibility',
]

const ENVELOPE_FIELDS = [
  { key: 'platform[deviceClass]', value: '"phone" | "tablet" | "tv" | "desktop"' },
  { key: 'platform[capabilities]', value: '["sse", "video-inline", "haptics"]' },
  { key: 'schemaVersion', value: '"2.4" (major.minor)' },
  { key: 'experiments', value: '{ "new-scoreboard": "variant-b" }' },
  { key: 'market[cohort]', value: '"league-pass-premium"' },
  { key: 'X-Correlation-ID', value: 'per-request UUID (header)' },
]

const WHY_BUILD_REASONS = [
  {
    title: 'No mixed refresh strategies',
    description: 'NBA needs scoreboard SSE + cached editorial on the same screen. Vendor systems assume one refresh mode per page.',
  },
  {
    title: 'No data binding to our services',
    description: 'Vendor backends create a dual source of truth. Our data lives in NBA services; binding must point there directly.',
  },
  {
    title: 'Limited design system integration',
    description: 'Off-the-shelf solutions replace native components rather than wrapping them. We need our design system rendered natively.',
  },
]

export function Architecture() {
  const header = useScrollReveal<HTMLDivElement>()
  const { containerRef: areasRef, visibleItems: areasVisible } = useStaggerReveal(SERVER_OWNED_AREAS.length, 100)
  const ownershipReveal = useScrollReveal<HTMLDivElement>({ threshold: 0.05 })
  const envelopeReveal = useScrollReveal<HTMLDivElement>({ threshold: 0.05 })
  const platformReveal = useScrollReveal<HTMLDivElement>({ threshold: 0.05 })
  const buildReveal = useScrollReveal<HTMLDivElement>({ threshold: 0.05 })
  const { containerRef: buildRef, visibleItems: buildVisible } = useStaggerReveal(WHY_BUILD_REASONS.length, 150)

  return (
    <section id="architecture">
      <div ref={header.ref} className={`reveal ${header.isVisible ? 'visible' : ''}`}>
        <div className="section-label">Architecture</div>
        <h2 className="section-title">Server-Driven Architecture</h2>
        <p className="section-subtitle">
          SDUI shifts ownership of seven key areas from the client to the server, enabling instant updates and platform-aware composition without app releases.
        </p>
      </div>

      {/* Seven Server-Owned Areas */}
      <div className="arch-areas" ref={areasRef}>
        <h3 className="arch-subsection-title">The Seven Server-Owned Areas</h3>
        <div className="areas-grid">
          {SERVER_OWNED_AREAS.map((area, index) => (
            <div
              key={area.id}
              className={`area-card stagger-item ${areasVisible.has(index) ? 'visible' : ''}`}
            >
              <div className="area-icon">
                <AreaIcon type={area.icon} />
              </div>
              <div className="area-info">
                <span className="area-title">{area.title}</span>
                <span className="area-desc">{area.description}</span>
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* Server vs Client Ownership */}
      <div ref={ownershipReveal.ref} className={`arch-ownership reveal ${ownershipReveal.isVisible ? 'visible' : ''}`}>
        <h3 className="arch-subsection-title">Server vs Client Ownership</h3>
        <div className="ownership-split">
          <div className="ownership-column ownership-server">
            <div className="ownership-header">
              <span className="ownership-badge badge-server">Server Controls</span>
            </div>
            <ul className="ownership-list">
              {SERVER_CONTROLS.map((item) => (
                <li key={item} className="ownership-item">
                  <span className="ownership-dot dot-server"></span>
                  {item}
                </li>
              ))}
            </ul>
          </div>
          <div className="ownership-divider">
            <div className="ownership-divider-line"></div>
          </div>
          <div className="ownership-column ownership-client">
            <div className="ownership-header">
              <span className="ownership-badge badge-client">Client Controls</span>
            </div>
            <ul className="ownership-list">
              {CLIENT_CONTROLS.map((item) => (
                <li key={item} className="ownership-item">
                  <span className="ownership-dot dot-client"></span>
                  {item}
                </li>
              ))}
            </ul>
          </div>
        </div>
      </div>

      {/* Request Envelope */}
      <div ref={envelopeReveal.ref} className={`arch-envelope reveal ${envelopeReveal.isVisible ? 'visible' : ''}`}>
        <h3 className="arch-subsection-title">Request Envelope</h3>
        <p className="arch-subsection-desc">
          The structured query/POST body for every composition fetch. The server uses this for platform-aware composition. Locale is intentionally absent — language is client-owned and cache-neutral.
        </p>
        <div className="envelope-card">
          <div className="envelope-header">
            <span className="envelope-label">Client Context</span>
            <span className="envelope-arrow">&#8594;</span>
            <span className="envelope-label">Server Composer</span>
          </div>
          <div className="envelope-fields">
            {ENVELOPE_FIELDS.map((field) => (
              <div key={field.key} className="envelope-field">
                <span className="envelope-key">{field.key}</span>
                <span className="envelope-val">{field.value}</span>
              </div>
            ))}
          </div>
        </div>
      </div>

      {/* Platform-Aware Composition */}
      <div ref={platformReveal.ref} className={`arch-platform reveal ${platformReveal.isVisible ? 'visible' : ''}`}>
        <h3 className="arch-subsection-title">Platform-Aware Composition</h3>
        <p className="arch-subsection-desc">
          Same section types and action vocabulary everywhere, but different ordering, density, action URIs, and image dimensions per platform.
        </p>
        <div className="platform-diagram">
          <div className="platform-source">
            <span className="platform-source-label">Shared Schema</span>
            <span className="platform-source-detail">Section types, actions, tokens</span>
          </div>
          <div className="platform-arrows">
            <div className="platform-arrow-line"></div>
            <div className="platform-arrow-branches">
              <div className="platform-arrow-branch"></div>
              <div className="platform-arrow-branch"></div>
              <div className="platform-arrow-branch"></div>
            </div>
          </div>
          <div className="platform-composers">
            <div className="platform-composer composer-mobile">
              <span className="composer-icon">
                <PhoneIcon />
              </span>
              <span className="composer-label">Mobile</span>
              <span className="composer-detail">Compact density, deep-link URIs, 2x images</span>
            </div>
            <div className="platform-composer composer-web">
              <span className="composer-icon">
                <DesktopIcon />
              </span>
              <span className="composer-label">Web</span>
              <span className="composer-detail">Wide grids, URL paths, responsive images</span>
            </div>
            <div className="platform-composer composer-tv">
              <span className="composer-icon">
                <TVIcon />
              </span>
              <span className="composer-label">TV</span>
              <span className="composer-detail">Focus-driven, large type, 4K assets</span>
            </div>
          </div>
          <div className="platform-output">
            <div className="platform-output-row">
              <span className="platform-output-label">Same section types</span>
              <span className="platform-output-label">Same action vocabulary</span>
              <span className="platform-output-label">Platform-tailored response</span>
            </div>
          </div>
        </div>
      </div>

      {/* Why Build, Not Buy */}
      <div ref={buildReveal.ref} className={`arch-build reveal ${buildReveal.isVisible ? 'visible' : ''}`}>
        <h3 className="arch-subsection-title">Why Build, Not Buy</h3>
        <p className="arch-subsection-desc">
          Evaluated DivKit (Yandex) and Nativeblocks. They fail on NBA-specific requirements:
        </p>
        <div className="build-reasons" ref={buildRef}>
          {WHY_BUILD_REASONS.map((reason, index) => (
            <div
              key={reason.title}
              className={`build-reason stagger-item ${buildVisible.has(index) ? 'visible' : ''}`}
            >
              <div className="reason-x">&#10005;</div>
              <div className="reason-content">
                <span className="reason-title">{reason.title}</span>
                <span className="reason-desc">{reason.description}</span>
              </div>
            </div>
          ))}
        </div>
      </div>
    </section>
  )
}

function AreaIcon({ type }: { type: string }) {
  switch (type) {
    case 'grid':
      return (
        <svg width="20" height="20" viewBox="0 0 20 20" fill="none" stroke="currentColor" strokeWidth="1.5">
          <rect x="2" y="2" width="7" height="7" rx="1" />
          <rect x="11" y="2" width="7" height="7" rx="1" />
          <rect x="2" y="11" width="7" height="7" rx="1" />
          <rect x="11" y="11" width="7" height="7" rx="1" />
        </svg>
      )
    case 'text':
      return (
        <svg width="20" height="20" viewBox="0 0 20 20" fill="none" stroke="currentColor" strokeWidth="1.5">
          <line x1="3" y1="5" x2="17" y2="5" />
          <line x1="3" y1="10" x2="14" y2="10" />
          <line x1="3" y1="15" x2="11" y2="15" />
        </svg>
      )
    case 'image':
      return (
        <svg width="20" height="20" viewBox="0 0 20 20" fill="none" stroke="currentColor" strokeWidth="1.5">
          <rect x="2" y="3" width="16" height="14" rx="2" />
          <circle cx="7" cy="8" r="2" />
          <path d="M2 14l4-4 3 3 4-5 5 6" />
        </svg>
      )
    case 'palette':
      return (
        <svg width="20" height="20" viewBox="0 0 20 20" fill="none" stroke="currentColor" strokeWidth="1.5">
          <circle cx="10" cy="10" r="8" />
          <circle cx="10" cy="6" r="1.5" fill="currentColor" />
          <circle cx="6.5" cy="10" r="1.5" fill="currentColor" />
          <circle cx="13.5" cy="10" r="1.5" fill="currentColor" />
          <circle cx="10" cy="14" r="1.5" fill="currentColor" />
        </svg>
      )
    case 'refresh':
      return (
        <svg width="20" height="20" viewBox="0 0 20 20" fill="none" stroke="currentColor" strokeWidth="1.5">
          <path d="M3 10a7 7 0 0112.9-3.7" />
          <path d="M17 10a7 7 0 01-12.9 3.7" />
          <polyline points="16 3 16 7 12 7" />
          <polyline points="4 17 4 13 8 13" />
        </svg>
      )
    case 'flow':
      return (
        <svg width="20" height="20" viewBox="0 0 20 20" fill="none" stroke="currentColor" strokeWidth="1.5">
          <circle cx="5" cy="5" r="2.5" />
          <circle cx="15" cy="15" r="2.5" />
          <path d="M7 6l6 8" />
          <line x1="15" y1="5" x2="15" y2="9" />
          <circle cx="15" cy="5" r="1" fill="currentColor" />
        </svg>
      )
    case 'action':
      return (
        <svg width="20" height="20" viewBox="0 0 20 20" fill="none" stroke="currentColor" strokeWidth="1.5">
          <polygon points="10,2 12,8 18,8 13,12 15,18 10,14 5,18 7,12 2,8 8,8" />
        </svg>
      )
    default:
      return null
  }
}

function PhoneIcon() {
  return (
    <svg width="20" height="20" viewBox="0 0 20 20" fill="none" stroke="currentColor" strokeWidth="1.5">
      <rect x="5" y="2" width="10" height="16" rx="2" />
      <line x1="8" y1="15" x2="12" y2="15" />
    </svg>
  )
}

function DesktopIcon() {
  return (
    <svg width="20" height="20" viewBox="0 0 20 20" fill="none" stroke="currentColor" strokeWidth="1.5">
      <rect x="2" y="3" width="16" height="11" rx="1.5" />
      <line x1="6" y1="17" x2="14" y2="17" />
      <line x1="10" y1="14" x2="10" y2="17" />
    </svg>
  )
}

function TVIcon() {
  return (
    <svg width="20" height="20" viewBox="0 0 20 20" fill="none" stroke="currentColor" strokeWidth="1.5">
      <rect x="1" y="4" width="18" height="12" rx="1.5" />
      <line x1="5" y1="18" x2="15" y2="18" />
    </svg>
  )
}
