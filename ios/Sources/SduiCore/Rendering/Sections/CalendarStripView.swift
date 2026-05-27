import SwiftUI
import os

private let logger = Logger(subsystem: "com.nba.sdui", category: "CalendarStrip")

// MARK: - Token references

private enum StripTokens {
    static let labelPrimary = "token:nba.label.primary"
    static let labelSecondary = "token:nba.label.secondary"
    static let labelSelection = "token:nba.label.selection"
    static let labelInteractive = "token:nba.label.interactive"
    static let bgTertiary = "token:nba.bg.tertiary"
    static let spacingSm = "token:nba.spacing.sm"
    static let spacingMd = "token:nba.spacing.md"
}

// MARK: - Shared constants

private let stripCalendar: Calendar = {
    var cal = Calendar(identifier: .gregorian)
    cal.locale = .current
    return cal
}()

private let monthYearFormatter: DateFormatter = {
    let fmt = DateFormatter()
    fmt.calendar = stripCalendar
    fmt.setLocalizedDateFormatFromTemplate("MMMM yyyy")
    return fmt
}()

private let fullDateFormatter: DateFormatter = {
    let fmt = DateFormatter()
    fmt.calendar = stripCalendar
    fmt.dateStyle = .full
    return fmt
}()

private let cellDiameter: CGFloat = 36
private let cellTotalWidth: CGFloat = 44

// MARK: - Cell model

/// Timezone-neutral calendar-day identity for a single strip cell.
/// Stores year/month/day components — never raw `Date` for identity —
/// so the ISO string survives round-trips regardless of device timezone.
struct CalendarCellData: Identifiable {
    let year: Int
    let month: Int
    let day: Int

    var id: String { isoString }

    var isoString: String {
        String(format: "%04d-%02d-%02d", year, month, day)
    }

    var dateComponents: DateComponents {
        DateComponents(year: year, month: month, day: day)
    }
}

// MARK: - Preference keys (iOS 17 fallback for scroll-position tracking)

private struct ScrollOffsetKey: PreferenceKey {
    static var defaultValue: CGFloat = 0
    static func reduce(value: inout CGFloat, nextValue: () -> CGFloat) {
        value = nextValue()
    }
}

private struct ScrollViewWidthKey: PreferenceKey {
    static var defaultValue: CGFloat = 0
    static func reduce(value: inout CGFloat, nextValue: () -> CGFloat) {
        value = nextValue()
    }
}

// MARK: - CalendarStripView

/// Platform-native horizontal date picker for the Games screen.
/// Reads `section.data` directly (no adapter — FormSectionView precedent).
///
/// On cell tap the renderer writes the tapped ET ISO date into
/// `screenState[stateKey]` synchronously, then dispatches
/// `data.onDateSelected` via the shared `onAction` path. The synchronous
/// write ensures `paramBindings` resolution reads the freshly-written value.
///
/// All dates on the wire are ET-anchored ISO `YYYY-MM-DD` strings.
/// Cell arithmetic uses `DateComponents` (timezone-neutral); `Date` is
/// used only for display through `DateFormatter`.
struct CalendarStripView: View {
    let section: Section
    let screenState: ScreenState
    let onAction: (Action) -> Void

    @Environment(\.colorScheme) private var colorScheme
    @State private var displayedMonthYear = ""
    @State private var scrollViewWidth: CGFloat = 0

    var body: some View {
        if let data = section.data,
           let stateKey = data.stateKey {
            let selectedISO = screenState.getString(stateKey) ?? data.selectedDate ?? ""
            let defaultDateISO = data.defaultDate ?? ""
            let cells = Self.generateCells(
                minISO: data.minDate,
                maxISO: data.maxDate,
                fallbackCenter: selectedISO
            )

            let spacing = LayoutTokenResolver.cgFloat(.string(StripTokens.spacingSm))

            VStack(alignment: .leading, spacing: spacing) {
                monthLabel(expandedAction: data.expandedAction)
                dateScrollView(
                    cells: cells,
                    selectedISO: selectedISO,
                    defaultDateISO: defaultDateISO,
                    stateKey: stateKey,
                    dateAction: data.onDateSelected
                )
            }
            .accessibilityElement(children: .contain)
            .onAppear { setMonthLabel(from: selectedISO) }
        } else {
            EmptyView().onAppear {
                logger.warning("CalendarStrip missing data/stateKey id=\(section.id, privacy: .public)")
            }
        }
    }

    // MARK: - Month / year label

