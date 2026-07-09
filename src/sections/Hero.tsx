import { useScrollReveal } from '../hooks/useScrollReveal'
import '../hooks/useScrollReveal.css'
import './Hero.css'

export function Hero() {
  const content = useScrollReveal<HTMLDivElement>({ threshold: 0.2 })
  const visual = useScrollReveal<HTMLDivElement>({ threshold: 0.2 })

  return (
    <section id="hero" className="hero-section">
      <div ref={content.ref} className={`hero-content reveal-left ${content.isVisible ? 'visible' : ''}`}>
        <div className="hero-badge">Server-Driven UI</div>
        <h1 className="hero-title">
          Build Once.<br />
          Update Everywhere.
        </h1>
        <p className="hero-description">
          Learn how Server-Driven UI lets you control what users see — without shipping app updates.
          The server decides the layout. Clients render it natively.
        </p>
        <div className="hero-cta">
          <a href="#how-it-works" className="btn-primary">Start Learning</a>
          <button className="btn-playground" onClick={() => window.dispatchEvent(new CustomEvent('launch-editor'))}>Try Playground</button>
        </div>
      </div>
      <div ref={visual.ref} className={`hero-visual reveal-right ${visual.isVisible ? 'visible' : ''}`}>
        <div className="hero-diagram">
          <div className="diagram-server">
            <div className="diagram-label">Server</div>
            <div className="diagram-code">
              <span className="code-punct">{'{'}</span>
              <br />
              <span className="code-indent">
                <span className="code-prop">"sections"</span>
                <span className="code-punct">: [</span>
              </span>
              <br />
              <span className="code-indent2">
                <span className="code-punct">{'{'}</span>
                <span className="code-prop">"type"</span>
                <span className="code-punct">: </span>
                <span className="code-str">"ScoreboardHeader"</span>
                <span className="code-punct">{'}'}</span>
              </span>
              <br />
              <span className="code-indent2">
                <span className="code-punct">{'{'}</span>
                <span className="code-prop">"type"</span>
                <span className="code-punct">: </span>
                <span className="code-str">"ContentRail"</span>
                <span className="code-punct">{'}'}</span>
              </span>
              <br />
              <span className="code-indent">
                <span className="code-punct">]</span>
              </span>
              <br />
              <span className="code-punct">{'}'}</span>
            </div>
          </div>
          <div className="diagram-arrow">
            <svg width="40" height="24" viewBox="0 0 40 24" fill="none">
              <path d="M2 12H34M34 12L26 4M34 12L26 20" stroke="var(--nba-tint)" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/>
            </svg>
          </div>
          <div className="diagram-client">
            <div className="diagram-label">Client</div>
            <div className="diagram-phone">
              <div className="phone-section phone-score">
                <div className="score-team">LAL 108</div>
                <div className="score-team">BOS 112</div>
              </div>
              <div className="phone-section phone-rail">
                <div className="rail-card"></div>
                <div className="rail-card"></div>
                <div className="rail-card"></div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>
  )
}
