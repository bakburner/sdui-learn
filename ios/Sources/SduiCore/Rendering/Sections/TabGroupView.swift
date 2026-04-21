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
                ?? tabs.first?.id
                ?? ""

            VStack(spacing: 0) {
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 16) {
                        ForEach(tabs, id: \.id) { tab in
                            tabButton(
                                tab: tab,
                                isSelected: tab.id == selectedTab,
                                onSelect: {
                                    screenState.set(stateKey, value: tab.id)
                                }
                            )
                        }
                    }
                    .padding(.horizontal)
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
