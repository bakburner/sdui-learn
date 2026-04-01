import React from 'react';
import type { Action, SectionStates } from '@sdui/models';

interface SectionErrorBoundaryProps {
  sectionStates?: SectionStates;
  sectionId: string;
  sectionType: string;
  onAction: (action: Action) => void;
  children: React.ReactNode;
}

interface SectionErrorBoundaryState {
  hasError: boolean;
  error: Error | null;
}

export class SectionErrorBoundary extends React.Component<
  SectionErrorBoundaryProps,
  SectionErrorBoundaryState
> {
  constructor(props: SectionErrorBoundaryProps) {
    super(props);
    this.state = { hasError: false, error: null };
  }

  static getDerivedStateFromError(error: Error): SectionErrorBoundaryState {
    return { hasError: true, error };
  }

  componentDidCatch(error: Error, info: React.ErrorInfo): void {
    console.error(
      `[SectionErrorBoundary] Section render failed: id="${this.props.sectionId}" type="${this.props.sectionType}"`,
      error,
      info.componentStack,
    );
  }

  handleRetry = (): void => {
    const retryAction = this.props.sectionStates?.error?.retryAction;
    if (retryAction) {
      this.props.onAction(retryAction);
    }
    this.setState({ hasError: false, error: null });
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

    return (
      <div style={styles.container}>
        <span style={styles.icon}>⚠</span>
        <p style={styles.message}>{message}</p>
        {hasRetry && (
          <button style={styles.retryButton} onClick={this.handleRetry}>
            Try Again
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
    padding: '24px 16px',
    backgroundColor: 'rgba(255, 107, 107, 0.08)',
    borderRadius: 8,
    margin: '8px 0',
  },
  icon: {
    fontSize: 24,
    marginBottom: 8,
  },
  message: {
    color: '#ff6b6b',
    fontSize: 13,
    textAlign: 'center',
    margin: '0 0 12px',
  },
  retryButton: {
    padding: '8px 20px',
    border: '1px solid #ff6b6b',
    borderRadius: 6,
    backgroundColor: 'transparent',
    color: '#ff6b6b',
    fontSize: 12,
    fontWeight: 600,
    cursor: 'pointer',
  },
};
