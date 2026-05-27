import CoreGraphics
import Foundation
import SwiftUI
import UIKit
import os

/// Resolves layout and design tokens using `LayoutTokenRegistry`, which parses
/// the bundled `schema/*-tokens.json` resources at startup (see AGENTS.md §3.6).
/// Token data is bundled (no runtime fetch), and form-factor updates are
/// propagated through SwiftUI reactivity (`FormFactorObserver` + `Environment`).
public enum LayoutTokenResolver {

    static let logger = Logger(subsystem: "com.nba.sdui", category: "LayoutTokenResolver")
    private static let tokenPrefix = "token:"

    static var missingTokenHook: ((String) -> Void)?

    /// Resolves a layout scalar to points for the given form factor and theme.
    static func cgFloat(
        _ scalar: LayoutScalar?,
        formFactor: String = RequestEnvelope.currentFormFactor,
        theme: String = RequestEnvelope.currentTheme
    ) -> CGFloat {
        CGFloat(intValue(scalar, formFactor: formFactor, theme: theme))
    }

    /// Preserved entry point for existing callers.
    static func resolve(
        _ scalar: LayoutScalar?,
        formFactor: FormFactor = currentFormFactor(),
        theme: String = RequestEnvelope.currentTheme
    ) -> Int {
        intValue(scalar, formFactor: formFactor.rawValue, theme: theme)
    }

    static func intValue(
        _ scalar: LayoutScalar?,
        formFactor: String = RequestEnvelope.currentFormFactor,
        theme: String = RequestEnvelope.currentTheme
    ) -> Int {
        guard let scalar else { return 0 }
        switch scalar {
        case .integer(let value):
            return value
        case .string(let wire):
            return resolveTokenString(wire, formFactor: FormFactor.fromWireValue(formFactor), theme: theme)
        }
    }

    static func resolveSpacing(_ token: String, formFactor: FormFactor = currentFormFactor()) -> Int? {
        guard let name = tokenName(from: token) else { return nil }
        return value(for: LayoutTokenRegistry.spacing[name], formFactor: formFactor)
    }

    static func resolveRadius(_ token: String, formFactor: FormFactor = currentFormFactor()) -> Int? {
        guard let name = tokenName(from: token) else { return nil }
        return value(for: LayoutTokenRegistry.radius[name], formFactor: formFactor)
    }

    static func typography(_ token: String, formFactor: FormFactor = currentFormFactor()) -> TypographySpec? {
        guard let name = tokenName(from: token),
              let variant = LayoutTokenRegistry.typographyVariants[name],
              let category = LayoutTokenRegistry.typographyCategories[variant.categoryRef] else {
            return nil
        }

        let size: Int
        switch formFactor {
        case .phone:
            size = variant.size.phone
        case .tablet:
            size = variant.size.tablet
        case .tv:
            size = variant.size.tv
        }

        return TypographySpec(
            familyRef: category.familyRef,
            weight: category.weight,
            textCase: category.textCase,
            lineHeight: category.lineHeight,
            size: size
        )
    }

    /// Resolve a wire-level `TextVariant` shorthand (e.g. `"score"`, `"labelSmall"`) to a
    /// `TypographySpec` via the bundled `typography-tokens.json` registry.
    ///
    /// The variant enum on the wire is a presentational shorthand for the full token name
    /// `nba.typography.<variant>`; this method bridges the shorthand into the same
    /// registry-driven path used for `token:`-prefixed presentational fields, so renderers
    /// never need a parallel hardcoded sizing table.
    static func typographyForVariant(_ variantName: String, formFactor: FormFactor = currentFormFactor()) -> TypographySpec? {
        return typography("token:nba.typography.\(variantName)", formFactor: formFactor)
    }

    static func shadowSpec(_ token: String) -> ShadowSpec? {
        guard let name = tokenName(from: token) else { return nil }
        return LayoutTokenRegistry.shadows[name]
    }

    static func resolveShadowOrToken(_ value: ShadowOrToken?) -> Shadow? {
        guard let value else { return nil }
        switch value {
        case .shadow(let shadow):
            return shadow
        case .string(let token):
            return tokenToShadow(token)
        }
    }

    static func resolveShadowOrTokens(_ values: [ShadowOrToken]?) -> [Shadow] {
        values?.compactMap { resolveShadowOrToken($0) } ?? []
    }

    static func motionDuration(_ token: String, formFactor: FormFactor = currentFormFactor()) -> Int? {
        guard let name = tokenName(from: token) else { return nil }
        return value(for: LayoutTokenRegistry.motionDuration[name], formFactor: formFactor)
    }

    static func motionEasing(_ token: String) -> String? {
        guard let name = tokenName(from: token) else { return nil }
        return LayoutTokenRegistry.motionEasing[name]
    }

    static func aspectRatio(_ union: AspectRatioUnion?) -> CGFloat? {
        guard let union else { return nil }
        switch union {
        case .double(let value):
            return CGFloat(value)
        case .enumeration(let value):
            switch value {
            case .the169: return 16.0 / 9.0
            case .the43: return 4.0 / 3.0
            case .the11: return 1.0
            case .the32: return 3.0 / 2.0
            case .the219: return 21.0 / 9.0
            }
        }
    }

