import SwiftUI

/// TabGroup renderer — owns the selected-tab state. The selection must
/// persist locally across server refreshes, and nested sections need the
/// current tab to resolve their content.
/// The selected tab ID is mirrored into ``ScreenState`` under the
/// server-declared `stateKey` so mutate/refresh actions can react to
/// it (`paramBindings` resolve `{{form_selected_tab}}` etc.).
struct TabGroupView: View {
    let section: Section
    let screenState: ScreenState
    let onAction: (Action) -> Void

    var body: some View {
        if let data = section.data, let tabs = data.tabs {
            let stateKey = data.stateKey ?? "tab"
            let selectedTab = screenState.getString(stateKey)
                ?? data.defaultTab
                ?? tabs.first?.stateValue
                ?? tabs.first?.id
                ?? ""

            VStack(spacing: 0) {
                // ScrollViewReader pins the strip's scroll offset to the
                // active tab on every selection change. Without this, tapping
                // a trailing tab (e.g. "League Pass" on Watch) lets SwiftUI's
                // implicit "bring focused button fully into view" behavior
                // shift the inner ScrollView's offset, which on iOS settled
                // with the leading tab ("Featured") cropped off the leading
                // edge. Anchoring on `.center` lets the ScrollView clamp at
                // its bounds — leading-tab selections stay clamped to the
                // leading edge and trailing-tab selections clamp to the
                // trailing edge, so all tabs remain visible whenever the
                // strip's natural width fits the screen, and the active tab
                // is always centered when it doesn't.
                ScrollViewReader { proxy in
                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 16) {
                            ForEach(tabs, id: \.id) { tab in
                                tabButton(
                                    tab: tab,
                                    isSelected: (tab.stateValue ?? tab.id) == selectedTab,
                                    onSelect: {
                                        screenState.set(stateKey, value: tab.stateValue ?? tab.id)
                                    }
                                )
                                .id(tab.id)
                            }
                        }
                        .padding(.horizontal)
                    }
                    .onAppear {
                        scrollToActiveTab(tabs: tabs, selectedTab: selectedTab, proxy: proxy, animated: false)
                    }
                    .onChange(of: selectedTab) { _, newValue in
                        scrollToActiveTab(tabs: tabs, selectedTab: newValue, proxy: proxy, animated: true)
                    }
                }

                if let tabContents = data.tabContents, let sections = tabContents[selectedTab] {
                    ForEach(Array(sections.enumerated()), id: \.offset) { _, childSection in
                        SectionRouter(section: childSection, screenState: screenState, onAction: onAction)
                    }
                }
            }
            .accessibilityElement(children: .contain)
        }
    }

    private func scrollToActiveTab(tabs: [TabData], selectedTab: String,
                                   proxy: ScrollViewProxy, animated: Bool) {
        guard let active = tabs.first(where: { ($0.stateValue ?? $0.id) == selectedTab })
                ?? tabs.first else { return }
        let scroll = { proxy.scrollTo(active.id, anchor: .center) }
        if animated {
            withAnimation(.easeInOut(duration: 0.2)) { scroll() }
        } else {
            scroll()
        }
    }

    @ViewBuilder
    private func tabButton(tab: TabData, isSelected: Bool, onSelect: @escaping () -> Void) -> some View {
        Button(action: onSelect) {
            Text(tab.label)
                .fontWeight(isSelected ? .bold : .regular)
                .foregroundColor(isSelected ? .primary : .secondary)
                .padding(.vertical, 8)
                .overlay(
                    Rectangle()
                        .frame(height: 2)
                        .foregroundColor(isSelected ? .accentColor : .clear),
                    alignment: .bottom
                )
        }
        .buttonStyle(.plain)
        .accessibilityAddTraits(isSelected ? [.isSelected, .isButton] : .isButton)
    }
}
