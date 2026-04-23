import SwiftUI

/// Stub renderer for SubscribeBanner — Phase 4 will add platform IAP
/// SDK integration. Outer chrome (card radius, margin, gradient
/// background, inner padding) comes from `section.display` via
/// `SectionContainer` — this renderer only lays out the banner's
/// content (logo, title, subtitle, CTA). See AGENTS.md §15.3.
struct SubscribeBannerView: View {
    let section: Section
    let onAction: (Action) -> Void

    var body: some View {
        if let data = section.data {
            HStack(spacing: 12) {
                if let logoURL = data.logoURL, let url = URL(string: logoURL) {
                    AsyncImage(url: url) { image in
                        image.resizable().scaledToFit()
                    } placeholder: { EmptyView() }
                    .frame(width: 40, height: 40)
                }

                VStack(alignment: .leading, spacing: 2) {
                    if let title = data.title {
                        Text(title)
                            .font(.subheadline)
                            .fontWeight(.semibold)
                            .foregroundColor(.white)
                    }
                    if let subtitle = data.subtitle {
                        Text(subtitle)
                            .font(.caption)
                            .foregroundColor(.white.opacity(0.85))
                    }
                }

                Spacer()

                if let ctaLabel = data.ctaLabel {
                    Button(action: {
                        if let action = data.ctaAction { onAction(action) }
                    }) {
                        Text(ctaLabel)
                            .font(.subheadline)
                            .fontWeight(.semibold)
                            .padding(.horizontal, 12)
                            .padding(.vertical, 6)
                            .background(Color.white)
                            .foregroundColor(Color(.sRGB, red: 0.11, green: 0.26, blue: 0.54, opacity: 1))
                            .cornerRadius(6)
                    }
                }
            }
        }
    }
}
