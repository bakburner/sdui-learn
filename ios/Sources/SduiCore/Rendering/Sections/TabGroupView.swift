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
                // active tab on every selection change. We anchor by the
                // tab's position in the list — tabs in the leading half use
                // `.leading`, tabs in the trailing half use `.trailing` —
                // so the ScrollView clamps at its natural bounds whenever
                // the strip fits the viewport. That keeps leading tabs
                // (e.g. "Featured" on Watch) from losing their first letter
                // when a trailing tab (e.g. "League Pass") is selected.
                // Cropping only kicks in when the strip is genuinely wider
                // than the viewport and a trailing tab is selected — the
                // only case where it's unavoidable.
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
                    VStack(spacing: 0) {
                        ForEach(sections, id: \.id) { childSection in
                            SectionRouter(section: childSection, screenState: screenState, onAction: onAction)
                        }
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .animation(.easeInOut(duration: 0.2), value: selectedTab)
                    .transition(.opacity)
                }
            }
            .accessibilityElement(children: .contain)
        }
    }

    private func scrollToActiveTab(tabs: [TabData], selectedTab: String,
                                   proxy: ScrollViewProxy, animated: Bool) {
        guard let activeIndex = tabs.firstIndex(where: {
            ($0.stateValue ?? $0.id) == selectedTab
        }) ?? (tabs.isEmpty ? nil : 0) else { return }
        let active = tabs[activeIndex]
        // Leading-half tabs clamp to `.leading` (offset 0 when the strip
        // fits), trailing-half tabs clamp to `.trailing`. With (n+1)/2 as
        // the split, an odd-length strip puts the middle tab on the
        // leading side, which keeps Featured-style first tabs un-cropped.
        let anchor: UnitPoint = activeIndex < (tabs.count + 1) / 2 ? .leading : .trailing
        let scroll = { proxy.scrollTo(active.id, anchor: anchor) }
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
