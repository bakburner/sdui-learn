import React from 'react';
import type { Action, SectionStates } from '@sdui/models';

interface SectionErrorBoundaryProps {
  sectionStates?: SectionStates;
  sectionId: string;
  sectionType: string;
  onAction: (action: Action | Action[]) => void;
  children: React.ReactNode;
}

interface SectionErrorBoundaryState {
  hasError: boolean;
  error: Error | null;
  componentStack: string | null;
}

export class SectionErrorBoundary extends React.Component<
  SectionErrorBoundaryProps,
  SectionErrorBoundaryState
> {
  constructor(props: SectionErrorBoundaryProps) {
    super(props);
    this.state = { hasError: false, error: null, componentStack: null };
  }

  static getDerivedStateFromError(error: Error): SectionErrorBoundaryState {
    return { hasError: true, error, componentStack: null };
  }

  componentDidCatch(error: Error, info: React.ErrorInfo): void {
    // Log loudly so this isn't lost in a noisy console. We log the message,
    // the Error object (for the stack), and the component stack separately
    // so devtools shows them in expandable groups.
    const { sectionId, sectionType } = this.props;
    console.group(
      `%c[SectionErrorBoundary] ${sectionType} "${sectionId}" failed to render`,
      'color:#fff;background:#b00020;padding:2px 6px;border-radius:3px;font-weight:bold;',
    );
    console.error(error);
    console.error('Component stack:', info.componentStack);
    console.groupEnd();
    this.setState({ componentStack: info.componentStack ?? null });
  }

  handleRetry = (): void => {
    const retryAction = this.props.sectionStates?.error?.retryAction;
    if (retryAction) {
      this.props.onAction(retryAction);
    }
    this.setState({ hasError: false, error: null, componentStack: null });
  };

  render(): React.ReactNode {
    if (!this.state.hasError) {
      return this.props.children;
    }

    const errorConfig = this.props.sectionStates?.error;

    if (errorConfig?.hideOnError) {
      return null;
    }

    const message = errorConfig?.message ?? 'Something went wrong';
    const hasRetry = !!errorConfig?.retryAction;
    const retryLabel = errorConfig?.retryLabel;

    // In dev builds, surface the failure inline so it's not silently hidden
    // behind a generic message. Production keeps the user-facing copy.
    const isDev = typeof import.meta !== 'undefined'
      ? Boolean((import.meta as { env?: { DEV?: boolean } }).env?.DEV)
      : false;
    const { sectionId, sectionType } = this.props;
    const errMsg = this.state.error?.message;

    return (
      <div style={styles.container}>
        <p style={styles.message}>{message}</p>
        {isDev && (
          <details style={styles.devDetails}>
            <summary style={styles.devSummary}>
              {sectionType} · {sectionId}{errMsg ? ` — ${errMsg}` : ''}
            </summary>
            {this.state.error?.stack && (
              <pre style={styles.devPre}>{this.state.error.stack}</pre>
            )}
            {this.state.componentStack && (
              <pre style={styles.devPre}>{this.state.componentStack}</pre>
            )}
          </details>
        )}
        {hasRetry && (
          <button style={styles.retryButton} onClick={this.handleRetry}>
            {retryLabel ?? 'Retry'}
          </button>
        )}
      </div>
    );
  }
}

const styles: Record<string, React.CSSProperties> = {
  container: {
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    justifyContent: 'center',
    padding: '16px',
    backgroundColor: 'var(--surface-variant, rgba(128, 128, 128, 0.08))',
    borderRadius: 8,
    margin: '8px 0',
  },
  message: {
    color: 'var(--on-surface-variant, inherit)',
    fontSize: 13,
    textAlign: 'center',
    margin: '0 0 12px',
  },
  retryButton: {
    padding: '8px 20px',
    border: '1px solid var(--outline, #888)',
    borderRadius: 6,
    backgroundColor: 'transparent',
    color: 'var(--on-surface-variant, inherit)',
    fontSize: 12,
    fontWeight: 600,
    cursor: 'pointer',
  },
  devDetails: {
    width: '100%',
    maxWidth: 720,
    margin: '0 0 12px',
    textAlign: 'left',
    fontSize: 11,
    color: 'var(--on-surface-variant, #444)',
  },
  devSummary: {
    cursor: 'pointer',
    fontFamily: 'ui-monospace, SFMono-Regular, Menlo, monospace',
    color: '#b00020',
    fontWeight: 600,
  },
  devPre: {
    whiteSpace: 'pre-wrap',
    fontFamily: 'ui-monospace, SFMono-Regular, Menlo, monospace',
    fontSize: 11,
    margin: '6px 0 0',
    padding: '6px 8px',
    background: 'rgba(0,0,0,0.04)',
    borderRadius: 4,
    overflowX: 'auto',
  },
};
