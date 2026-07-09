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
    narrative: 'The server composes an AtomicComposite section with a countdown timer, team stats, and injury report. The feed is built around anticipation.',
    json: `{
  "type": "AtomicComposite",
  "refreshPolicy": [{ "type": "poll", "intervalMs": 60000 }],
  "data": {
    "ui": { "type": "Container", "children": [...] },
    "content": { "countdown": "2h 30m", "status": "scheduled" }
  }
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
    narrative: 'The server re-composes — countdown becomes "Starting Soon" and a "Watch Live" Button with a navigate action is injected. No client update required.',
    json: `{
  "type": "AtomicComposite",
  "refreshPolicy": [{ "type": "poll", "intervalMs": 10000 }],
  "data": {
    "ui": {
      "type": "Container", "children": [
        { "type": "Button", "label": "Watch Live",
          "actions": [{ "trigger": "onActivate", "type": "navigate",
            "targetUri": "nba://watch/0022400123" }] }
      ]
    }
  }
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
    narrative: 'The game goes live. refreshPolicy switches to SSE for real-time score pushes. A LiveClock element ticks down the game clock. Data binding patches scores without re-fetching.',
    json: `{
  "type": "AtomicComposite",
  "refreshPolicy": [{ "type": "sse", "channel": "0022400123:linescore" }],
  "data": {
    "ui": {
      "type": "Container", "children": [
        { "type": "Text", "content": "0", "bindRef": "homeTeam.score" },
        { "type": "LiveClock", "bindRef": "clock", "tickDirection": "down" }
      ]
    },
    "content": { "homeTeam": { "score": 0 }, "awayTeam": { "score": 0 } }
  }
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
    narrative: 'Score freezes. The server re-composes the screen — injects halftime stats and a "Top Plays" ScrollContainer section. Zero code changes on the client.',
    json: `{
  "data": {
    "sections": [
      { "type": "AtomicComposite", "id": "scoreboard", "..." : "..." },
      { "type": "AtomicComposite", "id": "halftime-stats" },
      { "type": "AtomicComposite", "id": "top-plays-rail" }
    ]
  }
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
    narrative: 'Game ends. refreshPolicy reverts to static. The server reshapes the section — highlight cards and a "Watch Replay" navigate action are composed in.',
    json: `{
  "type": "AtomicComposite",
  "refreshPolicy": [{ "type": "static" }],
  "data": {
    "ui": {
      "type": "Container", "children": [
        { "type": "Text", "content": "FINAL", "variant": "labelSmall" },
        { "type": "Text", "content": "112", "bindRef": "homeTeam.score" },
        { "type": "Button", "label": "Watch Replay",
          "actions": [{ "trigger": "onActivate", "type": "navigate",
            "targetUri": "nba://video/recap/0022400123" }] }
      ]
    },
    "content": { "homeTeam": { "score": 112 }, "awayTeam": { "score": 105 } }
  }
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
    narrative: 'The feed completely reorganizes. A recap card, next game preview, and SubscribeUpsell promo are composed server-side. Same app, entirely different experience — no deploy.',
    json: `{
  "data": {
    "id": "for-you",
    "sections": [
      { "type": "AtomicComposite", "id": "game-recap" },
      { "type": "AtomicComposite", "id": "next-game-lal-mia" },
      { "type": "SubscribeUpsell", "id": "league-pass-promo" }
    ]
  }
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
