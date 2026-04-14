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
 * Supports two column layouts:
 * - Offset 1 (default): Column B = week/day, C = exercise, D = warmup, E = sets, F = reps, H = RPE, L = notes
 * - Offset 0 (PPL-style): Column A = week/day, B = exercise, C = warmup, D = sets, E = reps, G = RPE, K = notes
 *
 * Day names are detected dynamically (e.g., "Upper", "Full Body A", "Push #1").
 * Week headers support both "Week 1" and "Week 1 Day 1" formats.
 */
object XlsxParser {

    private val WEEK_PATTERN = Regex("^Week\\s+(\\d+).*", RegexOption.IGNORE_CASE)
    private val SKIP_VALUES = setOf("exercise", "warm-up sets", "working sets", "reps", "load", "rpe", "rest", "notes")

    fun parse(inputStream: InputStream): List<Exercise> {
        val workbook = XSSFWorkbook(inputStream)
        val sheet = workbook.getSheetAt(0)
        val offset = detectColumnOffset(sheet)
        val exercises = mutableListOf<Exercise>()

        var currentWeek: Int? = null
        var currentDay: String? = null
        // Track exercise count per (week, dayName) so the 4x program's repeated
        // "Upper"/"Lower" sessions within the same week get continuous indices.
        val dayCounters = mutableMapOf<String, Int>()
        // Track day name occurrences within a week to disambiguate duplicates
        // (e.g., 4x has Upper, Lower, Upper, Lower → Upper, Lower, Upper #2, Lower #2)
        val dayCountsPerWeek = mutableMapOf<String, Int>()

        for (row in sheet) {
            val labelCol = getCellString(row.getCell(offset))
            val exerciseCol = getCellString(row.getCell(offset + 1))

            if (labelCol.isNotBlank()) {
                // Check for week header first (before header-row skip, since week rows
                // also contain "Exercise" in the next column)
                val weekMatch = WEEK_PATTERN.matchEntire(labelCol.trim())
                if (weekMatch != null) {
                    currentWeek = weekMatch.groupValues[1].toInt()
                    currentDay = null
                    dayCountsPerWeek.clear()
                    continue
                }

                // Check for day label: non-week, non-skip, non-rest-day text
                if (isDayName(labelCol) && currentWeek != null) {
                    val dayLabel = labelCol.trim()
                    val count = (dayCountsPerWeek[dayLabel] ?: 0) + 1
                    dayCountsPerWeek[dayLabel] = count
                    currentDay = if (count > 1) "$dayLabel #$count" else dayLabel

                    // First exercise may be on the same row as the day label
                    if (exerciseCol.isNotBlank()) {
                        val sets = getNumericCell(row.getCell(offset + 3))
                        if (sets != null) {
                            val key = "$currentWeek-$currentDay"
                            val idx = dayCounters.merge(key, 1, Int::plus)!!
                            exercises.add(buildExercise(row, offset, currentWeek, currentDay, idx))
                        }
                    }
                    continue
                }

                // Other label text (e.g., "Suggested Rest Day", "Copyright", title rows) -- skip
                continue
            }

            // Exercise row: empty label column, has exercise name
            if (exerciseCol.isNotBlank() && currentDay != null && currentWeek != null) {
                val sets = getNumericCell(row.getCell(offset + 3))
                if (sets != null) {
                    val key = "$currentWeek-$currentDay"
                    val idx = dayCounters.merge(key, 1, Int::plus)!!
                    exercises.add(buildExercise(row, offset, currentWeek, currentDay, idx))
                }
            }
        }

        workbook.close()
        return exercises
    }

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

    private fun buildExercise(
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
