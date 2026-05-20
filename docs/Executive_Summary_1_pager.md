 # Server-Driven UI

*Bringing the web release model to mobile and 10ft platforms*

## The Opportunity

Changing the rendering or layout of an app surface today requires a code change, PR review, QA cycle and app store release, taking days to weeks per change across platforms requiring submission. That release friction caps how fast we can respond to live-season moments, run experiments and optimize the journeys that drive engagement and conversion.

## The Proposal

Server-Driven UI applies the web model to native and connected platforms. The server determines screen composition and order, and clients render those instructions with native components in the NBA design system. This gives us tighter control over rollout timing and engagement and conversion strategy while improving integration with our data services and reducing dependency on full app-release cycles for rendering or layout changes.

SDUI is a proven pattern at comparable scale. Airbnb has publicly documented SDUI usage for rapid experimentation. Netflix uses server-driven composition to personalize by behavior, device and region. Shopify and Slack use similar patterns to iterate and target experiences by cohort.

## Impact

| Capability | Today | With SDUI |
|---|---|---|
| Change UI on a live surface | Days to weeks | Hours |
| Roll out urgent UI fixes | App release and update adoption required | Server-side update with minimal app-store dependency |
| Runtime personalization | Primarily client-side flags and conditional logic | Server assembles layout by segment, tier and context |
| Cross-platform consistency | Diverges across client platforms. UI logic accumulates in app binaries | One schema, aligned structure across platforms. Composition moves server-side |
| Optimize engagement and conversion journeys | New code per variant per platform | Server selects journey variant per user, no client change |
| Mix real-time and static content on one screen | Custom engineering per screen | Declarative per-section refresh policies |
| Testability | Variant and composition checks rely heavily on emulator/device runs | API and contract tests cover most composition logic server-side |

## Outcomes and Key Results

**Time to Market:** Reduce median time from request to production on SDUI surfaces, with urgent fixes shipping same-day rather than next-release.

**Stability:** Reduce platform parity defects caused by implementation drift across platforms.

**Platform Rework:** Eliminate duplicate implementation effort for new layouts, variants and functionality. One server change updates all platforms.

**Engagement Optimization:** Reduce time from experiment hypothesis to production decision from weeks to days.

**Test Efficiency:** Shift the majority of composition and variant testing from emulator/device automation to server-side API and contract tests, reducing QA cycle time and easing manual test load.

## Execution Overview

**Phase 1 - Foundation:** Stand up the cross-functional team across SDUI engineering, iOS, Android, connected platforms, design, DevOps and QA. Ship the rendering runtime and schema, instrument observability and launch one initial surface across the priority client platforms.

**Phase 2 - Expansion:** Roll out SDUI to additional high-impact surfaces, harden performance and establish the patterns, tooling and review process that make SDUI a repeatable, sustainable delivery model.

**Phase 3 - Scale:** SDUI becomes the default for new consumer-facing surfaces. Legacy surfaces migrate opportunistically as they're touched.