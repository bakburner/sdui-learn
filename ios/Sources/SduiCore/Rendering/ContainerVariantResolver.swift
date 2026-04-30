import SwiftUI
import os

private let logger = Logger(subsystem: "com.nba.sdui", category: "ContainerVariantResolver")

/// Wire-level container variant vocabulary. Each case maps to a platform-native
/// realization via `ContainerVariantSpec`. Unknown server strings fall through
/// to the primitive's default rendering.
enum ContainerVariant: String {
    case hero
    case grouped
}

/// Semantic surface role. Mapped to a concrete SwiftUI `Color` at render time
/// so the same spec can adapt to dark mode via UIKit's semantic colors.
enum SurfaceRole {
    case systemGroupedBackground
    case secondarySystemGroupedBackground
    case tertiarySystemGroupedBackground
    case accent
    case custom(Color)
}

/// Policy applied when inline style props attempt to override a variant's
/// defaults. `.allow` lets the inline value win; `.lock` keeps the variant
/// default and emits a debug log.
enum OverridePolicy: String {
    case allow
    case lock
}

/// Decorative gradient layered over a container's background color (hero tier).
struct GradientOverlay {
    let topColor: Color
    let bottomColor: Color
}

/// Platform-native realization of a container variant for the iOS client.
/// Hand-authored; mirrors the iOS column of `schema/style-tokens.json`.
struct ContainerVariantSpec {
    let cornerRadius: CGFloat?
    let backgroundRole: SurfaceRole?
    let shadow: Shadow?
    let gradientOverlay: GradientOverlay?
    let fillWidth: Bool?
    let border: (width: CGFloat, role: SurfaceRole)?
    let overrideMatrix: [String: OverridePolicy]
}

/// Resolves a wire `variant` string into a `ContainerVariantSpec`. The spec
/// table is inlined so the library carries no runtime resource dependency;
/// update it whenever `schema/style-tokens.json` changes.
enum ContainerVariantResolver {

    /// Returns the realized spec for a recognized variant, or `nil` to let
    /// the primitive fall back to its default rendering.
    static func resolve(_ variant: String?, formFactor: String = RequestEnvelope.currentFormFactor) -> ContainerVariantSpec? {
        guard let raw = variant, !raw.isEmpty else { return nil }
        guard let value = ContainerVariant(rawValue: raw) else {
            logger.debug("unknown container variant \(raw, privacy: .public); rendering with primitive defaults")
            return nil
        }
        return spec(for: value, formFactor: formFactor)
    }

    /// Emits a debug log when an inline prop attempts to override an axis the
    /// variant's override matrix marks as locked.
    static func logOverrideBlocked(variant: String, axis: String, attemptedValue: Any?) {
        let described = attemptedValue.map { String(describing: $0) } ?? "nil"
        logger.debug(
            "variant override blocked: variant=\(variant, privacy: .public) axis=\(axis, privacy: .public) attempted=\(described, privacy: .public)"
        )
    }

    private static func spec(for variant: ContainerVariant, formFactor: String) -> ContainerVariantSpec {
        let isTablet = formFactor == "tablet"
        switch variant {
        case .hero:
            return ContainerVariantSpec(
                cornerRadius: 16,
                backgroundRole: .secondarySystemGroupedBackground,
                shadow: Shadow(color: "#000000", offsetX: 0, offsetY: isTablet ? 4 : 3, radius: isTablet ? 8 : 6, type: nil),
                gradientOverlay: GradientOverlay(
                    topColor: Color.accentColor.opacity(0.10),
                    bottomColor: .clear
                ),
                fillWidth: nil,
                border: nil,
                overrideMatrix: [
                    "padding": .allow,
                    "cornerRadius": .allow,
                    "background": .lock,
                    "shadow": .lock,
                    "color": .allow,
                    "gap": .allow,
                    "opacity": .allow,
                    "border": .allow
                ]
            )
        case .grouped:
            // Hand-rolled grouped block: .listStyle(.insetGrouped) can't wrap an
            // arbitrary VStack, so the grouped variant emits the visual equivalent
            // (secondarySystemGroupedBackground + 12pt radius). Interior divider
            // insets are the renderer's responsibility when it lays out rows.
            return ContainerVariantSpec(
                cornerRadius: 12,
                backgroundRole: .secondarySystemGroupedBackground,
                shadow: nil,
                gradientOverlay: nil,
                fillWidth: nil,
                border: nil,
                overrideMatrix: [
                    "padding": .allow,
                    "cornerRadius": .allow,
                    "background": .allow,
                    "shadow": .allow,
                    "color": .allow,
                    "gap": .allow,
                    "opacity": .allow,
                    "border": .allow
                ]
            )
        }
    }
}

