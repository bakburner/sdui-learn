import React, { useState, useCallback, useEffect, useRef } from 'react';
import type { Action, Section } from '@sdui/models';
import { useSduiScreen } from './hooks/useSduiScreen';
import { SectionRouter } from './components/SectionRouter';
import { TopNavigationBar } from './components/TopNavigationBar';
import { createActionHandler } from './runtime/ActionHandler';

// TODO(Rule 2): Bootstrap URI should come from a /sdui/init endpoint.
//               Hardcoded here only as a temporary prototype bootstrap.
const BOOTSTRAP_URI = 'nba://for-you';

/**
 * Convert an nba:// URI to a server endpoint path.
 *
 * Pure prefix swap — no special-casing of individual screens (Rule 10).
 * The server owns all routing semantics.
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
  const [variant, setVariant] = useState('A');
  const [currentUri, setCurrentUri] = useState(BOOTSTRAP_URI);

  // Variants come from the server response — no client-side URI sniffing (Rule 10).
  // We read screen.variants after the first fetch below.

  useEffect(() => {
    setVariant('A');
    setScreenState({});
  }, [currentUri]);

  const endpoint = resolveEndpoint(currentUri);
  const { screen, loading, error, refetch, setScreen } = useSduiScreen({ endpoint, variant });

  // Read available variants from the server response (empty if not provided).
  const variants: ReadonlyArray<{ id: string; label: string; description: string }> =
    (screen as Record<string, unknown> | undefined)?.variants as typeof variants ?? [];

  // Screen-level state for TabGroup and other stateful sections
  const [screenState, setScreenState] = useState<Record<string, unknown>>({});

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

  const handleAction = useCallback((action: Action) => {
    if (action.type === 'navigate' && action.targetUri) {
      handleUriNavigate(action.targetUri);
      return;
    }
    const handler = createActionHandler({
      state: screenState,
      onStateChange: handleStateChange,
      onRefresh: handleRefresh,
      onSectionUpdate: handleSectionUpdate,
    });
    handler(action);
  }, [screenState, handleStateChange, handleRefresh, handleSectionUpdate, handleUriNavigate]);

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
        <span style={styles.schemaVersion}>Schema v{screen.schemaVersion}</span>
      </header>

      <TopNavigationBar
        navigation={screen.navigation}
        onNavigate={handleUriNavigate}
      />

      {/* Variant Selector - proves composability with zero client rendering changes */}
      {variants.length > 0 && (
        <div style={styles.variantBar}>
          <span style={styles.variantLabel}>Variant:</span>
          {variants.map((v) => (
            <button
              key={v.id}
              style={{
                ...styles.variantButton,
                ...(variant === v.id ? styles.variantButtonActive : {}),
              }}
              onClick={() => setVariant(v.id)}
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
            <SectionRouter
              key={section.id}
              section={section}
              state={screenState}
              onAction={handleAction}
              onStateChange={handleStateChange}
            />
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
    backgroundColor: '#0f0f23',
  },
  header: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    padding: '16px',
    backgroundColor: '#1a1a2e',
    borderBottom: '1px solid #333',
  },
  backButton: {
    background: 'none',
    border: 'none',
    color: '#ffffff',
    fontSize: 20,
    cursor: 'pointer',
    padding: '4px 8px',
    marginRight: 8,
  },
  title: {
    fontSize: 18,
    fontWeight: 700,
    color: '#ffffff',
    margin: 0,
  },
  schemaVersion: {
    fontSize: 11,
    color: '#666666',
  },
  variantBar: {
    display: 'flex',
    alignItems: 'center',
    gap: 6,
    padding: '8px 16px',
    backgroundColor: '#12122a',
    borderBottom: '1px solid #333',
    overflowX: 'auto',
  },
  variantLabel: {
    fontSize: 11,
    color: '#888888',
    marginRight: 4,
    whiteSpace: 'nowrap',
  },
  variantButton: {
    padding: '4px 12px',
    border: '1px solid #444',
    borderRadius: 16,
    backgroundColor: 'transparent',
    color: '#aaaaaa',
    fontSize: 12,
    cursor: 'pointer',
    whiteSpace: 'nowrap',
  },
  variantButtonActive: {
    backgroundColor: '#ff6b6b',
    borderColor: '#ff6b6b',
    color: '#ffffff',
    fontWeight: 600,
  },
  main: {
    flex: 1,
    overflowY: 'auto',
    paddingBottom: 60,
  },
  footer: {
    width: '100%',
    display: 'flex',
    justifyContent: 'space-between',
    padding: '8px 16px',
    backgroundColor: '#1a1a2e',
    borderTop: '1px solid #333',
    fontSize: 11,
    color: '#666666',
  },
  emptyState: {
    padding: 24,
    color: '#9aa6ba',
    textAlign: 'center',
    fontSize: 14,
  },
  centered: {
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    justifyContent: 'center',
    minHeight: '100vh',
    padding: 24,
  },
  spinner: {
    width: 40,
    height: 40,
    border: '3px solid #333',
    borderTopColor: '#ff6b6b',
    borderRadius: '50%',
    animation: 'spin 1s linear infinite',
  },
  loadingText: {
    marginTop: 16,
    color: '#888888',
  },
  errorText: {
    color: '#ff6b6b',
    marginBottom: 16,
  },
  retryButton: {
    padding: '12px 24px',
    border: 'none',
    borderRadius: 8,
    backgroundColor: '#ff6b6b',
    color: '#ffffff',
    fontSize: 14,
    fontWeight: 600,
    cursor: 'pointer',
  },
  hint: {
    marginTop: 24,
    fontSize: 12,
    color: '#666666',
    textAlign: 'center',
  },
};

export default App;
