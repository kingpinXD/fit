package com.example.fit.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.io.InputStream

@RunWith(JUnit4::class)
class XlsxParserTest {

    private fun loadResource(name: String): InputStream {
        return javaClass.classLoader!!.getResourceAsStream("programmes/$name")!!
    }

    // --- essentials_5x ---

    @Test
    fun `parse essentials 5x - correct week and day count`() {
        val exercises = XlsxParser.parse(loadResource("essentials_5x.xlsx"))
        val weeks = exercises.map { it.weekNumber }.distinct().sorted()
        assertEquals(12, weeks.size)

        val week1Days = exercises.filter { it.weekNumber == 1 }.map { it.dayName }.distinct()
        assertEquals(5, week1Days.size)
        assertTrue(week1Days.containsAll(listOf("Upper", "Lower", "Push", "Pull", "Legs")))
    }

    @Test
    fun `parse essentials 5x - correct exercise count per day`() {
        val exercises = XlsxParser.parse(loadResource("essentials_5x.xlsx"))
        val week1Upper = exercises.filter { it.weekNumber == 1 && it.dayName == "Upper" }
        assertEquals(7, week1Upper.size)

        val week1Lower = exercises.filter { it.weekNumber == 1 && it.dayName == "Lower" }
        assertEquals(5, week1Lower.size)
    }

    @Test
    fun `parse essentials 5x - first exercise parsed correctly`() {
        val exercises = XlsxParser.parse(loadResource("essentials_5x.xlsx"))
        val first = exercises.first { it.weekNumber == 1 && it.dayName == "Upper" && it.orderIndex == 1 }
        assertEquals("Flat DB Press (Heavy)", first.exerciseName)
        assertEquals("8-9", first.rpe)
        assertEquals("2-3", first.warmupSets)
        assertEquals(1, first.sets)
        assertEquals("4-6", first.reps)
    }

    @Test
    fun `parse essentials 5x - dropset in reps preserved`() {
        val exercises = XlsxParser.parse(loadResource("essentials_5x.xlsx"))
        val cableRow = exercises.first {
            it.weekNumber == 1 && it.dayName == "Upper" && it.exerciseName == "Seated Cable Row"
        }
        assertTrue(cableRow.reps.contains("dropset"))
    }

    @Test
    fun `parse essentials 5x - exercises change between phases`() {
        val exercises = XlsxParser.parse(loadResource("essentials_5x.xlsx"))
        val week1Upper1 = exercises.first { it.weekNumber == 1 && it.dayName == "Upper" && it.orderIndex == 1 }
        val week5Upper1 = exercises.first { it.weekNumber == 5 && it.dayName == "Upper" && it.orderIndex == 1 }
        assertNotEquals(week1Upper1.exerciseName, week5Upper1.exerciseName)
    }

    @Test
    fun `parse essentials 5x - day order preserved`() {
        val exercises = XlsxParser.parse(loadResource("essentials_5x.xlsx"))
        val week1 = exercises.filter { it.weekNumber == 1 }
        val dayOrder = week1.map { it.dayName }.distinct()
        assertEquals(listOf("Upper", "Lower", "Push", "Pull", "Legs"), dayOrder)
    }

    @Test
    fun `parse essentials 5x - notes populated`() {
        val exercises = XlsxParser.parse(loadResource("essentials_5x.xlsx"))
        val withNotes = exercises.filter { it.notes.isNotBlank() }
        assertTrue("Should have exercises with notes", withNotes.isNotEmpty())
    }

    // --- essentials_2x ---

    @Test
    fun `parse essentials 2x - full body days`() {
        val exercises = XlsxParser.parse(loadResource("essentials_2x.xlsx"))
        val week1Days = exercises.filter { it.weekNumber == 1 }.map { it.dayName }.distinct()
        assertTrue(week1Days.containsAll(listOf("Full Body A", "Full Body B")))
        assertEquals(2, week1Days.size)
    }

    @Test
    fun `parse essentials 2x - exercise count per day`() {
        val exercises = XlsxParser.parse(loadResource("essentials_2x.xlsx"))
        val week1A = exercises.filter { it.weekNumber == 1 && it.dayName == "Full Body A" }
        assertEquals(8, week1A.size)
        val week1B = exercises.filter { it.weekNumber == 1 && it.dayName == "Full Body B" }
        assertEquals(7, week1B.size)
    }

    // --- essentials_3x ---

    @Test
    fun `parse essentials 3x - mixed day names`() {
        val exercises = XlsxParser.parse(loadResource("essentials_3x.xlsx"))
        val week1Days = exercises.filter { it.weekNumber == 1 }.map { it.dayName }.distinct()
        assertEquals(3, week1Days.size)
        assertTrue(week1Days.containsAll(listOf("Full Body", "Upper", "Lower")))
    }

    // --- essentials_4x ---

