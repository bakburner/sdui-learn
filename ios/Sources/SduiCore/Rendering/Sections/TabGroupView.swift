import SwiftUI
import os

private let logger = Logger(subsystem: "com.nba.sdui", category: "TabGroup")

/// Native tab-row tokens (aligned with server `secondaryStripSurface`).
private enum TabStripTokens {
    static let labelPrimary = "token:nba.label.primary"
    static let labelSecondary = "token:nba.label.secondary"
    static let accentBrand = "token:nba.label.accent.brand"
    static let divider = "token:nba.divider.moderate"
    static let padH = "token:nba.spacing.md"
    static let padV = "token:nba.spacing.sm"
}

/// TabGroup — thin host for tabbed section routing.
///
/// Server-owned: ``Section/surface``, ``Section/subsections`` (per-tab mutate),
/// tab metadata and ``TabGroupData/tabContents``. Optional ``TabGroupData/ui`` is
/// the tab header only.
///
/// Platform-native tab controls when `ui` is absent are client-realized
/// presentation; selection still dispatches declared subsection actions.
struct TabGroupView: View {
    let section: Section
    let screenState: ScreenState
    let onAction: (Action) -> Void

    @Environment(\.colorScheme) private var colorScheme

    var body: some View {
        if let data = section.data,
           let stateKey = data.stateKey,
           let tabs = data.tabs,
           !tabs.isEmpty,
           let selectedTab = screenState.getString(stateKey)
               ?? data.defaultTab
               ?? tabs.first?.stateValue
               ?? tabs.first?.id {

            VStack(spacing: 0) {
                if let headerUi = data.ui {
                    AtomicRouter(element: headerUi, screenState: screenState, onAction: onAction, depth: 0)
                } else {
                    nativeTabBar(tabs: tabs, selectedTab: selectedTab)
                }

                if let tabContents = data.tabContents, let sections = tabContents[selectedTab] {
                    ForEach(sections, id: \.id) { childSection in
                        SectionRouter(section: childSection, screenState: screenState, onAction: onAction)
                    }
                    .animation(.easeInOut(duration: 0.2), value: selectedTab)
                    .transition(.opacity)
                }
            }
            .accessibilityElement(children: .contain)
        }
    }

    @ViewBuilder
    private func nativeTabBar(tabs: [TabData], selectedTab: String) -> some View {
        let colors = tabStripColors
        let padH = LayoutTokenResolver.cgFloat(.string(TabStripTokens.padH))
        let padV = LayoutTokenResolver.cgFloat(.string(TabStripTokens.padV))

        ScrollViewReader { proxy in
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 0) {
                    ForEach(tabs, id: \.id) { tab in
                        let tabId = tab.id
                        let value = tab.stateValue ?? tab.id
                        let isSelected = value == selectedTab
                        Button {
                            dispatchTabSelect(tabId: tabId, stateValue: value)
                        } label: {
                            Text(tab.label)
                                .fontWeight(isSelected ? .bold : .regular)
                                .foregroundColor(isSelected ? colors.primary : colors.secondary)
                                .padding(.horizontal, padH)
                                .padding(.vertical, padV)
                                .overlay(alignment: .bottom) {
                                    Rectangle()
                                        .frame(height: 2)
                                        .foregroundColor(isSelected ? colors.accent : .clear)
                                }
                        }
                        .buttonStyle(.plain)
                        .accessibilityAddTraits(isSelected ? [.isSelected, .isButton] : .isButton)
                        .id(tabId)
                    }
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .overlay(alignment: .bottom) {
                Rectangle()
                    .frame(height: 1)
                    .foregroundColor(colors.divider)
            }
            .onAppear {
                scrollToActiveTab(tabs: tabs, selectedTab: selectedTab, proxy: proxy, animated: false)
            }
            .onChange(of: selectedTab) { _, newValue in
                scrollToActiveTab(tabs: tabs, selectedTab: newValue, proxy: proxy, animated: true)
            }
        }
    }

    private var tabStripColors: (primary: Color, secondary: Color, accent: Color, divider: Color) {
        let primary = ColorTokenResolver.resolve(TabStripTokens.labelPrimary, colorScheme: colorScheme) ?? .primary
        let secondary = ColorTokenResolver.resolve(TabStripTokens.labelSecondary, colorScheme: colorScheme) ?? .secondary
        let accent = ColorTokenResolver.resolve(TabStripTokens.accentBrand, colorScheme: colorScheme) ?? Color.accentColor
        let divider = ColorTokenResolver.resolve(TabStripTokens.divider, colorScheme: colorScheme) ?? Color.secondary.opacity(0.3)
        return (primary, secondary, accent, divider)
    }

    private func dispatchTabSelect(tabId: String, stateValue: String) {
        if let action = SectionInteractions.subsectionPrimaryAction(for: section, subsectionID: tabId) {
            onAction(action)
        } else {
            guard let stateKey = section.data?.stateKey else {
                logger.warning("missing subsection mutate action and stateKey sectionId=\(section.id, privacy: .public) tabId=\(tabId, privacy: .public)")
                return
            }
            logger.warning("missing subsection mutate action sectionId=\(section.id, privacy: .public) tabId=\(tabId, privacy: .public); falling back to tab stateValue")
            screenState.set(stateKey, value: stateValue)
        }
    }

    private func scrollToActiveTab(
        tabs: [TabData],
        selectedTab: String,
        proxy: ScrollViewProxy,
        animated: Bool
    ) {
        guard let activeIndex = tabs.firstIndex(where: {
            ($0.stateValue ?? $0.id) == selectedTab
        }) ?? (tabs.isEmpty ? nil : 0) else { return }
        let active = tabs[activeIndex]
        let anchor: UnitPoint = activeIndex < (tabs.count + 1) / 2 ? .leading : .trailing
        let scroll = { proxy.scrollTo(active.id, anchor: anchor) }
        if animated {
            withAnimation(.easeInOut(duration: 0.2)) { scroll() }
        } else {
            scroll()
        }
    }
}
