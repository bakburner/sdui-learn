import { useState } from 'react'
import { useScrollReveal, useStaggerReveal } from '../hooks/useScrollReveal'
import '../hooks/useScrollReveal.css'
import './UseCases.css'

interface UseCase {
  id: string
  name: string
  description: string
  before: { effort: string; timeline: string }
  after: { effort: string; timeline: string }
  json: string
}

const USE_CASES: UseCase[] = [
  {
    id: 'game-day',
    name: 'Game Day',
    description: 'Lakers vs. Celtics tonight — the home feed needs a live scoreboard hero, countdown timer, and streaming links pushed to the top.',
    before: {
      effort: 'Client feature flag + scheduled deploy',
      timeline: '2-3 day lead time, QA across platforms',
    },
    after: {
      effort: 'Server composition updated via CMS',
      timeline: 'Minutes before tip-off, instant for all users',
    },
    json: `{
  "sections": [
    {
      "sectionType": "LiveScoreboard",
      "priority": 1,
      "content": {
        "gameId": "LAL-BOS-20260108",
        "variant": "hero"
      },
      "refreshPolicy": { "type": "sse" }
    }
  ]
}`,
  },
  {
    id: 'off-season',
    name: 'Off-Season Content',
    description: 'No games for months — replace live sections with draft coverage, free agency tracker, and historical highlights.',
    before: {
      effort: 'Rebuild home screen layout in client code',
      timeline: '1-2 sprint cycles, app store release',
    },
    after: {
      effort: 'Swap section composition on the server',
      timeline: 'Same day as season ends, zero deploys',
    },
    json: `{
  "sections": [
    {
      "sectionType": "AtomicComposite",
      "content": {
        "variant": "editorial-rail",
        "title": "2026 NBA Draft"
      }
    },
    {
      "sectionType": "FreeAgencyTracker",
      "priority": 2
    }
  ]
}`,
  },
  {
    id: 'ab-test',
    name: 'A/B Test',
    description: 'Test whether showing player stats above game recaps drives more engagement than the current order.',
    before: {
      effort: 'Branch client code, implement both variants',
      timeline: '1-2 weeks dev + QA per variant',
    },
    after: {
      effort: 'Server returns different section order per cohort',
      timeline: 'Configure & launch in minutes',
    },
    json: `{
  "experiment": "stats-above-recaps",
  "cohort": "B",
  "sections": [
    { "sectionType": "PlayerStats", "priority": 1 },
    { "sectionType": "GameRecaps", "priority": 2 },
    { "sectionType": "TopStories", "priority": 3 }
  ]
}`,
  },
  {
    id: 'regional-promo',
    name: 'Regional Promo',
    description: 'Promote a Bay Area ticket sale only to users in Northern California — different hero, CTA, and pricing.',
    before: {
      effort: 'Geo-targeted feature flag + native implementation',
      timeline: '1 week minimum, per-platform testing',
    },
    after: {
      effort: 'Server checks geo, returns promo section',
      timeline: 'Hours to configure, live immediately',
    },
    json: `{
  "targeting": { "region": "US-CA-NOR" },
  "sections": [
    {
      "sectionType": "AtomicComposite",
      "content": {
        "variant": "promo-hero",
        "title": "Warriors Tickets",
        "cta": "From $49"
      }
    }
  ]
}`,
  },
  {
    id: 'breaking-news',
    name: 'Breaking News',
    description: 'A blockbuster trade just happened — immediately surface a breaking news banner and trade details above the fold.',
    before: {
      effort: 'Push notification + wait for users to open app',
      timeline: 'No layout control until next release',
    },
    after: {
      effort: 'Insert breaking-news section at priority 0',
      timeline: 'Seconds after confirmation, all users see it',
    },
    json: `{
  "sections": [
    {
      "sectionType": "BreakingNews",
      "priority": 0,
      "content": {
        "headline": "TRADE ALERT",
        "body": "Kevin Durant traded to...",
        "style": "urgent"
      },
      "refreshPolicy": { "type": "sse" }
    }
  ]
}`,
  },
]

interface TimelinePoint {
  id: string
  label: string
  note: string
}

