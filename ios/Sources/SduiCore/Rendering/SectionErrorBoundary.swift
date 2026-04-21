import SwiftUI
import os

private let logger = Logger(subsystem: "com.nba.sdui", category: "SectionErrorBoundary")

/// Per-section error boundary. Mirrors Android's
/// [`SectionErrorBoundary`](../../../../../android/sdui-core/src/main/java/com/nba/sdui/core/renderer/SectionErrorBoundary.kt).
///
/// SwiftUI (like Compose) can't wrap view bodies with `try/catch`, so the
/// boundary runs pre-validation on the section's `data` before rendering
/// and also respects explicit error propagation via ``reportError(_:)``.
/// Supports:
/// - `sectionStates.error.hideOnError` → collapses to `EmptyView`.
/// - `sectionStates.error.message`     → server-provided human message.
/// - `sectionStates.error.retryAction` → dispatched via `onAction`.
/// - Retry budget (default 5) to prevent infinite retry loops.
struct SectionErrorBoundary<Content: View>: View {
    let sectionID: String
    let sectionType: String
    let sectionStates: SectionStates?
    let data: [String: Any]?
    let onAction: (Action) -> Void
    let maxRetries: Int
    @ViewBuilder let content: () -> Content

    @State private var retryCount: Int = 0
    @State private var explicitError: String?

    init(
        sectionID: String,
        sectionType: String,
        sectionStates: SectionStates?,
        data: [String: Any]?,
        onAction: @escaping (Action) -> Void,
        maxRetries: Int = 5,
        @ViewBuilder content: @escaping () -> Content
    ) {
        self.sectionID = sectionID
        self.sectionType = sectionType
        self.sectionStates = sectionStates
        self.data = data
        self.onAction = onAction
        self.maxRetries = maxRetries
        self.content = content
    }

    var body: some View {
        let validationError = SectionErrorBoundary.validate(
            sectionID: sectionID,
            sectionType: sectionType,
            data: data
        )
        let activeError = explicitError ?? validationError

        if let message = activeError {
            let _ = logger.error("section render failed id=\(self.sectionID, privacy: .public) type=\(self.sectionType, privacy: .public) error=\(message, privacy: .public)")

            let errorConfig = sectionStates?.error
            if errorConfig?.hideOnError == true {
                EmptyView()
            } else {
                SectionErrorCard(
                    message: errorConfig?.message ?? message,
                    retryAction: errorConfig?.retryAction,
                    canRetry: retryCount < maxRetries,
                    onRetry: {
                        retryCount += 1
                        explicitError = nil
                    },
                    onAction: onAction
                )
            }
        } else {
            content()
        }
    }

    /// Pre-validation rules shared with Android's validateSection. Returns
    /// a human-readable message when rendering should be suppressed.
    static func validate(sectionID: String, sectionType: String, data: [String: Any]?) -> String? {
        if sectionType == OverlayType.atomicComposite.rawValue, data?["ui"] == nil {
            return "AtomicComposite section \(sectionID) has no ui element"
        }
        return nil
    }
}

// Extension access so SectionLayout can nudge the boundary into error
// state when e.g. a decode fails outside the view body.
extension SectionErrorBoundary {
    func reportError(_ message: String) {
        explicitError = message
    }
}

private struct SectionErrorCard: View {
    let message: String
    let retryAction: Action?
    let canRetry: Bool
    let onRetry: () -> Void
    let onAction: (Action) -> Void

    var body: some View {
        VStack(spacing: 12) {
            Image(systemName: "exclamationmark.triangle.fill")
                .font(.title)
                .foregroundStyle(.orange)
            Text(message)
                .font(.body)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
            if canRetry, let retryAction {
                Button {
                    onAction(retryAction)
                    onRetry()
                } label: {
                    Text("Try Again")
                }
                .buttonStyle(.bordered)
            }
        }
        .padding(24)
        .frame(maxWidth: .infinity)
        .background(
            RoundedRectangle(cornerRadius: 12, style: .continuous)
                .fill(Color.red.opacity(0.08))
        )
        .padding(.vertical, 4)
    }
}
