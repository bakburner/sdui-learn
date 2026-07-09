import { useState } from 'react'
import { useScrollReveal, useStaggerReveal } from '../hooks/useScrollReveal'
import '../hooks/useScrollReveal.css'
import './HowItWorks.css'

const STEPS = [
  {
    id: 'compose',
    number: '01',
    title: 'Server Composes the Screen',
    description: 'The server decides what the user sees — which sections appear, in what order, with what content. Think of it as a page builder that runs on the backend.',
    visual: 'compose',
  },
  {
    id: 'payload',
    number: '02',
    title: 'JSON Describes the UI',
    description: 'The server sends a JSON response describing the screen. Each Section carries id, type, data, sectionStates (skeleton/error), and optionally a refreshPolicy. The schema (sdui-schema.json) is the single source of truth.',
    visual: 'payload',
  },
  {
    id: 'router',
    number: '03',
    title: 'Client Routes Each Section',
    description: 'The app reads each section\'s type and decides how to render it. AtomicComposite sections are fully server-composed trees; Semantic sections invoke dedicated client renderers justified by client-owned state, SDK hosting, or runtime lifecycle.',
    visual: 'router',
  },
  {
    id: 'renderers',
    number: '04',
    title: 'Native Rendering',
    description: 'The app renders everything using native platform UI — not a webview. The server controls what appears; the client controls how it looks and feels on each device.',
    visual: 'renderers',
  },
  {
    id: 'update',
    number: '05',
    title: 'Update Instantly',
    description: 'Need to change the layout? Add a promo? Reorder the feed? Update the server response. Every app reflects the change on next load — no app store release, no waiting.',
    visual: 'update',
  },
]

export function HowItWorks() {
  const [activeStep, setActiveStep] = useState(0)
  const header = useScrollReveal<HTMLDivElement>()
  const { containerRef, visibleItems } = useStaggerReveal(STEPS.length, 120)
  const visualReveal = useScrollReveal<HTMLDivElement>({ threshold: 0.05 })

  return (
    <section id="how-it-works">
      <div ref={header.ref} className={`reveal ${header.isVisible ? 'visible' : ''}`}>
        <div className="section-label">How It Works</div>
        <h2 className="section-title">The SDUI Flow</h2>
        <p className="section-subtitle">
          Five steps from server to screen. The server decides what shows up; the app handles how it renders natively on each platform.
        </p>
      </div>

      <div className="flow-container">
        <div className="flow-steps" ref={containerRef}>
          {STEPS.map((step, index) => (
            <button
              key={step.id}
              className={`flow-step stagger-item ${visibleItems.has(index) ? 'visible' : ''} ${activeStep === index ? 'active' : ''}`}
              onClick={() => setActiveStep(index)}
            >
              <span className="step-number">{step.number}</span>
              <div className="step-content">
                <h3 className="step-title">{step.title}</h3>
                <p className="step-description">{step.description}</p>
              </div>
            </button>
          ))}
        </div>

        <div ref={visualReveal.ref} className={`flow-visual reveal-scale ${visualReveal.isVisible ? 'visible' : ''}`}>
          <FlowVisual step={STEPS[activeStep].visual} />
        </div>
      </div>

      <div className="concepts-grid">
        <div className="concepts-label">Key Concepts</div>
        <ConceptCard
          title="Sections"
          description="Top-level unit of refresh and visibility — the smallest chunk the server can replace independently. Semantic sections (BoxscoreTable, Form) have dedicated client renderers; AtomicComposite sections are fully server-composed"
        />
        <ConceptCard
          title="Atomic Primitives"
          description="12 element types the server composes into AtomicComposite trees: Container, Text, Image, Button, Spacer, Divider, ScrollContainer, Conditional, DisplayGrid, SectionSlot, LiveClock, OverlayContainer"
        />
        <ConceptCard
          title="Design Tokens"
          description="Three-layer style model: inline primitives (padding, cornerRadius), style tokens (nba.spacing.md — registered in style-tokens.json), and color tokens (light/dark/contrast in color-tokens.json). Variants express semantic visual intent. Override matrix: style token < variant < inline override"
        />
        <ConceptCard
          title="Refresh Policy"
          description="Section-level rules for live data — static (no refresh), poll (interval HTTP fetch via sectionEndpoint), SSE (server-sent events), or Ably channel. All push paths feed the same data-binding pipeline"
        />
        <ConceptCard
          title="Actions"
          description="Server-declared commands: navigate, fireAndForget (beacon), mutate, refresh, request, purchase, dismiss, flashMessage — fired by triggers (onActivate, onLongPress, onVisible, onSwipe, onFocus, onBlur, onSubmit)"
        />
        <ConceptCard
          title="Data Binding"
          description="Declarative mappings from incoming live-data payloads to paths inside a section's data. BindRef on atomic primitives resolves from data.content — the scoreboard updates without re-fetching the section"
        />
        <ConceptCard
          title="Request Envelope"
          description="Structured query/POST body for every composition fetch — platform, deviceClass, schemaVersion, experiments, capabilities, market/cohort. Locale is intentionally absent (client-owned, cache-neutral)"
        />
        <ConceptCard
          title="Screen State"
          description="Runtime per-screen key-value map owned by the client — mutate actions read/write it, paramBindings resolve against it, refresh responses merge into it. The client-side counterpart to server-emitted data"
        />
      </div>
    </section>
  )
}

