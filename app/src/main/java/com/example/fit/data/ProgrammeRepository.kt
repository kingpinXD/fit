package com.example.fit.data

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import org.json.JSONObject
import java.io.InputStream

class ProgrammeRepository(
    private val dao: ExerciseDao,
    private val logDao: ExerciseLogDao,
    private val context: Context
) {

    fun hasProgramme(): LiveData<Boolean> = dao.countLive().map { it > 0 }

    suspend fun importProgrammeFromJson(json: String) {
        val exercises = parseProgramme(json)
        dao.insertAll(exercises)
    }

    suspend fun importProgrammeFromXlsx(inputStream: InputStream) {
        val exercises = XlsxParser.parse(inputStream)
        dao.insertAll(exercises)
    }

    suspend fun deleteProgramme() {
        logDao.deleteAll()
        dao.deleteAll()
    }

    fun getExercises(weekNumber: Int, dayName: String): LiveData<List<Exercise>> =
        dao.getExercises(weekNumber, dayName)

    fun getDistinctWeeks(): LiveData<List<Int>> =
        dao.getDistinctWeeks()

    fun getDistinctDays(weekNumber: Int): LiveData<List<String>> =
        dao.getDistinctDays(weekNumber)

    fun getLog(exerciseId: Long): LiveData<ExerciseLog?> =
        logDao.getLog(exerciseId)

    fun getLogsForDay(weekNumber: Int, dayName: String): LiveData<List<ExerciseLog>> =
        logDao.getLogsForDay(weekNumber, dayName)

    suspend fun getLogSync(exerciseId: Long): ExerciseLog? =
        logDao.getLogSync(exerciseId)

    fun getHistory(exerciseName: String, currentWeek: Int): LiveData<List<ExerciseHistoryEntry>> =
        logDao.getHistory(exerciseName, currentWeek)

    suspend fun saveLog(exerciseLog: ExerciseLog) =
        logDao.upsert(exerciseLog)

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
                                warmupSets = exObj.optString("warmupSets", "0")
                            )
                        )
                    }
                }
            }

            return exercises
        }
    }
}
