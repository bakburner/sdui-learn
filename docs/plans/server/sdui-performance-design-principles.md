# SDUI Performance & Scalability Design Principles

> Derived from traffic analysis (May 2026 playoffs) and production architecture
> planning. These principles govern code changes and enhancements to the SDUI
> system. Every contributor — human or agentic — should validate proposed
> changes against these constraints before implementation.

## Why This Matters

The SDUI architecture targets 25-30K RPS at origin with a 30-35K ceiling.
The system achieves this not by brute-force scaling, but by **generating less
load** — SSE eliminates polling, long intervals reduce frequency, section
caching prevents recomposition, and CDN absorbs the head. Every code change
that undermines these mechanisms directly impacts origin load.

---

## Principle 1: Refresh intervals have direct RPS cost

**Rule:** Every refresh interval choice has a calculable RPS impact. Measure
before you ship.

**Formula:**
```
RPS impact = concurrent_users × screens_affected / interval_seconds
```

**Reference rates (mobile only, 112K concurrent users):**

| Interval | Impact per screen (mobile) | Impact all platforms (×2.5) |
|----------|---------------------------|----------------------------|
| 5s       | 22,400 RPS                | 56,000 RPS                 |
| 10s      | 11,200 RPS                | 28,000 RPS                 |
| 15s      | 7,467 RPS                 | 18,667 RPS                 |
| 30s      | 3,733 RPS                 | 9,333 RPS                  |
| 60s      | 1,867 RPS                 | 4,667 RPS                  |
| 5min     | 373 RPS                   | 933 RPS                    |

**Before setting or changing a `refreshPolicy` interval:**
1. Identify how many concurrent users will be on the affected screen
2. Calculate the RPS addition using the formula above
3. Confirm the total stays within the 25-30K origin target
4. Prefer SSE over polling for any data that changes more than once per minute

**Anti-patterns:**
- Setting 15s poll on a screen that doesn't show live-changing data
- Using poll for live scores (SSE exists for this)
- Adding a new polled section to a high-traffic screen without impact analysis

---

## Principle 2: SSE is load-bearing infrastructure — protect it

**Rule:** SSE (Ably) eliminates 46% of current traffic (~9,100 RPS). Any
change that degrades SSE reliability or forces poll fallback adds thousands
of RPS to origin.

**What this means for code changes:**
- Never add logic that disconnects or reconnects SSE unnecessarily
- Never change SSE channel subscription in a way that creates thundering-herd
  reconnection (e.g., disconnecting all clients to rebalance)
- The 30s poll fallback exists for resilience, not as an alternative design
- If modifying `dataBinding` or SSE channel configuration, validate that
  existing subscriptions aren't invalidated

**Load impact of SSE degradation:**
- 10% of clients fall back to poll: +930 RPS
- 50% of clients fall back: +4,650 RPS
- 100% (full SSE outage): +6,300 RPS (30s) or +9,100 RPS (15s)

---

## Principle 3: Section caching is a hard prerequisite — don't break cacheability

**Rule:** Section-level caching is what makes the 25-30K target achievable.
Without it, the system ceiling drops to 15-18K RPS. Never introduce patterns
that make sections uncacheable.

**Section cache key:** `section_type + input_data_hash + deviceClass + locale`

**What makes a section cacheable:**
- Same upstream data → same composed output (deterministic composition)
- No per-user data in shared sections (auth state, user preferences)
- No render-time randomness or timestamps in the section output
- Inputs are limited to upstream data + composition context (not request metadata)

**What breaks cacheability (never do these):**
- Injecting `RequestMetadata` (user token, deviceId, trace ID) into section composition
- Adding per-user content to shared sections (personalized text, user name)
- Using `System.currentTimeMillis()` or random values in composed output
- Making a section's output depend on fields not in the cache key

