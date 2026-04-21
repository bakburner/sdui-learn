import SwiftUI

/// Stub renderer for SubscribeBanner — Phase 4 will add platform IAP SDK integration.
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
                        Text(title).font(.subheadline).fontWeight(.semibold)
                    }
                    if let subtitle = data.subtitle {
                        Text(subtitle).font(.caption).foregroundColor(.secondary)
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
                            .background(Color.accentColor)
                            .foregroundColor(.white)
                            .cornerRadius(6)
                    }
                }
            }
            .padding()
            .background(resolveBackground(data.background))
            .cornerRadius(12)
        }
    }
}
