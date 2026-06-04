import React, { useState, useCallback, useEffect, useRef, useMemo } from 'react';
import type { Action, Section } from '@sdui/models';
import { useSduiScreen } from './hooks/useSduiScreen';
import { SectionRouter } from './components/SectionRouter';
import { TopNavigationBar } from './components/TopNavigationBar';
import { executeActionSequence } from './runtime/ActionHandler';
import { setColorSchemePreference, usePrefersColorScheme } from './utils/ColorTokenResolver';
import { ToastHost } from './components/ToastHost';
import { RequestEnvelopeBuilder } from './request/RequestEnvelopeBuilder';
import { resolveLayoutScalar, resolveSpacingPx } from './utils/LayoutTokenResolver';
import type { Spacing } from '@sdui/models';
import { WireAssetBaseUrlProvider } from './context/WireAssetBaseUrlContext';

// Degraded-connectivity fallback only — primary bootstrap URI comes from /v1/sdui/screen/init.
const FALLBACK_BOOTSTRAP_URI = 'nba://for-you';

/**
 * Convert an nba:// URI to a server endpoint path.
 *
 * Pure prefix swap — no special-casing of individual screens. The server
 * owns all routing semantics.
 *
 *   nba://scoreboard        → /v1/sdui/screen/scoreboard
 *   nba://game/0042300102   → /v1/sdui/screen/game/0042300102
 *   nba://boxscore/00423... → /v1/sdui/screen/boxscore/0042300102
 *   nba://demos             → /v1/sdui/screen/demos
 *   nba://anything/else     → /v1/sdui/screen/anything/else
 */