// MARK: - Surface role -> Color

/// Resolves a semantic `SurfaceRole` to a concrete SwiftUI `Color`. The
/// grouped-background roles go through the UIKit semantic colors so they
/// adapt to light/dark mode automatically. The `.accent` role is tinted at
/// 0.15 opacity to match the banner variant's spec.
func surfaceColor(for role: SurfaceRole) -> Color {
    switch role {
    case .systemGroupedBackground:
        return Color(UIColor.systemGroupedBackground)
    case .secondarySystemGroupedBackground:
        return Color(UIColor.secondarySystemGroupedBackground)
    case .tertiarySystemGroupedBackground:
        return Color(UIColor.tertiarySystemGroupedBackground)
    case .accent:
        return Color.accentColor.opacity(0.15)
    case .custom(let c):
        return c
    }
}

// MARK: - View integration

extension View {
    /// Applies a resolved `ContainerVariantSpec` to the view, respecting the
    /// override matrix for inline background / cornerRadius / shadow. When
    /// `spec` is `nil` (no variant on the wire), the view falls back to the
    /// primitive's default inline-only styling so existing payloads keep
    /// rendering unchanged.
    func applyContainerVariant(
        spec: ContainerVariantSpec?,
        variantName: String?,
        inlineBackground: BackgroundUnion?,
        inlineCornerRadius: LayoutScalar?,
        inlineCornerRadii: CornerRadii? = nil,
        inlineShadow: Shadow?
    ) -> some View {
        modifier(
            ContainerVariantApplier(
                spec: spec,
                variantName: variantName,
                inlineBackground: inlineBackground,
                inlineCornerRadius: inlineCornerRadius,
                inlineCornerRadii: inlineCornerRadii,
                inlineShadow: inlineShadow
            )
        )
    }
}

private struct ContainerVariantApplier: ViewModifier {
    let spec: ContainerVariantSpec?
    let variantName: String?
    let inlineBackground: BackgroundUnion?
    let inlineCornerRadius: LayoutScalar?
    let inlineCornerRadii: CornerRadii?
    let inlineShadow: Shadow?

    @Environment(\.colorScheme) private var colorScheme

    @ViewBuilder
    func body(content: Content) -> some View {
        if let spec = spec {
            content.modifier(
                ContainerVariantModifier(
                    spec: spec,
                    variantName: variantName ?? "",
                    inlineBackground: inlineBackground,
                    inlineCornerRadius: inlineCornerRadius,
                    inlineCornerRadii: inlineCornerRadii,
                    inlineShadow: inlineShadow
                )
            )
        } else {
            content
                .background { backgroundView(for: constrainedInlineBackground, colorScheme: colorScheme) }
                .modifier(ContainerCornerClipModifier(
                    radius: LayoutTokenResolver.cgFloat(inlineCornerRadius),
                    radii: inlineCornerRadii
                ))
                .applyShadow(inlineShadow)
        }
    }

    private var constrainedInlineBackground: BackgroundUnion? {
        guard isAtomicBackgroundImage(inlineBackground) else { return inlineBackground }
        logger.warning("Atomic background image is decoded but constrained out of mobile atomic rendering; use section.surface background or an Image child")
        return nil
    }
}

private struct ContainerVariantModifier: ViewModifier {
    let spec: ContainerVariantSpec
    let variantName: String
    let inlineBackground: BackgroundUnion?
    let inlineCornerRadius: LayoutScalar?
    let inlineCornerRadii: CornerRadii?
    let inlineShadow: Shadow?

