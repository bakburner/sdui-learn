import XCTest
@testable import SduiCore

final class WireUrlResolverTests: XCTestCase {
    func testAbsoluteUrlUnchanged() {
        XCTAssertEqual(
            WireUrlResolver.resolve("https://cdn.example.com/a.png", baseURL: "http://localhost:8080"),
            "https://cdn.example.com/a.png"
        )
    }

    func testRelativePathJoinedToBase() {
        XCTAssertEqual(
            WireUrlResolver.resolve("/sdui-demo/team.svg?v=1", baseURL: "http://10.0.2.2:8080"),
            "http://10.0.2.2:8080/sdui-demo/team.svg?v=1"
        )
    }

    func testBlankBaseReturnsTrimmedRelative() {
        XCTAssertEqual(
            WireUrlResolver.resolve("/sdui-demo/x.svg", baseURL: ""),
            "/sdui-demo/x.svg"
        )
    }
}
