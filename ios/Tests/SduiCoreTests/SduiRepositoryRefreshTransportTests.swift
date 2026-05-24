import XCTest
@testable import SduiCore

/// Contract tests asserting that parameterized refresh produces the *same*
/// encoded URL shape as a normal screen fetch — i.e. that refresh routes
/// through the canonical envelope transport rather than any bespoke URL
/// builder.
///
/// The systemic regression these tests guard against:
/// `SduiScreenViewModel.refresh()` used to assemble a URL by hand from a
/// relative endpoint, which (a) bypassed `SduiConfig.baseURL` (yielding
/// `unsupported URL` errors), (b) skipped envelope params, (c) skipped the
/// POST fallback, and (d) did not propagate the parent `X-Trace-Id`. All of
/// those invariants live in `SduiRepository.fetchScreen`, so the only safe
/// design is to route refresh through the same primitive.
final class SduiRepositoryRefreshTransportTests: XCTestCase {

    private var captured: CapturingURLProtocol.Captured!

    override func setUp() {
        super.setUp()
        CapturingURLProtocol.reset()
    }

    private func makeRepository(
        envelope: RequestEnvelope = .compactTestEnvelope(),
        traceID: String? = nil
    ) -> SduiRepository {
        let config = SduiConfig(
            baseURL: URL(string: "https://example.test/api")!,
            ablyTokenURL: URL(string: "https://example.test/rttoken")!,
            traceIDProvider: { traceID ?? "trace-fixed" }
        )
        let configuration = URLSessionConfiguration.ephemeral
        configuration.protocolClasses = [CapturingURLProtocol.self]
        let session = URLSession(configuration: configuration)
        return SduiRepository(
            config: config,
            session: session,
            envelopeProvider: { envelope }
        )
    }

    // MARK: GET path

    func testParameterizedRefreshGoesThroughBaseURLAndEncodesUserParams() async throws {
        let repo = makeRepository()
        CapturingURLProtocol.respond(with: Self.emptyScreenJSON)

        _ = try? await repo.fetchScreen(
            endpoint: "/v1/sdui/refresh/stats-leaders",
            userParams: [
                "perMode": "Totals",
                "season": "2025-26",
                "seasonType": "Regular Season"
            ],
            traceID: "trace-parent"
        )

        let request = try CapturingURLProtocol.requireCaptured()
        let url = try XCTUnwrap(request.url)
        XCTAssertEqual(url.scheme, "https", "must resolve against config.baseURL")
        XCTAssertEqual(url.host, "example.test")
        XCTAssertTrue(url.path.hasSuffix("/v1/sdui/refresh/stats-leaders"))

        let query = try XCTUnwrap(url.query)
        XCTAssertTrue(query.contains("perMode=Totals"))
        XCTAssertTrue(query.contains("season=2025-26"))
        XCTAssertTrue(query.contains("seasonType=Regular%20Season"),
                      "spaces in user params must be RFC-3986 encoded")
        XCTAssertFalse(query.contains("platform%5Bname%5D"),
                       "platform name must not appear in query string")
        XCTAssertTrue(query.contains("locale=en"))

        XCTAssertEqual(request.value(forHTTPHeaderField: "X-Trace-Id"), "trace-parent",
                       "parent screen's traceID must propagate so logs correlate")
        XCTAssertEqual(request.httpMethod, "GET")
    }

    func testRefreshUserParamsAreSortedDeterministically() async throws {
        let repo = makeRepository()
        CapturingURLProtocol.respond(with: Self.emptyScreenJSON)

        _ = try? await repo.fetchScreen(
            endpoint: "/v1/sdui/refresh/stats-leaders",
            userParams: ["zKey": "z", "aKey": "a", "mKey": "m"]
        )

        let request = try CapturingURLProtocol.requireCaptured()
        let query = try XCTUnwrap(request.url?.query)
        let aIdx = try XCTUnwrap(query.range(of: "aKey=a"))
        let mIdx = try XCTUnwrap(query.range(of: "mKey=m"))
        let zIdx = try XCTUnwrap(query.range(of: "zKey=z"))
        XCTAssertTrue(aIdx.lowerBound < mIdx.lowerBound)
        XCTAssertTrue(mIdx.lowerBound < zIdx.lowerBound)
    }

    func testRefreshAndScreenFetchProduceTheSameEncodedShape() async throws {
        let envelope = RequestEnvelope.compactTestEnvelope()
        let repoA = makeRepository(envelope: envelope)
        CapturingURLProtocol.respond(with: Self.emptyScreenJSON)
        _ = try? await repoA.fetchScreen(endpoint: "/v1/sdui/scoreboard")
        let screenURL = try CapturingURLProtocol.requireCaptured().url

        CapturingURLProtocol.reset()
        CapturingURLProtocol.respond(with: Self.emptyScreenJSON)
        let repoB = makeRepository(envelope: envelope)
        _ = try? await repoB.fetchScreen(endpoint: "/v1/sdui/scoreboard", userParams: ["k": "v"])
        let refreshURL = try CapturingURLProtocol.requireCaptured().url

        // The screen-fetch and parameterized-refresh URLs must differ ONLY by
        // the user params segment — same path, same envelope encoding, same
        // ordering rule. Anything more divergent means refresh has drifted
        // away from the canonical transport.
        let screenQuery = screenURL?.query ?? ""
        let refreshQuery = refreshURL?.query ?? ""
        XCTAssertEqual(screenURL?.path, refreshURL?.path)
        XCTAssertTrue(refreshQuery.hasPrefix("k=v&"),
                      "user params lead the query so the envelope tail is byte-identical to a screen fetch")
        XCTAssertEqual(String(refreshQuery.dropFirst("k=v&".count)), screenQuery)
    }

