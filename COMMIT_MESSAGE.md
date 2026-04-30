SDUI: batch action execution, doc consistency remediation, design-system sync

Implement element-level batch action execution across all platforms (iOS,
Android, web). Run doc-consistency audit against working tree and remediate
19 inconsistencies — primarily stale token names and deleted file references
in sdui-design-system.md following the Kinetic design system rebuild.

--- Batch action execution (all platforms) ---

Element-level actions now filter by trigger and execute as a single ordered
batch with failure-policy semantics (halt/continue/silent). Previously only
the first matching action fired.

- iOS: `BatchActionExecutor.swift` (new) — EnvironmentKey-based batch
  executor; `ActionTapModifier` in `RenderingHelpers.swift` reads from
  environment and filters onActivate/onTap actions; `ScreenShell.swift`
  provides the executor and fixes `fireOnVisibleActions` to batch.
- Android: `ActionExecutor.kt` (new) — `LocalActionExecutor` CompositionLocal
  + `getActivateActions()` helper; `AtomicButton`, `AtomicImage`, `AtomicText`,
  `AtomicContainer` updated to use batch execution via CompositionLocal.
  `GameDetailScreen` wires the provider.
- Web: Already had batch semantics via `executeActions()` in ActionHandler;
  verified `ActionWrapper` filters by trigger correctly.
- Contract: Added "Element-Level Batch Action Execution" subsection to
  `docs/client-implementors-contract.md` §6 documenting filter→batch→execute
  pattern and Container activation.

--- Doc consistency audit + skill update ---

- `prompts/skills/doc-consistency-audit/SKILL.md`: Added Step 1b (working tree
  changes via `git diff --stat`) and updated When to Use section.
- `prompts/skills/doc-consistency-audit/references/consistency-checklist.md`:
  Added "Working Tree Propagation" section.

--- Design-system doc remediation (docs/sdui-design-system.md) ---

Synced to current Kinetic v1.0.0 token state:

- Related files table: removed rows for deleted files (size-tokens.json,
  typography-tokens.json, shadow-tokens.json); added LayoutTokens.java and
  IconTokens.java references; added "Planned" note for future registries.
- Layer diagram: `token:color.brand.nba` → `token:nba.label.accent.brand`.
- Layer 3 description: `token:color.*` → `token:nba.color.*` / `token:nba.label.*`.
- §2.1 Color tokens: rewrote from two-tier (palette + semantic aliases) to
  current multi-tier Kinetic structure (primitives, semantic, labels, UI/bg,
  buttons, team). Added t-black/t-white families. Updated semantic alias
  inventory to match color-tokens.json (nba.label.*, nba.bg.*, nba.button.*).
- §2.2 Spacing: corrected values to Kinetic (xs=2, sm=4, md=8, lg=16, xl=32,
  2xl=40 phone base). Added LayoutTokens.java reference. Fixed wire token
  names (nba.spacing.*).
- §2.3 Size tokens: marked "Planned" (file was deleted — awaiting design).
- §2.4 Typography tokens: marked "Planned" (file was deleted). Retained
  TextVariant enum reference.
- §2.5 Corner radius: corrected to flat values from Kinetic (xs=2, sm=4,
  md=8, lg=16, xl=24, 2xl=32, full=9999). Added nba.radius.xs and
  nba.radius.2xl rows. Added LayoutTokens.java reference.
- §2.6 Shadow tokens: marked "Planned" (file was deleted).
- §2.7 Icon tokens: fixed 8 discrepancies vs icon-tokens.json (basketball.fill
  not basketball; play.rectangle→PlayCircle/play_circle not smart_display;
  list.number not chart.bar; Widgets/widgets not grid_view;
  antenna.radiowaves.left.and.right not dot.radiowaves; person.circle/
  AccountCircle not person; PictureInPicture not picture_in_picture_alt;
  airplayvideo not tv). Added sdui:lock row. Split Material column into
  Android (PascalCase) and Web (snake_case). Added IconTokens.java reference.
- §4 Figma mapping: added Container activation row; fixed token references
  to nba.* prefix; removed shadow row (planned); removed size variables
  reference.
- §4 Figma naming: updated color style examples to nba.* namespace; removed
  size variables line; updated spacing to nba.spacing prefix.
- §9 Gaps: updated token coverage checklist to note registries were removed
  (not merely incomplete).

--- Technical Proposal remediation (docs/SDUI_Technical_Proposal_v2.md) ---

- Line 81: `token:color.*` → `token:nba.color.*` / `token:nba.label.*` in
  design system summary row.
- Line 324: Removed reference to deleted size-tokens.json; updated to list
  current registries (spacing-tokens, corner-radius-tokens).
- Line 326: `token:color.*` → `token:nba.color.*` / `token:nba.label.*`.

--- Verification ---

- `make codegen` passes (no schema changes this session).
- grep for stale `token:color.` / `token:spacing.` / `token:radius.`
  references returns zero hits in source code.
