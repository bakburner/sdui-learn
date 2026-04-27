import SwiftUI

/// Renders a DisplayGrid — display-only, non-interactive, server-ordered
/// tabular data. The table owns its grid layout; margin / padding /
/// bg / cornerRadius / shadow / opacity come from `AtomicBoxModifier`
/// via `.atomicBox(...)`.
struct AtomicDisplayGridView: View {
    let element: AtomicElement

    var body: some View {
        if let columns = element.columns, let rows = element.rows {
            EquatableView(
                content: EquatableDisplayGrid(
                    element: element,
                    columns: columns,
                    rows: rows
                )
            )
            .atomicBox(element, screenState: ScreenState(), onAction: { _ in })
        }
    }
}

private struct EquatableDisplayGrid: View, Equatable {
    let element: AtomicElement
    let columns: [Column]
    let rows: [[String: String]]

    static func == (lhs: EquatableDisplayGrid, rhs: EquatableDisplayGrid) -> Bool {
        lhs.element.striped == rhs.element.striped
        && lhs.columns == rhs.columns
        && lhs.rows == rhs.rows
    }

    var body: some View {
        gridBody
    }

    @ViewBuilder
    private var gridBody: some View {
        VStack(spacing: 0) {
            HStack(spacing: 0) {
                ForEach(columns, id: \.key) { col in
                    Text(col.label)
                        .font(.subheadline)
                        .fontWeight(.semibold)
                        .frame(maxWidth: .infinity, alignment: alignment(for: col.align))
                        .padding(.vertical, 8)
                        .padding(.horizontal, 4)
                }
            }
            .background(Color.gray.opacity(0.1))

            Divider()

            ForEach(Array(rows.enumerated()), id: \.offset) { index, row in
                HStack(spacing: 0) {
                    ForEach(columns, id: \.key) { col in
                        Text(row[col.key] ?? "")
                            .font(.body)
                            .frame(maxWidth: .infinity, alignment: alignment(for: col.align))
                            .padding(.vertical, 6)
                            .padding(.horizontal, 4)
                    }
                }
                .background(
                    (element.striped ?? false) && index % 2 == 1
                        ? Color.gray.opacity(0.05)
                        : Color.clear
                )
            }
        }
    }

    private func alignment(for align: Align?) -> SwiftUI.Alignment {
        switch align {
        case .center: return .center
        case .end: return .trailing
        case .start: return .leading
        case .none: return .leading
        }
    }
}

extension Column: Equatable {
    static func == (lhs: Column, rhs: Column) -> Bool {
        lhs.key == rhs.key
            && lhs.label == rhs.label
            && lhs.align == rhs.align
            && lhs.width == rhs.width
    }
}

extension WidthUnion: Equatable {
    static func == (lhs: WidthUnion, rhs: WidthUnion) -> Bool {
        switch (lhs, rhs) {
        case let (.integer(a), .integer(b)): return a == b
        case let (.enumeration(a), .enumeration(b)): return a == b
        default: return false
        }
    }
}