function resolveEndpoint(uri: string): string {
  const path = uri.replace(/^nba:\/\//, '');
  return `/v1/sdui/screen/${path}`;
}

export function App(): React.ReactElement {
  const [currentUri, setCurrentUri] = useState<string | null>(null);
  const [bootstrapUri, setBootstrapUri] = useState<string | null>(null);
  const colorScheme = usePrefersColorScheme();

  // Fetch bootstrap URI from the server on mount; fall back if unavailable.
  useEffect(() => {
    let cancelled = false;
    const qs = new RequestEnvelopeBuilder().buildQueryString();
    fetch(`/api/v1/sdui/screen/init?${qs}`)
      .then(r => r.ok ? r.json() : Promise.reject(new Error(`init: ${r.status}`)))
      .then((data: { bootstrapUri?: string }) => {
        if (cancelled) return;
        const uri = data.bootstrapUri ?? FALLBACK_BOOTSTRAP_URI;
        // Validate URI matches expected nba:// scheme to prevent SSRF
        if (!/^nba:\/\/[a-zA-Z0-9\-_/]+$/.test(uri)) {
          console.warn('Invalid bootstrap URI from server, using fallback');
          setBootstrapUri(FALLBACK_BOOTSTRAP_URI);
          setCurrentUri(FALLBACK_BOOTSTRAP_URI);
          return;
        }
        setBootstrapUri(uri);
        setCurrentUri(uri);
      })
      .catch(() => {
        if (!cancelled) {
          setBootstrapUri(FALLBACK_BOOTSTRAP_URI);
          setCurrentUri(FALLBACK_BOOTSTRAP_URI);
        }
      });
    return () => { cancelled = true; };
  }, []);

  // Variant selection (A/B/C/...) is server-composed as an `AtomicComposite`
  // chip section emitted alongside the rest of `screen.sections`. Each chip's
  // navigate action re-fetches the same screen URI with the chosen variant in
  // its `experiments[<id>]` query param, so there is no client variant state.

  const endpoint = currentUri ? resolveEndpoint(currentUri) : null;
  const {
    screen,
    shellScreen,
    loading,
    error,
    upgradeRequired,
    correlationId,
    refetch,
    setScreen,
    replaceCurrentScreen,
    onSectionReplace,
    onSectionGone,
  } = useSduiScreen({
    endpoint,
  });

  // Screen-level state for TabGroup and other stateful sections
  const [screenState, setScreenState] = useState<Record<string, unknown>>({});
  // Mirror of `screenState` updated synchronously inside `handleStateChange`
  // so an action dispatched in the same event tick as the state write sees
  // the just-written value. Without this, `handleAction` would close over
  // the previous render's `screenState` snapshot and resolve placeholders
  // (e.g. `{{games_selected_date}}` in CalendarStrip's `onDateSelected`)
  // against the stale map — `paramBindings` would substitute to the empty
  // string, `handleRefresh` would drop the empty value, and the server
  // would be re-fetched without the user's selection.
  const screenStateRef = useRef<Record<string, unknown>>({});

  // Track sections that failed to refresh (stale indicator)
  const [staleSections, setStaleSections] = useState<Set<string>>(new Set());

  // Track whether the last screen mutation was a surgical section update
  // so we can skip re-seeding state from the (stale) screen.state.
  const isSectionUpdateRef = useRef(false);
  const lastScreenIdRef = useRef<string | undefined>(undefined);

  // Initialize or merge screen state from the server. Full replacement when
  // the screen id changes (navigation); merge when the same screen is updated.
  useEffect(() => {
    if (isSectionUpdateRef.current) {
      isSectionUpdateRef.current = false;
      return;
    }
    if (!screen) {
      return;
    }
    if (lastScreenIdRef.current !== screen.id) {
      lastScreenIdRef.current = screen.id;
      if (screen.state) {
        const next = { ...screen.state };
        screenStateRef.current = next;
        setScreenState(next);
      } else {
        screenStateRef.current = {};
        setScreenState({});
      }
      return;
    }
    if (screen.state) {
      screenStateRef.current = { ...screenStateRef.current, ...screen.state };
      setScreenState((prev) => ({ ...prev, ...screen.state }));
    }
  }, [screen]);

  const handleStateChange = useCallback((key: string, value: unknown) => {
    // Update the ref synchronously so an action dispatched on the next line
    // of the same event handler resolves placeholders against the new value.
    screenStateRef.current = { ...screenStateRef.current, [key]: value };
    setScreenState((prev) => ({ ...prev, [key]: value }));
  }, []);

  const handleRefresh = useCallback((_sectionId?: string) => {
    console.log('[App] Refresh requested:', _sectionId || 'full screen');
    refetch();
  }, [refetch]);

  // Surgical section replacement — merges a single updated section into the
  // current screen without touching any other section's state or data.
  const handleSectionUpdate = useCallback((sectionId: string, updatedSection: Section) => {
    isSectionUpdateRef.current = true;
    setScreen((prev) => {
      if (!prev) return prev;
      const idx = prev.sections.findIndex((s) => s.id === sectionId);
      if (idx === -1) {
        // Section not found — append it (server may have added a new section)
        console.log('[App] Appending new section:', sectionId);
        return { ...prev, sections: [...prev.sections, updatedSection] };
      }
      console.log('[App] Replacing section:', sectionId, 'at index', idx);
      const next = [...prev.sections];
      next[idx] = updatedSection;
      return { ...prev, sections: next };
    });
  }, []);

  const handleUriNavigate = useCallback((uri: string) => {
    setCurrentUri(uri);
  }, []);

  const chromeSpacing = useMemo(
    () => ({
      sm: resolveLayoutScalar('token:nba.spacing.sm'),
      md: resolveLayoutScalar('token:nba.spacing.md'),
      lg: resolveLayoutScalar('token:nba.spacing.lg'),
      xl: resolveLayoutScalar('token:nba.spacing.xl'),
    }),
    [],
  );

  const handleNavigateBack = useCallback(() => {
    const parent = shellScreen?.parentUri;
    if (parent) {
      setCurrentUri(parent);
    } else if (bootstrapUri) {
      setCurrentUri(bootstrapUri);
    }
  }, [shellScreen?.parentUri, bootstrapUri]);

  const handleSectionStale = useCallback((sectionId: string) => {
    setStaleSections((prev) => new Set(prev).add(sectionId));
  }, []);

  const handleStalenessChange = useCallback((sectionId: string, isStale: boolean) => {
    setStaleSections((prev) => {
      const next = new Set(prev);
      if (isStale) {
        next.add(sectionId);
      } else {
        next.delete(sectionId);
      }
      return next;
    });
  }, []);

  const handleAction = useCallback((actionOrActions: Action | Action[]) => {
    const actions = Array.isArray(actionOrActions) ? actionOrActions : [actionOrActions];
    const context = {
      // Read from the ref, not the closed-over `screenState`, so a state
      // mutation earlier in the same event handler is visible to placeholder
      // resolution in this dispatch.
      state: screenStateRef.current,
      onStateChange: handleStateChange,
      onRefresh: handleRefresh,
      replaceCurrentScreen,
      onSectionUpdate: handleSectionUpdate,
      onSectionStale: handleSectionStale,
      onNavigate: handleUriNavigate,
    };
    executeActionSequence(actions, context);
  }, [handleStateChange, handleRefresh, replaceCurrentScreen, handleSectionUpdate, handleUriNavigate, handleSectionStale]);

  const handleThemeToggle = useCallback(() => {
    setColorSchemePreference(colorScheme === 'dark' ? 'light' : 'dark');
  }, [colorScheme]);

  const navShell = shellScreen ?? screen;
  const parentUri = shellScreen?.parentUri;
  const showFeedError = Boolean(error || upgradeRequired);
  const feedInsets = resolveSpacingPx(
    (screen?.contentInsets ?? shellScreen?.contentInsets) as Spacing | undefined,
  );

  if (!currentUri && loading && !navShell) {
    return (
      <div style={{ ...styles.centered, padding: chromeSpacing.xl }}>
        <div style={styles.spinner} />
        <p style={styles.loadingText}>Loading...</p>
      </div>
    );
  }

  const wireAssetBaseUrl =
    typeof window !== 'undefined' ? `${window.location.origin}` : '';

  return (
    <WireAssetBaseUrlProvider baseUrl={wireAssetBaseUrl}>
    <div style={styles.container}>
      <ToastHost />
      {loading && (
        <div
          style={styles.navLoadingOverlay}
          role="status"
          aria-live="polite"
          aria-busy="true"
        >
          <div style={styles.spinner} />
        </div>
      )}
      {error && screen && (
        <div style={styles.navErrorBanner}>
          <span style={styles.navErrorText}>{error}</span>
          <button type="button" style={styles.navErrorRetry} onClick={() => { void refetch(); }}>
            Retry
          </button>
        </div>
      )}
      {/* App bar title/back are server-composed (:app-bar AtomicComposite). */}
      <button
        type="button"
        style={styles.themeFloating}
        onClick={handleThemeToggle}
        aria-label={`Switch to ${colorScheme === 'dark' ? 'light' : 'dark'} mode`}
        title={`Switch to ${colorScheme === 'dark' ? 'light' : 'dark'} mode`}
      >
        {colorScheme === 'dark' ? 'Light' : 'Dark'}
      </button>

      {navShell?.navigation && (
        <TopNavigationBar
          navigation={navShell.navigation}
          onNavigate={handleUriNavigate}
        />
      )}

      <main
        style={{
          ...styles.main,
          paddingTop: feedInsets.top,
          paddingRight: feedInsets.right,
          paddingBottom: feedInsets.bottom,
          paddingLeft: feedInsets.left,
          ...(loading ? styles.mainWhileLoading : {}),
        }}
      >
        {loading && !screen && (
          <div style={styles.centeredInline}>
            <div style={styles.spinner} />
            <p style={styles.loadingText}>Loading...</p>
          </div>
        )}
        {showFeedError && (
          <div
            style={{
              ...styles.centeredInline,
              padding: chromeSpacing.xl,
              gap: chromeSpacing.sm,
            }}
          >
            {upgradeRequired ? (
              <>
                <p style={styles.errorText}>Update Required</p>
                <p style={styles.hint}>
                  This version of the app is no longer supported. Please reload to get the latest
                  version.
                </p>
                <div
                  style={{
                    ...styles.errorActions,
                    gap: chromeSpacing.md,
                    marginTop: chromeSpacing.sm,
                  }}
                >
                  <button type="button" style={styles.retryButton} onClick={handleNavigateBack}>
                    Home
                  </button>
                  <button
                    type="button"
                    style={styles.retryButton}
                    onClick={() => window.location.reload()}
                  >
                    Reload
                  </button>
                </div>
              </>
            ) : (
              <>
                <p style={styles.errorText}>Failed to load screen</p>
                <p style={styles.hint}>{error}</p>
                <div
                  style={{
                    ...styles.errorActions,
                    gap: chromeSpacing.md,
                    marginTop: chromeSpacing.sm,
                  }}
                >
                  <button type="button" style={styles.outlineButton} onClick={handleNavigateBack}>
                    {parentUri ? 'Go back' : 'Home'}
                  </button>
                  <button type="button" style={styles.retryButton} onClick={() => { void refetch(); }}>
                    Retry
                  </button>
                </div>
                {!navShell && (
                  <p style={styles.hint}>
                    Make sure the SDUI server is running at http://localhost:8080
                  </p>
                )}
              </>
            )}
          </div>
        )}
        {!showFeedError && screen?.sections?.length ? (
          screen.sections.map((section) => (
            <React.Fragment key={section.id}>
              {staleSections.has(section.id) && (
                <div style={styles.staleBanner}>⚠ This section may be out of date</div>
              )}
              <SectionRouter
                section={section}
                state={screenState}
                onAction={handleAction}
                onStateChange={handleStateChange}
                onSectionReplace={onSectionReplace}
                onSectionGone={onSectionGone}
                onStalenessChange={handleStalenessChange}
                traceId={correlationId ?? undefined}
              />
            </React.Fragment>
          ))
        ) : null}
        {!showFeedError && !loading && screen && !screen.sections?.length && (
          <div style={styles.emptyState}>No games available right now.</div>
        )}
      </main>

      {(screen ?? shellScreen) && (
        <footer style={styles.footer}>
          <span>Correlation: {correlationId ?? '—'}</span>
          <span>Sections: {(screen ?? shellScreen)?.sections?.length || 0}</span>
        </footer>
      )}
    </div>
    </WireAssetBaseUrlProvider>
  );
}

const styles: Record<string, React.CSSProperties> = {
  container: {
    position: 'relative',
    minHeight: '100vh',
    display: 'flex',
    flexDirection: 'column',
    maxWidth: 1200,
    margin: '0 auto',
    padding: 0,
    backgroundColor: 'var(--canvas)',
    overflowX: 'hidden',
  },
  navLoadingOverlay: {
    position: 'absolute',
    inset: 0,
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: 'rgba(0,0,0,0.12)',
    zIndex: 10,
    pointerEvents: 'none',
    transition: 'opacity 0.2s ease',
  },
  navErrorBanner: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'space-between',
    gap: 12,
    padding: '8px 16px',
    backgroundColor: 'rgba(200, 40, 40, 0.12)',
    color: 'var(--negative, #c62828)',
    fontSize: 13,
  },
  navErrorText: {
    flex: 1,
  },
  navErrorRetry: {
    padding: '4px 12px',
    borderRadius: 'var(--rounded-base)',
    border: '1px solid var(--divider)',
    background: 'var(--surface)',
    cursor: 'pointer',
    fontSize: 12,
  },
  mainWhileLoading: {
    opacity: 0.5,
    transition: 'opacity 0.2s ease',
  },
  themeFloating: {
    position: 'absolute',
    top: 8,
    right: 8,
    zIndex: 20,
    border: '1px solid var(--divider)',
    borderRadius: 'var(--rounded-full)',
    backgroundColor: 'var(--surface-alt)',
    color: 'var(--text-primary)',
    cursor: 'pointer',
    fontFamily: 'var(--font-body)',
    fontSize: 12,
    fontWeight: 600,
    padding: '5px 12px',
  },
  main: {
    flex: 1,
    overflowY: 'auto',
    overflowX: 'hidden',
    paddingBottom: 60,
  },
  footer: {
    width: '100%',
    display: 'flex',
    justifyContent: 'space-between',
    padding: '8px 16px',
    backgroundColor: 'var(--surface)',
    borderTop: '1px solid var(--divider)',
    fontSize: 11,
    color: 'var(--text-secondary)',
    fontFamily: 'var(--font-mono)',
  },
  emptyState: {
    padding: 24,
    color: 'var(--text-secondary)',
    textAlign: 'center',
    fontSize: 14,
  },
  staleBanner: {
    padding: '6px 12px',
    backgroundColor: 'rgba(251,205,68,0.15)',
    color: 'var(--nba-tint)',
    fontSize: 12,
    textAlign: 'center',
    borderRadius: 'var(--rounded-base)',
    marginBottom: 4,
    fontWeight: 500,
  },
  centered: {
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    justifyContent: 'center',
    minHeight: '100vh',
    padding: 24,
    backgroundColor: 'var(--canvas)',
  },
  centeredInline: {
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    justifyContent: 'center',
  },
  errorActions: {
    display: 'flex',
  },
  outlineButton: {
    padding: '12px 24px',
    border: '1px solid var(--divider)',
    borderRadius: 'var(--rounded-base)',
    backgroundColor: 'var(--surface)',
    color: 'var(--text-primary)',
    fontSize: 14,
    fontWeight: 600,
    cursor: 'pointer',
    fontFamily: 'var(--font-body)',
  },
  spinner: {
    width: 40,
    height: 40,
    border: '3px solid var(--divider)',
    borderTopColor: 'var(--nba-tint)',
    borderRadius: '50%',
    animation: 'spin 1s linear infinite',
  },
  loadingText: {
    marginTop: 16,
    color: 'var(--text-secondary)',
  },
  errorText: {
    color: 'var(--negative)',
    marginBottom: 16,
  },
  retryButton: {
    padding: '12px 24px',
    border: 'none',
    borderRadius: 'var(--rounded-base)',
    backgroundColor: 'var(--button)',
    color: 'var(--button-text)',
    fontSize: 14,
    fontWeight: 600,
    cursor: 'pointer',
    fontFamily: 'var(--font-body)',
    transition: 'opacity var(--duration-fast) var(--ease-default)',
  },
  hint: {
    marginTop: 24,
    fontSize: 12,
    color: 'var(--text-secondary)',
    textAlign: 'center',
  },
};

export default App;
