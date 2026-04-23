import React from 'react';
import type { SectionProps } from '../SectionRouter';
import { mapForm } from '../../adapters/sectionUiAdapters';
import { accessibilityProps } from '../../utils/accessibility';

/**
 * Form — generic server-driven form section.
 *
 * Renders typed fields (select, radio, toggle, text, etc.) bound to
 * screen state.  Submit fires a parameterized refresh action.
 */
export function Form({ section, state, onAction, onStateChange }: SectionProps): React.ReactElement {
  const model = mapForm(section, state);
  if (!model) {
    return <div style={styles.container}>No form data available</div>;
  }

  const handleSubmit = () => {
    if (model.submitAction) {
      onAction(model.submitAction);
    }
  };

  const isHorizontal = model.layout === 'horizontal' || model.layout === 'inline';

  return (
    <div role="form" style={{ ...styles.container, backgroundColor: section.backgroundColor || 'var(--surface)' }} {...accessibilityProps(section.accessibility)}>
      <div style={{ ...styles.fields, flexDirection: isHorizontal ? 'row' : 'column' }}>
        {model.fields.map((field) => {
          const value = (state[field.stateKey] as string) ?? '';
          return (
            <div key={field.fieldId} style={styles.fieldGroup}>
              <label style={styles.label} htmlFor={`form-${field.fieldId}`}>
                {field.label}
                {field.required && <span style={styles.required}>*</span>}
              </label>

              {/* Select (variant-dispatched) */}
              {field.fieldType === 'select' && (() => {
                const variant = (field as { variant?: string }).variant ?? 'dropdown';
                if (variant === 'chips') {
                  return (
                    <div style={styles.chipsRow} role="radiogroup" aria-labelledby={`form-${field.fieldId}`}>
                      {field.options?.map((opt) => {
                        const selected = value === opt.value;
                        return (
                          <button
                            key={opt.value}
                            type="button"
                            role="radio"
                            aria-checked={selected}
                            disabled={field.disabled}
                            onClick={() => onStateChange(field.stateKey, opt.value)}
                            style={{
                              ...styles.chip,
                              ...(selected ? styles.chipSelected : {}),
                            }}
                          >
                            {opt.label}
                          </button>
                        );
                      })}
                    </div>
                  );
                }
                if (variant === 'segmented') {
                  return (
                    <div style={styles.segmentedGroup} role="radiogroup" aria-labelledby={`form-${field.fieldId}`}>
                      {field.options?.map((opt, i) => {
                        const selected = value === opt.value;
                        const isFirst = i === 0;
                        const isLast = i === (field.options?.length ?? 0) - 1;
                        return (
                          <button
                            key={opt.value}
                            type="button"
                            role="radio"
                            aria-checked={selected}
                            disabled={field.disabled}
                            onClick={() => onStateChange(field.stateKey, opt.value)}
                            style={{
                              ...styles.segmentedButton,
                              ...(selected ? styles.segmentedButtonSelected : {}),
                              borderTopLeftRadius: isFirst ? 6 : 0,
                              borderBottomLeftRadius: isFirst ? 6 : 0,
                              borderTopRightRadius: isLast ? 6 : 0,
                              borderBottomRightRadius: isLast ? 6 : 0,
                            }}
                          >
                            {opt.label}
                          </button>
                        );
                      })}
                    </div>
                  );
                }
                // dropdown (default) — native <select>
                return (
                  <select
                    id={`form-${field.fieldId}`}
                    value={value}
                    disabled={field.disabled}
                    onChange={(e) => onStateChange(field.stateKey, e.target.value)}
                    style={styles.select}
                  >
                    {field.placeholder && <option value="">{field.placeholder}</option>}
                    {field.options?.map((opt) => (
                      <option key={opt.value} value={opt.value}>{opt.label}</option>
                    ))}
                  </select>
                );
              })()}

              {/* Radio group */}
              {field.fieldType === 'radio' && (
                <div style={styles.radioGroup}>
                  {field.options?.map((opt) => (
                    <label key={opt.value} style={styles.radioLabel}>
                      <input
                        type="radio"
                        name={field.fieldId}
                        value={opt.value}
                        checked={value === opt.value}
                        disabled={field.disabled}
                        onChange={() => onStateChange(field.stateKey, opt.value)}
                        style={styles.radioInput}
                      />
                      <span style={styles.radioText}>{opt.label}</span>
                    </label>
                  ))}
                </div>
              )}

              {/* Checkbox / toggle */}
              {(field.fieldType === 'checkbox' || field.fieldType === 'toggle') && (
                <label style={styles.toggleLabel}>
                  <input
                    type="checkbox"
                    checked={value === 'true' || value === '1'}
                    disabled={field.disabled}
                    onChange={(e) =>
                      onStateChange(field.stateKey, e.target.checked ? 'true' : 'false')
                    }
                    style={styles.checkbox}
                  />
                  <span style={styles.toggleText}>
                    {value === 'true' || value === '1' ? 'On' : 'Off'}
                  </span>
                </label>
              )}

              {/* Date picker */}
              {field.fieldType === 'date' && (
                <input
                  id={`form-${field.fieldId}`}
                  type="date"
                  value={value}
                  disabled={field.disabled}
                  onChange={(e) => onStateChange(field.stateKey, e.target.value)}
                  style={styles.textInput}
                />
              )}

              {/* Number */}
              {field.fieldType === 'number' && (
                <input
                  id={`form-${field.fieldId}`}
                  type="number"
                  value={value}
                  placeholder={field.placeholder}
                  disabled={field.disabled}
                  onChange={(e) => onStateChange(field.stateKey, e.target.value)}
                  style={styles.textInput}
                />
              )}

              {/* Textarea */}
              {field.fieldType === 'textarea' && (
                <textarea
                  id={`form-${field.fieldId}`}
                  value={value}
                  placeholder={field.placeholder}
                  disabled={field.disabled}
                  onChange={(e) => onStateChange(field.stateKey, e.target.value)}
                  style={{ ...styles.textInput, minHeight: 80, resize: 'vertical' }}
                />
              )}

              {/* Text (default) */}
              {field.fieldType === 'text' && (
                <input
                  id={`form-${field.fieldId}`}
                  type="text"
                  value={value}
                  placeholder={field.placeholder}
                  disabled={field.disabled}
                  onChange={(e) => onStateChange(field.stateKey, e.target.value)}
                  style={styles.textInput}
                />
              )}
            </div>
          );
        })}
      </div>

      {/* Submit button */}
      {model.submitAction && (
        <button style={styles.submitButton} onClick={handleSubmit}>
          {model.submitLabel || 'Submit'}
        </button>
      )}
    </div>
  );
}

