import SwiftUI

/// Stub renderer for SubscribeHero — Phase 4 will add platform IAP
/// SDK integration. Outer chrome (card radius, margin, gradient
/// background, inner padding) comes from `section.display` via
/// `SectionContainer` — this renderer only lays out the hero's
/// content (logo, title, subtitle, features, tier cards, CTA).
/// See AGENTS.md §15.3.
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
                    Text(title)
                        .font(.title2)
                        .fontWeight(.bold)
                        .foregroundColor(.white)
                        .multilineTextAlignment(.center)
                }
                if let subtitle = data.subtitle {
                    Text(subtitle)
                        .font(.body)
                        .foregroundColor(.white.opacity(0.8))
                        .multilineTextAlignment(.center)
                }

                if let features = data.features {
                    VStack(alignment: .leading, spacing: 4) {
                        ForEach(features, id: \.self) { feature in
                            HStack {
                                Image(systemName: "checkmark.circle.fill").foregroundColor(.green)
                                Text(feature)
                                    .font(.subheadline)
                                    .foregroundColor(.white.opacity(0.9))
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
                            .background(Color.white)
                            .foregroundColor(Color(.sRGB, red: 0.11, green: 0.26, blue: 0.54, opacity: 1))
                            .cornerRadius(8)
                    }
                }
            }
        }
    }

    @ViewBuilder
    private func tierCard(_ tier: SubscriptionTier) -> some View {
        VStack(spacing: 4) {
            if let badge = tier.badgeText {
                Text(badge).font(.caption).fontWeight(.bold).foregroundColor(.yellow)
            }
            Text(tier.name).font(.headline).foregroundColor(.white)
            Text(tier.price).font(.title3).fontWeight(.bold).foregroundColor(.white)
            if let original = tier.originalPrice {
                Text(original)
                    .font(.caption)
                    .strikethrough()
                    .foregroundColor(.white.opacity(0.5))
            }
        }
        .padding()
        .frame(maxWidth: .infinity)
        .background(Color.white.opacity(0.1))
        .cornerRadius(12)
    }
}
