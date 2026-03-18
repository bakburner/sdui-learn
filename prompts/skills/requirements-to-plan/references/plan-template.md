# Implementation Plan Template

Use this structure for all `docs/plan-<topic>.md` files.

---

```markdown
# Plan: <Workstream Title>

> Source requirements: §<section numbers> from sdui-requirements-summary.md, ADR-0XX, etc.

## Summary

One-paragraph description of what this workstream delivers and why it matters.

## Current State

| Aspect | Status | Notes |
|--------|--------|-------|
| Schema support | Gap / Partial / Built | What exists today |
| Server support | Gap / Partial / Built | What exists today |
| Android support | Gap / Partial / Built | What exists today |
| Web support | Gap / Partial / Built | What exists today |
| Documentation | Gap / Partial / Built | What exists today |
| Tests | Gap / Partial / Built | What exists today |

## Requirements Addressed

- [ ] **REQ-1**: <requirement statement> (from §Xx)
- [ ] **REQ-2**: <requirement statement> (from ADR-0XX)
- ...

## Tasks

### Phase 1: Schema & Codegen
- [ ] <task description> — `schema/sdui-schema.json`
- [ ] Run codegen: `cd codegen && ./generate.sh`
- [ ] Verify generated outputs (Java, TypeScript, Swift)

### Phase 2: Server
- [ ] <task description> — `server/src/.../`
- [ ] Update composers / controllers as needed
- [ ] Add example payloads to `schema/examples/`

### Phase 3: Android
- [ ] <task description> — `android/sdui-core/src/.../`
- [ ] Update SectionRouter / AtomicRouter if needed
- [ ] Add renderer or update existing one

### Phase 4: Web
- [ ] <task description> — `web/src/`
- [ ] Update SectionRouter / AtomicRouter if needed
- [ ] Add component or update existing one

### Phase 5: Documentation & Tests
- [ ] Update `docs/sdui-requirements-summary.md` status matrix
- [ ] Update `AGENTS.md` if architecture changed
- [ ] Add/update ADR if decision was made
- [ ] Contract tests / unit tests / integration tests

## Dependencies

- <what must be done before this workstream can start>
- <cross-workstream dependencies>

## Open Questions

- [ ] <question that needs a decision before implementation>
- [ ] <question that needs a decision before implementation>
```
