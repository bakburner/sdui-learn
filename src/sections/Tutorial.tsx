import { useState } from 'react'
import { useScrollReveal } from '../hooks/useScrollReveal'
import '../hooks/useScrollReveal.css'
import './Tutorial.css'

interface TutorialStep {
  id: string
  title: string
  description: string
  concept: string
  json: string
  explanation: string[]
}

const STEPS: TutorialStep[] = [
  {
    id: 'basic-screen',
    title: 'A Minimal Screen',
    description: 'Every SDUI response is a screen containing an array of sections. The client iterates through them and renders each one.',
    concept: 'Screen → Sections',
    json: `{
  "screenId": "home",
  "title": "Home",
  "sections": [
    {
      "sectionId": "header-1",
      "sectionType": "AtomicComposite",
      "content": {
        "type": "Container",
        "direction": "column",
        "children": [
          { "type": "Text", "text": "Welcome", "variant": "headlineLarge" }
        ]
      }
    }
  ]
}`,
    explanation: [
      'screenId identifies the screen for caching & analytics',
      'sections is an ordered array — the server controls order',
      'sectionType tells the client which renderer to use',
      'AtomicComposite means the server fully describes the layout',
    ],
  },
  {
    id: 'atomic-elements',
    title: 'Atomic Elements',
    description: 'Atomic elements are layout primitives the server composes freely. Container, Text, Image, Button — no client logic needed.',
    concept: 'Server-composed layout',
    json: `{
  "sectionType": "AtomicComposite",
  "content": {
    "type": "Container",
    "direction": "row",
    "gap": "md",
    "padding": "lg",
    "background": { "color": "#191C23" },
    "children": [
      {
        "type": "Image",
        "url": "https://cdn.nba.com/logos/lal.svg",
        "width": 48,
        "height": 48
      },
      {
        "type": "Container",
        "direction": "column",
        "children": [
          { "type": "Text", "text": "Lakers", "variant": "titleMedium" },
          { "type": "Text", "text": "108 PTS", "variant": "score" }
        ]
      }
    ]
  }
}`,
    explanation: [
      'Container handles layout: direction, gap, padding, alignment',
      'Atomic elements are composable — nest freely up to depth 6',
      'Server resolves spacing tokens (md, lg) — not raw pixels',
      'Zero client business logic — the server owns the composition',
    ],
  },
  {
    id: 'container-deep',
    title: 'Container Properties',
    description: 'Container is the workhorse element. It handles flex layout, sizing, backgrounds, borders, and responsive behavior.',
    concept: 'Layout control',
    json: `{
  "type": "Container",
  "direction": "row",
  "gap": "md",
  "padding": "lg",
  "mainAxisAlignment": "spaceBetween",
  "crossAxisAlignment": "center",
  "background": {
    "type": "gradient",
    "colors": ["#1D428A", "#000000"],
    "angle": 135
  },
  "cornerRadius": "xl",
  "widthMode": "fill",
  "heightMode": "hug",
  "shadows": [
    { "type": "outer", "color": "#000000", "radius": 12, "offsetY": 4 }
  ],
  "children": [...]
}`,
    explanation: [
      'direction: "row" | "column" — flex axis',
      'gap: spacing token between children (xs/sm/md/lg/xl/xxl)',
      'mainAxisAlignment / crossAxisAlignment — flex justify/align',
      'widthMode/heightMode: "hug" (fit content), "fill" (stretch), "fixed"',
      'background supports solid color or gradient with angle',
      'shadows is an array — supports outer and inner shadows',
    ],
  },
  {
    id: 'text-variants',
    title: 'Typography System',
    description: 'Text elements use a variant system that maps to the design system\'s type scale. Each platform renders with its native fonts.',
    concept: 'Cross-platform type',
    json: `{
  "type": "Container",
  "direction": "column",
  "gap": "sm",
  "children": [
    { "type": "Text", "text": "GAME DAY", "variant": "displayLarge" },
    { "type": "Text", "text": "Lakers vs Celtics", "variant": "headlineMedium" },
    { "type": "Text", "text": "Tonight at 7:30 PM ET", "variant": "titleMedium" },
    { "type": "Text", "text": "Watch on NBA TV", "variant": "bodyMedium", "color": "#8E9196" },
    { "type": "Text", "text": "LIVE", "variant": "labelSmall", "color": "#C8102E" }
  ]
}`,
    explanation: [
      'display* — largest, Knockout font, all-caps (hero moments)',
      'headline* — large, Knockout font, all-caps (section headers)',
      'title* — medium, Roboto 500 weight (card titles, names)',
      'body* — reading size, Roboto 400 (descriptions, captions)',
      'label* — small, uppercase, letter-spaced (tags, categories)',
      'score — special variant for game scores (Knockout font)',
      'color overrides the theme default for that text element',
    ],
  },
  {
    id: 'actions',
    title: 'Actions & Navigation',
    description: 'Buttons and tappable elements carry actions — navigate, deeplink, refresh, or trigger analytics. The client executes a small set of known action types.',
    concept: 'Declarative interactions',
    json: `{
  "type": "Button",
  "label": "View Game",
  "variant": "primary",
  "actions": [
    {
      "actionType": "navigate",
      "destination": "nba://game-detail/0022400123"
    },
    {
      "actionType": "analytics",
      "event": "game_card_tap",
      "properties": { "gameId": "0022400123" }
    }
  ]
}`,
    explanation: [
      'Actions are an array — multiple fire on a single tap',
      'navigate uses URI schemes the client resolves to screens',
      'analytics fires without interrupting navigation',
      'New action types degrade gracefully — unknown ones are skipped',
    ],
  },
  {
    id: 'action-types',
    title: 'Action Type Reference',
    description: 'The action system supports navigation, screen refresh, deeplinks, web URLs, analytics, and share. Each has specific fields.',
    concept: 'Action vocabulary',
    json: `{
  "actions": [
    {
      "actionType": "navigate",
      "destination": "nba://screen/for-you"
    },
    {
      "actionType": "refresh",
      "target": "screen"
    },
    {
      "actionType": "deeplink",
      "url": "https://www.nba.com/game/123",
      "fallback": "nba://game-detail/123"
    },
    {
      "actionType": "web",
      "url": "https://leaguepass.nba.com",
      "presentation": "modal"
    },
    {
      "actionType": "share",
      "title": "Check out this game!",
      "url": "https://nba.com/game/123"
    }
  ]
}`,
    explanation: [
      'navigate — internal screen navigation via URI scheme',
      'refresh — re-fetch screen or section data from server',
      'deeplink — platform URL with in-app fallback',
      'web — opens URL in browser or in-app modal/sheet',
      'share — triggers native share sheet with title + URL',
      'analytics — fire-and-forget event tracking (see previous step)',
    ],
  },
  {
    id: 'refresh',
    title: 'Real-time Updates',
    description: 'Sections declare their own refresh strategy. Static content stays put. Live data polls or streams via SSE. The server decides — not the client.',
    concept: 'Server-controlled freshness',
    json: `{
  "sectionId": "live-score",
  "sectionType": "AtomicComposite",
  "refreshPolicy": {
    "type": "sse",
    "channel": "game:0022400123",
    "event": "score_update"
  },
  "content": {
    "type": "Container",
    "direction": "row",
    "children": [
      { "type": "Text", "text": "LAL 108", "variant": "score" },
      { "type": "Text", "text": "BOS 112", "variant": "score" },
      {
        "type": "LiveClock",
        "snapshotSeconds": 423,
        "isRunning": true,
        "format": "mm:ss"
      }
    ]
  }
}`,
    explanation: [
      'refreshPolicy is per-section — different sections update differently',
      'type: "sse" streams real-time via server-sent events',
      'type: "poll" + intervalMs for periodic refresh',
      'type: "static" — never refreshes (default)',
      'LiveClock ticks client-side from a server snapshot — no round trips',
      'sectionEndpoint refreshes just one section without refetching the screen',
    ],
  },
  {
    id: 'semantic',
    title: 'Semantic Sections',
    description: 'When client-owned state is needed (sorting, tab selection, form input), use a semantic section type. Only 10 exist — everything else is AtomicComposite.',
    concept: 'Client state ownership',
    json: `{
  "sectionId": "boxscore-1",
  "sectionType": "BoxscoreTable",
  "data": {
    "teamName": "Lakers",
    "headers": ["Player", "MIN", "PTS", "REB", "AST"],
    "rows": [
      ["L. James", "38", "32", "8", "7"],
      ["A. Davis", "36", "28", "12", "3"]
    ]
  },
  "refreshPolicy": {
    "type": "poll",
    "intervalMs": 30000,
    "sectionEndpoint": "/v1/sdui/section/boxscore-1"
  }
}`,
    explanation: [
      'BoxscoreTable — client owns sort/scroll state',
      'TabGroup — client owns selected tab state',
      'Form — client owns input values and validation',
      'CalendarStrip — client owns selected date and scroll position',
      'VideoPlayer — client owns playback state via native player SDK',
      'Only use semantic when AtomicComposite can\'t handle the interaction',
    ],
  },
  {
    id: 'conditional',
    title: 'Conditional Rendering',
    description: 'The Conditional element lets the server define if/else branches evaluated against client-side screen state — dark mode, login status, feature flags.',
    concept: 'State-driven branching',
    json: `{
  "type": "Conditional",
  "condition": {
    "field": "user.isSubscribed",
    "operator": "equals",
    "value": true
  },
  "then": {
    "type": "Container",
    "children": [
      { "type": "Text", "text": "Welcome back!", "variant": "titleMedium" }
    ]
  },
  "else": {
    "type": "Container",
    "children": [
      { "type": "Text", "text": "Subscribe now", "variant": "titleMedium" },
      {
        "type": "Button",
        "label": "Get League Pass",
        "variant": "primary"
      }
    ]
  }
}`,
    explanation: [
      'condition evaluates against screen state (client-side)',
      'field uses dot-notation paths into the state object',
      'Operators: equals, notEquals, greaterThan, contains, exists',
      'then/else are full atomic element trees',
      'Server controls the branching logic — client just evaluates',
      'Use for: dark mode, subscription gates, feature flags, A/B tests',
    ],
  },
  {
    id: 'experiments',
    title: 'A/B Testing & Variants',
    description: 'The client sends its experiment assignments in the request. The server composes different screen layouts based on which variant the user is in.',
    concept: 'Server-side experiments',
    json: `// Client request envelope
{
  "screenId": "scoreboard",
  "schemaVersion": "1.0",
  "experiments": {
    "promo_placement": "variant_b",
    "section_order": "control"
  }
}

// Server response (variant B)
{
  "screenId": "scoreboard",
  "variant": "B",
  "sections": [
    { "sectionType": "AtomicComposite", "content": { "...": "promo at top" } },
    { "sectionType": "AtomicComposite", "content": { "...": "game cards" } }
  ]
}`,
    explanation: [
      'Client sends experiments map in the request envelope',
      'Server resolves variant at composition time — not client-side',
      'Response includes variant identifier for analytics attribution',
      'Experiment changes need zero client deploys',
      'Compose different section orders, content, or entire screens',
      'Graceful degradation: unknown experiments are ignored',
    ],
  },
]

