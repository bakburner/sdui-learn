import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import type { Action, Section } from '@sdui/models';
import { ActionTrigger, ActionType, MutateOperation } from '@sdui/models';
import type { ActionContext } from './ActionHandler';
import { executeActionSequence } from './ActionHandler';

function buildContext(initialState: Record<string, unknown> = {}): ActionContext {
  const state = { ...initialState };

  return {
    state,
    onStateChange: vi.fn((key: string, value: unknown) => {
      state[key] = value;
    }) as unknown as ActionContext['onStateChange'],
    onRefresh: vi.fn() as unknown as ActionContext['onRefresh'],
    onSectionUpdate: vi.fn((_sectionId: string, _section: Section) => {}) as unknown as ActionContext['onSectionUpdate'],
    onSectionStale: vi.fn() as unknown as ActionContext['onSectionStale'],
    onNavigate: vi.fn() as unknown as ActionContext['onNavigate'],
  };
}

function mutateAction(overrides: Partial<Action> = {}): Action {
  return {
    trigger: ActionTrigger.OnActivate,
    type: ActionType.Mutate,
    target: 'counter',
    ...overrides,
  } as Action;
}

function navigateAction(overrides: Partial<Action> = {}): Action {
  return {
    trigger: ActionTrigger.OnActivate,
    type: ActionType.Navigate,
    ...overrides,
  } as Action;
}

describe('ActionHandler', () => {
  let warnSpy: ReturnType<typeof vi.spyOn>;

  beforeEach(() => {
    warnSpy = vi.spyOn(console, 'warn').mockImplementation(() => {});
  });

  afterEach(() => {
    warnSpy.mockRestore();
  });

  it('applies mutate set using target/value', async () => {
    const context = buildContext();

    const result = await executeActionSequence([
      mutateAction({ target: 'selectedTab', value: 'overview' }),
    ], context);

    expect(result.halted).toBe(false);
    expect(context.state.selectedTab).toBe('overview');
    expect(context.onStateChange).toHaveBeenCalledWith('selectedTab', 'overview');
  });

  it('toggles an existing boolean', async () => {
    const context = buildContext({ expanded: true });

    await executeActionSequence([
      mutateAction({ target: 'expanded', operation: MutateOperation.Toggle }),
    ], context);

    expect(context.state.expanded).toBe(false);
    expect(warnSpy).not.toHaveBeenCalled();
  });

  it('no-ops and warns when toggle targets a non-boolean', async () => {
    const context = buildContext({ expanded: 'yes' });

    await executeActionSequence([
      mutateAction({ target: 'expanded', operation: MutateOperation.Toggle }),
    ], context);

    expect(context.state.expanded).toBe('yes');
    expect(context.onStateChange).not.toHaveBeenCalled();
    // mutate noop console.warn + sequence continue policy actionWarn
    expect(warnSpy).toHaveBeenCalledTimes(2);
  });

  it('increments a numeric value with default and explicit deltas', async () => {
    const context = buildContext({ counter: 2, ratio: 1.5 });

    await executeActionSequence([
      mutateAction({ target: 'counter', operation: MutateOperation.Increment }),
      mutateAction({ target: 'ratio', operation: MutateOperation.Increment, value: 2.25 }),
    ], context);

    expect(context.state.counter).toBe(3);
    expect(context.state.ratio).toBe(3.75);
    expect(warnSpy).not.toHaveBeenCalled();
  });

  it('no-ops and warns when increment targets a non-numeric value', async () => {
    const context = buildContext({ counter: 'two' });

    await executeActionSequence([
      mutateAction({ target: 'counter', operation: MutateOperation.Increment, value: 3 }),
    ], context);

    expect(context.state.counter).toBe('two');
    expect(context.onStateChange).not.toHaveBeenCalled();
    expect(warnSpy).toHaveBeenCalledTimes(2);
  });

  it('no-ops and warns when increment targets a missing value', async () => {
    const context = buildContext();

    await executeActionSequence([
      mutateAction({ target: 'counter', operation: MutateOperation.Increment }),
    ], context);

    expect(context.state.counter).toBeUndefined();
    expect(context.onStateChange).not.toHaveBeenCalled();
    expect(warnSpy).toHaveBeenCalledTimes(2);
  });

  it('appends to arrays', async () => {
    const context = buildContext({ filters: ['live'] });

    await executeActionSequence([
      mutateAction({ target: 'filters', operation: MutateOperation.Append, value: 'final' }),
    ], context);

    expect(context.state.filters).toEqual(['live', 'final']);
    expect(warnSpy).not.toHaveBeenCalled();
  });

  it('concatenates strings when append receives string + string', async () => {
    const context = buildContext({ query: 'NBA' });

    await executeActionSequence([
      mutateAction({ target: 'query', operation: MutateOperation.Append, value: ' Finals' }),
    ], context);

    expect(context.state.query).toBe('NBA Finals');
    expect(warnSpy).not.toHaveBeenCalled();
  });

  it('creates a singleton array when append targets a missing key', async () => {
    const context = buildContext();

    await executeActionSequence([
      mutateAction({ target: 'filters', operation: MutateOperation.Append, value: 'featured' }),
    ], context);

    expect(context.state.filters).toEqual(['featured']);
    expect(warnSpy).not.toHaveBeenCalled();
  });

  it('no-ops and warns when append receives incompatible types', async () => {
    const context = buildContext({ filters: 'featured' });

    await executeActionSequence([
      mutateAction({ target: 'filters', operation: MutateOperation.Append, value: 2 }),
    ], context);

    expect(context.state.filters).toBe('featured');
    expect(context.onStateChange).not.toHaveBeenCalled();
    expect(warnSpy).toHaveBeenCalledTimes(2);
  });

  it('navigates with webUrl only', async () => {
    const context = buildContext();

    await executeActionSequence([
      navigateAction({ webUrl: '/v1/sdui/screen/scoreboard' }),
    ], context);

    expect(context.onNavigate).toHaveBeenCalledWith('/v1/sdui/screen/scoreboard');
  });

  it('navigates with targetUri only', async () => {
    const context = buildContext();

    await executeActionSequence([
      navigateAction({ targetUri: 'nba://game/0042300102' }),
    ], context);

    expect(context.onNavigate).toHaveBeenCalledWith('nba://game/0042300102');
  });

  it('prefers targetUri for native deeplinks when both targetUri and webUrl are present', async () => {
    const context = buildContext();

    await executeActionSequence([
      navigateAction({
        targetUri: 'nba://boxscore/0042300102',
        webUrl: '/v1/sdui/screen/boxscore/0042300102',
      }),
    ], context);

    expect(context.onNavigate).toHaveBeenCalledWith('nba://boxscore/0042300102');
  });

  it('does not navigate when only legacy fallbackUrl is present', async () => {
    const context = buildContext();

    const result = await executeActionSequence([
      navigateAction({ fallbackUrl: 'nba://legacy-only' } as unknown as Partial<Action>),
    ], context);

    expect(result.halted).toBe(true);
    expect(context.onNavigate).not.toHaveBeenCalled();
  });

  it('halts the sequence when a navigate action fails', async () => {
    const context = buildContext({ counter: 1 });

    const result = await executeActionSequence([
      navigateAction(),
      mutateAction({ target: 'counter', value: 2 }),
    ], context);

    expect(result.halted).toBe(true);
    expect(context.onNavigate).not.toHaveBeenCalled();
    expect(context.state.counter).toBe(1);
    expect(context.onStateChange).not.toHaveBeenCalled();
  });
});