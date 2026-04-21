import XCTest
@testable import SduiCore

final class IconTokenResolverTests: XCTestCase {

    func testResolvesKnownToken() {
        XCTAssertEqual(IconTokenResolver.shared.resolve("sdui:play"), "play.fill")
        XCTAssertEqual(IconTokenResolver.shared.resolve("sdui:live"), "antenna.radiowaves.left.and.right")
    }

    func testUnknownSduiTokenFallsBackToWarning() {
        XCTAssertEqual(
            IconTokenResolver.shared.resolve("sdui:not-a-thing"),
            "exclamationmark.triangle"
        )
    }

    func testNonTokenPassesThroughUnchanged() {
        XCTAssertEqual(IconTokenResolver.shared.resolve("star"), "star")
        XCTAssertEqual(IconTokenResolver.shared.resolve("custom.bundle"), "custom.bundle")
    }

    func testNilOrEmptyReturnsNil() {
        XCTAssertNil(IconTokenResolver.shared.resolve(nil))
        XCTAssertNil(IconTokenResolver.shared.resolve(""))
    }

    func testCustomResolverUsesInjectedMap() {
        let resolver = IconTokenResolver(tokens: [
            "sdui:custom": "bolt",
            "sdui:warning": "xmark"
        ])

        XCTAssertEqual(resolver.resolve("sdui:custom"), "bolt")
        XCTAssertEqual(resolver.resolve("sdui:unknown"), "xmark", "Falls back to injected warning mapping")
    }
}
