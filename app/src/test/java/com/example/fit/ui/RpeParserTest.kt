package com.example.fit.ui

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class RpeParserTest {

    @Test
    fun `parse range takes higher value`() {
        assertEquals(10, parseDefaultRpe("9-10"))
    }

    @Test
    fun `parse single value`() {
        assertEquals(8, parseDefaultRpe("8"))
    }

    @Test
    fun `parse empty returns default 5`() {
        assertEquals(5, parseDefaultRpe(""))
    }

    @Test
    fun `parse blank returns default 5`() {
        assertEquals(5, parseDefaultRpe("  "))
    }

    @Test
    fun `parse range 7-8 takes 8`() {
        assertEquals(8, parseDefaultRpe("7-8"))
    }

    @Test
    fun `parse range 8-9 takes 9`() {
        assertEquals(9, parseDefaultRpe("8-9"))
    }

    @Test
    fun `parse non-numeric returns default 5`() {
        assertEquals(5, parseDefaultRpe("See Notes"))
    }

    @Test
    fun `parse N-A returns default 5`() {
        assertEquals(5, parseDefaultRpe("N/A"))
    }

    @Test
    fun `parse value clamped to 10 max`() {
        assertEquals(10, parseDefaultRpe("12"))
    }

    @Test
    fun `parse value clamped to 1 min`() {
        assertEquals(1, parseDefaultRpe("0"))
    }
}