    @ViewBuilder
    private func monthLabel(expandedAction: Action?) -> some View {
        let pad = LayoutTokenResolver.cgFloat(.string(StripTokens.spacingMd))
        let label = HStack(spacing: 4) {
            Text(displayedMonthYear)
                .font(.system(size: 16, weight: .semibold))
                .foregroundColor(
                    resolveColor(StripTokens.labelPrimary) ?? .primary
                )
            Image(systemName: "chevron.right")
                .font(.system(size: 12, weight: .semibold))
                .foregroundColor(
                    resolveColor(StripTokens.labelSecondary) ?? .secondary
                )
        }
        .padding(.horizontal, pad)
        .accessibilityAddTraits(.isHeader)
        .accessibilityLabel(displayedMonthYear)

        if let expandedAction {
            Button {
                onAction(expandedAction)
            } label: {
                label
            }
            .buttonStyle(.plain)
            .accessibilityAddTraits(.isButton)
        } else {
            label
        }
    }

    // MARK: - Date scroll view

    @ViewBuilder
    private func dateScrollView(
        cells: [CalendarCellData],
        selectedISO: String,
        defaultDateISO: String,
        stateKey: String,
        dateAction: Action?
    ) -> some View {
        ScrollViewReader { proxy in
            ScrollView(.horizontal, showsIndicators: false) {
                LazyHStack(spacing: 0) {
                    ForEach(cells) { cell in
                        let isSelected = cell.isoString == selectedISO
                        let isDefault = cell.isoString == defaultDateISO

                        Button {
                            withAnimation(.easeInOut(duration: 0.2)) {
                                screenState.set(stateKey, value: cell.isoString)
                            }
                            if let action = dateAction {
                                onAction(action)
                            } else {
                                logger.warning(
                                    "CalendarStrip tap with no onDateSelected id=\(section.id, privacy: .public)"
                                )
                            }
                        } label: {
                            CalendarCellContent(
                                cell: cell,
                                isSelected: isSelected,
                                isDefault: isDefault,
                                colorScheme: colorScheme
                            )
                        }
                        .buttonStyle(.plain)
                        .frame(width: cellTotalWidth)
                        .id(cell.isoString)
                        .accessibilityElement(children: .ignore)
                        .accessibilityLabel(
                            Self.cellAccessibilityLabel(
                                cell: cell,
                                isDefault: isDefault,
                                isSelected: isSelected
                            )
                        )
                        .accessibilityAddTraits(.isButton)
                        .accessibilityAddTraits(isSelected ? .isSelected : [])
                    }
                }
                .background(
                    GeometryReader { geo in
                        Color.clear.preference(
                            key: ScrollOffsetKey.self,
                            value: geo.frame(in: .named("stripScroll")).minX
                        )
                    }
                )
            }
            .coordinateSpace(name: "stripScroll")
            .overlay {
                GeometryReader { geo in
                    Color.clear.preference(
                        key: ScrollViewWidthKey.self,
                        value: geo.size.width
                    )
                }
                .allowsHitTesting(false)
            }
            .onPreferenceChange(ScrollViewWidthKey.self) { scrollViewWidth = $0 }
            .onPreferenceChange(ScrollOffsetKey.self) { offset in
                updateMonthFromOffset(offset, cells: cells)
            }
            .onAppear {
                scroll(to: selectedISO, proxy: proxy, animated: false)
            }
            .onChange(of: selectedISO) { _, newISO in
                scroll(to: newISO, proxy: proxy, animated: true)
                setMonthLabel(from: newISO)
            }
        }
    }

    // MARK: - Helpers

    private func resolveColor(_ token: String) -> Color? {
        ColorTokenResolver.resolve(token, colorScheme: colorScheme)
    }

    private func scroll(to iso: String, proxy: ScrollViewProxy, animated: Bool) {
        guard !iso.isEmpty else { return }
        if animated {
            withAnimation(.easeInOut(duration: 0.25)) {
                proxy.scrollTo(iso, anchor: .center)
            }
        } else {
            proxy.scrollTo(iso, anchor: .center)
        }
    }

    private func setMonthLabel(from iso: String) {
        guard let comps = parseISO(iso),
              let y = comps.year, let m = comps.month,
              let date = stripCalendar.date(from: DateComponents(year: y, month: m, day: 1))
        else { return }
        displayedMonthYear = monthYearFormatter.string(from: date)
    }

    /// iOS 17 fallback for centered-week detection. Uses content offset
    /// + known cell width to find the centered cell index, then derives
    /// the month/year label. iOS 18+ could use `.onScrollGeometryChange`
    /// instead — upgrade when the deployment target advances.
    private func updateMonthFromOffset(_ offset: CGFloat, cells: [CalendarCellData]) {
        guard scrollViewWidth > 0, !cells.isEmpty else { return }
        let center = scrollViewWidth / 2 - offset
        let idx = max(0, min(cells.count - 1, Int(center / cellTotalWidth)))
        let cell = cells[idx]
        guard let date = stripCalendar.date(
            from: DateComponents(year: cell.year, month: cell.month, day: 1)
        ) else { return }
        let label = monthYearFormatter.string(from: date)
        if label != displayedMonthYear { displayedMonthYear = label }
    }

    // MARK: - Cell generation