    @Test
    fun `parse essentials 4x - upper lower split`() {
        val exercises = XlsxParser.parse(loadResource("essentials_4x.xlsx"))
        val week1Days = exercises.filter { it.weekNumber == 1 }.map { it.dayName }.distinct()
        assertTrue(week1Days.all { it in listOf("Upper", "Lower") })
    }

    @Test
    fun `parse essentials 4x - four sessions per week`() {
        val exercises = XlsxParser.parse(loadResource("essentials_4x.xlsx"))
        // 4x has two Upper and two Lower sessions per week
        val week1Upper = exercises.filter { it.weekNumber == 1 && it.dayName == "Upper" }
        val week1Lower = exercises.filter { it.weekNumber == 1 && it.dayName == "Lower" }
        // First Upper = 7 exercises, second Upper = 8 exercises
        assertTrue("Should have exercises from both Upper sessions", week1Upper.size > 7)
        assertTrue("Should have exercises from both Lower sessions", week1Lower.size > 5)
    }

    // --- ppl_5x ---

    @Test
    fun `parse ppl 5x - column A layout detected`() {
        val exercises = XlsxParser.parse(loadResource("ppl_5x.xlsx"))
        assertTrue(exercises.isNotEmpty())
        val week1Days = exercises.filter { it.weekNumber == 1 }.map { it.dayName }.distinct()
        assertTrue(week1Days.isNotEmpty())
        assertTrue(week1Days.any { it.contains("Push") || it.contains("Pull") || it.contains("Legs") })
    }

    @Test
    fun `parse ppl 5x - all five days present`() {
        val exercises = XlsxParser.parse(loadResource("ppl_5x.xlsx"))
        val dayNames = exercises.map { it.dayName }.distinct()
        assertEquals(5, dayNames.size)
        assertTrue(dayNames.containsAll(listOf("Push #1", "Pull #1", "Legs #1", "Upper #1", "Lower #1")))
    }

    @Test
    fun `parse ppl 5x - exercises have valid data`() {
        val exercises = XlsxParser.parse(loadResource("ppl_5x.xlsx"))
        val firstPush = exercises.filter { it.dayName.contains("Push") }.firstOrNull()
        assertNotNull(firstPush)
        assertTrue(firstPush!!.exerciseName.isNotBlank())
        assertTrue(firstPush.sets > 0)
        assertTrue(firstPush.reps.isNotBlank())
    }

    @Test
    fun `parse ppl 5x - push day has correct exercises`() {
        val exercises = XlsxParser.parse(loadResource("ppl_5x.xlsx"))
        val push = exercises.filter { it.dayName == "Push #1" }
        assertEquals(6, push.size)
        assertEquals("Bench Press", push.first().exerciseName)
    }

    @Test
    fun `parse ppl 5x - string RPE values preserved`() {
        val exercises = XlsxParser.parse(loadResource("ppl_5x.xlsx"))
        val seeNotes = exercises.filter { it.rpe == "See Notes" }
        assertTrue("PPL has exercises with 'See Notes' RPE", seeNotes.isNotEmpty())
    }

    @Test
    fun `parse ppl 5x - NA RPE values preserved`() {
        val exercises = XlsxParser.parse(loadResource("ppl_5x.xlsx"))
        val na = exercises.filter { it.rpe.equals("N/A", ignoreCase = true) }
        assertTrue("PPL has exercises with 'N/A' RPE", na.isNotEmpty())
    }

    // --- cross-file validation ---

    @Test
    fun `all files parse without exceptions`() {
        val files = listOf(
            "essentials_2x.xlsx",
            "essentials_3x.xlsx",
            "essentials_4x.xlsx",
            "essentials_5x.xlsx",
            "ppl_5x.xlsx"
        )
        files.forEach { file ->
            val exercises = XlsxParser.parse(loadResource(file))
            assertTrue("$file should produce exercises", exercises.isNotEmpty())
            exercises.forEach { ex ->
                assertTrue("$file: exercise name should not be blank", ex.exerciseName.isNotBlank())
                assertTrue("$file: sets should be positive", ex.sets > 0)
                assertTrue("$file: reps should not be blank", ex.reps.isNotBlank())
                assertTrue("$file: weekNumber should be positive", ex.weekNumber > 0)
                assertTrue("$file: dayName should not be blank", ex.dayName.isNotBlank())
            }
        }
    }

    @Test
    fun `all exercises have sequential order indices within each day`() {
        val files = listOf(
            "essentials_2x.xlsx",
            "essentials_3x.xlsx",
            "essentials_4x.xlsx",
            "essentials_5x.xlsx",
            "ppl_5x.xlsx"
        )
        files.forEach { file ->
            val exercises = XlsxParser.parse(loadResource(file))
            val grouped = exercises.groupBy { "${it.weekNumber}-${it.dayName}" }
            grouped.forEach { (key, dayExercises) ->
                val indices = dayExercises.map { it.orderIndex }
                assertEquals(
                    "$file: $key should have sequential order indices starting at 1",
                    (1..dayExercises.size).toList(),
                    indices
                )
            }
        }
    }
}
