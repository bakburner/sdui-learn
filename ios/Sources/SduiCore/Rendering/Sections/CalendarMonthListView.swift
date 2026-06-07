import SwiftUI
import os

private let logger = Logger(subsystem: "com.nba.sdui", category: "CalendarMonthList")

private enum CalendarMonthListTokens {
    static let labelPrimary = "token:nba.label.primary"
    static let labelSecondary = "token:nba.label.secondary"
    static let labelSelection = "token:nba.label.selection"
    static let labelInteractive = "token:nba.label.interactive"
    static let accentBrand = "token:nba.label.accent.brand"
    static let bgTertiary = "token:nba.bg.tertiary"
    static let divider = "token:nba.divider.subtle"
    static let bgSecondary = "token:nba.bg.secondary"
    static let spacingXS = "token:nba.spacing.xs"
    static let spacingSm = "token:nba.spacing.sm"
    static let spacingMd = "token:nba.spacing.md"
    static let titleSmall = "token:nba.typography.titleSmall"
    static let labelSmall = "token:nba.typography.labelSmall"
    static let bodySmall = "token:nba.typography.bodySmall"
}

private let monthListCalendar: Calendar = {
    var calendar = Calendar(identifier: .gregorian)
    calendar.locale = .current
    calendar.firstWeekday = Calendar.current.firstWeekday
    return calendar
}()

private let monthHeaderFormatter: DateFormatter = {
    let formatter = DateFormatter()
    formatter.calendar = monthListCalendar
    formatter.setLocalizedDateFormatFromTemplate("MMMM yyyy")
    return formatter
}()

private let fullDateFormatter: DateFormatter = {
    let formatter = DateFormatter()
    formatter.calendar = monthListCalendar
    formatter.dateStyle = .full
    return formatter
}()

private struct CalendarMonthData: Identifiable {
    let year: Int
    let month: Int
    let dayCells: [CalendarGridCell]

    var id: String { String(format: "%04d-%02d", year, month) }

    var monthStartDate: Date? {
        monthListCalendar.date(from: DateComponents(year: year, month: month, day: 1))
    }
}

private struct CalendarGridCell: Identifiable {
    let id: String
    let day: CalendarDayIdentity?
}

private struct CalendarDayIdentity: Identifiable {
    let year: Int
    let month: Int
    let day: Int

    var id: String { isoDate }
    var isoDate: String { String(format: "%04d-%02d-%02d", year, month, day) }
    var dateComponents: DateComponents { DateComponents(year: year, month: month, day: day) }
}

struct CalendarMonthListView: View {
    let section: Section
    let screenState: ScreenState
    let onAction: (Action) -> Void

    @Environment(\.colorScheme) private var colorScheme

    var body: some View {
        if let data = section.data,
           let stateKey = data.stateKey {
            let selectedISO = screenState.getString(stateKey) ?? data.selectedDate ?? data.defaultDate ?? ""
            let defaultDateISO = data.defaultDate ?? ""
            let months = Self.buildMonths(data: data, selectedISO: selectedISO)

            ScrollViewReader { proxy in
                ScrollView(.vertical, showsIndicators: true) {
                    LazyVStack(alignment: .leading, spacing: verticalMonthSpacing, pinnedViews: [.sectionHeaders]) {
                        ForEach(months) { month in
                            SwiftUI.Section {
                                VStack(alignment: .leading, spacing: sectionSpacing) {
                                    weekdayHeaderRow
                                    monthGrid(
                                        stateKey: stateKey,
                                        dayCells: month.dayCells,
                                        data: data,
                                        selectedISO: selectedISO,
                                        defaultDateISO: defaultDateISO
                                    )
                                }
                                .id(month.id)
                            } header: {
                                monthHeader(month)
                            }
                        }
                    }
                    .padding(.horizontal, horizontalPadding)
                    .padding(.vertical, verticalPadding)
                }
                .onAppear {
                    scrollToMonth(containing: selectedISO, in: months, proxy: proxy, animated: false)
                }
                .onChange(of: selectedISO) { _, newISO in
                    scrollToMonth(containing: newISO, in: months, proxy: proxy, animated: true)
                }
            }
        } else {
            EmptyView().onAppear {
                logger.warning("CalendarMonthList missing data/stateKey id=\(section.id, privacy: .public)")
            }
        }
    }

    private var horizontalPadding: CGFloat {
        LayoutTokenResolver.cgFloat(.string(CalendarMonthListTokens.spacingMd))
    }

    private var verticalPadding: CGFloat {
        LayoutTokenResolver.cgFloat(.string(CalendarMonthListTokens.spacingSm))
    }

    private var verticalMonthSpacing: CGFloat {
        LayoutTokenResolver.cgFloat(.string(CalendarMonthListTokens.spacingMd))
    }

    private var sectionSpacing: CGFloat {
        LayoutTokenResolver.cgFloat(.string(CalendarMonthListTokens.spacingSm))
    }

