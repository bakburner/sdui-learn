import XCTest
@testable import SduiCore

final class RequestEnvelopeBuilderTests: XCTestCase {

    func testQueryStringUsesBracketNotation() {
        let envelope = RequestEnvelope(
            locale: "en",
            schemaVersion: "1.0",
            platformName: "ios",
            appVersion: "8.3.0",
            osVersion: "17.5",
            deviceClass: "phone",
            sseCapable: true,
            countryCode: "US",
            experiments: ["gd_tab_order_v2": "variant_b"]
        )

        let query = envelope.buildQueryString()

        XCTAssertTrue(query.contains("locale=en"))
        XCTAssertTrue(query.contains("schemaVersion=1.0"))
        XCTAssertTrue(query.contains("platform%5Bname%5D=ios"))
        XCTAssertTrue(query.contains("platform%5BappVersion%5D=8.3.0"))
        XCTAssertTrue(query.contains("device%5BcountryCode%5D=US"))
        XCTAssertTrue(query.contains("experiments%5Bgd_tab_order_v2%5D=variant_b"))
    }

    func testPercentEncodingMatchesRFC3986() {
        XCTAssertEqual(RequestEnvelope.percentEncode("hello world"), "hello%20world")
        XCTAssertEqual(RequestEnvelope.percentEncode("a=b&c=d"), "a%3Db%26c%3Dd")
        XCTAssertEqual(RequestEnvelope.percentEncode("[bracket]"), "%5Bbracket%5D")
    }

    func testJSONBodyShape() throws {
        let envelope = RequestEnvelope(
            locale: "en",
            schemaVersion: "1.0",
            platformName: "ios",
            appVersion: "8.3.0",
            osVersion: "17.5",
            deviceClass: "phone",
            sseCapable: true,
            countryCode: "US",
            experiments: ["exp_a": "1"]
        )

        let data = try envelope.jsonBody()
        let decoded = try JSONSerialization.jsonObject(with: data) as? [String: Any]
        XCTAssertEqual(decoded?["locale"] as? String, "en")

        let platform = decoded?["platform"] as? [String: Any]
        XCTAssertEqual(platform?["name"] as? String, "ios")
        XCTAssertEqual(platform?["appVersion"] as? String, "8.3.0")

        let device = decoded?["device"] as? [String: Any]
        XCTAssertEqual(device?["countryCode"] as? String, "US")

        let experiments = decoded?["experiments"] as? [String: Any]
        XCTAssertEqual(experiments?["exp_a"] as? String, "1")
    }

    func testExperimentsSortedDeterministically() {
        let envelope = RequestEnvelope(
            experiments: ["b_exp": "2", "a_exp": "1", "c_exp": "3"]
        )

        let query = envelope.buildQueryString()
        guard let aIdx = query.range(of: "a_exp"),
              let bIdx = query.range(of: "b_exp"),
              let cIdx = query.range(of: "c_exp") else {
            return XCTFail("expected all experiment keys in output")
        }

        XCTAssertTrue(aIdx.lowerBound < bIdx.lowerBound)
        XCTAssertTrue(bIdx.lowerBound < cIdx.lowerBound)
    }
}
