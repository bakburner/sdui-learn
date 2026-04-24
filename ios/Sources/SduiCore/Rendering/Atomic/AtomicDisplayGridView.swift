import SwiftUI

/// Renders a DisplayGrid — display-only, non-interactive, server-ordered tabular data.
struct AtomicDisplayGridView: View {
    let element: AtomicElement

    var body: some View {
        if let columns = element.columns, let rows = element.rows {
            gridBody(columns: columns, rows: rows)
                .padding(edgeInsets(from: element.padding))
        }
    }

    @ViewBuilder
    private func gridBody(columns: [Column], rows: [[String: String]]) -> some View {
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
