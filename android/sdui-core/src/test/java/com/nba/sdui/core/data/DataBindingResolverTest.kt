package com.nba.sdui.core.data

import android.util.Log
import com.nba.sdui.core.models.generated.DataBinding
import com.nba.sdui.core.models.generated.DataBindingPath
import com.nba.sdui.core.models.generated.Transform
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * JVM unit tests for the shared `liveClockSnapshot` transform applied by
 * [DataBindingResolver]. Mirrors the iOS and web tests so the three
 * implementations of the shared binding contract stay in lock-step.
 *
 * Uses JUnit 4 because the module's Gradle test task runs the JUnit 4
 * platform; the existing Jupiter-annotated test classes alongside this one
 * are not currently discovered. Static logging from the resolver is
 * stubbed via mockk so the test focuses on transform behavior.
 */
class DataBindingResolverTest {

    @Before
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0
        every { Log.w(any(), any<String>(), any()) } returns 0
        every { Log.e(any(), any<String>()) } returns 0
    }

    @After
    fun tearDown() {
        unmockkStatic(Log::class)
    }

    @Test
    fun `liveClockSnapshot anchors clock from ISO duration and running flag`() {
        val resolver = DataBindingResolver()
        val binding = DataBinding(
            bindings = listOf(
                DataBindingPath(
                    sourcePath = "$.clock",
                    targetPath = "content.clock",
                    transform = Transform.LiveClockSnapshot
                )
            ),
            stringKeys = null
        )

        // Mirrors the production NBA Ably linescore wire format: scalar
        // ISO-8601 duration string at $.clock plus sibling `clockRunning`
        // at the message root (matches AblyGame.kt in the production app).
        val incoming = mapOf(
            "clock" to "PT04M32.00S",
            "clockRunning" to "1"
        )
        val current = mapOf(
            "content" to mapOf(
                "clock" to mapOf(
                    "snapshotSeconds" to 300,
                    "isRunning" to false
                )
            )
        )

        val result = resolver.applyBindings(
            currentData = current,
            incomingMessage = incoming,
            dataBinding = binding,
            correlationId = null,
            stringTable = null,
            sectionId = "s"
        )

        @Suppress("UNCHECKED_CAST")
        val clock = (result["content"] as Map<String, Any?>)["clock"] as Map<String, Any?>
        assertEquals(272, (clock["snapshotSeconds"] as Number).toInt())
        assertEquals(true, clock["isRunning"])
        assertNotNull(clock["snapshotAt"])
        assertTrue((clock["snapshotAt"] as String).isNotBlank())
    }

    @Test
    fun `liveClockSnapshot defaults missing running flag to paused`() {
        val resolver = DataBindingResolver()
        val binding = DataBinding(
            bindings = listOf(
                DataBindingPath(
                    sourcePath = "$.clock",
                    targetPath = "content.clock",
                    transform = Transform.LiveClockSnapshot
                )
            ),
            stringKeys = null
        )

        val incoming = mapOf("clock" to "Q3 04:32")
        val current = mapOf(
            "content" to mapOf(
                "clock" to mapOf(
                    "snapshotSeconds" to 300,
                    "isRunning" to true
                )
            )
        )

        val result = resolver.applyBindings(
            currentData = current,
            incomingMessage = incoming,
            dataBinding = binding,
            correlationId = null,
            stringTable = null,
            sectionId = "s"
        )

        @Suppress("UNCHECKED_CAST")
        val clock = (result["content"] as Map<String, Any?>)["clock"] as Map<String, Any?>
        assertEquals(272, (clock["snapshotSeconds"] as Number).toInt())
        assertEquals(false, clock["isRunning"])
    }
}
