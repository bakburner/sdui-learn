import React, { useState, useCallback, useEffect } from 'react';
import type { Action } from '@sdui/models';
import { useSduiScreen } from './hooks/useSduiScreen';
import { SectionRouter } from './components/SectionRouter';
import { TopNavigationBar } from './components/TopNavigationBar';
import { createActionHandler, showToast } from './runtime/ActionHandler';

// Demo game ID - Celtics vs Heat
const DEMO_GAME_ID = '0042300102';

const GAME_DETAIL_VARIANTS = [
  { id: 'A', label: 'Default', description: 'All sections, standard order' },
  { id: 'B', label: 'Reorder', description: 'ContentRail and TabGroup swapped' },
  { id: 'C', label: 'Minimal', description: 'StatLine and PromoBanner removed' },
  { id: 'D', label: 'Extra Rail', description: 'Second ContentRail added' },
] as const;

const SCOREBOARD_VARIANTS = [
  { id: 'A', label: 'Default', description: 'Standard scoreboard' },
  { id: 'E', label: 'Promo', description: 'Promo banner at top' },
  { id: 'F', label: 'Promo + Rail', description: 'Promo banner + content rail (when >2 games)' },
] as const;

export function App(): React.ReactElement {
  const [variant, setVariant] = useState('A');
  const [route, setRoute] = useState<{ screenType: 'scoreboard' | 'game-detail'; gameId?: string }>({
    screenType: 'scoreboard',
  });

  const variants = route.screenType === 'scoreboard' ? SCOREBOARD_VARIANTS : GAME_DETAIL_VARIANTS;

  useEffect(() => {
    setVariant('A');
  }, [route.screenType]);

  const { screen, loading, error, refetch } = useSduiScreen({
    screenType: route.screenType,
    gameId: route.gameId ?? DEMO_GAME_ID,
    gameState: 'live',
    variant,
  });

  // Screen-level state for TabGroup and other stateful sections
  const [screenState, setScreenState] = useState<Record<string, unknown>>({});

  // Initialize screen state from server response when it arrives
  useEffect(() => {
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

  const handleUriNavigate = useCallback((uri: string) => {
    if (uri.startsWith('nba://game/')) {
      setRoute({
        screenType: 'game-detail',
        gameId: uri.replace('nba://game/', ''),
      });
      return;
    }
    if (uri === 'nba://scoreboard') {
      setRoute({ screenType: 'scoreboard' });
      return;
    }
    const name = uri
      .replace('nba://', '')
      .replace(/\//g, ' ')
      .replace(/-/g, ' ')
      .replace(/^\w/, (c) => c.toUpperCase());
    showToast(`Navigating to ${name} (not implemented)`);
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
    });
    handler(action);
  }, [screenState, handleStateChange, handleRefresh, handleUriNavigate]);

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
        <h1 style={styles.title}>{screen.title || (route.screenType === 'scoreboard' ? "Today's Games" : 'Game Detail')}</h1>
        <span style={styles.schemaVersion}>Schema v{screen.schemaVersion}</span>
      </header>

      <TopNavigationBar
        navigation={screen.navigation}
        onNavigate={handleUriNavigate}
      />

      {/* Variant Selector - proves composability with zero client rendering changes */}
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
    maxWidth: 480,
    margin: '0 auto',
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
