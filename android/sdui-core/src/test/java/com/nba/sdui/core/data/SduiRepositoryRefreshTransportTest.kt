package com.nba.sdui.core.data

import android.util.Log
import com.nba.sdui.core.request.RequestEnvelopeBuilder
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Contract tests asserting that parameterized refresh produces the *same*
 * encoded URL shape as a normal screen fetch — i.e. that refresh routes
 * through the canonical envelope transport rather than any bespoke URL
 * builder.
 *
 * The systemic regression these tests guard against:
 * `ActionHandler.handleRefresh` used to assemble a URL by hand from raw
 * string concatenation, which (a) skipped envelope params, (b) skipped
 * RFC-3986 percent-encoding, (c) skipped the POST fallback, and (d) did
 * not propagate the parent `X-Trace-Id`. All of those invariants live in
 * `SduiRepository.fetchScreen`, so the only safe design is to route
 * refresh through the same primitive.
 */
class SduiRepositoryRefreshTransportTest {

    private lateinit var captured: MutableList<Request>
    private lateinit var client: OkHttpClient
    private lateinit var repository: SduiRepository

    @Before
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.d(any(), any<String>()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0
        every { Log.e(any(), any<String>()) } returns 0
        every { Log.e(any(), any<String>(), any()) } returns 0

        captured = mutableListOf()
        client = OkHttpClient.Builder()
            .addInterceptor(captureAndShortCircuit(captured))
            .build()
        repository = SduiRepository(baseUrl = "https://example.test/api", httpClient = client)
    }

    @After
    fun tearDown() {
        unmockkStatic(Log::class)
    }

    // MARK: GET path

    @Test
    fun `parameterized refresh resolves baseUrl and percent-encodes user params`() = runBlocking {
        repository.fetchScreen(
            path = "/sdui/refresh/stats-leaders",
            envelope = compactEnvelope(),
            userParams = mapOf(
                "perMode" to "Totals",
                "season" to "2025-26",
                "seasonType" to "Regular Season"
            ),
            traceIdOverride = "trace-parent"
        )

        val request = captured.single()
        val url = request.url
        assertEquals("https", url.scheme)
        assertEquals("example.test", url.host)
        assertTrue(url.encodedPath.endsWith("/sdui/refresh/stats-leaders"))

        val query = url.encodedQuery!!
        assertTrue(query.contains("perMode=Totals"))
        assertTrue(query.contains("season=2025-26"))
        assertTrue(
            "spaces in user params must be RFC-3986 encoded as %20: $query",
            query.contains("seasonType=Regular%20Season")
        )
        assertTrue("envelope must travel as bracket-notation: $query",
            query.contains("platform%5Bname%5D=android"))
        assertTrue("envelope must carry formFactor on every request: $query",
            query.contains("platform%5BformFactor%5D=phone"))
        assertTrue(query.contains("locale=en"))

        assertEquals("trace-parent", request.header("X-Trace-Id"))
        assertEquals("GET", request.method)
    }

    @Test
    fun `refresh user params are sorted deterministically`() = runBlocking {
        repository.fetchScreen(
            path = "/sdui/refresh/x",
            envelope = compactEnvelope(),
            userParams = mapOf("zKey" to "z", "aKey" to "a", "mKey" to "m")
        )

        val query = captured.single().url.encodedQuery!!
        val a = query.indexOf("aKey=a")
        val m = query.indexOf("mKey=m")
        val z = query.indexOf("zKey=z")
        assertTrue(a in 0 until m)
        assertTrue(m < z)
    }

    @Test
    fun `refresh and screen fetch produce the same encoded shape modulo user params`() = runBlocking {
        val envelope = compactEnvelope()

        repository.fetchScreen("/sdui/scoreboard", envelope)
        val screenQuery = captured.single().url.encodedQuery!!
        captured.clear()

        repository.fetchScreen("/sdui/scoreboard", envelope, userParams = mapOf("k" to "v"))
        val refreshQuery = captured.single().url.encodedQuery!!

        assertTrue(
            "refresh URL must lead with user params then envelope: $refreshQuery",
            refreshQuery.startsWith("k=v&")
        )
        assertEquals(
            "envelope tail must be byte-identical between screen fetch and refresh",
            screenQuery,
            refreshQuery.removePrefix("k=v&")
        )
    }

    // MARK: POST path

    @Test
    fun `POST fallback keeps user params on URL and envelope in body`() = runBlocking {
        val oversized = oversizedEnvelope()
        assertTrue("fixture must exceed GET threshold", oversized.exceedsGetThreshold())

        repository.fetchScreen(
            path = "/sdui/refresh/stats-leaders",
            envelope = oversized,
            userParams = mapOf("perMode" to "Totals")
        )

        val request = captured.single()
        assertEquals("POST", request.method)
        assertEquals("perMode=Totals", request.url.encodedQuery)

        val body = readBody(request)
        // The body is the same bracket-notation query string the GET path
        // would have used, sent as JSON-shaped content per RequestEnvelopeBuilder.
        assertTrue("envelope body must include platform brackets: $body",
            body.contains("platform%5Bname%5D=android"))
    }

    // MARK: Helpers

    private fun compactEnvelope() = RequestEnvelopeBuilder()
        .locale("en")
        .schemaVersion("1.0")
        .platformName("android")
        .appVersion("8.3.0")
        .osVersion("34")
        .deviceClass("phone")
        .sseCapable(true)
        // Pin form factor for byte-stable URL assertions; production paths
        // pick this up from the runtime classifier.
        .formFactor("phone")

    private fun oversizedEnvelope(): RequestEnvelopeBuilder {
        val builder = compactEnvelope()
        val value = "x".repeat(200)
        val experiments = (0 until 100).associate { "exp_$it" to value }
        return builder.experiments(experiments)
    }

    private fun readBody(request: Request): String {
        val buffer = Buffer()
        request.body?.writeTo(buffer)
        return buffer.readUtf8()
    }

    private fun captureAndShortCircuit(sink: MutableList<Request>): Interceptor =
        Interceptor { chain ->
            sink.add(chain.request())
            Response.Builder()
                .request(chain.request())
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body(EMPTY_SCREEN_JSON.toResponseBody("application/json".toMediaType()))
                .build()
        }

    companion object {
        private const val EMPTY_SCREEN_JSON =
            """{"id":"stats-leaders","schemaVersion":"1.0","sections":[]}"""
    }
}
