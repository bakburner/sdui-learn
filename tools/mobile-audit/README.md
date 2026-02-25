# Mobile Audit Tooling

This folder contains tooling for a human-in-the-loop mobile reverse-engineering audit that feeds SDUI planning and, later, constrained code generation.

## Purpose

- Keep audit automation separate from runtime prototype code.
- Produce repeatable findings from native mobile codebases.
- Require human review and approval before any downstream codegen input is considered valid.

## Scope

- Inputs:
  - Android native repo(s)
  - Apple native repo(s)
  - SDUI prototype repo for capability and gap mapping
- Outputs:
  - Markdown audit report
  - Structured findings artifact for review and approval workflow

## Guardrails

- No direct runtime code changes from raw model output.
- Code generation can only consume approved findings.
- Non-mobile targets remain excluded unless explicitly requested.

## Proposed Structure

- `extractors/` static analyzers for modules, style tokens, components, layouts, and interactions
- `synthesis/` gap analysis and prioritization logic
- `contracts/` finding schema and approval state definitions
- `reports/` generated report templates and formatters

## Next Step

Implement a minimal MVP that can parse source repos, emit evidence-linked findings, and export an approval-ready findings file.