function ConceptCard({ title, description }: { title: string; description: string }) {
  const reveal = useScrollReveal<HTMLDivElement>({ threshold: 0.3 })
  return (
    <div ref={reveal.ref} className={`concept-card reveal ${reveal.isVisible ? 'visible' : ''}`}>
      <span className="concept-title">{title}</span>
      <span className="concept-desc">{description}</span>
    </div>
  )
}

function FlowVisual({ step }: { step: string }) {
  switch (step) {
    case 'compose':
      return (
        <div className="visual-card">
          <div className="visual-header">Server — UI Composer</div>
          <div className="visual-layers">
            <div className="layer layer-data">
              <span className="layer-label">What appears</span>
              <span className="layer-detail">screen structure, sections</span>
            </div>
            <div className="layer layer-logic">
              <span className="layer-label">Composition</span>
              <span className="layer-detail">layout, ordering, personalization</span>
            </div>
            <div className="layer layer-layout">
              <span className="layer-label">Presentation</span>
              <span className="layer-detail">tokens, variants, A/B testing</span>
            </div>
          </div>
          <div className="visual-output">
            <span className="output-arrow">↓</span>
            <span className="output-label">SDUI Payload (JSON)</span>
          </div>
        </div>
      )
    case 'payload':
      return (
        <div className="visual-card">
          <div className="visual-header">SDUI Payload</div>
          <div className="payload-structure">
            <div className="payload-item">
              <span className="payload-key">data.id</span>
              <span className="payload-val">"for-you"</span>
            </div>
            <div className="payload-item">
              <span className="payload-key">data.schemaVersion</span>
              <span className="payload-val">"1.0"</span>
            </div>
            <div className="payload-item">
              <span className="payload-key">data.sections[]</span>
              <span className="payload-val">ordered renderable sections</span>
            </div>
            <div className="payload-item payload-nested">
              <span className="payload-key">id / type</span>
              <span className="payload-val">section identity + router instruction</span>
            </div>
            <div className="payload-item payload-nested">
              <span className="payload-key">data.ui</span>
              <span className="payload-val">atomic element tree</span>
            </div>
            <div className="payload-item payload-nested">
              <span className="payload-key">data.content</span>
              <span className="payload-val">bindRef resolution data</span>
            </div>
            <div className="payload-item payload-nested">
              <span className="payload-key">refreshPolicy / surface</span>
              <span className="payload-val">lifecycle + chrome</span>
            </div>
            <div className="payload-item">
              <span className="payload-key">meta.degraded</span>
              <span className="payload-val">service health signal</span>
            </div>
          </div>
        </div>
      )
    case 'router':
      return (
        <div className="visual-card">
          <div className="visual-header">Section Router</div>
          <div className="router-diagram">
            <div className="router-input">
              <span className="router-label">Incoming Section</span>
            </div>
            <div className="router-arrow-down">↓</div>
            <div className="router-decision">
              <span className="router-q">sectionType?</span>
            </div>
            <div className="router-branches">
              <div className="router-branch branch-atomic">
                <span className="branch-label">AtomicComposite</span>
                <span className="branch-desc">Atomic Renderer</span>
              </div>
              <div className="router-branch branch-semantic">
                <span className="branch-label">BoxscoreTable, Form…</span>
                <span className="branch-desc">Semantic Renderer</span>
              </div>
              <div className="router-branch branch-bridge">
                <span className="branch-label">SectionSlot</span>
                <span className="branch-desc">Bridge (Combined)</span>
              </div>
            </div>
          </div>
        </div>
      )
    case 'renderers':
      return (
        <div className="visual-card">
          <div className="visual-header">Client — Rendering Engine</div>
          <div className="visual-renderers">
            <div className="renderer-box">
              <div className="renderer-box-label">Atomic Renderer</div>
              <div className="renderer-box-desc">Generic UI elements composed by server</div>
              <div className="renderer-box-items">
                <span className="renderer-chip">Container</span>
                <span className="renderer-chip">Text</span>
                <span className="renderer-chip">Image</span>
                <span className="renderer-chip">Button</span>
              </div>
            </div>
            <div className="renderer-box semantic-box">
              <div className="renderer-box-label">Semantic Renderer</div>
              <div className="renderer-box-desc">Native platform components invoked</div>
              <div className="renderer-box-items">
                <span className="renderer-chip">BoxscoreTable</span>
                <span className="renderer-chip">TabGroup</span>
                <span className="renderer-chip">VideoPlayer</span>
              </div>
            </div>
          </div>
          <p className="visual-footnote">Rendering implementation · Native interactions · Platform behavior</p>
        </div>
      )
    case 'update':
      return (
        <div className="visual-card">
          <div className="visual-header">Instant Updates</div>
          <div className="visual-timeline">
            <div className="timeline-item">
              <div className="timeline-dot dot-before"></div>
              <div className="timeline-content">
                <span className="timeline-label">Before</span>
                <div className="timeline-blocks">
                  <div className="tblock t1">Header</div>
                  <div className="tblock t2">Stats</div>
                  <div className="tblock t3">Rail</div>
                </div>
              </div>
            </div>
            <div className="timeline-change">Server composition change →</div>
            <div className="timeline-item">
              <div className="timeline-dot dot-after"></div>
              <div className="timeline-content">
                <span className="timeline-label">After</span>
                <div className="timeline-blocks">
                  <div className="tblock t4">Promo</div>
                  <div className="tblock t1">Header</div>
                  <div className="tblock t2">Stats</div>
                  <div className="tblock t3">Rail</div>
                </div>
              </div>
            </div>
          </div>
          <p className="visual-footnote">No app store review. No client release. Presentation controlled server-side.</p>
        </div>
      )
    default:
      return null
  }
}
