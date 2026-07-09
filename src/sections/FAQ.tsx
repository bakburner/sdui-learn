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
    answer: 'Design tokens keep payloads compact — "gap": "md" instead of pixel values. A typical screen is ~15KB raw, ~4KB gzipped. Constraints (max depth 6, max 20 children) prevent unbounded growth.',
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
