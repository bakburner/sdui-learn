import React from 'react';
import type { SectionProps } from '../SectionRouter';

interface ErrorStateUiModel {
  title: string;
  message: string;
  icon?: string;
  retryAction?: { type: string; trigger: string; targetUri?: string };
}

function mapErrorState(data: Record<string, unknown> | undefined): ErrorStateUiModel {
  return {
    title: (data?.title as string) ?? 'Something went wrong',
    message: (data?.message as string) ?? '',
    icon: data?.icon as string | undefined,
    retryAction: data?.retryAction as ErrorStateUiModel['retryAction'],
  };
}

const iconMap: Record<string, string> = {
  error: '⚠️',
  wifi_off: '📡',
  not_found: '🔍',
  timeout: '⏱️',
};

export function ErrorState({ section, onAction }: SectionProps): React.ReactElement {
  const model = mapErrorState(section.data as Record<string, unknown>);
  const emoji = model.icon ? (iconMap[model.icon] ?? '⚠️') : '⚠️';

  return (
    <div
      style={{
        padding: '32px 24px',
        textAlign: 'center',
        backgroundColor: '#1a1a2e',
        borderRadius: 12,
        margin: '12px 16px',
      }}
    >
      <div style={{ fontSize: 48, marginBottom: 12 }}>{emoji}</div>
      <h3 style={{ color: '#fff', margin: '0 0 8px', fontSize: 18 }}>{model.title}</h3>
      <p style={{ color: '#aaa', margin: '0 0 16px', fontSize: 14, lineHeight: 1.5 }}>
        {model.message}
      </p>
      {model.retryAction && (
        <button
          onClick={() => onAction(model.retryAction as any)}
          style={{
            padding: '10px 24px',
            borderRadius: 8,
            border: 'none',
            backgroundColor: '#1d428a',
            color: '#fff',
            fontSize: 14,
            fontWeight: 600,
            cursor: 'pointer',
          }}
        >
          Try Again
        </button>
      )}
    </div>
  );
}
