import React from 'react';
import type { SectionProps } from '../SectionRouter';
import { mapForm } from '../../adapters/sectionUiAdapters';

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
    <div style={{ ...styles.container, backgroundColor: section.backgroundColor || '#1a1a2e' }}>
      <div style={{ ...styles.fields, flexDirection: isHorizontal ? 'row' : 'column' }}>
        {model.fields.map((field) => {
          const value = (state[field.stateKey] as string) ?? '';
          return (
            <div key={field.fieldId} style={styles.fieldGroup}>
              <label style={styles.label} htmlFor={`form-${field.fieldId}`}>
                {field.label}
                {field.required && <span style={styles.required}>*</span>}
              </label>

              {/* Select (dropdown) */}
              {field.fieldType === 'select' && (
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
              )}

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
    color: '#9aa6ba',
    marginBottom: 2,
  },
  required: {
    color: '#ff6b6b',
    marginLeft: 2,
  },
  select: {
    padding: '8px 12px',
    borderRadius: 8,
    border: '1px solid #333',
    backgroundColor: '#12122a',
    color: '#e0e0e0',
    fontSize: 13,
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
    accentColor: '#ff6b6b',
  },
  radioText: {
    fontSize: 13,
    color: '#e0e0e0',
  },
  toggleLabel: {
    display: 'flex',
    alignItems: 'center',
    gap: 8,
    cursor: 'pointer',
  },
  checkbox: {
    accentColor: '#ff6b6b',
    width: 18,
    height: 18,
    cursor: 'pointer',
  },
  toggleText: {
    fontSize: 13,
    color: '#e0e0e0',
  },
  textInput: {
    padding: '8px 12px',
    borderRadius: 8,
    border: '1px solid #333',
    backgroundColor: '#12122a',
    color: '#e0e0e0',
    fontSize: 13,
    outline: 'none',
  },
  submitButton: {
    marginTop: 16,
    padding: '10px 24px',
    border: 'none',
    borderRadius: 8,
    backgroundColor: '#ff6b6b',
    color: '#ffffff',
    fontSize: 14,
    fontWeight: 600,
    cursor: 'pointer',
    transition: 'background-color 0.2s',
    width: '100%',
  },
};
