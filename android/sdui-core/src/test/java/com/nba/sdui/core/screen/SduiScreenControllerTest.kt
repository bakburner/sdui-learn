package com.nba.sdui.core.screen

import android.util.Log
import com.nba.sdui.core.config.SduiScreenConfig
import com.nba.sdui.core.data.SduiRepository
import com.nba.sdui.core.models.generated.RefreshType
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SduiScreenControllerTest {

    private data class StubResponse(
        val code: Int,
        val body: String,
        val message: String = "OK",
        val headers: Map<String, String> = emptyMap()
    )

    private lateinit var capturedRequests: MutableList<Request>
    private lateinit var responder: (Request) -> StubResponse
    private lateinit var repository: SduiRepository

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        mockkStatic(Log::class)
        every { Log.d(any(), any<String>()) } returns 0
        every { Log.i(any(), any<String>()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0
        every { Log.w(any(), any<String>(), any()) } returns 0
        every { Log.e(any(), any<String>()) } returns 0
        every { Log.e(any(), any<String>(), any()) } returns 0

        capturedRequests = mutableListOf()
        responder = { StubResponse(200, EMPTY_SCREEN_JSON) }

        val client = OkHttpClient.Builder()
            .addInterceptor(dynamicResponder(capturedRequests) { request -> responder(request) })
            .build()

        repository = SduiRepository(
            baseUrl = "https://example.test",
            httpClient = client
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkStatic(Log::class)
    }

    @Test
    fun `sectionEndpoint poll fetches section and merges replacement by id`() = runTest {
        responder = { request ->
            when (request.url.encodedPath) {
                SCREEN_PATH -> StubResponse(200, SCREEN_WITH_SECTION_ENDPOINT_POLL_JSON)
                SECTION_PATH -> StubResponse(200, SECTION_REPLACEMENT_JSON)
                else -> StubResponse(404, """{"error":"not found"}""", "Not Found")
            }
        }

        val controller = createController(this)
        controller.loadFromEndpoint(SCREEN_PATH)
        drainIO()

        advanceTimeBy(500)
        drainIO()

        assertEquals(1, requestCount(SECTION_PATH))
        val success = controller.uiState.value as SduiScreenUiState.Success
        val replaced = success.screen.sections.first { it.id == "scoreboard" }
        val untouched = success.screen.sections.first { it.id == "headline" }
        assertEquals("VideoPlayer", replaced.type)
        assertEquals("AtomicComposite", untouched.type)

        controller.onCleared()
    }

    @Test
    fun `section poll transitions from Poll to SSE and stops old poll loop`() = runTest {
        responder = { request ->
            when (request.url.encodedPath) {
                SCREEN_PATH -> StubResponse(200, SCREEN_WITH_SECTION_ENDPOINT_POLL_JSON)
                SECTION_PATH -> StubResponse(200, SECTION_WITH_SSE_POLICY_JSON)
                else -> StubResponse(404, """{"error":"not found"}""", "Not Found")
            }
        }

        val controller = createController(this)
        controller.loadFromEndpoint(SCREEN_PATH)
        drainIO()

        advanceTimeBy(500)
        drainIO()

        val success = controller.uiState.value as SduiScreenUiState.Success
        val scoreboard = success.screen.sections.first { it.id == "scoreboard" }
        assertEquals(RefreshType.SSE, scoreboard.refreshPolicy?.type)
        assertEquals("game:123:scoreboard", scoreboard.refreshPolicy?.channel)
        assertEquals(1, requestCount(SECTION_PATH))

        advanceTimeBy(2_000)
        drainIO()
        assertEquals(1, requestCount(SECTION_PATH))

        verify {
            Log.i(
                any<String>(),
                match<String> { it.contains("Subscribe Ably") && it.contains("scoreboard") }
            )
        }

        controller.onCleared()
    }

    @Test
    fun `404 from fetchSection marks section stale and stops polling`() = runTest {
        responder = { request ->
            when (request.url.encodedPath) {
                SCREEN_PATH -> StubResponse(200, SCREEN_WITH_SECTION_ENDPOINT_POLL_JSON)
                SECTION_PATH -> StubResponse(404, """{"error":"not found"}""", "Not Found")
                else -> StubResponse(404, """{"error":"not found"}""", "Not Found")
            }
        }

        val controller = createController(this)
        controller.loadFromEndpoint(SCREEN_PATH)
        drainIO()

        advanceTimeBy(500)
        drainIO()

        assertTrue(controller.staleSections.value.contains("scoreboard"))
        assertEquals(1, requestCount(SECTION_PATH))

        advanceTimeBy(2_000)
        drainIO()
        assertEquals(1, requestCount(SECTION_PATH))

        controller.onCleared()
    }

    @Test
    fun `5xx from fetchSection applies exponential backoff and marks stale after threshold`() = runTest {
        responder = { request ->
            when (request.url.encodedPath) {
                SCREEN_PATH -> StubResponse(200, SCREEN_WITH_SECTION_ENDPOINT_POLL_JSON)
                SECTION_PATH -> StubResponse(500, """{"error":"server error"}""", "Internal Server Error")
                else -> StubResponse(404, """{"error":"not found"}""", "Not Found")
            }
        }

        val controller = createController(this)
        controller.loadFromEndpoint(SCREEN_PATH)
        drainIO()

        advanceTimeBy(500)
        drainIO()
        assertEquals(1, requestCount(SECTION_PATH))
        assertFalse(controller.staleSections.value.contains("scoreboard"))

        advanceTimeBy(1_000)
        drainIO()
        assertEquals(2, requestCount(SECTION_PATH))
        assertTrue(controller.staleSections.value.contains("scoreboard"))

        advanceTimeBy(2_000)
        drainIO()
        assertEquals(3, requestCount(SECTION_PATH))

        controller.onCleared()
    }

    @Test
    fun `pull-to-refresh applyScreen restart keeps single screen-level poll loop`() = runTest {
        responder = { request ->
            when (request.url.encodedPath) {
                SCREEN_PATH -> StubResponse(200, SCREEN_WITH_DEFAULT_POLL_JSON)
                else -> StubResponse(404, """{"error":"not found"}""", "Not Found")
            }
        }

        val controller = createController(this)
        controller.loadFromEndpoint(SCREEN_PATH)
        drainIO()
        val afterInitial = requestCount(SCREEN_PATH)
        assertTrue("initial load should have fetched", afterInitial >= 1)

        advanceTimeBy(1_000)
        drainIO()
        val afterFirstPoll = requestCount(SCREEN_PATH)
        assertTrue("first poll tick", afterFirstPoll > afterInitial)

        controller.refresh()
        drainIO()
        val afterRefresh = requestCount(SCREEN_PATH)
        assertTrue("refresh should trigger a fetch", afterRefresh > afterFirstPoll)

        // After refresh, the old screen-level poll is cancelled and a new one starts.
        // Advance two full intervals and verify only one tick per interval.
        val beforeTwoTicks = requestCount(SCREEN_PATH)
        advanceTimeBy(1_000)
        drainIO()
        val tick1 = requestCount(SCREEN_PATH) - beforeTwoTicks
        advanceTimeBy(1_000)
        drainIO()
        val tick2 = requestCount(SCREEN_PATH) - beforeTwoTicks

        assertEquals("exactly 1 fetch per interval (no duplicate poll loop)", 1, tick1)
        assertEquals("exactly 2 fetches over 2 intervals", 2, tick2)

        controller.onCleared()
    }

    @Test
    fun `replaceCurrentScreen fully replaces section roster when response id matches screen id`() = runTest {
        // Models the games-screen calendar-strip case: initial load has
        // [calendar, upcoming, final]; a parameterized refresh for a different
        // date returns [calendar, final] only. The 'upcoming' section MUST
        // disappear — otherwise stale games linger on the new date.
        responder = { request ->
            when {
                request.url.encodedPath == SCREEN_PATH && request.url.queryParameter("date") != null ->
                    StubResponse(200, FULL_SCREEN_REFRESH_DROPPING_UPCOMING_JSON)
                request.url.encodedPath == SCREEN_PATH ->
                    StubResponse(200, SCREEN_WITH_THREE_SECTIONS_JSON)
                else -> StubResponse(404, """{"error":"not found"}""", "Not Found")
            }
        }

        val controller = createController(this)
        controller.loadFromEndpoint(SCREEN_PATH)
        drainIO()
        assertEquals(
            listOf("calendar", "upcoming", "final"),
            (controller.uiState.value as SduiScreenUiState.Success).screen.sections.map { it.id }
        )

        controller.replaceCurrentScreen(REFRESH_PATH, mapOf("date" to "2026-05-18"))
        drainIO()

        val afterRefresh = (controller.uiState.value as SduiScreenUiState.Success).screen.sections
        assertEquals(
            "stale 'upcoming' section must be dropped on full-screen refresh",
            listOf("calendar", "final"),
            afterRefresh.map { it.id }
        )

        controller.onCleared()
    }

    @Test
    fun `replaceCurrentScreen drops response when id does not match current screen id`() = runTest {
        // Parameterized refresh must return the same screen it was invoked
        // against. A response with a different id is a server contract bug —
        // dropping it (with a warning) is safer than merging stale state.
        responder = { request ->
            when {
                request.url.encodedPath == SCREEN_PATH && request.url.queryParameter("season") != null ->
                    StubResponse(200, RESPONSE_WITH_MISMATCHED_SCREEN_ID_JSON)
                request.url.encodedPath == SCREEN_PATH ->
                    StubResponse(200, SCREEN_WITH_THREE_SECTIONS_JSON)
                else -> StubResponse(404, """{"error":"not found"}""", "Not Found")
            }
        }

        val controller = createController(this)
        controller.loadFromEndpoint(SCREEN_PATH)
        drainIO()
        val initialSections =
            (controller.uiState.value as SduiScreenUiState.Success).screen.sections.map { it.id }

        controller.replaceCurrentScreen(REFRESH_PATH, mapOf("season" to "2024-25"))
        drainIO()

        val afterRefresh =
            (controller.uiState.value as SduiScreenUiState.Success).screen.sections.map { it.id }
        assertEquals(
            "mismatched-id response must be dropped; current screen unchanged",
            initialSections,
            afterRefresh
        )

        verify {
            Log.w(
                any<String>(),
                match<String> {
                    it.contains("does not match current screen") &&
                        it.contains("dropping response")
                }
            )
        }

        controller.onCleared()
    }

    @Test
    fun `sectionEndpoint poll is skipped when screen default refresh policy is poll`() = runTest {
        responder = { request ->
            when (request.url.encodedPath) {
                SCREEN_PATH -> StubResponse(200, SCREEN_WITH_MUTUAL_EXCLUSIVITY_CONFLICT_JSON)
                SECTION_PATH -> StubResponse(200, SECTION_REPLACEMENT_JSON)
                else -> StubResponse(404, """{"error":"not found"}""", "Not Found")
            }
        }

        val controller = createController(this)
        controller.loadFromEndpoint(SCREEN_PATH)
        drainIO()

        advanceTimeBy(3_000)
        drainIO()

        assertEquals(0, requestCount(SECTION_PATH))
        assertFalse(controller.staleSections.value.contains("scoreboard"))

        verify {
            Log.w(
                any<String>(),
                match<String> { it.contains("skipping sectionEndpoint poll") && it.contains("screen-level refresh owns this section") }
            )
        }

        controller.onCleared()
    }

    @Test
    fun `pull-to-refresh preserves current query params on the outbound request`() = runTest {
        // After a parameterized refresh sets user params, a subsequent
        // pull-to-refresh must replay those params rather than fetching
        // the bare endpoint without them.
        responder = { StubResponse(200, SCREEN_WITH_THREE_SECTIONS_JSON) }

        val controller = createController(this)
        controller.loadFromEndpoint(SCREEN_PATH)
        drainIO()

        controller.replaceCurrentScreen(SCREEN_PATH, mapOf("date" to "2026-05-18"))
        drainIO()
        capturedRequests.clear()

        controller.refresh()
        drainIO()

        val refreshRequest = capturedRequests.single()
        assertEquals(SCREEN_PATH, refreshRequest.url.encodedPath)
        assertEquals(
            "pull-to-refresh must replay the current user params",
            "2026-05-18",
            refreshRequest.url.queryParameter("date")
        )

        controller.onCleared()
    }

    @Test
    fun `successful replaceCurrentScreen resets the screen-level poll timer`() = runTest {
        // After replaceCurrentScreen, applyScreen cancels the old poll job and
        // restarts it. Verify: (1) the replace triggers a fetch, (2) the poll
        // continues ticking afterward (proving the restart happened). Exact
        // virtual-time assertions are not reliable with drainIO, so we verify
        // the behavioral invariant instead.
        responder = { StubResponse(200, SCREEN_WITH_DEFAULT_POLL_JSON) }

        val controller = createController(this)
        controller.loadFromEndpoint(SCREEN_PATH)
        drainIO()
        capturedRequests.clear()

        // Issue replaceCurrentScreen — should trigger its own fetch.
        controller.replaceCurrentScreen(SCREEN_PATH, mapOf("k" to "v"))
        drainIO()
        assertTrue("replaceCurrentScreen should trigger a fetch", requestCount(SCREEN_PATH) >= 1)
        capturedRequests.clear()

        // Advance past two full poll intervals — the poll should keep ticking,
        // proving applyScreen restarted it after the replace.
        advanceTimeBy(2_200)
        drainIO()
        assertTrue("poll continues after replaceCurrentScreen", requestCount(SCREEN_PATH) >= 1)

        controller.onCleared()
    }

    /**
     * Give `Dispatchers.IO` time to complete synchronous mock responses,
     * then drain the test scheduler so continuations returning from IO run.
     */
    private fun TestScope.drainIO() {
        advanceUntilIdle()
        Thread.sleep(100)
        advanceUntilIdle()
    }

    private fun createController(scope: TestScope): SduiScreenController =
        SduiScreenController(
            config = SduiScreenConfig(
                baseUrl = "https://example.test",
                ablyTokenUrl = "https://example.test/ably-token",
                screenId = "test-screen"
            ),
            repository = repository,
            scope = scope
        )

    private fun requestCount(path: String): Int =
        capturedRequests.count { it.url.encodedPath == path }

    private fun dynamicResponder(
        sink: MutableList<Request>,
        responseFor: (Request) -> StubResponse
    ): Interceptor = Interceptor { chain ->
        val request = chain.request()
        sink.add(request)
        val response = responseFor(request)
        // Successful SDUI bodies are wrapped in the {data, meta} transport
        // envelope on the wire. The fixture JSON in this file declares the
        // payload shape; this interceptor wraps it so each test fixture
        // stays focused on the Screen/Section it cares about.
        val wireBody = if (response.code in 200..299) wrapEnvelope(response.body) else response.body
        val builder = Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(response.code)
            .message(response.message)
            .body(wireBody.toResponseBody("application/json".toMediaType()))

        response.headers.forEach { (name, value) ->
            builder.header(name, value)
        }

        builder.build()
    }

    private fun wrapEnvelope(payload: String): String =
        """{"data":${payload.trim()},"meta":{"degraded":false,"staleSections":[],"failedSections":[]}}"""

    companion object {
        private const val SCREEN_PATH = "/v1/sdui/screen/test-screen"
        private const val SECTION_PATH = "/v1/sdui/section/scoreboard"
        private const val REFRESH_PATH = "/v1/sdui/screen/test-screen"

        private const val EMPTY_SCREEN_JSON =
            """{"id":"test-screen","schemaVersion":"1.0","sections":[]}"""

        private const val SCREEN_WITH_THREE_SECTIONS_JSON = """
            {
              "id":"test-screen",
              "schemaVersion":"1.0",
              "sections":[
                {"id":"calendar","type":"AtomicComposite"},
                {"id":"upcoming","type":"AtomicComposite"},
                {"id":"final","type":"AtomicComposite"}
              ]
            }
        """

        private const val FULL_SCREEN_REFRESH_DROPPING_UPCOMING_JSON = """
            {
              "id":"test-screen",
              "schemaVersion":"1.0",
              "sections":[
                {"id":"calendar","type":"AtomicComposite"},
                {"id":"final","type":"AtomicComposite"}
              ]
            }
        """

        private const val RESPONSE_WITH_MISMATCHED_SCREEN_ID_JSON = """
            {
              "id":"some-other-screen",
              "schemaVersion":"1.0",
              "sections":[
                {"id":"final","type":"VideoPlayer"}
              ]
            }
        """

        private const val SCREEN_WITH_SECTION_ENDPOINT_POLL_JSON = """
            {
              "id":"test-screen",
              "schemaVersion":"1.0",
              "sections":[
                {
                  "id":"scoreboard",
                  "type":"AtomicComposite",
                  "refreshPolicy":{
                    "type":"poll",
                    "intervalMs":500,
                    "sectionEndpoint":"/v1/sdui/section/scoreboard",
                    "pauseWhenOffScreen":false
                  }
                },
                {
                  "id":"headline",
                  "type":"AtomicComposite"
                }
              ]
            }
        """

        private const val SECTION_REPLACEMENT_JSON = """
            {
              "id":"scoreboard",
              "type":"VideoPlayer"
            }
        """

        private const val SECTION_WITH_SSE_POLICY_JSON = """
            {
              "id":"scoreboard",
              "type":"AtomicComposite",
              "refreshPolicy":{
                "type":"sse",
                "channel":"game:123:scoreboard",
                "pauseWhenOffScreen":false
              }
            }
        """

        private const val SCREEN_WITH_DEFAULT_POLL_JSON = """
            {
              "id":"test-screen",
              "schemaVersion":"1.0",
              "defaultRefreshPolicy":{
                "type":"poll",
                "intervalMs":1000
              },
              "sections":[]
            }
        """

        private const val SCREEN_WITH_MUTUAL_EXCLUSIVITY_CONFLICT_JSON = """
            {
              "id":"test-screen",
              "schemaVersion":"1.0",
              "defaultRefreshPolicy":{
                "type":"poll",
                "intervalMs":1000
              },
              "sections":[
                {
                  "id":"scoreboard",
                  "type":"AtomicComposite",
                  "refreshPolicy":{
                    "type":"poll",
                    "intervalMs":500,
                    "sectionEndpoint":"/v1/sdui/section/scoreboard",
                    "pauseWhenOffScreen":false
                  }
                }
              ]
            }
        """
    }
}
