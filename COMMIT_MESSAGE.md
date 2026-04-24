docs: GamePanel → AtomicComposite migration + comment sweep

Complete the GamePanel composite migration (plan Phase D):

Schema & counts:
- 9 section types (8 permanent + AtomicComposite), down from 10
- 11 atomic element types (added LiveClock), up from 10
- 10 migrated section types (added GamePanel), up from 9

Doc updates:
- README.md: counts, tables, design philosophy, recent changes
- SDUI_Executive_Summary_v2.md: renderer counts, atomic layer bullets
- SDUI_Technical_Proposal_v2.md: decision examples, §2b framework,
  router/renderer code samples, platform coverage table, JSON examples,
  revision history entry
- backend-architect.agent.md: section layer, atomic count
- consistency-checklist.md: counts, permanent/migrated/pruned lists

Source-comment sweep (AGENTS.md §10.3 compliance):
- Stripped Rule N / AGENTS.md / §N citations from 20 non-generated
  source files across server (Java), Android (Kotlin), iOS (Swift),
  and web (TypeScript). Replaced with behavioral descriptions.
- Generated model files (SduiModels.kt/.swift/.ts) left untouched.

Server:
- DemoScreenComposer: "GamePanel" UI labels → "Game Card",
  Javadoc comments updated, AGENTS.md citation removed

Migration plan:
- plan-gamepanel-composite-migration.md: Status → Implemented