const TIMELINE_POINTS: TimelinePoint[] = [
  {
    id: 'preseason',
    label: 'Pre-Season',
    note: 'Added "New Season Preview" hero section, preseason schedule rail',
  },
  {
    id: 'opening',
    label: 'Opening Night',
    note: 'Live scoreboard promoted to priority 0, countdown timer injected',
  },
  {
    id: 'allstar',
    label: 'All-Star Break',
    note: 'Voting widget section added, fan engagement quiz inserted mid-feed',
  },
  {
    id: 'trade',
    label: 'Trade Deadline',
    note: 'Breaking news section enabled, trade tracker given top priority',
  },
  {
    id: 'playoffs',
    label: 'Playoffs',
    note: 'Bracket section added, series-specific content, elimination alerts',
  },
  {
    id: 'finals',
    label: 'Finals',
    note: 'Finals-branded hero, trophy tracker, champion celebration overlay',
  },
]

export function UseCases() {
  const [activeCard, setActiveCard] = useState<string | null>(null)
  const header = useScrollReveal<HTMLDivElement>()
  const { containerRef, visibleItems } = useStaggerReveal(USE_CASES.length, 120)
  const timelineReveal = useScrollReveal<HTMLDivElement>({ threshold: 0.1 })
  const { containerRef: timelineContainer, visibleItems: timelineVisible } = useStaggerReveal(TIMELINE_POINTS.length, 150)

  return (
    <section id="use-cases">
      <div ref={header.ref} className={`reveal ${header.isVisible ? 'visible' : ''}`}>
        <div className="section-label">For Product &amp; Design</div>
        <h2 className="section-title">Use Cases</h2>
        <p className="section-subtitle">
          Real NBA scenarios where server-driven UI turns weeks of cross-platform work into instant, targeted updates — no client releases required.
        </p>
      </div>

      <div className="uc-gallery" ref={containerRef}>
        {USE_CASES.map((uc, index) => (
          <div
            key={uc.id}
            className={`uc-card stagger-item ${visibleItems.has(index) ? 'visible' : ''} ${activeCard === uc.id ? 'uc-card-expanded' : ''}`}
            onClick={() => setActiveCard(activeCard === uc.id ? null : uc.id)}
          >
            <div className="uc-card-top">
              <h3 className="uc-card-name">{uc.name}</h3>
              <span className="uc-card-toggle">{activeCard === uc.id ? '−' : '+'}</span>
            </div>
            <p className="uc-card-desc">{uc.description}</p>

            <div className="uc-comparison">
              <div className="uc-state uc-state-before">
                <span className="uc-state-badge">Traditional</span>
                <span className="uc-state-effort">{uc.before.effort}</span>
                <span className="uc-state-timeline">{uc.before.timeline}</span>
              </div>
              <div className="uc-state uc-state-after">
                <span className="uc-state-badge">SDUI</span>
                <span className="uc-state-effort">{uc.after.effort}</span>
                <span className="uc-state-timeline">{uc.after.timeline}</span>
              </div>
            </div>

            {activeCard === uc.id && (
              <div className="uc-json-panel">
                <div className="uc-json-header">
                  <span className="uc-json-label">Server Response</span>
                  <span className="uc-json-endpoint">GET /home-feed</span>
                </div>
                <pre className="uc-json-code"><code>{uc.json}</code></pre>
              </div>
            )}
          </div>
        ))}
      </div>

      {/* Timeline Section */}
      <div ref={timelineReveal.ref} className={`uc-timeline-section reveal ${timelineReveal.isVisible ? 'visible' : ''}`}>
        <div className="uc-timeline-header">
          <h3 className="uc-timeline-title">One Endpoint, Entire Season</h3>
          <p className="uc-timeline-subtitle">
            How <code className="uc-inline-code">/home-feed</code> evolves across an NBA season — zero client deploys.
          </p>
        </div>

        <div className="uc-timeline" ref={timelineContainer}>
          <div className="uc-timeline-track" />
          {TIMELINE_POINTS.map((point, index) => (
            <div
              key={point.id}
              className={`uc-timeline-point stagger-item ${timelineVisible.has(index) ? 'visible' : ''}`}
            >
              <div className="uc-timeline-dot" />
              <div className="uc-timeline-content">
                <span className="uc-timeline-label">{point.label}</span>
                <span className="uc-timeline-note">{point.note}</span>
              </div>
            </div>
          ))}
        </div>

        <div className="uc-timeline-footer">
          <span className="uc-timeline-stat">6 major layout changes</span>
          <span className="uc-timeline-divider-dot" />
          <span className="uc-timeline-stat">0 app store submissions</span>
          <span className="uc-timeline-divider-dot" />
          <span className="uc-timeline-stat">100% user coverage, instantly</span>
        </div>
      </div>
    </section>
  )
}
