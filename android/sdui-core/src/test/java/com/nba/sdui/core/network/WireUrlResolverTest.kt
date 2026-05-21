package com.nba.sdui.core.network

import org.junit.Assert.assertEquals
import org.junit.Test

class WireUrlResolverTest {
    @Test
    fun `leaves absolute urls unchanged`() {
        assertEquals(
            "https://cdn.example.com/a.png",
            WireUrlResolver.resolve("https://cdn.example.com/a.png", "http://10.0.2.2:8080")
        )
    }

    @Test
    fun `prefixes root relative demo assets`() {
        assertEquals(
            "http://10.0.2.2:8080/sdui-demo/card-wide.svg?v=heroarena",
            WireUrlResolver.resolve("/sdui-demo/card-wide.svg?v=heroarena", "http://10.0.2.2:8080")
        )
    }
}
