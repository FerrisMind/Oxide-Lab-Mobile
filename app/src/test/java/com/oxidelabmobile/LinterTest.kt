package com.oxidelabmobile

import org.junit.Assert.assertEquals
import org.junit.Test

class LinterTest {
    @Test
    fun testLinterSetup() {
        val value = 1 + 2
        assertEquals(3, value)
    }

    fun unusedFunction() {
        val unusedVariable = "test"
    }
}
