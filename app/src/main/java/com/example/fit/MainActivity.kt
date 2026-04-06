package com.example.fit

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {

    private val viewModel: ProgrammeViewModel by viewModels()
    private lateinit var exerciseAdapter: ExerciseAdapter

    private var suppressWeekSelection = false
    private var suppressDaySelection = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val spinnerWeek = findViewById<Spinner>(R.id.spinnerWeek)
        val spinnerDay = findViewById<Spinner>(R.id.spinnerDay)
        val rvExercises = findViewById<RecyclerView>(R.id.rvExercises)

        exerciseAdapter = ExerciseAdapter()
        rvExercises.layoutManager = LinearLayoutManager(this)
        rvExercises.adapter = exerciseAdapter

        setupWeekSpinner(spinnerWeek)
        setupDaySpinner(spinnerDay)

        viewModel.exercises.observe(this) { exercises ->
            exerciseAdapter.submitList(exercises)
        }
    }

    private fun setupWeekSpinner(spinner: Spinner) {
        viewModel.weeks.observe(this) { weeks ->
            if (weeks.isNullOrEmpty()) return@observe

            val labels = weeks.map { "Week $it" }
            val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, labels)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

            suppressWeekSelection = true
            spinner.adapter = adapter

            // Restore selection if already set
            val currentWeek = viewModel.selectedWeek.value
            if (currentWeek != null) {
                val idx = weeks.indexOf(currentWeek)
                if (idx >= 0) spinner.setSelection(idx)
            }
        }

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                if (suppressWeekSelection) {
                    suppressWeekSelection = false
                    // Still set the week if not already set
                    if (viewModel.selectedWeek.value == null) {
                        val weeks = viewModel.weeks.value ?: return
                        if (pos < weeks.size) viewModel.selectedWeek.value = weeks[pos]
                    }
                    return
                }
                val weeks = viewModel.weeks.value ?: return
                if (pos < weeks.size) {
                    viewModel.selectedWeek.value = weeks[pos]
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupDaySpinner(spinner: Spinner) {
        viewModel.days.observe(this) { days ->
            if (days.isNullOrEmpty()) return@observe

            val labels = days.mapIndexed { i, day -> "Day ${i + 1}: $day" }
            val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, labels)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

            suppressDaySelection = true
            spinner.adapter = adapter

            // When week changes, select first day (or restore current)
            val currentDay = viewModel.selectedDay.value
            val idx = if (currentDay != null) days.indexOf(currentDay) else -1
            if (idx >= 0) {
                spinner.setSelection(idx)
            } else {
                // Reset to first day for the new week
                spinner.setSelection(0)
                viewModel.selectedDay.value = days[0]
            }
        }

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                if (suppressDaySelection) {
                    suppressDaySelection = false
                    return
                }
                val days = viewModel.days.value ?: return
                if (pos < days.size) {
                    viewModel.selectedDay.value = days[pos]
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }
}