    @ViewBuilder
    private func monthHeader(_ month: CalendarMonthData) -> some View {
        let label = month.monthStartDate.map(monthHeaderFormatter.string(from:)) ?? month.id
        let titleColor = resolveColor(CalendarMonthListTokens.labelPrimary) ?? .primary
        let divider = resolveColor(CalendarMonthListTokens.divider) ?? Color.secondary.opacity(0.25)

        VStack(alignment: .leading, spacing: 0) {
            Text(label)
                .font(tokenFont(CalendarMonthListTokens.titleSmall, fallback: .headline))
                .foregroundColor(titleColor)
                .padding(.vertical, sectionSpacing)
            Rectangle()
                .fill(divider)
                .frame(height: 1)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(resolveColor(CalendarMonthListTokens.bgSecondary) ?? Color(.systemBackground))
        .accessibilityAddTraits(.isHeader)
    }

    private var weekdayHeaderRow: some View {
        LazyVGrid(columns: Array(repeating: GridItem(.flexible()), count: 7), spacing: sectionSpacing) {
            ForEach(weekdaySymbols, id: \.self) { symbol in
                Text(symbol)
                    .font(tokenFont(CalendarMonthListTokens.labelSmall, fallback: .caption))
                    .foregroundColor(resolveColor(CalendarMonthListTokens.labelSecondary) ?? .secondary)
                    .frame(maxWidth: .infinity)
            }
        }
        .padding(.top, LayoutTokenResolver.cgFloat(.string(CalendarMonthListTokens.spacingXS)))
    }

    @ViewBuilder
    private func monthGrid(
        stateKey: String,
        dayCells: [CalendarGridCell],
        data: SectionData,
        selectedISO: String,
        defaultDateISO: String
    ) -> some View {
        LazyVGrid(columns: Array(repeating: GridItem(.flexible()), count: 7), spacing: sectionSpacing) {
            ForEach(dayCells) { cell in
                if let day = cell.day {
                    let metadata = data.dateMetadata?[day.isoDate]
                    MonthDayCellView(
                        day: day,
                        metadata: metadata,
                        isSelected: day.isoDate == selectedISO,
                        isDefault: day.isoDate == defaultDateISO,
                        colorScheme: colorScheme
                    ) {
                        screenState.set(stateKey, value: day.isoDate)
                        if let action = data.onDateSelected {
                            onAction(action)
                        } else {
                            logger.warning("CalendarMonthList tap with no onDateSelected id=\(section.id, privacy: .public)")
                        }
                    }
                } else {
                    Color.clear
                        .frame(height: 42)
                        .accessibilityHidden(true)
                }
            }
        }
    }

    private var weekdaySymbols: [String] {
        let all = monthListCalendar.veryShortStandaloneWeekdaySymbols
        let start = max(0, monthListCalendar.firstWeekday - 1)
        let tail = Array(all[start...])
        let head = Array(all[..<start])
        return tail + head
    }

    private func scrollToMonth(
        containing isoDate: String,
        in months: [CalendarMonthData],
        proxy: ScrollViewProxy,
        animated: Bool
    ) {
        guard let comps = parseISODate(isoDate),
              let y = comps.year,
              let m = comps.month else { return }
        let monthID = String(format: "%04d-%02d", y, m)
        guard months.contains(where: { $0.id == monthID }) else { return }
        let scroll = { proxy.scrollTo(monthID, anchor: .top) }
        if animated {
            withAnimation(.easeInOut(duration: 0.2)) { scroll() }
        } else {
            scroll()
        }
    }

    private func resolveColor(_ token: String?) -> Color? {
        ColorTokenResolver.resolve(token, colorScheme: colorScheme)
    }

    private func tokenFont(_ token: String, fallback: Font) -> Font {
        guard let spec = LayoutTokenResolver.typography(token) else { return fallback }
        return .system(size: CGFloat(spec.size), weight: fontWeight(for: spec.weight))
    }

    private func fontWeight(for weight: Int) -> Font.Weight {
        switch weight {
        case ..<450:
            return .regular
        case 450..<600:
            return .medium
        case 600..<700:
            return .semibold
        default:
            return .bold
        }
    }

    private static func buildMonths(data: SectionData, selectedISO: String) -> [CalendarMonthData] {
        let selected = parseISODate(selectedISO)
            ?? parseISODate(data.defaultDate ?? "")
            ?? DateComponents(year: 2026, month: 1, day: 1)

        var minComps = parseISODate(data.minDate ?? "")
            ?? offsetDays(selected, by: -180)
            ?? selected
        var maxComps = parseISODate(data.maxDate ?? "")
            ?? offsetDays(selected, by: 180)
            ?? selected

        guard let minDate = monthListCalendar.date(from: minComps),
              let maxDate = monthListCalendar.date(from: maxComps) else {
            return []
        }

        if minDate > maxDate {
            swap(&minComps, &maxComps)
        }

        guard let startMonth = monthListCalendar.date(
            from: DateComponents(year: minComps.year, month: minComps.month, day: 1)
        ),
        let endMonth = monthListCalendar.date(
            from: DateComponents(year: maxComps.year, month: maxComps.month, day: 1)
        ) else {
            return []
        }

        var months: [CalendarMonthData] = []
        var cursor = startMonth
        while cursor <= endMonth {
            let comps = monthListCalendar.dateComponents([.year, .month], from: cursor)
            guard let year = comps.year, let month = comps.month else { break }
            months.append(
                CalendarMonthData(
                    year: year,
                    month: month,
                    dayCells: buildDayCells(year: year, month: month)
                )
            )
            guard let next = monthListCalendar.date(byAdding: .month, value: 1, to: cursor) else { break }
            cursor = next
        }
        return months
    }

    private static func buildDayCells(year: Int, month: Int) -> [CalendarGridCell] {
        guard let first = monthListCalendar.date(from: DateComponents(year: year, month: month, day: 1)),
              let dayRange = monthListCalendar.range(of: .day, in: .month, for: first) else {
            return []
        }

        let weekday = monthListCalendar.component(.weekday, from: first)
        let leading = (weekday - monthListCalendar.firstWeekday + 7) % 7

        var cells: [CalendarGridCell] = []
        cells.reserveCapacity(leading + dayRange.count)
        for i in 0..<leading {
            cells.append(CalendarGridCell(id: "\(year)-\(month)-p\(i)", day: nil))
        }
        for day in dayRange {
            let identity = CalendarDayIdentity(year: year, month: month, day: day)
            cells.append(CalendarGridCell(id: identity.id, day: identity))
        }
        return cells
    }
}

private struct MonthDayCellView: View {
    let day: CalendarDayIdentity
    let metadata: DateMetadatum?
    let isSelected: Bool
    let isDefault: Bool
    let colorScheme: ColorScheme
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            VStack(spacing: 4) {
                ZStack {
                    dayCircle
                    Text("\(day.day)")
                        .font(dayFont)
                        .foregroundColor(dayNumberColor)
                }
                .frame(width: 32, height: 32)

                Circle()
                    .fill(dotColor)
                    .frame(width: 5, height: 5)
                    .opacity(shouldShowDot ? 1 : 0)
            }
            .frame(maxWidth: .infinity, minHeight: 42)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .accessibilityLabel(accessibilityLabel)
        .accessibilityAddTraits(isSelected ? [.isButton, .isSelected] : .isButton)
    }