export function Tutorial() {
  const [currentStep, setCurrentStep] = useState(0)
  const step = STEPS[currentStep]
  const header = useScrollReveal<HTMLDivElement>()
  const content = useScrollReveal<HTMLDivElement>({ threshold: 0.05 })

  return (
    <section id="tutorial">
      <div ref={header.ref} className={`reveal ${header.isVisible ? 'visible' : ''}`}>
        <div className="section-label">Step-by-Step</div>
        <h2 className="section-title">Build Your Understanding</h2>
        <p className="section-subtitle">
          Walk through the core concepts of SDUI, from basic screens to real-time updates and experimentation.
        </p>
      </div>

      <div ref={content.ref} className={`tutorial-container reveal ${content.isVisible ? 'visible' : ''}`}>
        <div className="tutorial-progress">
          {STEPS.map((s, i) => (
            <button
              key={s.id}
              className={`progress-step ${i === currentStep ? 'active' : ''} ${i < currentStep ? 'completed' : ''}`}
              onClick={() => setCurrentStep(i)}
            >
              <span className="progress-dot"></span>
              <span className="progress-label">{s.title}</span>
            </button>
          ))}
        </div>

        <div className="tutorial-content">
          <div className="tutorial-header">
            <div className="tutorial-concept">{step.concept}</div>
            <h3 className="tutorial-title">{step.title}</h3>
            <p className="tutorial-description">{step.description}</p>
          </div>

          <div className="tutorial-split">
            <div className="tutorial-code">
              <div className="code-header">
                <span className="code-dot red"></span>
                <span className="code-dot yellow"></span>
                <span className="code-dot green"></span>
                <span className="code-filename">response.json</span>
              </div>
              <pre className="code-body">
                <code>{step.json}</code>
              </pre>
            </div>

            <div className="tutorial-notes">
              <div className="notes-header">Key Points</div>
              <ul className="notes-list">
                {step.explanation.map((note, i) => (
                  <li key={i} className="note-item">
                    <span className="note-bullet">→</span>
                    <span>{note}</span>
                  </li>
                ))}
              </ul>
            </div>
          </div>

          <div className="tutorial-nav">
            <button
              className="tutorial-btn"
              disabled={currentStep === 0}
              onClick={() => setCurrentStep((s) => s - 1)}
            >
              ← Previous
            </button>
            <span className="tutorial-counter">
              {currentStep + 1} / {STEPS.length}
            </span>
            <button
              className="tutorial-btn"
              disabled={currentStep === STEPS.length - 1}
              onClick={() => setCurrentStep((s) => s + 1)}
            >
              Next →
            </button>
          </div>
        </div>
      </div>
    </section>
  )
}
