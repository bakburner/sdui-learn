import Foundation
import XCTest
@testable import SduiCore

final class TeamColorResolverTests: XCTestCase {

    override func tearDown() {
        super.tearDown()
        URLProtocol.unregisterClass(TeamColorNetworkProbe.self)
        TeamColorNetworkProbe.reset()
    }

    // MARK: - Background (team-background mode)

    func testTeamBgAtlResolvesToPrimary() {
        XCTAssertEqual(
            ColorTokenResolver.resolveTeamColor(token: "nba.team.bg", teamId: "atl", theme: "dark"),
            "#C8102E"
        )
    }

    func testTeamBgBknResolvesToSecondaryOverride() {
        XCTAssertEqual(
            ColorTokenResolver.resolveTeamColor(token: "nba.team.bg", teamId: "bkn", theme: "dark"),
            "#707271"
        )
    }

    func testTeamBgSasResolvesToTertiaryOverride() {
        XCTAssertEqual(
            ColorTokenResolver.resolveTeamColor(token: "nba.team.bg", teamId: "sas", theme: "dark"),
            "#4A4A4A"
        )
    }

    // MARK: - Accent (theme-split modes)

    func testTeamAccentSacDarkResolvesToLiteralHex() {
        XCTAssertEqual(
            ColorTokenResolver.resolveTeamColor(token: "nba.team.accent", teamId: "sac", theme: "dark"),
            "#BEC9CF"
        )
    }

    // MARK: - Accent-label (ref resolution)

    func testTeamAccentLabelIndDarkResolvesRefThroughPalette() {
        let result = ColorTokenResolver.resolveTeamColor(
            token: "nba.team.accent-label", teamId: "ind", theme: "dark"
        )
        // ind dark → { "ref": "nba.color.primary.10" } → grey.10 → #191C23
        XCTAssertEqual(result, "#191C23")
    }

    // MARK: - Unknown team

    func testUnknownTeamReturnsNil() {
        XCTAssertNil(
            ColorTokenResolver.resolveTeamColor(token: "nba.team.bg", teamId: "zzz", theme: "dark")
        )
    }

    // MARK: - Zero network calls

    func testZeroURLProtocolCallsDuringResolution() {
        TeamColorNetworkProbe.reset()
        URLProtocol.registerClass(TeamColorNetworkProbe.self)

        _ = ColorTokenResolver.resolveTeamColor(token: "nba.team.bg", teamId: "atl", theme: "dark")
        _ = ColorTokenResolver.resolveTeamColor(token: "nba.team.accent", teamId: "sac", theme: "dark")
        _ = ColorTokenResolver.resolveTeamColor(token: "nba.team.accent-label", teamId: "ind", theme: "dark")
        _ = ColorTokenResolver.resolveTeamColor(token: "nba.team.bg", teamId: "zzz", theme: "dark")

        XCTAssertEqual(
            TeamColorNetworkProbe.canInitCount, 0,
            "Team color resolution must not trigger any HTTP requests"
        )
    }
}

// MARK: - Network probe

private final class TeamColorNetworkProbe: URLProtocol {
    private(set) static var canInitCount = 0

    static func reset() { canInitCount = 0 }

    override class func canInit(with request: URLRequest) -> Bool {
        canInitCount += 1
        return false
    }

    override class func canonicalRequest(for request: URLRequest) -> URLRequest { request }
    override func startLoading() {}
    override func stopLoading() {}
}
