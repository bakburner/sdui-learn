import { useState } from 'react'
import { useScrollReveal, useStaggerReveal } from '../hooks/useScrollReveal'
import '../hooks/useScrollReveal.css'
import './DayInTheLife.css'

interface TimelineStep {
  id: string
  time: string
  label: string
  narrative: string
  json: string
  preview: string[]
}

const TIMELINE_STEPS: TimelineStep[] = [
  {
    id: 'pregame',
    time: '5:00 PM',
    label: 'Pre-Game',
    narrative: 'The server sends an upcoming game card with a countdown timer, team stats comparison, and injury report. The feed is composed around building anticipation.',
    json: `{
  "sectionType": "GameCard",
  "status": "scheduled",
  "countdown": "2h 30m",
  "elements": ["teamStats", "injuryReport"]
}`,
    preview: [
      'LAL vs BOS — 7:30 PM',
      '[ 2h 30m until tip-off ]',
      'LeBron: Probable | Tatum: Active',
    ],
  },
  {
    id: 'almost-time',
    time: '7:25 PM',
    label: 'Almost Time',
    narrative: 'Countdown shifts to "Starting Soon." A "Watch Live" CTA button is injected by the server — no client update required. Urgency elements appear.',
    json: `{
  "sectionType": "GameCard",
  "status": "starting_soon",
  "countdown": null,
  "cta": { "label": "Watch Live", "action": "deeplink://watch" }
}`,
    preview: [
      'LAL vs BOS — Starting Soon',
      '[ Watch Live ]',
      'Lineups confirmed',
    ],
  },
  {
    id: 'tipoff',
    time: '7:30 PM',
    label: 'Tip-Off',
    narrative: 'The game goes live. The server swaps the static card for a live scoreboard with a ticking clock. refreshPolicy changes from "poll" to "sse" for real-time updates.',
    json: `{
  "sectionType": "LiveScoreboard",
  "status": "in_progress",
  "refreshPolicy": "sse",
  "score": { "LAL": 0, "BOS": 0 },
  "clock": "12:00 Q1"
}`,
    preview: [
      'LIVE  LAL 0 — BOS 0',
      'Q1  12:00',
      '[ Play-by-play updating... ]',
    ],
  },
  {
    id: 'halftime',
    time: '8:15 PM',
    label: 'Halftime',
    narrative: 'Score freezes. The server injects halftime stats and a "Top Plays" carousel section — all through a new SDUI payload, zero code changes on the client.',
    json: `{
  "sectionType": "LiveScoreboard",
  "status": "halftime",
  "score": { "LAL": 54, "BOS": 51 },
  "injectedSections": ["halftimeStats", "topPlays"]
}`,
    preview: [
      'HALFTIME  LAL 54 — BOS 51',
      'LeBron: 18 pts | Tatum: 15 pts',
      '[ Top Plays carousel ]',
    ],
  },
  {
    id: 'final',
    time: '9:45 PM',
    label: 'Final',
    narrative: 'Game ends. The scoreboard becomes static. Highlight cards and a "Watch Replay" button are composed in. The server reshapes the entire section hierarchy.',
    json: `{
  "sectionType": "GameCard",
  "status": "final",
  "score": { "LAL": 112, "BOS": 105 },
  "highlights": ["LeBron: 32 PTS, 8 AST"],
  "cta": { "label": "Watch Replay" }
}`,
    preview: [
      'FINAL  LAL 112 — BOS 105',
      'LeBron: 32 PTS, 8 AST',
      '[ Watch Replay ]',
    ],
  },
  {
    id: 'postgame',
    time: '10:00 PM',
    label: 'Post-Game',
    narrative: 'The feed completely reorganizes. A recap card, next game preview, and League Pass promo are composed server-side. Same app, entirely different experience — no deploy.',
    json: `{
  "screenId": "for-you",
  "sections": [
    { "type": "GameRecap", "gameId": "lal-bos" },
    { "type": "NextGame", "matchup": "LAL@MIA" },
    { "type": "Promo", "campaign": "league-pass" }
  ]
}`,
    preview: [
      'Game Recap: Lakers defeat Celtics',
      'Next up: LAL @ MIA — Thursday',
      '[ League Pass: Watch every game ]',
    ],
  },
]

export function DayInTheLife() {
  const [activeStep, setActiveStep] = useState(0)
  const header = useScrollReveal<HTMLDivElement>()
  const { containerRef, visibleItems } = useStaggerReveal(TIMELINE_STEPS.length, 150)
  const detailReveal = useScrollReveal<HTMLDivElement>({ threshold: 0.05 })

  const currentStep = TIMELINE_STEPS[activeStep]

  return (
    <section id="day-in-the-life">
      <div ref={header.ref} className={`reveal ${header.isVisible ? 'visible' : ''}`}>
        <div className="section-label">Experience</div>
        <h2 className="section-title">A Day in the Life</h2>
        <p className="section-subtitle">
          Walk through a real game-day timeline and see how the server reshapes your
          experience — same app, six different UIs, zero deploys.
        </p>
      </div>

      <div className="ditl-scenario">
        <span className="ditl-scenario-text">
          It's game day — Lakers vs Celtics, tip-off at 7:30 PM
        </span>
      </div>

      <div className="ditl-container">
        <div className="ditl-timeline" ref={containerRef}>
          <div className="ditl-timeline-line" />
          {TIMELINE_STEPS.map((step, index) => (
            <button
              key={step.id}
              className={`ditl-step stagger-item ${visibleItems.has(index) ? 'visible' : ''} ${activeStep === index ? 'active' : ''}`}
              onClick={() => setActiveStep(index)}
            >
              <div className="ditl-step-dot" />
              <div className="ditl-step-time">{step.time}</div>
              <div className="ditl-step-label">{step.label}</div>
            </button>
          ))}
        </div>

        <div ref={detailReveal.ref} className={`ditl-detail reveal-scale ${detailReveal.isVisible ? 'visible' : ''}`}>
          <div className="ditl-detail-header">
            <span className="ditl-detail-time">{currentStep.time}</span>
            <span className="ditl-detail-label">{currentStep.label}</span>
          </div>

          <p className="ditl-narrative">{currentStep.narrative}</p>

          <div className="ditl-panels">
            <div className="ditl-panel ditl-panel-json">
              <div className="ditl-panel-label">Server Response</div>
              <pre className="ditl-json"><code>{currentStep.json}</code></pre>
            </div>

            <div className="ditl-panel ditl-panel-preview">
              <div className="ditl-panel-label">What the User Sees</div>
              <div className="ditl-preview-device">
                <div className="ditl-preview-notch" />
                <div className="ditl-preview-content">
                  {currentStep.preview.map((line, i) => (
                    <div key={i} className="ditl-preview-line">{line}</div>
                  ))}
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>
  )
}