    static func generateCells(
        minISO: String?,
        maxISO: String?,
        fallbackCenter: String
    ) -> [CalendarCellData] {
        let cal = stripCalendar
        let centerComps = parseISO(fallbackCenter)

        let startComps = minISO.flatMap(parseISO)
            ?? centerComps.flatMap { offset($0, by: -90, calendar: cal) }
            ?? DateComponents(year: 2025, month: 10, day: 1)

        let endComps = maxISO.flatMap(parseISO)
            ?? centerComps.flatMap { offset($0, by: 90, calendar: cal) }
            ?? DateComponents(year: 2026, month: 6, day: 30)

        guard let start = cal.date(from: startComps),
              let end = cal.date(from: endComps),
              start <= end
        else {
            logger.error("CalendarStrip invalid range min=\(minISO ?? "nil") max=\(maxISO ?? "nil")")
            return []
        }

        var cells: [CalendarCellData] = []
        var cursor = start
        while cursor <= end {
            let c = cal.dateComponents([.year, .month, .day], from: cursor)
            if let y = c.year, let m = c.month, let d = c.day {
                cells.append(CalendarCellData(year: y, month: m, day: d))
            }
            guard let next = cal.date(byAdding: .day, value: 1, to: cursor) else { break }
            cursor = next
        }
        return cells
    }

    static func cellAccessibilityLabel(
        cell: CalendarCellData,
        isDefault: Bool,
        isSelected: Bool
    ) -> String {
        guard let date = stripCalendar.date(from: cell.dateComponents) else {
            return cell.isoString
        }
        var label = fullDateFormatter.string(from: date)
        if isDefault { label += ", default date" }
        if isSelected { label += ", selected" }
        return label
    }

    private static func offset(
        _ comps: DateComponents,
        by days: Int,
        calendar: Calendar
    ) -> DateComponents? {
        guard let date = calendar.date(from: comps),
              let shifted = calendar.date(byAdding: .day, value: days, to: date)
        else { return nil }
        return calendar.dateComponents([.year, .month, .day], from: shifted)
    }
}

// MARK: - ISO parsing

private func parseISO(_ iso: String) -> DateComponents? {
    let parts = iso.split(separator: "-")
    guard parts.count >= 3,
          let y = Int(parts[0]),
          let m = Int(parts[1]),
          let d = Int(parts[2])
    else { return nil }
    return DateComponents(year: y, month: m, day: d)
}

// MARK: - Cell content

/// Individual day cell: weekday letter (top) + day number (bottom) with
/// selection circle. Four visual states match the production NBA app idiom:
///   1. Default + selected → interactive-color filled circle, selection-color number
///   2. Default + unselected → interactive-color outlined circle
///   3. Selected (non-default) → tertiary-bg filled circle
///   4. Unselected (non-default) → no circle
private struct CalendarCellContent: View {
    let cell: CalendarCellData
    let isSelected: Bool
    let isDefault: Bool
    let colorScheme: ColorScheme

    var body: some View {
        VStack(spacing: 2) {
            Text(weekdaySymbol)
                .font(.system(size: 11, weight: .medium))
                .foregroundColor(weekdayColor)

            ZStack {
                circle
                Text("\(cell.day)")
                    .font(.system(size: 14, weight: isSelected ? .bold : .regular))
                    .foregroundColor(numberColor)
            }
            .frame(width: cellDiameter, height: cellDiameter)
        }
    }

    // MARK: - Circle

    @ViewBuilder
    private var circle: some View {
        if isSelected && isDefault {
            Circle()
                .fill(resolve(StripTokens.labelInteractive) ?? .blue)
                .transition(.opacity)
        } else if isSelected {
            Circle()
                .fill(resolve(StripTokens.bgTertiary) ?? Color.gray.opacity(0.2))
                .transition(.opacity)
        } else if isDefault {
            Circle()
                .stroke(
                    resolve(StripTokens.labelInteractive) ?? .blue,
                    lineWidth: 1.5
                )
        }
    }

    // MARK: - Colors

    private var weekdayColor: Color {
        if isSelected && isDefault {
            return resolve(StripTokens.labelInteractive) ?? .blue
        }
        return resolve(StripTokens.labelSecondary) ?? .secondary
    }

    private var numberColor: Color {
        if isSelected && isDefault {
            return resolve(StripTokens.labelSelection) ?? .white
        }
        if isSelected {
            return resolve(StripTokens.labelPrimary) ?? .primary
        }
        if isDefault {
            return resolve(StripTokens.labelInteractive) ?? .blue
        }
        return resolve(StripTokens.labelPrimary) ?? .primary
    }

    private var weekdaySymbol: String {
        guard let date = stripCalendar.date(from: cell.dateComponents) else { return "?" }
        let idx = stripCalendar.component(.weekday, from: date) - 1
        return stripCalendar.veryShortStandaloneWeekdaySymbols[idx]
    }

    private func resolve(_ token: String) -> Color? {
        ColorTokenResolver.resolve(token, colorScheme: colorScheme)
    }
}
