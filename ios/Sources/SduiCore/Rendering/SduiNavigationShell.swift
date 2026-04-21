import SwiftUI

/// Renders a server-declared bottom navigation bar around its child content.
///
/// Mirrors the Android `SduiNavigationShell`: when `navigation` is absent or
/// empty the child content is rendered on its own (no chrome), otherwise a
/// bottom bar is pinned above the content with one item per
/// ``NavigationItem``. Tapping an item invokes `onNavigate` with the
/// server-provided `targetUri`.
///
/// This shell is pure presentation — it never derives behaviour from screen
/// identity, section data, or client state. The server decides which items
/// to show and which is selected.
struct SduiNavigationShell<Content: View>: View {
    let navigation: Navigation?
    let onNavigate: (String) -> Void
    @ViewBuilder let content: () -> Content

    var body: some View {
        let items = navigation?.items ?? []
        if items.isEmpty {
            content()
        } else {
            content()
                .safeAreaInset(edge: .bottom, spacing: 0) {
                    NavigationBarView(items: items, onNavigate: onNavigate)
                }
        }
    }
}

private struct NavigationBarView: View {
    let items: [NavigationItem]
    let onNavigate: (String) -> Void

    var body: some View {
        HStack(alignment: .center, spacing: 0) {
            ForEach(items, id: \.id) { item in
                NavigationBarItemView(item: item, onNavigate: onNavigate)
                    .frame(maxWidth: .infinity)
            }
        }
        .padding(.vertical, 8)
        .background(.bar)
        .overlay(alignment: .top) {
            Divider()
        }
    }
}

private struct NavigationBarItemView: View {
    let item: NavigationItem
    let onNavigate: (String) -> Void

    var body: some View {
        let selected = item.selected ?? false
        Button {
            if let targetURI = item.targetURI {
                onNavigate(targetURI)
            }
        } label: {
            VStack(spacing: 2) {
                Image(systemName: IconTokenResolver.shared.resolve(item.icon) ?? "circle.dashed")
                    .font(.system(size: 20, weight: selected ? .semibold : .regular))
                Text(item.label)
                    .font(.caption2)
                    .lineLimit(1)
            }
            .frame(maxWidth: .infinity)
            .foregroundStyle(selected ? Color.accentColor : .secondary)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .accessibilityLabel(item.label)
        .accessibilityAddTraits(selected ? [.isSelected, .isButton] : [.isButton])
    }
}