    public static func currentFormFactor() -> FormFactor {
#if os(tvOS)
        return .tv
#elseif targetEnvironment(macCatalyst)
        return .tablet
#else
        let traits = activeTraitCollection()
        _ = UIDevice.current.orientation
        switch (traits.horizontalSizeClass, traits.verticalSizeClass) {
        case (.compact, .regular):
            return .phone
        case (.regular, .regular):
            return .tablet
        default:
            return .phone
        }
#endif
    }

    private static func resolveTokenString(_ wire: String, formFactor: FormFactor, theme: String) -> Int {
        _ = theme

        if let spacing = resolveSpacing(wire, formFactor: formFactor) { return spacing }
        if let radius = resolveRadius(wire, formFactor: formFactor) { return radius }

        logMissingToken(wire)
        return 0
    }

    private static func tokenToShadow(_ token: String) -> Shadow? {
        guard let spec = shadowSpec(token) else { return nil }
        return Shadow(
            color: spec.color,
            offsetX: Double(spec.offsetX),
            offsetY: Double(spec.offsetY),
            radius: Double(spec.radius),
            type: spec.type.lowercased() == "inner" ? .inner : .drop
        )
    }

    private static func tokenName(from wire: String) -> String? {
        guard wire.hasPrefix(tokenPrefix) else { return nil }
        return String(wire.dropFirst(tokenPrefix.count))
    }

    private static func value(for matrix: FormFactorMatrix<Int>?, formFactor: FormFactor) -> Int? {
        guard let matrix else { return nil }
        switch formFactor {
        case .phone: return matrix.phone
        case .tablet: return matrix.tablet
        case .tv: return matrix.tv
        }
    }

    private static func logMissingToken(_ wire: String) {
        logger.debug("token_resolver_missing: \(wire, privacy: .public)")
        missingTokenHook?(wire)
    }

    private static func activeTraitCollection() -> UITraitCollection {
        let scenes = UIApplication.shared.connectedScenes.compactMap { $0 as? UIWindowScene }
        let keyWindow = scenes
            .flatMap(\.windows)
            .first(where: \.isKeyWindow)

        return keyWindow?.traitCollection ?? UIScreen.main.traitCollection
    }
}

public enum FormFactor: String, CaseIterable {
    case phone
    case tablet
    case tv

    static func fromWireValue(_ rawValue: String) -> FormFactor {
        switch rawValue {
        case FormFactor.tablet.rawValue:
            return .tablet
        case FormFactor.tv.rawValue:
            return .tv
        default:
            return .phone
        }
    }
}

public struct TypographySpec {
    public let familyRef: String
    public let weight: Int
    public let textCase: String
    public let lineHeight: Double
    public let size: Int
}

public final class FormFactorObserver: ObservableObject {
    @Published public var formFactor: FormFactor

    private var notificationObservers: [NSObjectProtocol] = []

    public init(initialFormFactor: FormFactor = LayoutTokenResolver.currentFormFactor()) {
        formFactor = initialFormFactor
        UIDevice.current.beginGeneratingDeviceOrientationNotifications()

        let center = NotificationCenter.default
        notificationObservers.append(
            center.addObserver(
                forName: UIDevice.orientationDidChangeNotification,
                object: nil,
                queue: .main
            ) { [weak self] _ in
                self?.refresh()
            }
        )
        notificationObservers.append(
            center.addObserver(
                forName: FormFactorObserver.traitCollectionDidChangeNotification,
                object: nil,
                queue: .main
            ) { [weak self] _ in
                self?.refresh()
            }
        )
    }

    deinit {
        let center = NotificationCenter.default
        notificationObservers.forEach(center.removeObserver)
        UIDevice.current.endGeneratingDeviceOrientationNotifications()
    }

    public func refresh() {
        formFactor = LayoutTokenResolver.currentFormFactor()
    }

    public static let traitCollectionDidChangeNotification = Notification.Name("com.nba.sdui.formFactor.traitsDidChange")
}

private struct FormFactorEnvironmentKey: EnvironmentKey {
    static let defaultValue: FormFactor = LayoutTokenResolver.currentFormFactor()
}

public extension EnvironmentValues {
    var formFactor: FormFactor {
        get { self[FormFactorEnvironmentKey.self] }
        set { self[FormFactorEnvironmentKey.self] = newValue }
    }
}

private struct FormFactorTraitBridge: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> FormFactorTraitBridgeController {
        FormFactorTraitBridgeController()
    }

    func updateUIViewController(_ uiViewController: FormFactorTraitBridgeController, context: Context) {}
}

private final class FormFactorTraitBridgeController: UIViewController {
    override func traitCollectionDidChange(_ previousTraitCollection: UITraitCollection?) {
        super.traitCollectionDidChange(previousTraitCollection)
        NotificationCenter.default.post(name: FormFactorObserver.traitCollectionDidChangeNotification, object: nil)
    }
}

public extension View {
    func withFormFactorEnvironment(_ observer: FormFactorObserver) -> some View {
        environmentObject(observer)
            .environment(\.formFactor, observer.formFactor)
            .background(FormFactorTraitBridge().frame(width: 0, height: 0))
    }
}
