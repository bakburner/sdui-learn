import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import React from 'react';
import { LiveSectionWrapper } from '../../components/LiveSectionWrapper';
import type { Section } from '@sdui/models';
import { RefreshType } from '@sdui/models';

// --- Mocks ---
let visibilityState = true;
vi.mock('../../hooks/useSectionVisibility', () => ({
  useSectionVisibility: () => visibilityState,
}));

let appVisibilityState = true;
vi.mock('../../hooks/useAppVisibility', () => ({
  useAppVisibility: () => appVisibilityState,
}));

const mockUseRefreshPolicy = vi.fn();
vi.mock('../../hooks/useRefreshPolicy', () => ({
  useRefreshPolicy: (opts: unknown) => mockUseRefreshPolicy(opts),
  getEffectiveRefreshPolicy: (section: Section) => {
    const allPolicies = section.refreshPolicy ?? [];
    return {
      allPolicies,
      opaquePolicy: allPolicies.find((policy) => Boolean(policy.channel || policy.url)),
      sectionRefreshPolicy: allPolicies.find((policy) => Boolean(policy.sectionEndpoint)),
    };
  },
}));

vi.mock('../../runtime/DataBindingApplier', () => ({
  applyDataBindings: vi.fn((data: unknown) => data),
}));

vi.mock('../../components/SectionSkeleton', () => ({
  SectionSkeleton: () => React.createElement('div', { 'data-testid': 'skeleton' }, 'loading'),
}));

beforeEach(() => {
  visibilityState = true;
  appVisibilityState = true;
  mockUseRefreshPolicy.mockClear();
});

afterEach(() => {
  vi.restoreAllMocks();
});

function makeSection(overrides: Partial<Section> = {}): Section {
  return {
    id: 'section-1',
    type: 'AtomicComposite',
    data: { title: 'Hello' },
    refreshPolicy: [{ type: RefreshType.Poll, intervalMs: 5000, url: '/api/poll', pauseWhenOffScreen: true }],
    ...overrides,
  } as Section;
}

describe('LiveSectionWrapper — visibility gating', () => {
  it('passes enabled=true when near viewport and app visible', () => {
    visibilityState = true;
    appVisibilityState = true;

    const section = makeSection();

    render(
      <LiveSectionWrapper
        section={section}
        state={{}}
        onAction={() => {}}
        onStateChange={() => {}}
      >
        {(data) => <div data-testid="child">{JSON.stringify(data)}</div>}
      </LiveSectionWrapper>,
    );

    expect(mockUseRefreshPolicy).toHaveBeenCalledWith(
      expect.objectContaining({ enabled: true }),
    );
  });

  it('passes visibility flags when section is off-screen (pauseWhenOffScreen=true)', () => {
    visibilityState = false;
    appVisibilityState = true;

    const section = makeSection({
      refreshPolicy: [{ type: RefreshType.Poll, intervalMs: 5000, url: '/api/poll', pauseWhenOffScreen: true }],
    });

    render(
      <LiveSectionWrapper
        section={section}
        state={{}}
        onAction={() => {}}
        onStateChange={() => {}}
      >
        {(data) => <div>{JSON.stringify(data)}</div>}
      </LiveSectionWrapper>,
    );

    expect(mockUseRefreshPolicy).toHaveBeenCalledWith(
      expect.objectContaining({ enabled: true, isNearViewport: false, isAppVisible: true }),
    );
  });

  it('passes enabled=true when off-screen but pauseWhenOffScreen=false', () => {
    visibilityState = false;
    appVisibilityState = true;

    const section = makeSection({
      refreshPolicy: [{ type: RefreshType.SSE, channel: 'game:1', pauseWhenOffScreen: false }],
    });

    render(
      <LiveSectionWrapper
        section={section}
        state={{}}
        onAction={() => {}}
        onStateChange={() => {}}
      >
        {(data) => <div>{JSON.stringify(data)}</div>}
      </LiveSectionWrapper>,
    );

    expect(mockUseRefreshPolicy).toHaveBeenCalledWith(
      expect.objectContaining({ enabled: true }),
    );
  });

  it('passes app visibility when app is backgrounded', () => {
    visibilityState = true;
    appVisibilityState = false;

    const section = makeSection();

    render(
      <LiveSectionWrapper
        section={section}
        state={{}}
        onAction={() => {}}
        onStateChange={() => {}}
      >
        {(data) => <div>{JSON.stringify(data)}</div>}
      </LiveSectionWrapper>,
    );

    expect(mockUseRefreshPolicy).toHaveBeenCalledWith(
      expect.objectContaining({ enabled: true, isNearViewport: true, isAppVisible: false }),
    );
  });

  it('passes app visibility when backgrounded with pauseWhenOffScreen=false', () => {
    visibilityState = true;
    appVisibilityState = false;

    const section = makeSection({
      refreshPolicy: [{ type: RefreshType.SSE, channel: 'game:1', pauseWhenOffScreen: false }],
    });

    render(
      <LiveSectionWrapper
        section={section}
        state={{}}
        onAction={() => {}}
        onStateChange={() => {}}
      >
        {(data) => <div>{JSON.stringify(data)}</div>}
      </LiveSectionWrapper>,
    );

    expect(mockUseRefreshPolicy).toHaveBeenCalledWith(
      expect.objectContaining({ enabled: true, isNearViewport: true, isAppVisible: false }),
    );
  });

  it('defaults pauseWhenOffScreen to true when not specified in policy element', () => {
    visibilityState = false;
    appVisibilityState = true;

    const section = makeSection({
      refreshPolicy: [{ type: RefreshType.Poll, intervalMs: 5000, url: '/api/poll' }],
    });

    render(
      <LiveSectionWrapper
        section={section}
        state={{}}
        onAction={() => {}}
        onStateChange={() => {}}
      >
        {(data) => <div>{JSON.stringify(data)}</div>}
      </LiveSectionWrapper>,
    );

    expect(mockUseRefreshPolicy).toHaveBeenCalledWith(
      expect.objectContaining({ enabled: true, isNearViewport: false, isAppVisible: true }),
    );
  });

  it('renders children with section data', () => {
    const section = makeSection({ data: { title: 'Game Score' } });

    render(
      <LiveSectionWrapper
        section={section}
        state={{}}
        onAction={() => {}}
        onStateChange={() => {}}
      >
        {(data) => <div data-testid="child">{(data as Record<string, string>)?.title}</div>}
      </LiveSectionWrapper>,
    );

    expect(screen.getByTestId('child')).toHaveTextContent('Game Score');
  });
});
