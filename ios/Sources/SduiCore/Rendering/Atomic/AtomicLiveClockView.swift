import SwiftUI
import os

private let logger = Logger(subsystem: "com.nba.sdui", category: "AtomicLiveClock")

/// Renders a `LiveClock` atomic element. Carries the server-provided
/// snapshot (`snapshotSeconds` at `snapshotAt`) plus the `isRunning`
/// flag, and locally interpolates the displayed value between Ably
/// updates.
///
/// While `isRunning == true`, a `TimelineView` drives a ~10Hz update
/// schedule — coarse enough for battery, fine enough that the visible
/// "1" digit transitions feel natural. SwiftUI suspends `TimelineView`
/// updates when the view is offscreen, so no additional lifecycle
/// plumbing is required.
///
/// The displayed text inherits `variant` (defaulting to `.score`),
/// `color`, and the rest of the standard text styling; the box model
/// (padding / background / cornerRadius / shadow / ...) is applied via
/// `.atomicBox(...)` just like every other atomic primitive.
struct AtomicLiveClockView: View {
    let element: AtomicElement

    @Environment(\.colorScheme) private var colorScheme
    @Environment(\.compositeContent) private var compositeContent

    var body: some View {
        TimelineView(.periodic(from: .now, by: tickInterval)) { context in
            Text(display(at: context.date))
                .font(font(for: resolvedVariant))
                .foregroundColor(ColorTokenResolver.resolve(element.color, colorScheme: colorScheme))
                .monospacedDigit()
                .sduiAccessibility(element.accessibility, fallbackLabel: display(at: .now))
                .atomicBox(element, screenState: ScreenState(), onAction: { _ in })
        }
    }

    // MARK: - Computation

    /// Resolved tuple of `(snapshotSeconds, snapshotAt, isRunning)`.
    /// When `bindRef` is set it points at an object inside the enclosing
    /// composite's `data.content` with those three keys — that lets the
    /// server push a single `{clock: {...}}` snapshot on every tick
    /// instead of threading three independent binding paths.
    private var resolvedState: (snapshot: Int, at: Date?, running: Bool) {
        if let dict = BindRefResolver.resolveDictionary(bindRef: element.bindRef, in: compositeContent) {
            let snapshot = (dict["snapshotSeconds"] as? Int) ?? intValue(dict["snapshotSeconds"]) ?? (element.snapshotSeconds ?? 0)
            let at = parseDate(dict["snapshotAt"]) ?? element.snapshotAt
            let running = (dict["isRunning"] as? Bool) ?? element.isRunning ?? false
            return (snapshot, at, running)
        }
        return (element.snapshotSeconds ?? 0, element.snapshotAt, element.isRunning ?? false)
    }

    private func intValue(_ any: Any?) -> Int? {
        if let i = any as? Int { return i }
        if let d = any as? Double { return Int(d) }
        if let s = any as? String { return Int(s) }
        return nil
    }

    /// Accept either a `Date` (when `JSONAny` successfully decoded an
    /// ISO-8601 string) or a raw string the server re-emitted.
    private func parseDate(_ any: Any?) -> Date? {
        if let d = any as? Date { return d }
        if let s = any as? String {
            let fmt = ISO8601DateFormatter()
            fmt.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
            if let d = fmt.date(from: s) { return d }
            fmt.formatOptions = [.withInternetDateTime]
            return fmt.date(from: s)
        }
        return nil
    }

    /// Tick cadence. When the clock is not running we only need a single
    /// update (`TimelineView` still requires a positive interval; an hour
    /// is effectively never for a paused clock), which saves the render
    /// loop from waking up.
    private var tickInterval: TimeInterval {
        resolvedState.running ? 0.1 : 3600
    }

    private func display(at date: Date) -> String {
        let state = resolvedState
        let snapshotValue = Double(state.snapshot)
        let directionDown = (element.tickDirection ?? .down) == .down

        let displayed: Double
        if state.running, let snapshotAt = state.at {
            let elapsed = max(0, date.timeIntervalSince(snapshotAt))
            displayed = directionDown ? snapshotValue - elapsed : snapshotValue + elapsed
        } else {
            displayed = snapshotValue
        }

        let clamped = clamp(displayed, directionDown: directionDown)
        return format(seconds: clamped)
    }

    private func clamp(_ value: Double, directionDown: Bool) -> Double {
        if let stop = element.stopAtSeconds.map(Double.init) {
            return directionDown ? max(value, stop) : min(value, stop)
        }
        return directionDown ? max(value, 0) : value
    }

    private func format(seconds: Double) -> String {
        let total = max(0, Int(seconds.rounded(.down)))
        let fmt = element.format ?? .mSs
        switch fmt {
        case .hMmSs:
            let h = total / 3600
            let m = (total % 3600) / 60
            let s = total % 60
            return String(format: "%d:%02d:%02d", h, m, s)
        case .mmSs:
            let m = total / 60
            let s = total % 60
            return String(format: "%02d:%02d", m, s)
        case .mSs:
            let m = total / 60
            let s = total % 60
            return String(format: "%d:%02d", m, s)
        }
    }

    // MARK: - Typography

    private var resolvedVariant: TextVariant {
        guard let raw = element.variant else { return .score }
        if let parsed = TextVariant(rawValue: raw) { return parsed }
        logger.debug("variant_resolver_missing: variant=\(raw, privacy: .public) elementId=\(element.id ?? "nil", privacy: .public)")
        return .score
    }

    private func font(for variant: TextVariant) -> Font {
        switch variant {
        case .displayLarge:   return .system(size: 57, weight: .heavy)
        case .displayMedium:  return .system(size: 45, weight: .heavy)
        case .displaySmall:   return .system(size: 36, weight: .bold)
        case .headlineLarge:  return .system(size: 32, weight: .bold)
        case .headlineMedium: return .system(size: 28, weight: .bold)
        case .headlineSmall:  return .system(size: 24, weight: .bold)
        case .titleLarge:     return .system(size: 22, weight: .medium)
        case .titleMedium:    return .system(size: 16, weight: .medium)
        case .titleSmall:     return .system(size: 14, weight: .medium)
        case .bodyLarge:      return .system(size: 16, weight: .regular)
        case .bodyMedium:     return .system(size: 14, weight: .regular)
        case .bodySmall:      return .system(size: 12, weight: .regular)
        case .labelLarge:     return .system(size: 14, weight: .medium)
        case .labelMedium:    return .system(size: 12, weight: .medium)
        case .labelSmall:     return .system(size: 11, weight: .medium)
        case .score:          return .system(size: 48, weight: .bold, design: .rounded)
        }
    }
}
