import SwiftUI

/// Full renderer for BoxscoreTable. Owns expandable row state; real-time
/// data binding happens upstream via ``DataBindingApplier`` so this view
/// only owns user interaction.
struct BoxscoreTableView: View {
    let section: Section
    let onAction: (Action) -> Void

    @State private var expandedPlayerID: String?

    var body: some View {
        if let data = section.data, let columns = data.columns, let players = data.players {
            VStack(alignment: .leading, spacing: 0) {
                if let teamName = data.teamName {
                    HStack {
                        if let logoURL = data.teamLogoURL, let url = URL(string: logoURL) {
                            AsyncImage(url: url) { image in
                                image.resizable().scaledToFit()
                            } placeholder: { EmptyView() }
                            .frame(width: 24, height: 24)
                        }
                        Text(teamName).font(.headline)
                    }
                    .padding(.horizontal)
                    .padding(.vertical, 8)
                    .accessibilityAddTraits(.isHeader)
                }

                ScrollView(.horizontal, showsIndicators: false) {
                    VStack(spacing: 0) {
                        HStack(spacing: 0) {
                            ForEach(columns, id: \.key) { col in
                                Text(col.label)
                                    .font(.caption)
                                    .fontWeight(.semibold)
                                    .frame(width: 50, alignment: .center)
                            }
                        }
                        .padding(.vertical, 4)
                        .background(Color.gray.opacity(0.1))

                        ForEach(players, id: \.playerID) { player in
                            playerRow(player: player, columns: columns)
                        }
                    }
                }
            }
            .accessibilityElement(children: .contain)
            .accessibilityLabel("Boxscore for \(data.teamName ?? "team")")
        }
    }

    @ViewBuilder
    private func playerRow(player: PlayerRow, columns: [BoxscoreColumnDefinition]) -> some View {
        let isExpanded = expandedPlayerID == player.playerID
        VStack(spacing: 0) {
            Button {
                withAnimation(.easeInOut(duration: 0.2)) {
                    expandedPlayerID = isExpanded ? nil : player.playerID
                }
            } label: {
                HStack(spacing: 0) {
                    ForEach(columns, id: \.key) { col in
                        Text(statValue(player.stats[col.key]))
                            .font(.caption)
                            .frame(width: 50, alignment: .center)
                    }
                }
                .padding(.vertical, 2)
            }
            .buttonStyle(.plain)
            .accessibilityLabel(player.name)
            .accessibilityHint(isExpanded ? "Collapse row" : "Expand row")

            if isExpanded {
                expandedDetails(player: player)
                    .transition(.opacity.combined(with: .move(edge: .top)))
            }
        }
    }

    @ViewBuilder
    private func expandedDetails(player: PlayerRow) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(player.name).font(.caption).fontWeight(.semibold)
            if let jersey = player.jerseyNumber {
                Text("#\(jersey)").font(.caption2).foregroundColor(.secondary)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(8)
        .background(Color.gray.opacity(0.05))
    }

    private func statValue(_ value: JSONAny?) -> String {
        guard let v = value else { return "-" }
        if let s = v.value as? String { return s }
        if let i = v.value as? Int64 { return String(i) }
        if let d = v.value as? Double { return String(format: "%.1f", d) }
        return "-"
    }
}
