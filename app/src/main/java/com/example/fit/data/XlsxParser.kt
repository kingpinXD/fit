package com.example.fit.data

import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.DateUtil
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.InputStream
import java.util.Calendar

/**
 * Parses workout programme XLSX files into Exercise entities.
 *
 * Auto-detects two format families:
 *
 * Week-based (Essentials / PPL):
 * - Column B (or A) = week/day headers, exercises follow beneath
 * - Supports offset 0 (PPL) and offset 1 (Essentials) column layouts
 *
 * Header-based (weekly plan):
 * - Row 1 has column headers (Day, Focus, Exercise, Sets, Reps/Duration, Notes)
 * - Day labels like "Day 1 – Push" in column A
 * - Single week stored as week 1
 */
object XlsxParser {

    private val WEEK_PATTERN = Regex("^Week\\s+(\\d+).*", RegexOption.IGNORE_CASE)
    private val SKIP_VALUES = setOf("exercise", "warm-up sets", "working sets", "reps", "load", "rpe", "rest", "notes")
    private val DAY_LABEL_PATTERN = Regex("Day\\s+\\d+\\s*[–\\-]\\s*(.+)", RegexOption.IGNORE_CASE)

    private enum class Format { WEEK_BASED, HEADER_BASED }

    fun parse(inputStream: InputStream): List<Exercise> {
        val workbook = XSSFWorkbook(inputStream)
        val sheet = workbook.getSheetAt(0)

        val format = detectFormat(sheet)
        val exercises = when (format) {
            Format.WEEK_BASED -> parseWeekBased(sheet)
            Format.HEADER_BASED -> parseHeaderBased(sheet)
        }

        workbook.close()
        return exercises
    }

    /**
     * Detect format by scanning the first rows:
     * - If any row matches "Week N" → week-based (Essentials/PPL)
     * - If row 1 has header-like column names → header-based
     * - Default to week-based
     */
    private fun detectFormat(sheet: Sheet): Format {
        // Check for "Week N" pattern in first 20 rows
        for (i in 0..minOf(20, sheet.lastRowNum)) {
            val row = sheet.getRow(i) ?: continue
            for (j in 0..minOf(5, row.lastCellNum.toInt())) {
                val value = getCellString(row.getCell(j)).trim()
                if (WEEK_PATTERN.matches(value)) return Format.WEEK_BASED
            }
        }

        // Check if row 1 (index 0) has header-like values
        val firstRow = sheet.getRow(0)
        if (firstRow != null) {
            val headerNames = setOf("exercise", "exercises", "sets", "reps", "day", "days",
                "reps/duration", "notes", "note", "focus", "description")
            var headerCount = 0
            for (j in 0..minOf(10, firstRow.lastCellNum.toInt())) {
                val value = getCellString(firstRow.getCell(j)).trim().lowercase()
                if (value in headerNames) headerCount++
            }
            if (headerCount >= 2) return Format.HEADER_BASED
        }

        return Format.WEEK_BASED
    }

    // ---- Week-based parsing (Essentials / PPL) ----

    private fun parseWeekBased(sheet: Sheet): List<Exercise> {
        val offset = detectColumnOffset(sheet)
        val exercises = mutableListOf<Exercise>()

        var currentWeek: Int? = null
        var currentDay: String? = null
        val dayCounters = mutableMapOf<String, Int>()
        val dayCountsPerWeek = mutableMapOf<String, Int>()

        for (row in sheet) {
            val labelCol = getCellString(row.getCell(offset))
            val exerciseCol = getCellString(row.getCell(offset + 1))

            if (labelCol.isNotBlank()) {
                val weekMatch = WEEK_PATTERN.matchEntire(labelCol.trim())
                if (weekMatch != null) {
                    currentWeek = weekMatch.groupValues[1].toInt()
                    currentDay = null
                    dayCountsPerWeek.clear()
                    continue
                }

                if (isDayName(labelCol) && currentWeek != null) {
                    val dayLabel = labelCol.trim()
                    val count = (dayCountsPerWeek[dayLabel] ?: 0) + 1
                    dayCountsPerWeek[dayLabel] = count
                    currentDay = if (count > 1) "$dayLabel #$count" else dayLabel

                    if (exerciseCol.isNotBlank()) {
                        val sets = getNumericCell(row.getCell(offset + 3))
                        if (sets != null) {
                            val key = "$currentWeek-$currentDay"
                            val idx = dayCounters.merge(key, 1, Int::plus)!!
                            exercises.add(buildWeekBasedExercise(row, offset, currentWeek, currentDay, idx))
                        }
                    }
                    continue
                }

                continue
            }

            if (exerciseCol.isNotBlank() && currentDay != null && currentWeek != null) {
                val sets = getNumericCell(row.getCell(offset + 3))
                if (sets != null) {
                    val key = "$currentWeek-$currentDay"
                    val idx = dayCounters.merge(key, 1, Int::plus)!!
                    exercises.add(buildWeekBasedExercise(row, offset, currentWeek, currentDay, idx))
                }
            }
        }

        return exercises
    }

