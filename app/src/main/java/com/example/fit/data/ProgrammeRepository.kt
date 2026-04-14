package com.example.fit.data

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream
import java.time.Instant

enum class ImportResult { IMPORTED, SWITCHED }

class ProgrammeRepository(
    private val dao: ExerciseDao,
    private val logDao: ExerciseLogDao,
    private val programmeDao: ProgrammeDao,
    private val context: Context
) {

    private val prefs = context.getSharedPreferences("fit_prefs", Context.MODE_PRIVATE)

    fun getProgrammeName(): String = prefs.getString("programme_name", "") ?: ""

    fun setProgrammeName(name: String) {
        prefs.edit().putString("programme_name", name).apply()
    }

    fun hasProgramme(): LiveData<Boolean> =
        dao.countLive(getProgrammeName()).map { it > 0 }

    fun hasProgrammeByName(name: String): LiveData<Boolean> =
        dao.countLive(name).map { it > 0 }

    fun getDistinctWeeksByName(name: String): LiveData<List<Int>> =
        dao.getDistinctWeeks(name)

    fun getCompletedWeeksByName(name: String): LiveData<List<Int>> =
        dao.getCompletedWeeks(name)

    suspend fun programmeExists(name: String): Boolean =
        programmeDao.exists(name) || dao.countByProgramme(name) > 0

    suspend fun importProgrammeFromJson(json: String, name: String): ImportResult {
        if (programmeExists(name)) {
            setProgrammeName(name)
            return ImportResult.SWITCHED
        }
        val exercises = parseProgramme(json).map { it.copy(programmeName = name) }
        dao.insertAll(exercises)
        programmeDao.upsert(Programme(name = name, importedAt = Instant.now().toString()))
        setProgrammeName(name)
        return ImportResult.IMPORTED
    }

    suspend fun importProgrammeFromXlsx(inputStream: InputStream, name: String): ImportResult {
        if (programmeExists(name)) {
            setProgrammeName(name)
            return ImportResult.SWITCHED
        }
        val exercises = XlsxParser.parse(inputStream).map { it.copy(programmeName = name) }
        dao.insertAll(exercises)
        programmeDao.upsert(Programme(name = name, importedAt = Instant.now().toString()))
        setProgrammeName(name)
        return ImportResult.IMPORTED
    }

    suspend fun deleteProgramme() {
        val name = getProgrammeName()
        if (name.isNotBlank()) {
            logDao.deleteByProgramme(name)
            dao.deleteByProgramme(name)
            programmeDao.delete(name)
        }
        setProgrammeName("")
    }

    fun getExercises(weekNumber: Int, dayName: String): LiveData<List<Exercise>> =
        dao.getExercises(getProgrammeName(), weekNumber, dayName)

    fun getDistinctWeeks(): LiveData<List<Int>> =
        dao.getDistinctWeeks(getProgrammeName())

    fun getDistinctDays(weekNumber: Int): LiveData<List<String>> =
        dao.getDistinctDays(getProgrammeName(), weekNumber)

    fun getCompletedDays(weekNumber: Int): LiveData<List<String>> =
        dao.getCompletedDays(getProgrammeName(), weekNumber)

    fun getCompletedWeeks(): LiveData<List<Int>> =
        dao.getCompletedWeeks(getProgrammeName())

    fun getLog(exerciseId: Long): LiveData<ExerciseLog?> =
        logDao.getLog(exerciseId)

    fun getLogsForDay(weekNumber: Int, dayName: String): LiveData<List<ExerciseLog>> =
        logDao.getLogsForDay(getProgrammeName(), weekNumber, dayName)

    suspend fun getLogSync(exerciseId: Long): ExerciseLog? =
        logDao.getLogSync(exerciseId)

    fun getHistory(exerciseName: String, currentWeek: Int): LiveData<List<ExerciseHistoryEntry>> =
        logDao.getHistory(getProgrammeName(), exerciseName, currentWeek)

    suspend fun saveLog(exerciseLog: ExerciseLog) =
        logDao.upsert(exerciseLog)

    suspend fun buildExportJson(programmeName: String, identifier: String): String {
        val name = programmeName.ifBlank { getProgrammeName() }
        val allExercises = dao.getAllExercises(name)
        val exportLogs = logDao.getExportLogs(name)

        val root = JSONObject()
        root.put("programmeName", name)
        root.put("identifier", identifier)
        root.put("exportDate", Instant.now().toString())

        // Build programme structure grouped by week then day
        val weekMap = linkedMapOf<Int, MutableMap<String, MutableList<Exercise>>>()
        for (ex in allExercises) {
            weekMap.getOrPut(ex.weekNumber) { linkedMapOf() }
                .getOrPut(ex.dayName) { mutableListOf() }
                .add(ex)
        }

        val weeksArray = JSONArray()
        for ((weekNum, dayMap) in weekMap) {
            val weekObj = JSONObject()
            weekObj.put("week", weekNum)
            val daysArray = JSONArray()
            for ((dayName, exercises) in dayMap) {
                val dayObj = JSONObject()
                dayObj.put("day", dayName)
                val exArray = JSONArray()
                for (ex in exercises) {
                    exArray.put(JSONObject().apply {
                        put("name", ex.exerciseName)
                        put("sets", ex.sets)
                        put("reps", ex.reps)
                        put("rpe", ex.rpe)
                        put("rest", ex.rest)
                        put("warmupSets", ex.warmupSets)
                        put("notes", ex.notes)
                        put("order", ex.orderIndex)
                        put("sub1", ex.sub1)
                        put("sub2", ex.sub2)
                    })
                }
                dayObj.put("exercises", exArray)
                daysArray.put(dayObj)
            }
            weekObj.put("days", daysArray)
            weeksArray.put(weekObj)
        }

        root.put("programme", JSONObject().put("weeks", weeksArray))

        // Build logs array
        val logsArray = JSONArray()
        for (log in exportLogs) {
            logsArray.put(JSONObject().apply {
                put("exerciseName", log.exerciseName)
                put("weekNumber", log.weekNumber)
                put("dayName", log.dayName)
                put("userWeight", log.userWeight)
                put("equipmentType", log.equipmentType)
                put("observedRpe", log.observedRpe)
                put("userComments", log.userComments)
                put("status", log.status)
            })
        }
        root.put("logs", logsArray)

        return root.toString(2)
    }

    fun getAvailableProgrammes(): LiveData<List<Programme>> = programmeDao.getAll()

    suspend fun preloadProgrammes() {
        // Clean up old duplicate names (e.g. "the_essentials_5x" → "essentials_5x")
        cleanupLegacyNames()

        // Fix 4x day names that were preloaded without #2 suffixes
        repair4xDayNames()

        // Deduplicate exercises that may have been doubled by previous versions
        deduplicateExercises()

        for ((name, assetFile) in BUNDLED_PROGRAMMES) {
            if (programmeExists(name)) continue
            val json = context.assets.open(assetFile).bufferedReader().use { it.readText() }
            val exercises = parseProgramme(json).map { it.copy(programmeName = name) }
            dao.insertAll(exercises)
            programmeDao.upsert(Programme(name = name, importedAt = Instant.now().toString()))
        }
    }

    private suspend fun deduplicateExercises() {
        for ((name, _) in BUNDLED_PROGRAMMES) {
            val expected = when {
                name.contains("2x") -> 180
                name.contains("3x") -> 240
                name.contains("4x") -> 288
                name.contains("5x") -> 324
                else -> continue
            }
            val actual = dao.countByProgramme(name)
            if (actual > expected) {
                // Delete duplicates: keep the lowest IDs, remove the rest
                dao.deduplicateByProgramme(name, expected)
            }
        }
    }

    private suspend fun repair4xDayNames() {
        val name4x = BUNDLED_PROGRAMMES.firstOrNull { it.first.contains("4x") }?.first ?: return
        val count = dao.countByProgramme(name4x)
        if (count == 0) return

        // Should have 4 unique days per week; if only 2, the old preload used wrong names
        val exercises = dao.getAllExercises(name4x)
        val week1Days = exercises.filter { it.weekNumber == 1 }.map { it.dayName }.distinct()
        if (week1Days.size < 4) {
            dao.deleteByProgramme(name4x)
            programmeDao.delete(name4x)
        }
    }

    private suspend fun cleanupLegacyNames() {
        // Map of old names to new normalized names
        val legacyMap = mapOf(
            "the_essentials_2x" to "essentials_2x",
            "the_essentials_3x" to "essentials_3x",
            "the_essentials_4x" to "essentials_4x",
            "the_essentials_5x" to "essentials_5x",
            "Essentials 2x" to "essentials_2x",
            "Essentials 3x" to "essentials_3x",
            "Essentials 4x" to "essentials_4x",
            "Essentials 5x" to "essentials_5x",
        )
        for ((oldName, newName) in legacyMap) {
            if (!programmeExists(oldName)) continue
            // Rename exercises
            dao.renameProgramme(oldName, newName)
            // Update programme registry
            programmeDao.delete(oldName)
            programmeDao.upsert(Programme(name = newName, importedAt = Instant.now().toString()))
            // Update active programme name if it matches
            if (getProgrammeName() == oldName) {
                setProgrammeName(newName)
            }
        }
    }

    companion object {
        val BUNDLED_PROGRAMMES = listOf(
            "essentials_2x.json",
            "essentials_3x.json",
            "essentials_4x.json",
            "essentials_5x.json",
        ).map { ProgrammeNameNormalizer.normalize(it) to it }

        fun parseProgramme(json: String): List<Exercise> {
            val root = JSONObject(json)
            val weeks = root.getJSONArray("weeks")
            val exercises = mutableListOf<Exercise>()

            for (w in 0 until weeks.length()) {
                val weekObj = weeks.getJSONObject(w)
                val weekNumber = weekObj.getInt("week")
                val days = weekObj.getJSONArray("days")

                for (d in 0 until days.length()) {
                    val dayObj = days.getJSONObject(d)
                    val dayName = dayObj.getString("day")
                    val exArr = dayObj.getJSONArray("exercises")

                    for (e in 0 until exArr.length()) {
                        val exObj = exArr.getJSONObject(e)
                        exercises.add(
                            Exercise(
                                weekNumber = weekNumber,
                                dayName = dayName,
                                exerciseName = exObj.getString("name"),
                                sets = exObj.getInt("sets"),
                                reps = exObj.getString("reps"),
                                orderIndex = exObj.optInt("order", exercises.size),
                                rpe = exObj.optString("rpe", ""),
                                rest = exObj.optString("rest", ""),
                                notes = exObj.optString("notes", ""),
                                warmupSets = exObj.optString("warmupSets", "0"),
                                sub1 = exObj.optString("sub1", ""),
                                sub2 = exObj.optString("sub2", ""),
                                videoUrl = exObj.optString("videoUrl", ""),
                                sub1VideoUrl = exObj.optString("sub1VideoUrl", ""),
                                sub2VideoUrl = exObj.optString("sub2VideoUrl", "")
                            )
                        )
                    }
                }
            }

            return exercises
        }
    }
}
