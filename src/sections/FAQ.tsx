import { useState } from 'react'
import { useScrollReveal, useStaggerReveal } from '../hooks/useScrollReveal'
import '../hooks/useScrollReveal.css'
import './FAQ.css'

interface FAQItem {
  question: string
  answer: string
  misconception?: string
}

const FAQS: FAQItem[] = [
  {
    question: 'Is this a WebView? Will it feel janky?',
    misconception: 'SDUI = WebViews or hybrid apps',
    answer: 'No. The server sends JSON that describes the layout. Each platform renders it with fully native UI — SwiftUI, Jetpack Compose, React. The rendering path is identical to hand-coded native code; only the layout instructions come from the server.',
  },
  {
    question: 'What happens if the server is down?',
    misconception: 'SDUI requires constant connectivity',
    answer: 'Clients cache the last successful response. Offline users see slightly stale content rather than blank screens. Sections declare their own ErrorState (message, retryAction, hideOnError) and Skeleton (shimmer / spinner / placeholder / none) for loading states. Unknown section types are gracefully skipped — never crash.',
  },
  {
    question: 'Who controls what?',
    misconception: 'SDUI means the server controls everything',
    answer: 'The server controls what appears and where — section composition, ordering, content, actions, refresh policies. Platform teams still own how things render: native component implementation, gesture/focus handling, networking primitives, nav/analytics SDK integration, and platform accessibility. The section is the boundary of server authority.',
  },
  {
    question: 'Can each platform look different?',
    misconception: 'One JSON = identical UI everywhere',
    answer: 'Yes. The server produces different compositions per platform family. Phone, tablet, TV, and web each get layouts tuned to their form factor. Semantic sections (TabGroup, BoxscoreTable, Form, SeasonLeadersTable, AdSlot, SubscribeUpsell) invoke dedicated platform-native renderers. Surfaces (section.surface) adapt per OS tier — Liquid Glass on iOS 26+, Material 3 on Android, CSS filter on Web.',
  },
  {
    question: 'How do we decide what should be SDUI vs. native?',
    misconception: 'Everything must go through SDUI',
    answer: 'Simple rule: if the UI changes frequently, varies by user segment, or needs to ship without a release — use an AtomicComposite section (server-composed tree of atomic primitives, the default section type for stateless layout surfaces). If it requires client-owned state, SDK hosting, or runtime lifecycle — use a Semantic section (dedicated client renderer). Both coexist on the same screen.',
  },
  {
    question: 'Won\'t the JSON payloads get huge?',
    misconception: 'Describing UI in JSON is bloated',
    answer: 'Design tokens keep payloads compact — "gap": "md" instead of pixel values. A typical screen is ~15KB raw, ~4KB gzipped. Constraints (max depth 6, max 10 children per container, max 256 nodes per atomic tree) prevent unbounded growth.',
  },
  {
    question: 'Why build our own instead of using an off-the-shelf SDUI platform?',
    misconception: 'DivKit, Nativeblocks, etc. already solve this',
    answer: 'We evaluated existing platforms and they don\'t meet our requirements. Our game screens need the scoreboard updating in real time (SSE) while editorial content below is cached for hours — off-the-shelf platforms treat the entire screen as one data unit. They also can\'t bind to our live data services (dual source of truth) and their rendering engines replace native components rather than wrapping them, breaking design system consistency.',
  },
  {
    question: 'How does A/B testing work with SDUI?',
    misconception: 'Experiments require client code per variant',
    answer: 'Clients send their experiment assignments in the request envelope. The server uses those assignments to branch composition — different section ordering, content, or layout per variant. No client code changes needed per experiment. The client SDK owns assignment and kill switches; exposure tracking fires via fireAndForget actions.',
  },
  {
    question: 'How does the schema evolve without breaking older app versions?',
    misconception: 'Schema changes require forced app updates',
    answer: 'Schema evolution is additive-only. Clients send their schemaVersion (major.minor) in the request envelope. When below currentVersion, the server strips fields/enums introduced after that version. When below minSupportedVersion, the server returns X-Schema-Version-Mismatch: upgrade-required and an ErrorState section — clients detect the header and show a platform-appropriate upgrade prompt. Unknown section types and action types are gracefully skipped.',
  },
  {
    question: 'How are typed models generated for each platform?',
    misconception: 'Each platform hand-writes its own models',
    answer: 'A single JSON Schema file (schema/sdui-schema.json) is the source of truth for the wire contract. A codegen build step (make codegen) emits Swift, Kotlin, TypeScript, and Java models. Generated files are checked in; do not hand-edit.',
  },
  {
    question: 'What about internationalization?',
    misconception: 'The server must know the user\'s language',
    answer: 'The server is locale-blind — locale and Accept-Language are not composition inputs or cache-key dimensions. It emits LocalizedString values: either a bare string literal (render as-is) or { key, args?, fallback }. Clients resolve keyed values from their bundled catalog using device locale; misses render the required English fallback. For live-data strings, dataBinding.stringKeys map target paths to localization keys so clients translate dynamically-bound fields.',
  },
]

export function FAQ() {
  const [openIndex, setOpenIndex] = useState<number | null>(null)
  const header = useScrollReveal<HTMLDivElement>()
  const { containerRef, visibleItems } = useStaggerReveal(FAQS.length, 80)

  return (
    <section id="faq">
      <div ref={header.ref} className={`reveal ${header.isVisible ? 'visible' : ''}`}>
        <div className="section-label">Common Questions</div>
        <h2 className="section-title">FAQ & Misconceptions</h2>
        <p className="section-subtitle">
          Addressing the most common concerns about adopting Server-Driven UI.
        </p>
      </div>

      <div className="faq-list" ref={containerRef}>
        {FAQS.map((faq, i) => (
          <div
            key={i}
            className={`faq-item stagger-item ${visibleItems.has(i) ? 'visible' : ''} ${openIndex === i ? 'open' : ''}`}
          >
            <button
              className="faq-question"
              onClick={() => setOpenIndex(openIndex === i ? null : i)}
            >
              <div className="faq-question-content">
                {faq.misconception && (
                  <span className="faq-misconception">Misconception: {faq.misconception}</span>
                )}
                <span className="faq-question-text">{faq.question}</span>
              </div>
              <span className="faq-toggle">{openIndex === i ? '−' : '+'}</span>
            </button>
            {openIndex === i && (
              <div className="faq-answer">
                <p>{faq.answer}</p>
              </div>
            )}
          </div>
        ))}
      </div>
    </section>
  )
}