    private var dayFont: Font {
        guard let spec = LayoutTokenResolver.typography(CalendarMonthListTokens.bodySmall) else {
            return .system(size: 13, weight: isSelected ? .bold : .regular)
        }
        return .system(
            size: CGFloat(spec.size),
            weight: isSelected ? .bold : .regular
        )
    }

    @ViewBuilder
    private var dayCircle: some View {
        if isSelected && isDefault {
            Circle().fill(resolveColor(CalendarMonthListTokens.labelInteractive) ?? .blue)
        } else if isSelected {
            Circle().fill(resolveColor(CalendarMonthListTokens.bgTertiary) ?? Color.gray.opacity(0.2))
        } else if isDefault {
            Circle().stroke(resolveColor(CalendarMonthListTokens.labelInteractive) ?? .blue, lineWidth: 1.5)
        }
    }

    private var dayNumberColor: Color {
        if isSelected && isDefault {
            return resolveColor(CalendarMonthListTokens.labelSelection) ?? .white
        }
        if isSelected {
            return resolveColor(CalendarMonthListTokens.labelPrimary) ?? .primary
        }
        if isDefault {
            return resolveColor(CalendarMonthListTokens.labelInteractive) ?? .blue
        }
        return resolveColor(CalendarMonthListTokens.labelPrimary) ?? .primary
    }

    private var shouldShowDot: Bool {
        (metadata?.gameCount ?? 0) > 0
    }

    private var dotColor: Color {
        if metadata?.hasTeamGame == true {
            return resolveColor(CalendarMonthListTokens.accentBrand) ?? .blue
        }
        return resolveColor(CalendarMonthListTokens.labelSecondary) ?? .secondary
    }

    private var accessibilityLabel: String {
        guard let date = monthListCalendar.date(from: day.dateComponents) else { return day.isoDate }
        var label = fullDateFormatter.string(from: date)
        if isDefault { label += ", default date" }
        if isSelected { label += ", selected" }
        if shouldShowDot {
            label += metadata?.hasTeamGame == true ? ", team game" : ", games scheduled"
        }
        return label
    }

    private func resolveColor(_ token: String) -> Color? {
        ColorTokenResolver.resolve(token, colorScheme: colorScheme)
    }
}

private func parseISODate(_ iso: String) -> DateComponents? {
    let parts = iso.split(separator: "-")
    guard parts.count >= 3,
          let y = Int(parts[0]),
          let m = Int(parts[1]),
          let d = Int(parts[2]) else {
        return nil
    }
    return DateComponents(year: y, month: m, day: d)
}

private func offsetDays(_ comps: DateComponents, by days: Int) -> DateComponents? {
    guard let date = monthListCalendar.date(from: comps),
          let shifted = monthListCalendar.date(byAdding: .day, value: days, to: date) else {
        return nil
    }
    return monthListCalendar.dateComponents([.year, .month, .day], from: shifted)
}
