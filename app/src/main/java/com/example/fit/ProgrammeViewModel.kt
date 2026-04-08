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
import com.example.fit.data.FirebaseSyncManager
import com.example.fit.data.ImportResult
import com.example.fit.data.ProgrammeRepository
import kotlinx.coroutines.launch
import java.io.InputStream

class ProgrammeViewModel(app: Application) : AndroidViewModel(app) {

    private val repository: ProgrammeRepository
    private val firebaseSyncManager = FirebaseSyncManager()

    val weeks: LiveData<List<Int>>
    val selectedWeek = MutableLiveData<Int>()
    val selectedDay = MutableLiveData<String>()
    val days: LiveData<List<String>>
    val exercises: LiveData<List<Exercise>>
    val programmeName = MutableLiveData("")

    val selectedExercise = MutableLiveData<Exercise?>()
    val showTable = MutableLiveData(false)
    val showHistory = MutableLiveData(false)

    val hasProgramme: LiveData<Boolean>
    val completedWeeks: LiveData<List<Int>>
    val completedDays: LiveData<List<String>>
    val selectedExerciseLog: LiveData<ExerciseLog?>
    val exerciseLogs: LiveData<List<ExerciseLog>>
    val exerciseHistory: LiveData<List<ExerciseHistoryEntry>>

    init {
        val db = AppDatabase.getInstance(app)
        repository = ProgrammeRepository(db.exerciseDao(), db.exerciseLogDao(), db.programmeDao(), app)

        programmeName.value = repository.getProgrammeName()

        // All queries react to programme name changes
        hasProgramme = programmeName.switchMap { name ->
            if (name.isNullOrBlank()) MutableLiveData(false)
            else repository.hasProgrammeByName(name)
        }

        weeks = programmeName.switchMap { name ->
            if (name.isNullOrBlank()) MutableLiveData(emptyList())
            else repository.getDistinctWeeksByName(name)
        }

        completedWeeks = programmeName.switchMap { name ->
            if (name.isNullOrBlank()) MutableLiveData(emptyList())
            else repository.getCompletedWeeksByName(name)
        }

        days = selectedWeek.switchMap { week ->
            repository.getDistinctDays(week)
        }

        completedDays = selectedWeek.switchMap { week ->
            repository.getCompletedDays(week)
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

    }

    fun importProgramme(json: String, name: String = "", onResult: ((ImportResult) -> Unit)? = null) {
        viewModelScope.launch {
            try {
                val result = repository.importProgrammeFromJson(json, name)
                programmeName.value = repository.getProgrammeName()
                onResult?.invoke(result)
            } catch (e: Exception) {
                Log.e("ProgrammeViewModel", "Failed to import programme", e)
            }
        }
    }

    fun importProgrammeFromXlsx(inputStream: InputStream, name: String = "", onResult: ((ImportResult) -> Unit)? = null) {
        viewModelScope.launch {
            try {
                val result = repository.importProgrammeFromXlsx(inputStream, name)
                programmeName.value = repository.getProgrammeName()
                onResult?.invoke(result)
            } catch (e: Exception) {
                Log.e("ProgrammeViewModel", "Failed to import XLSX programme", e)
            }
        }
    }

    fun deleteProgramme() {
        viewModelScope.launch {
            try {
                repository.deleteProgramme()
                programmeName.value = ""
                selectedWeek.value = null
                selectedDay.value = null
                selectedExercise.value = null
                showTable.value = false
                showHistory.value = false
            } catch (e: Exception) {
                Log.e("ProgrammeViewModel", "Failed to delete programme", e)
            }
        }
    }

    /**
     * Builds export JSON and pushes to Firebase. Returns the JSON string via callback
     * so the caller can trigger a share intent.
     */
    fun exportProgramme(identifier: String, onResult: (json: String?, success: Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val name = programmeName.value ?: ""
                val json = repository.buildExportJson(name, identifier)

                firebaseSyncManager.exportProgramme(name, identifier, json) { firebaseOk ->
                    onResult(json, firebaseOk)
                }
            } catch (e: Exception) {
                Log.e("ProgrammeViewModel", "Failed to export programme", e)
                onResult(null, false)
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
