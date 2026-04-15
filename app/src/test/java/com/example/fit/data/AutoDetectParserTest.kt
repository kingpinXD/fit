package com.example.fit.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.io.InputStream

@RunWith(JUnit4::class)
class AutoDetectParserTest {

    private fun loadResource(name: String): InputStream {
        return javaClass.classLoader!!.getResourceAsStream("programmes/$name")!!
    }

    @Test
    fun `auto-detect weekly plan - produces exercises`() {
        val exercises = XlsxParser.parse(loadResource("weekly_plan.xlsx"))
        assertTrue("Should produce exercises", exercises.isNotEmpty())
    }

    @Test
    fun `auto-detect weekly plan - all exercises in week 1`() {
        val exercises = XlsxParser.parse(loadResource("weekly_plan.xlsx"))
        assertTrue(exercises.all { it.weekNumber == 1 })
    }

    @Test
    fun `auto-detect weekly plan - has correct day names`() {
        val exercises = XlsxParser.parse(loadResource("weekly_plan.xlsx"))
        val days = exercises.map { it.dayName }.distinct()
        assertTrue(days.any { it.contains("Push") })
        assertTrue(days.any { it.contains("Pull") })
        assertTrue(days.any { it.contains("Legs") || it.contains("Core") })
        assertTrue(days.any { it.contains("Upper") })
    }

    @Test
    fun `auto-detect weekly plan - push day has 6 exercises`() {
        val exercises = XlsxParser.parse(loadResource("weekly_plan.xlsx"))
        val pushExercises = exercises.filter { it.dayName.contains("Push") }
        assertEquals(6, pushExercises.size)
    }

    @Test
    fun `auto-detect weekly plan - pull day has 5 exercises`() {
        val exercises = XlsxParser.parse(loadResource("weekly_plan.xlsx"))
        val pullExercises = exercises.filter { it.dayName.contains("Pull") }
        assertEquals(5, pullExercises.size)
    }

    @Test
    fun `auto-detect weekly plan - first exercise has correct data`() {
        val exercises = XlsxParser.parse(loadResource("weekly_plan.xlsx"))
        val first = exercises.first()
        assertEquals("Incline Dumbbell Press", first.exerciseName)
        assertEquals(3, first.sets)
        assertTrue(first.reps.contains("10"))
        assertEquals(1, first.orderIndex)
    }

    @Test
    fun `auto-detect weekly plan - exercises with notes preserved`() {
        val exercises = XlsxParser.parse(loadResource("weekly_plan.xlsx"))
        val withNotes = exercises.filter { it.notes.isNotBlank() }
        assertTrue("Some exercises should have notes", withNotes.isNotEmpty())
        val ote = exercises.find { it.exerciseName == "Overhead Triceps Extension" }
        assertNotNull(ote)
        assertEquals("Light, controlled", ote!!.notes)
    }

    @Test
    fun `auto-detect weekly plan - exercises without sets default to 1`() {
        val exercises = XlsxParser.parse(loadResource("weekly_plan.xlsx"))
        // Day 6 cardio exercises have no sets column
        val cardio = exercises.filter { it.dayName.contains("Cardio") }
        assertTrue(cardio.all { it.sets >= 1 })
    }

    @Test
    fun `auto-detect weekly plan - duration reps preserved as string`() {
        val exercises = XlsxParser.parse(loadResource("weekly_plan.xlsx"))
        val plank = exercises.find { it.exerciseName == "Plank" }
        assertNotNull(plank)
        assertTrue(plank!!.reps.contains("sec") || plank.reps.contains("30"))
    }

    @Test
    fun `auto-detect weekly plan - skips note-only rows`() {
        val exercises = XlsxParser.parse(loadResource("weekly_plan.xlsx"))
        // Row 8 is note-only: "Avoid deep barbell bench or heavy overhead presses"
        val noteAsExercise = exercises.find { it.exerciseName.contains("Avoid") }
        assertNull("Note-only rows should be skipped", noteAsExercise)
    }

    @Test
    fun `auto-detect weekly plan - missing fields are empty strings`() {
        val exercises = XlsxParser.parse(loadResource("weekly_plan.xlsx"))
        assertTrue(exercises.all { it.rpe.isBlank() || it.rpe == "" })
        assertTrue(exercises.all { it.warmupSets == "0" || it.warmupSets.isBlank() })
    }

    @Test
    fun `auto-detect weekly plan - recovery day exercise stored`() {
        val exercises = XlsxParser.parse(loadResource("weekly_plan.xlsx"))
        // Day 4 has "Walking / Cycling / Mobility" with no sets/reps
        val recovery = exercises.filter { it.dayName.contains("Recovery") }
        assertEquals(1, recovery.size)
        assertEquals("Walking / Cycling / Mobility", recovery.first().exerciseName)
        assertEquals(1, recovery.first().sets)
    }

    @Test
    fun `auto-detect weekly plan - cardio day exercises parsed`() {
        val exercises = XlsxParser.parse(loadResource("weekly_plan.xlsx"))
        val cardio = exercises.filter { it.dayName.contains("Cardio") }
        assertEquals(4, cardio.size)
        assertEquals("Incline treadmill walk", cardio.first().exerciseName)
    }

    @Test
    fun `auto-detect weekly plan - rest day has no exercises`() {
        val exercises = XlsxParser.parse(loadResource("weekly_plan.xlsx"))
        val rest = exercises.filter { it.dayName.contains("Rest") }
        assertTrue("Rest day should have no exercises", rest.isEmpty())
    }

    @Test
    fun `auto-detect weekly plan - order indices sequential per day`() {
        val exercises = XlsxParser.parse(loadResource("weekly_plan.xlsx"))
        val grouped = exercises.groupBy { it.dayName }
        grouped.forEach { (day, dayExercises) ->
            val indices = dayExercises.map { it.orderIndex }
            assertEquals(
                "$day should have sequential order indices starting at 1",
                (1..dayExercises.size).toList(),
                indices
            )
        }
    }

    @Test
    fun `existing essentials format still works after auto-detect changes`() {
        val exercises = XlsxParser.parse(loadResource("essentials_5x.xlsx"))
        assertEquals(324, exercises.size)
        val week1Days = exercises.filter { it.weekNumber == 1 }.map { it.dayName }.distinct()
        assertEquals(5, week1Days.size)
    }
}