    func body(content: Content) -> some View {
        let bgPolicy = policy("background")
        let radiusPolicy = policy("cornerRadius")
        let shadowPolicy = policy("shadow")

        let useInlineBg = inlineBackground != nil && bgPolicy == .allow && !isAtomicBackgroundImage(inlineBackground)
        let useInlineRadius = inlineCornerRadius != nil && radiusPolicy == .allow
        let useInlineRadii = inlineCornerRadii != nil && radiusPolicy == .allow
        let useInlineShadow = inlineShadow != nil && shadowPolicy == .allow

        if inlineBackground != nil && bgPolicy == .lock {
            ContainerVariantResolver.logOverrideBlocked(
                variant: variantName, axis: "background", attemptedValue: inlineBackground
            )
        }
        if isAtomicBackgroundImage(inlineBackground) {
            logger.warning("Atomic background image is decoded but constrained out of mobile atomic rendering; use section.surface background or an Image child")
        }
        if inlineCornerRadius != nil && radiusPolicy == .lock {
            ContainerVariantResolver.logOverrideBlocked(
                variant: variantName, axis: "cornerRadius", attemptedValue: inlineCornerRadius
            )
        }
        if inlineShadow != nil && shadowPolicy == .lock {
            ContainerVariantResolver.logOverrideBlocked(
                variant: variantName, axis: "shadow", attemptedValue: inlineShadow
            )
        }

        let effectiveRadius: CGFloat = useInlineRadius
            ? LayoutTokenResolver.cgFloat(inlineCornerRadius)
            : (spec.cornerRadius ?? 0)
        let effectiveRadii: CornerRadii? = useInlineRadii ? inlineCornerRadii : nil
        let effectiveShadow: Shadow? = useInlineShadow ? inlineShadow : spec.shadow

        return content
            .modifier(
                ContainerVariantBackgroundModifier(
                    spec: spec,
                    variantName: variantName,
                    useInlineBg: useInlineBg,
                    inlineBackground: inlineBackground
                )
            )
            .modifier(ContainerCornerClipModifier(radius: effectiveRadius, radii: effectiveRadii))
            .applyShadow(effectiveShadow)
            .modifier(FillWidthIfNeeded(enabled: spec.fillWidth == true))
    }

    private func policy(_ axis: String) -> OverridePolicy {
        spec.overrideMatrix[axis] ?? .allow
    }
}

private func isAtomicBackgroundImage(_ background: BackgroundUnion?) -> Bool {
    guard case .background(let value) = background else { return false }
    return value.imageURL?.isEmpty == false
}

private struct ContainerVariantBackgroundModifier: ViewModifier {
    let spec: ContainerVariantSpec
    let variantName: String
    let useInlineBg: Bool
    let inlineBackground: BackgroundUnion?

    @Environment(\.colorScheme) private var colorScheme

    @ViewBuilder
    func body(content: Content) -> some View {
        if useInlineBg {
            content.background { backgroundView(for: inlineBackground, colorScheme: colorScheme) }
        } else if let role = spec.backgroundRole {
            content.background {
                ZStack {
                    surfaceColor(for: role)
                    if let gradient = spec.gradientOverlay {
                        LinearGradient(
                            colors: [gradient.topColor, gradient.bottomColor],
                            startPoint: .top,
                            endPoint: .bottom
                        )
                    }
                }
            }
        } else {
            content
        }
    }
}

private struct FillWidthIfNeeded: ViewModifier {
    let enabled: Bool

    @ViewBuilder
    func body(content: Content) -> some View {
        if enabled {
            content.frame(maxWidth: .infinity)
        } else {
            content
        }
    }
}

/// Clips a container to either a uniform corner radius (single-value
/// `cornerRadius`) or an asymmetric `UnevenRoundedRectangle` when
/// per-corner `cornerRadii` is present. Per-corner values fall back to
/// `radius` for any corner axis omitted, matching the schema contract.
private struct ContainerCornerClipModifier: ViewModifier {
    let radius: CGFloat
    let radii: CornerRadii?

    @ViewBuilder
    func body(content: Content) -> some View {
        if let radii = radii, hasAnyCorner(radii) {
            let r = radius
            content.clipShape(UnevenRoundedRectangle(
                topLeadingRadius: cornerScalar(radii.topStart, fallback: r),
                bottomLeadingRadius: cornerScalar(radii.bottomStart, fallback: r),
                bottomTrailingRadius: cornerScalar(radii.bottomEnd, fallback: r),
                topTrailingRadius: cornerScalar(radii.topEnd, fallback: r),
                style: .continuous
            ))
        } else if radius > 0 {
            content.cornerRadius(radius)
        } else {
            content
        }
    }

    private func hasAnyCorner(_ r: CornerRadii) -> Bool {
        LayoutTokenResolver.cgFloat(r.topStart) != 0 || LayoutTokenResolver.cgFloat(r.topEnd) != 0
            || LayoutTokenResolver.cgFloat(r.bottomStart) != 0 || LayoutTokenResolver.cgFloat(r.bottomEnd) != 0
    }

    private func cornerScalar(_ s: LayoutScalar?, fallback: CGFloat) -> CGFloat {
        guard let s else { return fallback }
        return LayoutTokenResolver.cgFloat(s)
    }
}
