package com.example.fit

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.example.fit.data.AppDatabase
import com.example.fit.data.Exercise
import com.example.fit.data.ExerciseHistoryEntry
import com.example.fit.data.ExerciseLog
import com.example.fit.data.ProgrammeRepository
import kotlinx.coroutines.launch

class ProgrammeViewModel(app: Application) : AndroidViewModel(app) {

    private val repository: ProgrammeRepository

    val weeks: LiveData<List<Int>>
    val selectedWeek = MutableLiveData<Int>()
    val selectedDay = MutableLiveData<String>()
    val days: LiveData<List<String>>
    val exercises: LiveData<List<Exercise>>

    val selectedExercise = MutableLiveData<Exercise?>()
    val showTable = MutableLiveData(false)
    val showHistory = MutableLiveData(false)

    val selectedExerciseLog: LiveData<ExerciseLog?>
    val exerciseLogs: LiveData<List<ExerciseLog>>
    val exerciseHistory: LiveData<List<ExerciseHistoryEntry>>

    init {
        val db = AppDatabase.getInstance(app)
        repository = ProgrammeRepository(db.exerciseDao(), db.exerciseLogDao(), app)

        weeks = repository.getDistinctWeeks()

        days = selectedWeek.switchMap { week ->
            repository.getDistinctDays(week)
        }

        // Exercises respond to both selectedWeek and selectedDay changes
        val trigger = MediatorLiveData<Pair<Int?, String?>>()
        trigger.addSource(selectedWeek) { week ->
            trigger.value = Pair(week, selectedDay.value)
        }
        trigger.addSource(selectedDay) { day ->
            trigger.value = Pair(selectedWeek.value, day)
        }
        exercises = trigger.switchMap { (week, day) ->
            if (week != null && day != null) {
                repository.getExercises(week, day)
            } else {
                MutableLiveData(emptyList())
            }
        }

        exerciseLogs = trigger.switchMap { (week, day) ->
            if (week != null && day != null) {
                repository.getLogsForDay(week, day)
            } else {
                MutableLiveData(emptyList())
            }
        }

        selectedExerciseLog = selectedExercise.switchMap { exercise ->
            if (exercise != null) {
                repository.getLog(exercise.id)
            } else {
                MutableLiveData(null)
            }
        }

        // History: all logs for same exercise name in previous weeks (descending)
        exerciseHistory = selectedExercise.switchMap { exercise ->
            if (exercise != null) {
                repository.getHistory(exercise.exerciseName, exercise.weekNumber)
            } else {
                MutableLiveData(emptyList())
            }
        }

        viewModelScope.launch {
            try {
                repository.loadProgrammeIfNeeded()
            } catch (e: Exception) {
                Log.e("ProgrammeViewModel", "Failed to load programme", e)
            }
        }
    }

    fun markDone(exercise: Exercise, weight: String, equipment: String, comments: String, observedRpe: String) {
        val rpe = if (observedRpe.isBlank()) exercise.rpe else observedRpe
        viewModelScope.launch {
            val existing = repository.getLogSync(exercise.id)
            repository.saveLog(
                ExerciseLog(
                    id = existing?.id ?: 0,
                    exerciseId = exercise.id,
                    userWeight = weight,
                    equipmentType = equipment,
                    userComments = comments,
                    observedRpe = rpe,
                    status = "DONE"
                )
            )
        }
    }

    fun markSkipped(exercise: Exercise) {
        viewModelScope.launch {
            val existing = repository.getLogSync(exercise.id)
            repository.saveLog(
                ExerciseLog(
                    id = existing?.id ?: 0,
                    exerciseId = exercise.id,
                    userWeight = "",
                    equipmentType = "",
                    userComments = "",
                    observedRpe = "",
                    status = "SKIPPED"
                )
            )
        }
    }
}
