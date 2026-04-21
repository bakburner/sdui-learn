import SwiftUI

/// Stub renderer for SubscribeHero — Phase 4 will add platform IAP SDK integration.
struct SubscribeHeroView: View {
    let section: Section
    let onAction: (Action) -> Void

    var body: some View {
        if let data = section.data {
            VStack(spacing: 16) {
                if let logoURL = data.logoURL, let url = URL(string: logoURL) {
                    AsyncImage(url: url) { image in
                        image.resizable().scaledToFit()
                    } placeholder: { EmptyView() }
                    .frame(height: 48)
                }

                if let title = data.title {
                    Text(title).font(.title2).fontWeight(.bold).multilineTextAlignment(.center)
                }
                if let subtitle = data.subtitle {
                    Text(subtitle).font(.body).foregroundColor(.secondary).multilineTextAlignment(.center)
                }

                if let features = data.features {
                    VStack(alignment: .leading, spacing: 4) {
                        ForEach(features, id: \.self) { feature in
                            HStack {
                                Image(systemName: "checkmark.circle.fill").foregroundColor(.green)
                                Text(feature).font(.subheadline)
                            }
                        }
                    }
                }

                if let tiers = data.tiers {
                    ForEach(tiers, id: \.id) { tier in
                        tierCard(tier)
                    }
                }

                if let ctaLabel = data.ctaLabel {
                    Button(action: {
                        if let action = data.ctaAction { onAction(action) }
                    }) {
                        Text(ctaLabel)
                            .frame(maxWidth: .infinity)
                            .padding()
                            .background(Color.accentColor)
                            .foregroundColor(.white)
                            .cornerRadius(8)
                    }
                }
            }
            .padding()
            .background(resolveBackground(data.background))
        }
    }

    @ViewBuilder
    private func tierCard(_ tier: SubscriptionTier) -> some View {
        VStack(spacing: 4) {
            if let badge = tier.badgeText {
                Text(badge).font(.caption).fontWeight(.bold).foregroundColor(.accentColor)
            }
            Text(tier.name).font(.headline)
            Text(tier.price).font(.title3).fontWeight(.bold)
            if let original = tier.originalPrice {
                Text(original).font(.caption).strikethrough().foregroundColor(.secondary)
            }
        }
        .padding()
        .frame(maxWidth: .infinity)
        .background(Color.gray.opacity(0.05))
        .cornerRadius(12)
    }
}
