import XCTest
@testable import SduiCore

final class UriResolverTests: XCTestCase {

    func testNBASchemeMapsToSduiPath() {
        XCTAssertEqual(UriResolver.resolveEndpoint(uri: "nba://for-you"), "/v1/sdui/for-you")
        XCTAssertEqual(UriResolver.resolveEndpoint(uri: "nba://game-detail/0042300102"),
                       "/v1/sdui/game-detail/0042300102")
    }

    func testExistingSduiPathPassesThrough() {
        XCTAssertEqual(UriResolver.resolveEndpoint(uri: "/v1/sdui/scoreboard"), "/v1/sdui/scoreboard")
    }

    func testExternalURLPassesThrough() {
        XCTAssertEqual(UriResolver.resolveEndpoint(uri: "https://nba.com"), "https://nba.com")
    }
}
