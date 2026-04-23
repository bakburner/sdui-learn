package com.nba.sdui.core

import com.fasterxml.jackson.databind.JsonNode
import com.nba.sdui.core.models.generated.SduiModels
import com.nba.sdui.core.models.generated.mapper
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File
import java.io.FileFilter

class SchemaRoundTripTest {

    @Test
    fun `round-trip schema examples`() {
        val examplesDir = findExamplesDir()
        val fixturesArray: Array<File>? = examplesDir.listFiles(FileFilter { file ->
            file.isFile && file.extension == "json"
        })

        if (fixturesArray == null || fixturesArray.isEmpty()) {
            throw AssertionError("No JSON fixtures found under ${examplesDir.path}")
        }

        val fixtures = fixturesArray.toMutableList()
        fixtures.sortBy { file -> file.name }

        for (fixture in fixtures) {
            val originalTree = mapper.readTree(fixture)
            if (isScreenPayload(originalTree)) {
                val model = mapper.readValue(fixture, SduiModels::class.java)
                val roundTripTree = mapper.readTree(mapper.writeValueAsString(model))
                assertJsonEquals(fixture.name, originalTree, roundTripTree)
            } else {
                // Non-screen payloads should still be valid JSON.
                assertTrue(!originalTree.isMissingNode, "${fixture.name}: JSON parse failed")
            }
        }
    }

    private fun findExamplesDir(): File {
        var current = File(System.getProperty("user.dir"))
        repeat(6) {
            val candidate = File(current, "schema/examples")
            if (candidate.isDirectory) {
                return candidate
            }
            val parent = current.parentFile ?: return@repeat
            current = parent
        }
        error("schema/examples not found from ${System.getProperty("user.dir")}")
    }

    private fun isScreenPayload(node: JsonNode): Boolean {
        return node.has("schemaVersion") && node.has("sections") && node.get("sections").isArray
    }

    private fun assertJsonEquals(fixtureName: String?, expected: JsonNode, actual: JsonNode) {
        if (!expected.equals(actual)) {
            val name = fixtureName ?: "<unknown>"
            throw AssertionError("${name}: round-trip mismatch")
        }
    }
}