    // ---- Header-based parsing (weekly plan) ----

    private fun parseHeaderBased(sheet: Sheet): List<Exercise> {
        val headerRow = sheet.getRow(0) ?: return emptyList()
        val headers = buildHeaderMap(headerRow)

        val exerciseCol = headers["exercise"] ?: return emptyList()
        val setsCol = headers["sets"]
        val repsCol = headers["reps"]
        val notesCol = headers["notes"]
        val dayCol = headers["day"]

        val exercises = mutableListOf<Exercise>()
        var currentDay: String? = null
        val dayCounters = mutableMapOf<String, Int>()

        for (i in 1..sheet.lastRowNum) {
            val row = sheet.getRow(i) ?: continue

            // Check for day label
            val dayValue = if (dayCol != null) getCellString(row.getCell(dayCol)).trim() else ""
            if (dayValue.isNotBlank()) {
                val extracted = extractDayName(dayValue)
                // Rest days with no exercises on this row
                if (extracted.equals("Rest", ignoreCase = true)) continue
                currentDay = extracted
            }

            if (currentDay == null) continue

            val exerciseName = getCellString(row.getCell(exerciseCol)).trim()
            val setsValue = if (setsCol != null) getNumericCell(row.getCell(setsCol)) else null
            val repsValue = if (repsCol != null) getCellString(row.getCell(repsCol)).trim() else ""
            val notesValue = if (notesCol != null) getCellString(row.getCell(notesCol)).trim() else ""

            // Skip note-only rows (no exercise name)
            if (exerciseName.isBlank()) continue

            val sets = setsValue ?: 1

            val key = currentDay
            val idx = dayCounters.merge(key, 1, Int::plus)!!

            exercises.add(
                Exercise(
                    weekNumber = 1,
                    dayName = currentDay,
                    exerciseName = exerciseName,
                    sets = sets,
                    reps = repsValue,
                    orderIndex = idx,
                    rpe = "",
                    rest = "",
                    notes = notesValue,
                    warmupSets = "0",
                    sub1 = "",
                    sub2 = "",
                    videoUrl = "",
                    sub1VideoUrl = "",
                    sub2VideoUrl = ""
                )
            )
        }

        return exercises
    }

    /**
     * Map header row values to column indices. Case-insensitive matching.
     */
    private fun buildHeaderMap(row: Row): Map<String, Int> {
        val map = mutableMapOf<String, Int>()
        for (j in 0..row.lastCellNum.toInt()) {
            val value = getCellString(row.getCell(j)).trim().lowercase()
            when {
                value in setOf("exercise", "exercises") -> map["exercise"] = j
                value in setOf("sets", "set") -> map["sets"] = j
                value in setOf("reps", "rep", "reps/duration") -> map["reps"] = j
                value in setOf("notes", "note") -> map["notes"] = j
                value in setOf("day", "days") -> map["day"] = j
                value in setOf("rpe") -> map["rpe"] = j
                value in setOf("rest") -> map["rest"] = j
                value in setOf("warm-up", "warmup", "warm-up sets") -> map["warmup"] = j
            }
        }
        return map
    }

    /**
     * Extract the meaningful day name from labels like "Day 1 – Push" → "Push".
     * Falls back to the raw string if no match.
     */
    internal fun extractDayName(raw: String): String {
        val match = DAY_LABEL_PATTERN.find(raw.trim())
        return match?.groupValues?.get(1)?.trim() ?: raw.trim()
    }

    // ---- Shared utilities ----

    /**
     * Scan the first 20 rows to determine whether week/day labels live in column A (offset=0)
     * or column B (offset=1).
     */
    internal fun detectColumnOffset(sheet: Sheet): Int {
        for (i in 0..minOf(20, sheet.lastRowNum)) {
            val row = sheet.getRow(i) ?: continue
            val colA = getCellString(row.getCell(0))
            if (WEEK_PATTERN.matches(colA.trim())) return 0
            val colB = getCellString(row.getCell(1))
            if (WEEK_PATTERN.matches(colB.trim())) return 1
        }
        return 1
    }

