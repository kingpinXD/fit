package com.example.fit.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class PreloadProgrammesTest {

    @Test
    fun `parse essentials 2x produces 180 exercises`() {
        val json = loadResource("essentials_2x.json")
        val exercises = ProgrammeRepository.parseProgramme(json)
        assertEquals(180, exercises.size)
    }

    @Test
    fun `parse essentials 3x produces 240 exercises`() {
        val json = loadResource("essentials_3x.json")
        val exercises = ProgrammeRepository.parseProgramme(json)
        assertEquals(240, exercises.size)
    }

    @Test
    fun `parse essentials 4x produces 288 exercises`() {
        val json = loadResource("essentials_4x.json")
        val exercises = ProgrammeRepository.parseProgramme(json)
        assertEquals(288, exercises.size)
    }

    @Test
    fun `parse essentials 5x produces 324 exercises`() {
        val json = loadResource("essentials_5x.json")
        val exercises = ProgrammeRepository.parseProgramme(json)
        assertEquals(324, exercises.size)
    }

    @Test
    fun `parse essentials 2x has Full Body A and Full Body B days`() {
        val json = loadResource("essentials_2x.json")
        val exercises = ProgrammeRepository.parseProgramme(json)
        val week1Days = exercises.filter { it.weekNumber == 1 }.map { it.dayName }.distinct()
        assertEquals(listOf("Full Body A", "Full Body B"), week1Days)
    }

    @Test
    fun `parse essentials 3x has Full Body Upper and Lower days`() {
        val json = loadResource("essentials_3x.json")
        val exercises = ProgrammeRepository.parseProgramme(json)
        val week1Days = exercises.filter { it.weekNumber == 1 }.map { it.dayName }.distinct()
        assertEquals(listOf("Full Body", "Upper", "Lower"), week1Days)
    }

    @Test
    fun `parse essentials 4x has four distinct days`() {
        val json = loadResource("essentials_4x.json")
        val exercises = ProgrammeRepository.parseProgramme(json)
        val week1Days = exercises.filter { it.weekNumber == 1 }.map { it.dayName }.distinct()
        assertEquals(listOf("Upper", "Lower", "Upper #2", "Lower #2"), week1Days)
    }

    @Test
    fun `parse essentials 5x has five distinct days`() {
        val json = loadResource("essentials_5x.json")
        val exercises = ProgrammeRepository.parseProgramme(json)
        val week1Days = exercises.filter { it.weekNumber == 1 }.map { it.dayName }.distinct()
        assertEquals(listOf("Upper", "Lower", "Push", "Pull", "Legs"), week1Days)
    }

    @Test
    fun `bundled programmes list has 4 entries`() {
        assertEquals(4, ProgrammeRepository.BUNDLED_PROGRAMMES.size)
    }

    @Test
    fun `each bundled programme has name and asset path`() {
        ProgrammeRepository.BUNDLED_PROGRAMMES.forEach { (name, asset) ->
            assertTrue("Name should not be blank", name.isNotBlank())
            assertTrue("Asset should end with .json", asset.endsWith(".json"))
        }
    }

    @Test
    fun `parse essentials 5x JSON has sub1 and videoUrl`() {
        val json = loadResource("essentials_5x.json")
        val exercises = ProgrammeRepository.parseProgramme(json)
        val first = exercises.first { it.weekNumber == 1 && it.dayName == "Upper" && it.orderIndex == 1 }
        assertEquals("Machine Chest Press", first.sub1)
        assertEquals("Weighted Dip", first.sub2)
        assertTrue(first.videoUrl.contains("youtu"))
    }

    @Test
    fun `parse essentials 4x JSON has 4 unique days`() {
        val json = loadResource("essentials_4x.json")
        val exercises = ProgrammeRepository.parseProgramme(json)
        val week1Days = exercises.filter { it.weekNumber == 1 }.map { it.dayName }.distinct()
        assertEquals(4, week1Days.size)
        assertTrue(week1Days.contains("Upper #2"))
    }

    @Test
    fun `parse essentials 2x JSON has sub1`() {
        val json = loadResource("essentials_2x.json")
        val exercises = ProgrammeRepository.parseProgramme(json)
        val withSubs = exercises.filter { it.sub1.isNotBlank() }
        assertTrue("Should have exercises with substitutions", withSubs.isNotEmpty())
    }

    private fun loadResource(name: String): String {
        return javaClass.classLoader!!.getResourceAsStream(name)!!
            .bufferedReader().use { it.readText() }
    }
}
