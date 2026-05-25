import Foundation
import XCTest
@testable import SduiCore

final class LayoutTokenResolverTests: XCTestCase {

    override func tearDown() {
        super.tearDown()
        LayoutTokenResolver.missingTokenHook = nil
        URLProtocol.unregisterClass(NetworkProbeURLProtocol.self)
        NetworkProbeURLProtocol.reset()
    }

    func testNumericScalarPassesThroughUnchanged() {
        let value = LayoutTokenResolver.intValue(.integer(24), formFactor: "phone")
        XCTAssertEqual(value, 24)
    }

    func testNilScalarResolvesToZero() {
        let value = LayoutTokenResolver.intValue(nil, formFactor: "phone")
        XCTAssertEqual(value, 0)
    }

    func testSpacingTokenResolvesByFormFactor() {
        XCTAssertEqual(LayoutTokenResolver.resolveSpacing("token:nba.spacing.lg", formFactor: .phone), 16)
        XCTAssertEqual(LayoutTokenResolver.resolveSpacing("token:nba.spacing.lg", formFactor: .tablet), 20)
        XCTAssertEqual(LayoutTokenResolver.resolveSpacing("token:nba.spacing.lg", formFactor: .tv), 24)
        XCTAssertEqual(LayoutTokenResolver.resolveSpacing("token:nba.spacing.lg", formFactor: .web), 16)
    }

    func testRadiusTokenResolvesByFormFactor() {
        XCTAssertEqual(LayoutTokenResolver.resolveRadius("token:nba.radius.full", formFactor: .phone), 9999)
        XCTAssertEqual(LayoutTokenResolver.resolveRadius("token:nba.radius.full", formFactor: .tablet), 9999)
        XCTAssertEqual(LayoutTokenResolver.resolveRadius("token:nba.radius.full", formFactor: .tv), 9999)
        XCTAssertEqual(LayoutTokenResolver.resolveRadius("token:nba.radius.full", formFactor: .web), 9999)
    }

    func testUnknownLayoutTokenReturnsZeroAndLogsMissing() {
        var logged: [String] = []
        LayoutTokenResolver.missingTokenHook = { logged.append($0) }

        let value = LayoutTokenResolver.intValue(.string("token:nba.spacing.unknown"), formFactor: "phone")

        XCTAssertEqual(value, 0)
        XCTAssertEqual(logged, ["token:nba.spacing.unknown"])
    }

    func testTypographyHeadlineLargePhoneResolvesExpectedSpec() {
        let spec = LayoutTokenResolver.typography("token:nba.typography.headlineLarge", formFactor: .phone)

        XCTAssertEqual(spec?.familyRef, "nba.font.knockout")
        XCTAssertEqual(spec?.weight, 360)
        XCTAssertEqual(spec?.textCase, "uppercase")
        XCTAssertEqual(spec?.lineHeight ?? 0, 0.8, accuracy: 0.0001)

        guard let size = spec?.size else {
            XCTFail("Expected typography size")
            return
        }
        switch size {
        case .scalar(let value):
            XCTAssertEqual(value, 32)
        case .envelope:
            XCTFail("Expected scalar size for phone form factor")
        }
    }

    func testTypographyBodyMediumWebReturnsEnvelopeSize() {
        let spec = LayoutTokenResolver.typography("token:nba.typography.bodyMedium", formFactor: .web)

        XCTAssertNotNil(spec)
        guard let size = spec?.size else {
            XCTFail("Expected typography size")
            return
        }

        switch size {
        case .scalar:
            XCTFail("Expected web envelope size for bodyMedium")
        case .envelope(let envelope):
            XCTAssertEqual(envelope.min, 14)
            XCTAssertEqual(envelope.max, 18)
            XCTAssertEqual(envelope.minVw, 320)
            XCTAssertEqual(envelope.maxVw, 1440)
        }
    }

    func testUnknownTypographyTokenReturnsNil() {
        XCTAssertNil(LayoutTokenResolver.typography("token:nba.typography.notReal", formFactor: .phone))
    }

    func testShadowSpecResolvesKnownToken() {
        let shadow = LayoutTokenResolver.shadowSpec("token:nba.shadow.md")

        XCTAssertEqual(shadow?.radius, 8)
        XCTAssertEqual(shadow?.offsetY, 2)
        XCTAssertEqual(shadow?.color, "rgba(0,0,0,0.15)")
    }

    func testShadowSpecUnknownTokenReturnsNil() {
        XCTAssertNil(LayoutTokenResolver.shadowSpec("token:nba.shadow.unknown"))
    }

    func testShadowSpecNonTokenStringReturnsNil() {
        XCTAssertNil(LayoutTokenResolver.shadowSpec("nba.shadow.md"))
    }

    func testMotionDurationFastPhoneResolves150() {
        XCTAssertEqual(
            LayoutTokenResolver.motionDuration("token:nba.motion.duration.fast", formFactor: .phone),
            150
        )
    }

    func testMotionEasingDefaultResolvesExpectedCurve() {
        XCTAssertEqual(
            LayoutTokenResolver.motionEasing("token:nba.motion.easing.default"),
            "cubic-bezier(0.16, 1, 0.3, 1)"
        )
    }

    func testZeroNetworkRequestsDuringFormFactorChangeResolution() {
        NetworkProbeURLProtocol.reset()
        URLProtocol.registerClass(NetworkProbeURLProtocol.self)

        let observer = FormFactorObserver(initialFormFactor: .phone)
        let first = LayoutTokenResolver.resolveSpacing("token:nba.spacing.md", formFactor: observer.formFactor)
        observer.formFactor = .tablet
        let second = LayoutTokenResolver.resolveSpacing("token:nba.spacing.md", formFactor: observer.formFactor)

        XCTAssertEqual(first, 12)
        XCTAssertEqual(second, 15)
        XCTAssertEqual(NetworkProbeURLProtocol.canInitCount, 0, "Resolver must not trigger HTTP during resize")
    }
}

private final class NetworkProbeURLProtocol: URLProtocol {
    private(set) static var canInitCount = 0

    static func reset() {
        canInitCount = 0
    }

    override class func canInit(with request: URLRequest) -> Bool {
        canInitCount += 1
        return false
    }

    override class func canonicalRequest(for request: URLRequest) -> URLRequest {
        request
    }

    override func startLoading() {}
    override func stopLoading() {}
}