    // MARK: POST path

    func testRefreshPostFallbackKeepsUserParamsOnURLAndEnvelopeInBody() async throws {
        let oversized = RequestEnvelope.oversizedTestEnvelope()
        XCTAssertTrue(oversized.exceedsGetThreshold, "fixture must exceed GET threshold")

        let repo = makeRepository(envelope: oversized)
        CapturingURLProtocol.respond(with: Self.emptyScreenJSON)

        _ = try? await repo.fetchScreen(
            endpoint: "/v1/sdui/refresh/stats-leaders",
            userParams: ["perMode": "Totals"]
        )

        let request = try CapturingURLProtocol.requireCaptured()
        XCTAssertEqual(request.httpMethod, "POST")
        XCTAssertEqual(request.value(forHTTPHeaderField: "Content-Type"), "application/json")

        let url = try XCTUnwrap(request.url)
        XCTAssertEqual(url.query, "perMode=Totals",
                       "user params still ride the URL on POST so the server reads them through @RequestParam")

        let body = try XCTUnwrap(request.bodyData)
        let json = try XCTUnwrap(JSONSerialization.jsonObject(with: body) as? [String: Any])
        XCTAssertNotNil(json["platform"])
        XCTAssertNotNil(json["experiments"])
    }

    // MARK: Fixtures

    private static let emptyScreenJSON: Data = """
        {
          "id": "stats-leaders",
          "schemaVersion": "1.0",
          "sections": []
        }
        """.data(using: .utf8)!
}

// MARK: - Test helpers

private extension RequestEnvelope {
    static func compactTestEnvelope() -> RequestEnvelope {
        RequestEnvelope(
            locale: "en",
            schemaVersion: "1.0",
            platformName: "ios",
            appVersion: "8.3.0",
            osVersion: "17.5",
            deviceClass: "phone",
            sseCapable: true,
            experiments: [:]
        )
    }

    static func oversizedTestEnvelope() -> RequestEnvelope {
        var experiments: [String: String] = [:]
        let value = String(repeating: "x", count: 200)
        for i in 0..<100 {
            experiments["exp_\(i)"] = value
        }
        return RequestEnvelope(
            locale: "en",
            schemaVersion: "1.0",
            platformName: "ios",
            appVersion: "8.3.0",
            osVersion: "17.5",
            deviceClass: "phone",
            sseCapable: true,
            experiments: experiments
        )
    }
}

/// Lightweight in-process URL-protocol stub. Not thread-shared with other
/// suites because each test hands the protocol a fresh `URLSession`
/// configuration; we still reset the static slot in `setUp` to be safe.
final class CapturingURLProtocol: URLProtocol {
    struct Captured {
        let url: URL?
        let httpMethod: String?
        let allHTTPHeaderFields: [String: String]?
        let bodyData: Data?

        func value(forHTTPHeaderField name: String) -> String? {
            allHTTPHeaderFields?.first(where: { $0.key.caseInsensitiveCompare(name) == .orderedSame })?.value
        }
    }

    private static let lock = NSLock()
    private static var stubbedResponse: Data = Data()
    private static var stubbedStatusCode: Int = 200
    private static var captured: Captured?

    static func respond(with data: Data) {
        lock.withLock { stubbedResponse = data }
    }

    static func respond(with data: Data, statusCode: Int) {
        lock.withLock {
            stubbedResponse = data
            stubbedStatusCode = statusCode
        }
    }

    static func reset() {
        lock.withLock {
            stubbedResponse = Data()
            stubbedStatusCode = 200
            captured = nil
        }
    }

    static func requireCaptured(file: StaticString = #file, line: UInt = #line) throws -> Captured {
        let value = lock.withLock { captured }
        guard let value else {
            XCTFail("no request captured by stub", file: file, line: line)
            throw NSError(domain: "CapturingURLProtocol", code: 0)
        }
        return value
    }

    override class func canInit(with request: URLRequest) -> Bool { true }
    override class func canonicalRequest(for request: URLRequest) -> URLRequest { request }

    override func startLoading() {
        let body: Data?
        if let stream = request.httpBodyStream {
            body = Self.readAll(stream)
        } else {
            body = request.httpBody
        }
        Self.lock.withLock {
            Self.captured = Captured(
                url: request.url,
                httpMethod: request.httpMethod,
                allHTTPHeaderFields: request.allHTTPHeaderFields,
                bodyData: body
            )
        }
        let statusCode = Self.lock.withLock { Self.stubbedStatusCode }
        let response = HTTPURLResponse(
            url: request.url ?? URL(string: "https://example.test")!,
            statusCode: statusCode,
            httpVersion: "HTTP/1.1",
            headerFields: ["Content-Type": "application/json"]
        )!
        client?.urlProtocol(self, didReceive: response, cacheStoragePolicy: .notAllowed)
        let payload = Self.lock.withLock { Self.stubbedResponse }
        client?.urlProtocol(self, didLoad: payload)
        client?.urlProtocolDidFinishLoading(self)
    }

    override func stopLoading() {}

    private static func readAll(_ stream: InputStream) -> Data {
        stream.open()
        defer { stream.close() }
        var data = Data()
        let bufferSize = 1024
        var buffer = [UInt8](repeating: 0, count: bufferSize)
        while stream.hasBytesAvailable {
            let read = stream.read(&buffer, maxLength: bufferSize)
            if read <= 0 { break }
            data.append(buffer, count: read)
        }
        return data
    }
}
