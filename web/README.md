# SDUI Web Client

React-based web client for the SDUI prototype.

## Quick Start

```bash
npm install
npm run dev
```

Open http://localhost:3000 in your browser.

## Development

The development setup runs two servers:

1. **Express** (port 3000) - Proxies `/api/*` to the SDUI server and all other requests to Vite
2. **Vite** (port 5173) - Serves the React app with hot module replacement

With `npm run dev`, both servers start via `concurrently`. You can access the app at either:
- http://localhost:3000 - Via Express proxy (recommended)
- http://localhost:5173 - Directly from Vite

### Scripts

| Script | Description |
|--------|-------------|
| `npm run dev` | Start both Express proxy and Vite dev server |
| `npm run dev:client` | Start Vite dev server only |
| `npm run dev:server` | Start Express proxy only |
| `npm run build` | Build for production |
| `npm run preview` | Preview production build |

## Architecture

```
web/src/
├── App.tsx                     # Main app, fetches SDUI screen
├── hooks/
│   ├── useSduiScreen.ts        # Fetch screen data from /api
│   └── useRefreshPolicy.ts     # Handle poll/SSE refresh per section
├── runtime/
│   ├── ActionHandler.ts        # Execute navigate/mutate/refresh actions
│   └── DataBindingApplier.ts   # Apply JSONPath bindings from updates
└── components/
    ├── SectionRouter.tsx       # Route sections to appropriate renderers
    ├── LiveSectionWrapper.tsx  # Wire refresh + data bindings to sections
    └── sections/               # Section-specific renderers (8 permanent)
        ├── AdSlot.tsx
        ├── BoxscoreTable.tsx
        ├── Form.tsx
        ├── SeasonLeadersTable.tsx
        ├── SubscribeBanner.tsx
        ├── SubscribeHero.tsx
        ├── TabGroup.tsx
        └── VideoPlayerStub.tsx
```

## Live Data Updates

Sections with `refreshPolicy` and/or `dataBindings` automatically receive live updates:

1. **SectionRouter** wraps eligible sections in `LiveSectionWrapper`
2. **LiveSectionWrapper** calls `useRefreshPolicy` to start polling
3. When data arrives, `DataBindingApplier` maps incoming payload to section data
4. The section re-renders with updated data

### Refresh Policy Types

| Type | Behavior |
|------|----------|
| `static` | No refresh (default) |
| `poll` | Re-fetch at `intervalMs` from `url` |
| `sse` | Real-time via Ably channel (logged, not yet wired) |

### Data Bindings

Data bindings map incoming poll/SSE payloads to section data using JSONPath-like syntax:

```json
{
  "dataBindings": {
    "bindings": [
      { "sourcePath": "$.homeTeam.score", "targetPath": "homeTeam.score" },
      { "sourcePath": "$.awayTeam.score", "targetPath": "awayTeam.score" }
    ]
  }
}
```

## TypeScript Models

TypeScript models are generated from the JSON Schema via the codegen pipeline:

```bash
cd ../codegen
./generate.sh
```

The web app imports models via the `@sdui/models` alias defined in [vite.config.ts](vite.config.ts).
