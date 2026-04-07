package com.example.fit.data

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ProgrammeNameNormalizerTest {

    @Test
    fun `essentials program 2x`() {
        assertEquals(
            "the_essentials_2x",
            ProgrammeNameNormalizer.normalize("The Essentials Program 2x.xlsx")
        )
    }

    @Test
    fun `essentials program 5x`() {
        assertEquals(
            "the_essentials_5x",
            ProgrammeNameNormalizer.normalize("The Essentials Program 5x.xlsx")
        )
    }

    @Test
    fun `edited ppl 5x`() {
        assertEquals(
            "ppl_5x",
            ProgrammeNameNormalizer.normalize("Edited PPL 5x.xlsx")
        )
    }

    @Test
    fun `powerbuilding 6x with hyphenated spreadsheet`() {
        assertEquals(
            "powerbuilding-6x",
            ProgrammeNameNormalizer.normalize("POWERBUILDING-6x-Spreadsheet.xlsx")
        )
    }

    @Test
    fun `4x with parent folder fallback`() {
        assertEquals(
            "2.0_powerbuilding_4x",
            ProgrammeNameNormalizer.normalize("4x - SPREADSHEET.xlsx", "2.0 Powerbuilding Program")
        )
    }

    @Test
    fun `5-6x with parent folder fallback`() {
        assertEquals(
            "2.0_powerbuilding_5-6x",
            ProgrammeNameNormalizer.normalize("5-6x - SPREADSHEET.xlsx", "2.0 Powerbuilding Program")
        )
    }

    @Test
    fun `powerbuilding 3 dot 0`() {
        assertEquals(
            "powerbuilding_3.0",
            ProgrammeNameNormalizer.normalize("PowerBuilding 3.0.xlsx")
        )
    }

    @Test
    fun `pure bodybuilding full body with underscores and dash`() {
        assertEquals(
            "pure_bodybuilding_full_body",
            ProgrammeNameNormalizer.normalize("Pure_Bodybuilding_-_Full_Body.xlsx")
        )
    }

    @Test
    fun `pure bodybuilding phase 2 full body sheet`() {
        assertEquals(
            "pure_bodybuilding_phase_2_full_body",
            ProgrammeNameNormalizer.normalize("Pure Bodybuilding Phase 2 - Full Body Sheet.xlsx")
        )
    }

    @Test
    fun `full body program 4x`() {
        assertEquals(
            "full_body_4x",
            ProgrammeNameNormalizer.normalize("Full Body Program - 4x.xlsx")
        )
    }

    @Test
    fun `full body program 5x`() {
        assertEquals(
            "full_body_5x",
            ProgrammeNameNormalizer.normalize("Full Body Program - 5x.xlsx")
        )
    }

    @Test
    fun `ultimate push pull legs system 4x`() {
        assertEquals(
            "the_ultimate_push_pull_legs_system_4x",
            ProgrammeNameNormalizer.normalize("The_Ultimate_Push_Pull_Legs_System_-_4x.xlsx")
        )
    }

    @Test
    fun `essentials program no variant`() {
        assertEquals(
            "the_essentials",
            ProgrammeNameNormalizer.normalize("The Essentials Program.xlsx")
        )
    }
}