    /**
     * A day name is any non-blank label that is not a week header, not a header row value,
     * and not a rest/copyright row.
     */
    internal fun isDayName(value: String): Boolean {
        val trimmed = value.trim()
        if (trimmed.isBlank()) return false
        if (WEEK_PATTERN.matches(trimmed)) return false
        if (SKIP_VALUES.any { trimmed.equals(it, ignoreCase = true) }) return false
        if (trimmed.contains("Suggested", ignoreCase = true)) return false
        if (trimmed.contains("Rest Day", ignoreCase = true)) return false
        if (trimmed.contains("Mandatory", ignoreCase = true)) return false
        if (trimmed.contains("Copyright", ignoreCase = true)) return false
        if (trimmed.contains("Program", ignoreCase = true)) return false
        if (trimmed.contains("Volume", ignoreCase = true)) return false
        if (trimmed.contains("Intensity", ignoreCase = true)) return false
        if (trimmed.contains("Substitution", ignoreCase = true)) return false
        if (trimmed.contains("Warm-up Sets", ignoreCase = true)) return false
        return true
    }

    private fun buildWeekBasedExercise(
        row: Row,
        offset: Int,
        weekNumber: Int,
        dayName: String,
        orderIndex: Int
    ): Exercise {
        val exerciseName = getCellString(row.getCell(offset + 1)).trim()
        val warmup = parseDateOrNumber(row.getCell(offset + 2), default = "0")
        val sets = getNumericCell(row.getCell(offset + 3)) ?: 1
        val reps = getCellString(row.getCell(offset + 4)).trim()
        val rpe = parseDateOrNumber(row.getCell(offset + 6))
        val rest = getCellString(row.getCell(offset + 7)).trim()
        val sub1 = getCellString(row.getCell(offset + 8)).trim()
        val sub2 = getCellString(row.getCell(offset + 9)).trim()
        val notes = getCellString(row.getCell(offset + 10)).trim()
        val videoUrl = getHyperlink(row.getCell(offset + 1))
        val sub1VideoUrl = getHyperlink(row.getCell(offset + 8))
        val sub2VideoUrl = getHyperlink(row.getCell(offset + 9))

        return Exercise(
            weekNumber = weekNumber,
            dayName = dayName,
            exerciseName = exerciseName,
            sets = sets,
            reps = reps,
            orderIndex = orderIndex,
            rpe = rpe,
            rest = rest,
            notes = notes,
            warmupSets = warmup,
            sub1 = sub1,
            sub2 = sub2,
            videoUrl = videoUrl,
            sub1VideoUrl = sub1VideoUrl,
            sub2VideoUrl = sub2VideoUrl
        )
    }

    /**
     * Excel stores values like "8-9" as dates (2022-08-09). Extract month-day as the range.
     * Integers stay as-is. Strings pass through.
     */
    internal fun parseDateOrNumber(cell: Cell?, default: String = ""): String {
        if (cell == null) return default
        return when (cell.cellType) {
            CellType.NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    val date = cell.dateCellValue
                    val cal = Calendar.getInstance().apply { time = date }
                    "${cal.get(Calendar.MONTH) + 1}-${cal.get(Calendar.DAY_OF_MONTH)}"
                } else {
                    val num = cell.numericCellValue
                    if (num == num.toLong().toDouble()) {
                        num.toLong().toString()
                    } else {
                        num.toString()
                    }
                }
            }
            CellType.STRING -> cell.stringCellValue.trim().ifBlank { default }
            else -> default
        }
    }

    internal fun getCellString(cell: Cell?): String {
        if (cell == null) return ""
        return when (cell.cellType) {
            CellType.STRING -> cell.stringCellValue ?: ""
            CellType.NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) ""
                else {
                    val num = cell.numericCellValue
                    if (num == num.toLong().toDouble()) num.toLong().toString()
                    else num.toString()
                }
            }
            else -> ""
        }
    }

    private fun getNumericCell(cell: Cell?): Int? {
        if (cell == null) return null
        return when (cell.cellType) {
            CellType.NUMERIC -> cell.numericCellValue.toInt()
            CellType.STRING -> cell.stringCellValue.trim().toIntOrNull()
            else -> null
        }
    }

    private fun getHyperlink(cell: Cell?): String {
        if (cell == null) return ""
        return cell.hyperlink?.address ?: ""
    }
}
