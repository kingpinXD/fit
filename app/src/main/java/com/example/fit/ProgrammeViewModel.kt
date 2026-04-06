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
import com.example.fit.data.ProgrammeRepository
import kotlinx.coroutines.launch

class ProgrammeViewModel(app: Application) : AndroidViewModel(app) {

    private val repository: ProgrammeRepository

    val weeks: LiveData<List<Int>>
    val selectedWeek = MutableLiveData<Int>()
    val selectedDay = MutableLiveData<String>()
    val days: LiveData<List<String>>
    val exercises: LiveData<List<Exercise>>

    init {
        val db = AppDatabase.getInstance(app)
        repository = ProgrammeRepository(db.exerciseDao(), app)

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

        viewModelScope.launch {
            try {
                repository.loadProgrammeIfNeeded()
            } catch (e: Exception) {
                Log.e("ProgrammeViewModel", "Failed to load programme", e)
            }
        }
    }
}
