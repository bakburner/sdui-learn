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
    description: 'The server sends a JSON response that describes the entire screen: sections, layout elements, design tokens, and when to refresh. This is the single source of truth.',
    visual: 'payload',
  },
  {
    id: 'router',
    number: '03',
    title: 'Client Routes Each Section',
    description: 'The app reads each section\'s type and decides how to render it. Some sections are fully server-controlled (Atomic), others use native components (Semantic) for complex interactions.',
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
        <ConceptCard
          title="Section"
          description="A block of UI on the screen (scoreboard, content rail, promo card)"
        />
        <ConceptCard
          title="Elements"
          description="UI building blocks — containers hold other elements, leaves display content"
        />
        <ConceptCard
          title="Token"
          description="A named design value — spacing, radius, color, or font style"
        />
        <ConceptCard
          title="Refresh Policy"
          description="Rules for when the app should fetch updated content (timer, push, or manual)"
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
              <span className="payload-key">screenId</span>
              <span className="payload-val">"for-you"</span>
            </div>
            <div className="payload-item">
              <span className="payload-key">sections[]</span>
              <span className="payload-val">ordered renderable areas</span>
            </div>
            <div className="payload-item payload-nested">
              <span className="payload-key">sectionType</span>
              <span className="payload-val">router instruction</span>
            </div>
            <div className="payload-item payload-nested">
              <span className="payload-key">content</span>
              <span className="payload-val">elements (Container / Leaf)</span>
            </div>
            <div className="payload-item payload-nested">
              <span className="payload-key">refreshPolicy</span>
              <span className="payload-val">when to update</span>
            </div>
            <div className="payload-item">
              <span className="payload-key">tokens</span>
              <span className="payload-val">design values (spacing, color)</span>
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
