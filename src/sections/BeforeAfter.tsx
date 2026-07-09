import { useScrollReveal } from '../hooks/useScrollReveal'
import '../hooks/useScrollReveal.css'
import './BeforeAfter.css'

export function BeforeAfter() {
  const header = useScrollReveal<HTMLDivElement>()
  const cards = useScrollReveal<HTMLDivElement>({ threshold: 0.1 })

  return (
    <section id="before-after">
      <div ref={header.ref} className={`reveal ${header.isVisible ? 'visible' : ''}`}>
        <div className="section-label">Why SDUI</div>
        <h2 className="section-title">Before & After</h2>
        <p className="section-subtitle">
          How a single layout change goes from idea to user — traditional releases vs. server-driven UI.
        </p>
      </div>

      <div ref={cards.ref} className={`ba-comparison reveal ${cards.isVisible ? 'visible' : ''}`}>
        <div className="ba-card ba-before">
          <div className="ba-card-header">
            <span className="ba-badge ba-badge-before">Traditional</span>
            <span className="ba-timeline">~2–4 weeks to reach users</span>
          </div>
          <div className="ba-steps">
            <BaStep number={1} label="Design" detail="Finalize layout in Figma" duration="2–3 days" />
            <BaStep number={2} label="Implement" detail="Build on each platform (iOS, Android, Web)" duration="3–5 days" />
            <BaStep number={3} label="QA" detail="Test on each platform, regression testing" duration="2–3 days" />
            <BaStep number={4} label="Release" detail="Submit to App Store & Play Store" duration="1–3 days" />
            <BaStep number={5} label="Review" detail="App Store review process" duration="1–3 days" />
            <BaStep number={6} label="Rollout" detail="Wait for user adoption" duration="2+ weeks" />
          </div>
          <div className="ba-result ba-result-before">
            <span className="ba-result-metric">~30%</span>
            <span className="ba-result-label">of users on new version after 2 weeks</span>
          </div>
        </div>

        <div className="ba-divider">
          <span className="ba-vs">vs</span>
        </div>

        <div className="ba-card ba-after">
          <div className="ba-card-header">
            <span className="ba-badge ba-badge-after">SDUI</span>
            <span className="ba-timeline">Minutes to reach users</span>
          </div>
          <div className="ba-steps">
            <BaStep number={1} label="Design" detail="Finalize layout in Figma" duration="2–3 days" />
            <BaStep number={2} label="Compose" detail="Update server composition (JSON)" duration="Hours" />
            <BaStep number={3} label="Deploy" detail="Push server change" duration="Minutes" />
          </div>
          <div className="ba-result ba-result-after">
            <span className="ba-result-metric">100%</span>
            <span className="ba-result-label">of users see the change instantly</span>
          </div>
        </div>
      </div>

    </section>
  )
}

function BaStep({ number, label, detail, duration }: { number: number; label: string; detail: string; duration: string }) {
  return (
    <div className="ba-step">
      <span className="ba-step-number">{number}</span>
      <div className="ba-step-content">
        <span className="ba-step-label">{label}</span>
        <span className="ba-step-detail">{detail}</span>
      </div>
      <span className="ba-step-duration">{duration}</span>
    </div>
  )
}
