import React, { useState, useCallback, useEffect, useRef } from 'react';
import type { Action, Section } from '@sdui/models';
import { useSduiScreen } from './hooks/useSduiScreen';
import { SectionRouter } from './components/SectionRouter';
import { TopNavigationBar } from './components/TopNavigationBar';
import { executeActionSequence } from './runtime/ActionHandler';
import { setColorSchemePreference, usePrefersColorScheme } from './utils/ColorTokenResolver';

// TODO: Bootstrap URI should come from a /sdui/init endpoint.
//       Hardcoded here only as a temporary prototype bootstrap.
const BOOTSTRAP_URI = 'nba://for-you';

/**
 * Convert an nba:// URI to a server endpoint path.
 *
 * Pure prefix swap — no special-casing of individual screens. The server
 * owns all routing semantics.
 *
 *   nba://scoreboard        → /sdui/scoreboard
 *   nba://game/0042300102   → /sdui/game/0042300102
 *   nba://boxscore/00423... → /sdui/boxscore/0042300102
 *   nba://demos             → /sdui/demos
 *   nba://anything/else     → /sdui/anything/else
 */
function resolveEndpoint(uri: string): string {
  const path = uri.replace(/^nba:\/\//, '');
  return `/sdui/${path}`;
}

export function App(): React.ReactElement {
  const [experiments, setExperiments] = useState<Record<string, string>>({});
  const [currentUri, setCurrentUri] = useState(BOOTSTRAP_URI);
  const colorScheme = usePrefersColorScheme();

  // Variants come from the server response — no client-side URI sniffing.
  // We read screen.variants after the first fetch below.

  useEffect(() => {
    setExperiments({});
    setScreenState({});
  }, [currentUri]);

  const endpoint = resolveEndpoint(currentUri);
  const { screen, loading, error, refetch, setScreen } = useSduiScreen({ endpoint, experiments });

  // Read available variants from the server response (empty if not provided).
  const variantsData = (screen as Record<string, unknown> | undefined)?.variants as
    { experimentId?: string; options?: ReadonlyArray<{ id: string; label: string; description: string }> } | undefined;
  const variantOptions = variantsData?.options ?? [];
  const variantExperimentId = variantsData?.experimentId ?? 'variant';

  // Screen-level state for TabGroup and other stateful sections
  const [screenState, setScreenState] = useState<Record<string, unknown>>({});

  // Track sections that failed to refresh (stale indicator)
  const [staleSections, setStaleSections] = useState<Set<string>>(new Set());

  // Track whether the last screen mutation was a surgical section update
  // so we can skip re-seeding state from the (stale) screen.state.
  const isSectionUpdateRef = useRef(false);

  // Initialize screen state from server response when it arrives.
  // Skip when the screen reference changed due to a section-level merge
  // (the original screen.state still has defaults that would overwrite
  // the user's form selections).
  useEffect(() => {
    if (isSectionUpdateRef.current) {
      isSectionUpdateRef.current = false;
      return;
    }
    if (screen?.state) {
      setScreenState((prev) => ({ ...prev, ...screen.state }));
    }
  }, [screen]);

  const handleStateChange = useCallback((key: string, value: unknown) => {
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

  const handleAction = useCallback((action: Action) => {
    if (action.type === 'navigate' && action.targetUri) {
      handleUriNavigate(action.targetUri);
      return;
    }
    const context = {
      state: screenState,
      onStateChange: handleStateChange,
      onRefresh: handleRefresh,
      onSectionUpdate: handleSectionUpdate,
      onSectionStale: handleSectionStale,
    };
    executeActionSequence([action], context);
  }, [screenState, handleStateChange, handleRefresh, handleSectionUpdate, handleUriNavigate, handleSectionStale]);

  const handleThemeToggle = useCallback(() => {
    setColorSchemePreference(colorScheme === 'dark' ? 'light' : 'dark');
  }, [colorScheme]);

  // Loading state
  if (loading) {
    return (
      <div style={styles.centered}>
        <div style={styles.spinner} />
        <p style={styles.loadingText}>Loading Game Detail...</p>
      </div>
    );
  }

  // Error state
  if (error) {
    return (
      <div style={styles.centered}>
        <p style={styles.errorText}>Error: {error}</p>
        <button style={styles.retryButton} onClick={refetch}>
          Retry
        </button>
        <p style={styles.hint}>
          Make sure the SDUI server is running at http://localhost:8080
        </p>
      </div>
    );
  }

  // No screen data
  if (!screen) {
    return (
      <div style={styles.centered}>
        <p style={styles.errorText}>No screen data available</p>
      </div>
    );
  }

  return (
    <div style={styles.container}>
      {/* Header */}
      <header style={styles.header}>
        {screen.parentUri && (
          <button
            style={styles.backButton}
            onClick={() => handleUriNavigate(screen.parentUri!)}
            aria-label="Back"
          >
            ←
          </button>
        )}
        <h1 style={styles.title}>{screen.title || 'NBA'}</h1>
        <div style={styles.headerActions}>
          <button
            type="button"
            style={styles.themeButton}
            onClick={handleThemeToggle}
            aria-label={`Switch to ${colorScheme === 'dark' ? 'light' : 'dark'} mode`}
            title={`Switch to ${colorScheme === 'dark' ? 'light' : 'dark'} mode`}
          >
            {colorScheme === 'dark' ? 'Light' : 'Dark'}
          </button>
          <span style={styles.schemaVersion}>Schema v{screen.schemaVersion}</span>
        </div>
      </header>

      <TopNavigationBar
        navigation={screen.navigation}
        onNavigate={handleUriNavigate}
      />

      {/* Variant Selector - proves composability with zero client rendering changes */}
      {variantOptions.length > 0 && (
        <div style={styles.variantBar}>
          <span style={styles.variantLabel}>Variant:</span>
          {variantOptions.map((v) => (
            <button
              key={v.id}
              style={{
                ...styles.variantButton,
                ...(experiments[variantExperimentId] === v.id ? styles.variantButtonActive : {}),
              }}
              onClick={() =>
                setExperiments((prev) => ({
                  ...prev,
                  [variantExperimentId]: v.id,
                }))
              }
              title={v.description}
            >
              {v.label}
            </button>
          ))}
        </div>
      )}

      {/* Sections */}
      <main style={styles.main}>
        {screen.sections?.length ? (
          screen.sections.map((section) => (
            <div
              key={section.id}
              style={{
                marginTop: section.layoutHints?.marginTop ?? 0,
                marginBottom: section.layoutHints?.marginBottom ?? 0,
              }}
            >
              {section.layoutHints?.dividerAbove && <hr className="sdui-divider" />}
              {staleSections.has(section.id) && (
                <div style={styles.staleBanner}>⚠ This section may be out of date</div>
              )}
              <SectionRouter
                section={section}
                state={screenState}
                onAction={handleAction}
                onStateChange={handleStateChange}
                onStalenessChange={handleStalenessChange}
              />
              {section.layoutHints?.dividerBelow && <hr className="sdui-divider" />}
            </div>
          ))
        ) : (
          <div style={styles.emptyState}>No games available right now.</div>
        )}
      </main>

      {/* Debug Footer */}
      <footer style={styles.footer}>
        <span>TraceId: {screen.traceId}</span>
        <span>Sections: {screen.sections?.length || 0}</span>
      </footer>
    </div>
  );
}

const styles: Record<string, React.CSSProperties> = {
  container: {
    minHeight: '100vh',
    display: 'flex',
    flexDirection: 'column',
    maxWidth: 1200,
    margin: '0 auto',
    padding: '0 16px',
    backgroundColor: 'var(--canvas)',
    overflowX: 'hidden',
  },
  header: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    padding: '12px 16px',
    backgroundColor: 'var(--surface)',
    borderBottom: '1px solid var(--divider)',
  },
  backButton: {
    background: 'none',
    border: 'none',
    color: 'var(--text-primary)',
    fontSize: 20,
    cursor: 'pointer',
    padding: '4px 8px',
    marginRight: 8,
    borderRadius: 'var(--rounded-base)',
    transition: 'background var(--duration-fast) var(--ease-default)',
  },
  title: {
    fontSize: 18,
    fontWeight: 700,
    fontFamily: 'var(--font-body)',
    color: 'var(--text-primary)',
    margin: 0,
    letterSpacing: '-0.01em',
  },
  schemaVersion: {
    fontSize: 11,
    color: 'var(--text-secondary)',
    fontFamily: 'var(--font-mono)',
  },
  headerActions: {
    display: 'flex',
    alignItems: 'center',
    gap: 10,
  },
  themeButton: {
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
  variantBar: {
    display: 'flex',
    alignItems: 'center',
    gap: 6,
    padding: '8px 16px',
    backgroundColor: 'var(--surface)',
    borderBottom: '1px solid var(--divider)',
    overflowX: 'auto',
  },
  variantLabel: {
    fontSize: 11,
    color: 'var(--text-secondary)',
    marginRight: 4,
    whiteSpace: 'nowrap',
    textTransform: 'uppercase' as const,
    fontWeight: 600,
    letterSpacing: '0.05em',
  },
  variantButton: {
    padding: '4px 12px',
    border: '1px solid var(--divider)',
    borderRadius: 'var(--rounded-full)',
    backgroundColor: 'transparent',
    color: 'var(--text-secondary)',
    fontSize: 12,
    cursor: 'pointer',
    whiteSpace: 'nowrap',
    fontFamily: 'var(--font-body)',
    transition: 'all var(--duration-fast) var(--ease-default)',
  },
  variantButtonActive: {
    backgroundColor: 'var(--nba-tint)',
    borderColor: 'var(--nba-tint)',
    color: 'var(--nba-on-tint)',
    fontWeight: 600,
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
