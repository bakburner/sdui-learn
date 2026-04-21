import SwiftUI

/// Full renderer for SeasonLeadersTable. Owns sort interaction state
/// (tap a column header to sort descending, tap again to toggle
/// ascending) — a client-side behaviour that can't be moved server-side
/// without round-tripping every tap.
struct SeasonLeadersTableView: View {
    let section: Section
    let onAction: (Action) -> Void

    @State private var sortKey: String?
    @State private var sortAscending: Bool = false

    var body: some View {
        if let data = section.data {
            VStack(alignment: .leading, spacing: 8) {
                if let title = data.title {
                    Text(title).font(.headline).padding(.horizontal)
                }
                if let subtitle = data.subtitle {
                    Text(subtitle).font(.caption).foregroundColor(.secondary).padding(.horizontal)
                }

                if let columns = data.columns, let players = data.players {
                    ScrollView(.horizontal, showsIndicators: false) {
                        VStack(spacing: 0) {
                            header(columns: columns)
                            rows(columns: columns, players: sortedPlayers(players))
                        }
                    }
                }
            }
        }
    }

    @ViewBuilder
    private func header(columns: [BoxscoreColumnDefinition]) -> some View {
        HStack(spacing: 0) {
            ForEach(columns, id: \.key) { col in
                Button {
                    if sortKey == col.key {
                        sortAscending.toggle()
                    } else {
                        sortKey = col.key
                        sortAscending = false
                    }
                } label: {
                    HStack(spacing: 2) {
                        Text(col.label)
                            .font(.caption)
                            .fontWeight(.semibold)
                        if sortKey == col.key {
                            Image(systemName: sortAscending ? "chevron.up" : "chevron.down")
                                .font(.caption2)
                        }
                    }
                    .frame(width: 60, alignment: .center)
                }
                .buttonStyle(.plain)
                .accessibilityLabel("\(col.label) column, tap to sort")
            }
        }
        .padding(.vertical, 4)
        .background(Color.gray.opacity(0.1))
    }

    @ViewBuilder
    private func rows(columns: [BoxscoreColumnDefinition], players: [PlayerRow]) -> some View {
        ForEach(players, id: \.playerID) { player in
            HStack(spacing: 0) {
                ForEach(columns, id: \.key) { col in
                    Text(statString(player.stats[col.key]))
                        .font(.caption)
                        .frame(width: 60, alignment: .center)
                }
            }
            .padding(.vertical, 2)
        }
    }

    private func sortedPlayers(_ players: [PlayerRow]) -> [PlayerRow] {
        guard let key = sortKey else { return players }
        return players.sorted { lhs, rhs in
            let l = statSortKey(lhs.stats[key])
            let r = statSortKey(rhs.stats[key])
            return sortAscending ? l < r : l > r
        }
    }

    private func statSortKey(_ value: JSONAny?) -> Double {
        guard let v = value else { return -.greatestFiniteMagnitude }
        if let d = v.value as? Double { return d }
        if let i = v.value as? Int64 { return Double(i) }
        if let s = v.value as? String, let d = Double(s) { return d }
        return -.greatestFiniteMagnitude
    }

    private func statString(_ value: JSONAny?) -> String {
        guard let v = value else { return "-" }
        if let s = v.value as? String { return s }
        if let i = v.value as? Int64 { return String(i) }
        if let d = v.value as? Double { return String(format: "%.1f", d) }
        return "-"
    }
}
