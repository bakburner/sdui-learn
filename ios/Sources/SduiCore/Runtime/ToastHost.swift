import Foundation
import Observation
import SwiftUI

/// Observable host for transient toast messages.
///
/// Rendered as an overlay at the top of `ScreenShell`. `ActionDispatcher`
/// pushes messages here for two cases:
/// - `toast` action type (§4)
/// - `halt` failure policy with server-provided `failureFeedback.message`
@Observable
@MainActor
public final class ToastHost {

    public struct Toast: Identifiable, Equatable, Sendable {
        public let id = UUID()
        public let message: String
        public let style: Style

        public enum Style: Sendable {
            case info
            case error
        }
    }

    public private(set) var current: Toast?

    private var dismissTask: Task<Void, Never>?

    public init() {}

    public func show(_ message: String, style: Toast.Style = .info, duration: Duration = .seconds(3)) {
        dismissTask?.cancel()
        current = Toast(message: message, style: style)
        dismissTask = Task { [weak self] in
            try? await Task.sleep(for: duration)
            guard !Task.isCancelled else { return }
            self?.current = nil
        }
    }

    public func dismiss() {
        dismissTask?.cancel()
        current = nil
    }
}

// MARK: - View

/// Overlay view that renders the current toast. Place at the top of the view
/// hierarchy inside `ScreenShell`.
public struct ToastOverlay: View {
    @Bindable private var host: ToastHost

    public init(host: ToastHost) {
        self._host = Bindable(wrappedValue: host)
    }

    public var body: some View {
        VStack {
            if let toast = host.current {
                Text(toast.message)
                    .font(.footnote.weight(.medium))
                    .padding(.horizontal, 16)
                    .padding(.vertical, 10)
                    .background(background(for: toast.style), in: Capsule())
                    .foregroundStyle(.white)
                    .shadow(color: Color.black.opacity(0.2), radius: 4, y: 2)
                    .padding(.top, 12)
                    .transition(.move(edge: .top).combined(with: .opacity))
                    .onTapGesture { host.dismiss() }
            }
            Spacer()
        }
        .animation(.snappy, value: host.current)
        .accessibilityElement(children: .combine)
        .allowsHitTesting(host.current != nil)
    }

    private func background(for style: ToastHost.Toast.Style) -> Color {
        switch style {
        case .info: return .black.opacity(0.85)
        case .error: return .red.opacity(0.9)
        }
    }
}