// ── Styles ───────────────────────────────────────────────────────────

const styles: Record<string, React.CSSProperties> = {
  container: {
    padding: 16,
    borderRadius: 12,
    margin: 8,
  },
  fields: {
    display: 'flex',
    gap: 12,
    flexWrap: 'wrap',
  },
  fieldGroup: {
    display: 'flex',
    flexDirection: 'column',
    gap: 4,
    flex: '1 1 180px',
    minWidth: 0,
  },
  label: {
    fontSize: 12,
    fontWeight: 600,
    color: 'var(--text-secondary)',
    marginBottom: 2,
    letterSpacing: '0.02em',
  },
  required: {
    color: 'var(--negative)',
    marginLeft: 2,
  },
  select: {
    padding: '8px 12px',
    borderRadius: 4,
    border: '1px solid var(--divider)',
    backgroundColor: 'var(--surface)',
    color: 'var(--text-primary)',
    fontSize: 13,
    fontFamily: 'var(--font-body)',
    appearance: 'auto' as React.CSSProperties['appearance'],
    cursor: 'pointer',
  },
  radioGroup: {
    display: 'flex',
    flexWrap: 'wrap',
    gap: 8,
  },
  radioLabel: {
    display: 'flex',
    alignItems: 'center',
    gap: 4,
    cursor: 'pointer',
  },
  radioInput: {
    accentColor: 'var(--nba-tint)',
  },
  radioText: {
    fontSize: 13,
    color: 'var(--text-primary)',
  },
  toggleLabel: {
    display: 'flex',
    alignItems: 'center',
    gap: 8,
    cursor: 'pointer',
  },
  checkbox: {
    accentColor: 'var(--nba-tint)',
    width: 18,
    height: 18,
    cursor: 'pointer',
  },
  toggleText: {
    fontSize: 13,
    color: 'var(--text-primary)',
  },
  textInput: {
    padding: '8px 12px',
    borderRadius: 4,
    border: '1px solid var(--divider)',
    backgroundColor: 'var(--surface)',
    color: 'var(--text-primary)',
    fontSize: 13,
    fontFamily: 'var(--font-body)',
    outline: 'none',
  },
  submitButton: {
    marginTop: 16,
    padding: '10px 24px',
    border: 'none',
    borderRadius: 4,
    backgroundColor: 'var(--button)',
    color: 'var(--button-text)',
    fontSize: 14,
    fontWeight: 600,
    fontFamily: 'var(--font-body)',
    cursor: 'pointer',
    transition: 'opacity 0.2s',
    width: '100%',
  },
  chipsRow: {
    display: 'flex',
    gap: 8,
    overflowX: 'auto',
    paddingBottom: 4,
    scrollbarWidth: 'none',
  },
  chip: {
    flex: '0 0 auto',
    padding: '6px 14px',
    borderRadius: 999,
    border: '1px solid var(--divider)',
    backgroundColor: 'var(--surface)',
    color: 'var(--text-primary)',
    fontSize: 13,
    fontFamily: 'var(--font-body)',
    fontWeight: 500,
    cursor: 'pointer',
    whiteSpace: 'nowrap',
  },
  chipSelected: {
    backgroundColor: 'var(--nba-tint)',
    borderColor: 'var(--nba-tint)',
    color: 'var(--text-on-tint, #fff)',
  },
  segmentedGroup: {
    display: 'inline-flex',
    border: '1px solid var(--divider)',
    borderRadius: 6,
    overflow: 'hidden',
    alignSelf: 'flex-start',
  },
  segmentedButton: {
    padding: '6px 14px',
    border: 'none',
    borderRight: '1px solid var(--divider)',
    backgroundColor: 'var(--surface)',
    color: 'var(--text-primary)',
    fontSize: 13,
    fontFamily: 'var(--font-body)',
    fontWeight: 500,
    cursor: 'pointer',
    whiteSpace: 'nowrap',
  },
  segmentedButtonSelected: {
    backgroundColor: 'var(--nba-tint)',
    color: 'var(--text-on-tint, #fff)',
  },
};
