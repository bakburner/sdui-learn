import SwiftUI

/// GamePanel renderer. Owns client-side live-score presentation: swaps
/// the period/clock badge styling based on `gameStatus`, promotes the
/// status row to a live region when the game is in progress, and
/// animates score changes. Server-provided Ably SSE updates flow into
/// `section.data` via ``DataBindingApplier`` upstream.
struct GamePanelView: View {
    let section: Section
    let onAction: (Action) -> Void

    @Environment(\.colorScheme) private var colorScheme

    var body: some View {
        if let data = section.data {
            let variant = data.variant ?? .standard
            VStack(spacing: 8) {
                if let badge = data.badgeText {
                    Text(badge)
                        .font(.caption)
                        .fontWeight(.bold)
                        .padding(.horizontal, 8)
                        .padding(.vertical, 2)
                        .background(ColorTokenResolver.resolve(data.displayConfig?.badgeColor, colorScheme: colorScheme) ?? Color.red)
                        .foregroundColor(.white)
                        .cornerRadius(4)
                        .accessibilityAddTraits(isLive(status: data.gameStatus) ? .updatesFrequently : [])
                }

                HStack {
                    teamView(team: data.awayTeam, isLive: isLive(status: data.gameStatus))
                    Spacer()
                    if let statusText = data.gameStatusText {
                        Text(statusText)
                            .font(variant == .featured ? .subheadline : .caption)
                            .fontWeight(variant == .featured ? .semibold : .regular)
                            .foregroundColor(.secondary)
                            .accessibilityAddTraits(isLive(status: data.gameStatus) ? .updatesFrequently : [])
                    }
                    Spacer()
                    teamView(team: data.homeTeam, isLive: isLive(status: data.gameStatus))
                }
                .padding(variant == .featured ? 20 : 16)
            }
            .background(resolveBackground(data.displayConfig?.background, colorScheme: colorScheme))
            .cornerRadius(CGFloat(data.displayConfig?.cornerRadius ?? (variant == .featured ? 16 : 12)))
            .shadow(
                color: Color.black.opacity(variant == .featured ? 0.18 : 0.08),
                radius: variant == .featured ? 8 : 3,
                x: 0,
                y: variant == .featured ? 4 : 2
            )
            .applyActionTriggers(section.actions, onAction: onAction)
            .onTapGesture {
                if let action = SectionInteractions.primaryAction(for: section) {
                    onAction(action)
                }
            }
        }
    }

    /// gameStatus semantics (per server composer): 1=scheduled, 2=live, 3=final.
    private func isLive(status: Int?) -> Bool {
        status == 2
    }

    @ViewBuilder
    private func teamView(team: TeamData?, isLive: Bool) -> some View {
        if let team = team {
            VStack(spacing: 4) {
                if let logoURL = team.logoURL, let url = URL(string: logoURL) {
                    AsyncImage(url: url) { image in
                        image.resizable().scaledToFit()
                    } placeholder: {
                        ProgressView()
                    }
                    .frame(width: 48, height: 48)
                }
                Text(team.teamTricode)
                    .font(.headline)
                Text("\(team.score)")
                    .font(.system(size: 32, weight: .bold, design: .rounded))
                    .contentTransition(.numericText())
                    .animation(.spring(duration: 0.3), value: team.score)
            }
            .accessibilityElement(children: .combine)
            .accessibilityLabel("\(team.teamTricode) \(team.score)")
            .accessibilityAddTraits(isLive ? .updatesFrequently : [])
        }
    }
}
