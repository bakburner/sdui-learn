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
    answer: 'Clients cache the last successful response. Offline users see slightly stale content rather than blank screens. Unknown section types are gracefully skipped — never crash.',
  },
  {
    question: 'Who controls what?',
    misconception: 'SDUI means the server controls everything',
    answer: 'The server controls what appears and where — screen structure, sections, ordering, content. Platform teams still own how things render: animations, interactions, accessibility, and platform-native behaviors. It\'s a clear split.',
  },
  {
    question: 'Can each platform look different?',
    misconception: 'One JSON = identical UI everywhere',
    answer: 'Yes. The server can produce different compositions per platform family. Phone, tablet, TV, and web each get layouts tuned to their form factor. And "semantic" sections invoke platform-specific components (video players, tab bars) when needed.',
  },
  {
    question: 'How do we decide what should be SDUI vs. native?',
    misconception: 'Everything must go through SDUI',
    answer: 'Simple rule: if the UI changes frequently, varies by user segment, or needs to ship without a release — use SDUI (Atomic). If it requires complex native state, gestures, or platform SDKs — keep it native (Semantic). Both coexist on the same screen.',
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
    answer: 'Schema evolution is additive-only. Clients send their schemaVersion in the request envelope; the server strips unsupported fields and signals force-upgrade via an X-Schema-Version-Mismatch header only when absolutely necessary. Unknown section types and action types are gracefully skipped.',
  },
  {
    question: 'How are typed models generated for each platform?',
    misconception: 'Each platform hand-writes its own models',
    answer: 'A single JSON Schema file is the source of truth. A codegen pipeline produces Java POJOs (via jsonschema2pojo) for server and Android, Swift structs (via quicktype) for iOS, and TypeScript types for web. Generated models are never hand-edited — if the schema changes, regenerate.',
  },
  {
    question: 'What about internationalization?',
    misconception: 'The server must know the user\'s language',
    answer: 'The server is locale-blind. It emits LocalizedString keys with English fallbacks. Clients resolve strings from their bundled translation catalog using device locale. For real-time data (like game status text arriving via SSE), the server attaches string keys to data bindings so the client can translate dynamically-bound fields too.',
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
