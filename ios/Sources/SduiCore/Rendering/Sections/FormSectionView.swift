import SwiftUI

/// Stub renderer for Form — Phase 4 will add validation state + keyboard integration.
struct FormSectionView: View {
    let section: Section
    let screenState: ScreenState
    let onAction: (Action) -> Void

    var body: some View {
        if let data = section.data, let fields = data.fields {
            VStack(alignment: .leading, spacing: 12) {
                ForEach(fields, id: \.fieldID) { field in
                    FormFieldView(field: field, screenState: screenState)
                }

                if let submitLabel = data.submitLabel {
                    Button(action: handleSubmit) {
                        Text(submitLabel)
                            .frame(maxWidth: .infinity)
                            .padding()
                            .background(Color.accentColor)
                            .foregroundColor(.white)
                            .cornerRadius(8)
                    }
                }
            }
            .padding()
        }
    }

    private func handleSubmit() {
        if let submitAction = section.data?.submitAction {
            onAction(submitAction)
        }
    }
}

struct FormFieldView: View {
    let field: FormField
    let screenState: ScreenState

    @State private var text: String = ""

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(field.label)
                .font(.subheadline)
                .fontWeight(.medium)

            switch field.fieldType {
            case .text, .number:
                TextField(field.placeholder ?? "", text: $text)
                    .textFieldStyle(.roundedBorder)
                    .disabled(field.disabled ?? false)
                    .keyboardType(field.fieldType == .number ? .numberPad : .default)
                    .onChange(of: text) { _, newValue in
                        screenState.set(field.stateKey, value: newValue)
                    }
            case .textarea:
                TextEditor(text: $text)
                    .frame(minHeight: 80)
                    .overlay(RoundedRectangle(cornerRadius: 4).stroke(Color.gray.opacity(0.3)))
                    .onChange(of: text) { _, newValue in
                        screenState.set(field.stateKey, value: newValue)
                    }
            case .toggle:
                Toggle(field.label, isOn: Binding(
                    get: { (screenState.getBool(field.stateKey) ?? false) },
                    set: { screenState.set(field.stateKey, value: $0) }
                ))
            case .select:
                selectField
            default:
                TextField(field.placeholder ?? "", text: $text)
                    .textFieldStyle(.roundedBorder)
            }

            if let error = validationError, !error.isEmpty {
                Text(error)
                    .font(.caption)
                    .foregroundColor(.red)
            }
        }
        .onAppear {
            // Hydrate initial value from screen state so server-seeded
            // defaults survive across re-renders.
            if let current = screenState.get(field.stateKey) {
                text = String(describing: current)
            }
        }
    }

    private var validationError: String? {
        // FormField validation isn't part of v1 schema; screenState can
        // surface validation errors via a conventional key
        // (`form_<fieldId>_error`). Section-level validators push values
        // there via mutate actions.
        screenState.getString("form_\(field.fieldID)_error")
    }

    /// Resolves the server-sent SelectVariant into a native SwiftUI control.
    /// Missing variant falls back to `.dropdown`; unrecognised values decode
    /// as nil upstream and land here via the same fallback.
    @ViewBuilder
    private var selectField: some View {
        switch field.variant ?? .dropdown {
        case .dropdown:
            dropdownSelect
        case .chips:
            chipsSelect
        }
    }

    @ViewBuilder
    private var dropdownSelect: some View {
        Menu {
            ForEach(field.options ?? [], id: \.value) { option in
                Button(option.label) {
                    text = option.value
                    screenState.set(field.stateKey, value: option.value)
                }
            }
        } label: {
            HStack {
                Text(text.isEmpty ? (field.placeholder ?? field.label) : text)
                    .foregroundColor(text.isEmpty ? .secondary : .primary)
                Spacer()
                Image(systemName: "chevron.down")
            }
            .padding(8)
            .overlay(RoundedRectangle(cornerRadius: 6).stroke(Color.gray.opacity(0.4)))
        }
    }

    @ViewBuilder
    private var chipsSelect: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                ForEach(field.options ?? [], id: \.value) { option in
                    let isSelected = text == option.value
                    Button {
                        text = option.value
                        screenState.set(field.stateKey, value: option.value)
                    } label: {
                        Text(option.label)
                            .font(.subheadline)
                            .padding(.horizontal, 14)
                            .padding(.vertical, 8)
                            .background(
                                Capsule()
                                    .fill(isSelected ? Color.accentColor : Color.gray.opacity(0.12))
                            )
                            .foregroundColor(isSelected ? .white : .primary)
                    }
                    .disabled(field.disabled ?? false)
                }
            }
            .padding(.vertical, 2)
        }
    }
}