**If a section genuinely needs per-user data:**
- It must be in the authenticated tier (CAS/WECS modules)
- It cannot be CDN-cached (keyed on Authorization header)
- It should be separate from shared sections (don't mix auth/non-auth in one section)
- Document the RPS impact — per-user sections cannot collapse

---

## Principle 4: CDN key cardinality is finite — don't add dimensions carelessly

**Rule:** Every new dimension in `CompositionContext` multiplies the CDN cache
keyspace and reduces hit rate. The current blended hit rate is ~65%. Dropping
to 50% adds ~6,000 RPS to origin.

**Current keyspace dimensions:**
```
deviceClass(4) × capabilities(4) × locale(5) × gameState(5)
  × marketCohort(~220) × experiments(~32)
= ~30,000-50,000 keys per screen
```

**Before adding a new composition context field:**
1. Calculate the multiplication factor on existing keyspace
2. Estimate the CDN hit rate reduction
3. Quantify the origin RPS increase: `current_RPS × (1/new_hit_rate - 1/old_hit_rate)`
4. Consider: can this be resolved server-side without a cache key dimension?

**Safer alternatives to new cache dimensions:**
- Resolve at aggregation time (upstream data) instead of composition time
- Use `RequestMetadata` (not in cache key) for analytics/logging-only fields
- Make it a section-level concern (section cache is cheaper to fragment than CDN)

**The experiments dimension is the biggest wildcard.** Each active experiment
doubles the keyspace for affected screens. Limit active experiments per screen
and consider CDN hit rate impact when planning experiment coverage.

---

## Principle 5: Token forwarding is selective — don't expand auth scope

**Rule:** The system accepts `Authorization` header on all requests but only
forwards JWT to CAS/WECS modules. Anonymous modules (game, content, schedule,
media, promo) never receive user tokens.

**Why this matters:**
- Anonymous responses are CDN-cacheable and shareable across all users
- Per-user (token-forwarded) responses cannot be CDN-cached
- Per-user requests cannot be collapsed (each user's token = unique request)
- Expanding auth scope = linear RPS growth with user count (no caching benefit)

**Before adding token forwarding to a module:**
1. Confirm the upstream genuinely requires per-user identity (not just "logged in")
2. Check if the data can be fetched anonymously and gated client-side
3. Calculate the RPS impact: `concurrent_streamers × requests_per_minute`
4. Implement stale-if-error — auth state (subscriptions) changes slowly

**Current auth-required modules:** CAS (entitlements, concurrency, play-options),
WECS (bookmarks, stream selection, upsell). Everything else is anonymous.

---

## Principle 6: Request collapsing depends on cache key identity — keep keys narrow

**Rule:** SAF collapses concurrent requests to the same cache key into a single
upstream call. Wider keys = less collapsing = more upstream fan-out.

**Effective collapsing:**
- 190K users requesting game-detail for the same gameId → 1 upstream call per TTL
- 112K users on scoreboard → 1 upstream call per TTL (all see the same screen)

**Broken collapsing:**
- Adding user-specific params to shared-tier cache keys (each user = unique key)
- Using timestamps in cache keys (every second = unique key)
- Over-specializing aggregation responses per-client (phone vs tablet when content is identical)

**Before changing a `DataRequirement` cache key:**
1. Estimate how many concurrent users share the same key value
2. Higher sharing = better collapsing = less upstream fan-out
3. If the key must fragment (e.g., per-game), confirm the upstream can handle
   `unique_key_count / TTL_seconds` calls per second

---

## Principle 7: Composition must be pure and deterministic

**Rule:** Section composers are pure functions of `(CompositionContext + domain objects) → SectionEnvelope`. Any non-determinism breaks caching at every layer.

**Deterministic composition means:**
- Same inputs always produce byte-identical JSON output
- Field ordering in composed JSON is stable (Jackson property order configured)
- No randomized content selection, A/B logic at compose time (experiments resolve upstream)
- No wall-clock time in composed output (use server-pushed timestamps from upstream data)

**Why this matters beyond correctness:**
- CDN dedup relies on identical responses for identical URLs
- Section cache relies on input-hash → output being stable
- Non-determinism means every request is a cache miss

---

## Principle 8: Upstream latency is the critical UX constraint

**Rule:** Upstream services average 2s P50 latency. A cache miss = 2s client
wait time. Every design choice should minimize the frequency and impact of
cache misses.

**Defense layers (in order):**
1. CDN hit (0ms origin cost) — design for high hit rate
2. Section cache hit (~5ms) — design for input data sharing
3. Stale-while-revalidate (~5ms) — serve stale, refresh background
4. Full upstream call (~2s) — unavoidable on cold start only

**When adding a new upstream dependency:**
1. Define its expected latency (P50 and P99)
2. Set a SAF timeout at 2.5× P50
3. Mark it non-critical if the screen can render without it
4. Configure stale-if-error so degradation doesn't cascade to clients

---

## Principle 9: Two peak patterns — design for both

**Rule:** The system faces two fundamentally different load patterns that
stress different subsystems. Changes must be validated against both.

| | Busy Game Night (15 games) | Finals (1 game) |
|---|---|---|
| Stresses | Cache key cardinality, upstream fan-out, CDN fragmentation | Per-user auth concentration, SSE channel density, thundering herd |
| Origin RPS | Higher (~22K sustained) | Lower (~15-18K, CDN extreme-hit) |
| Cache behavior | 15 unique game keys fragment traffic | 1 key absorbs all shared traffic |

**When evaluating a change:**
- Does it increase cache key count? → Impacts busy game night
- Does it add per-user load on one game? → Impacts Finals
- Does it increase upstream fan-out? → Impacts busy game night
- Does it concentrate auth calls? → Impacts Finals

---

## Principle 10: Pod scaling is the last resort — reduce load first

**Rule:** Scaling from 18 to 45 pods is available but expensive. The
architecture's primary defense is load reduction, not capacity increase.

**Load reduction hierarchy (prefer higher over lower):**
1. **Eliminate the request** — SSE push instead of poll (saves 9,100 RPS)
2. **Reduce frequency** — 5min poll instead of 15s (saves 8,600 RPS)
3. **CDN absorb** — cacheable shared responses (eliminates 65% of remaining)
4. **Section cache hit** — skip recomposition on CDN miss (85-95% hit)
5. **Request collapsing** — one upstream call per key per TTL
6. **Stale-if-error** — serve cached on upstream failure (zero upstream cost)
7. **Scale pods** — last resort, linear cost increase

**If your change requires scaling pods to accommodate:**
- You've likely violated one of the principles above
- Go back and find where the load reduction broke
- Document why scaling is the only option (it rarely is)

---

## Quick Reference: Impact of Common Code Changes

| Change | Potential RPS Impact | Validate Before Merging |
|--------|---------------------|------------------------|
| New polled section on home screen | +373 to +11,200 RPS depending on interval | Interval choice, user count |
| New composition context field | +3,000-15,000 RPS (CDN hit rate drop) | Keyspace multiplication, CDN analytics |
| Token forwarding to new module | +500-3,000 RPS (uncollapsible per-user traffic) | Can it be anonymous instead? |
| New upstream dependency | +latency per miss, +fan-out per TTL | Timeout, criticality, stale-if-error |
| Changing section cache key | Potentially breaks 85%+ hit rate | Test with production key distribution |
| Adding experiment to high-traffic screen | ×2 CDN keys for that screen | CDN hit rate, experiment scope |
| Reducing poll interval (5min→60s) | +1,500 RPS per screen | Product requirement vs. load cost |
| New SSE channel subscription | Ably channel subscriber density | 190K subscriber ceiling per channel |

---

## Principle 11: Never put user preferences in shared cache keys

**Rule:** User-specific preferences (favorite team, personalized ordering,
VIP status) must never appear in cache keys for shared data. This was
CoreAPI's most expensive architectural mistake.

**The CoreAPI lesson:**

CoreAPI included `FavoriteTeamTri` (user's favorite team — 31 values) and
`Platform` (4 values) in the cache key of **every endpoint** via the base
query class. This created a 124× multiplier on cache entries for data that
was 95%+ identical across all users.

```
GameDetails for 1 live game:
  CoreAPI: 4(platform) × 31(team) × 5(region) × 2(vip) × 7(tabs) = 8,680 Redis keys
  SDUI:    1(game data in L1) × 4(section fragments per deviceClass) = 4 cache entries
  
Result: CoreAPI stores the same game data 8,680 times in Redis.
SDUI stores the upstream data once in L1, and at most 4 section fragments
(one per deviceClass layout when they differ). 190K users served from ≤5 entries.
```

**Why this kills Redis (write amplification + thundering herd):**

The problem isn't simple shard skew — Redis distributes full key strings
fairly uniformly. The real damage is threefold:

1. **Write amplification**: Every game score change forces the primer to
   rewrite 4,340 keys (4 × 31 × 5 × 7) simultaneously. That's 2+ GB of
   Redis writes every 30-60 seconds per live game.
2. **Synchronized TTL expiry**: All 4,340 keys were written at the same
   time, so they expire at the same instant. This creates a thundering
   herd: thousands of cache misses simultaneously bypass Redis and hit
   origin. (This was **observed in production** — large batches of keys
   expiring at identical intervals, followed by upstream latency spikes.)
3. **Popular-key read skew**: During Finals, one key (e.g., LAL + Android
   + US + boxscore) gets 50-100× more reads than unpopular combinations,
   creating modest but persistent hot spots on individual shards.

**The principle for SDUI:**
- `CompositionContext` fields determine what content is shown (cache key)
- `RequestMetadata` fields describe who is asking (not in cache key)
- User preferences (favorite team, VIP status, device ID) are **not**
  composition inputs for shared/game screens
- For screens that legitimately differ by preference (ForYou feed, team hub):
  prefer experiment cohort bucketing (3-5 variants) over raw preference
  cardinality (31 teams). Only use full cardinality when the content is
  genuinely unique per value AND the business case justifies the cache cost.
- Region and stats source should collapse to a single `marketCohort` dimension
  where the mapping is deterministic

**Before adding any field to a cache key, ask:**
1. Does the response content actually differ based on this field?
2. If yes, by how much? (If 95% identical, it shouldn't be a cache key)
3. What's the cardinality? (31 teams × 5 regions = 155× multiplier)
4. Can you bucket it? (31 teams → 5 conference/division cohorts, or 3 experiment arms)
5. Will specific values become hot keys? (Popular teams during playoffs)
6. Is this redundant with another dimension? (Region → StatsRegionId is deterministic)
7. Does this belong on the base class or only on specific endpoints?

---

## Validation Checklist for Code Reviews

Before approving any enhancement that touches composition, refresh policy,
caching, or upstream integration:

- [ ] What is the RPS impact at 112K concurrent users (Round 2 baseline)?
- [ ] What is the RPS impact at 190K concurrent users (Finals projection)?
- [ ] Does this change fragment CDN cache keys? By how much?
- [ ] Does this break section cache determinism?
- [ ] Does this add per-user (uncollapsible) traffic to a shared tier?
- [ ] Does this change a refresh interval? What's the delta?
- [ ] Does this add a new upstream dependency? What's its latency/timeout?
- [ ] Under the SSE-failure scenario (30s poll fallback), does this still work within the 35K ceiling?
- [ ] Under the Finals pattern (190K users, 1 game), does this create a thundering herd?
- [ ] Does this add user-specific data to a shared cache key? (hot shard risk)
- [ ] What's the cardinality multiplication of any new cache key dimension?

If any answer reveals a >1,000 RPS increase, it requires explicit
justification and sign-off on capacity impact.
