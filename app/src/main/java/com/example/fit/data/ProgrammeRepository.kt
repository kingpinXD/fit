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
        programmeDao.exists(name)

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

    companion object {
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
                                notes = exObj.optString("notes", ""),
                                warmupSets = exObj.optString("warmupSets", "0"),
                                sub1 = exObj.optString("sub1", ""),
                                sub2 = exObj.optString("sub2", "")
                            )
                        )
                    }
                }
            }

            return exercises
        }
    }
}
