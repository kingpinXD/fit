package com.example.fit

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

class MainActivity : AppCompatActivity() {

    private val viewModel: ProgrammeViewModel by viewModels()

    private lateinit var exerciseListAdapter: ExerciseListAdapter
    private lateinit var exerciseTableAdapter: ExerciseTableAdapter

    private var suppressWeekSelection = false
    private var suppressDaySelection = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val spinnerWeek = findViewById<Spinner>(R.id.spinnerWeek)
        val spinnerDay = findViewById<Spinner>(R.id.spinnerDay)
        val rvExerciseList = findViewById<RecyclerView>(R.id.rvExerciseList)
        val detailPanel = findViewById<ScrollView>(R.id.detailPanel)
        val tablePanel = findViewById<View>(R.id.tablePanel)
        val rvTable = findViewById<RecyclerView>(R.id.rvTable)

        // Detail panel views
        val tvDetailName = findViewById<TextView>(R.id.tvDetailName)
        val tvDetailInfo = findViewById<TextView>(R.id.tvDetailInfo)
        val tvNotesToggle = findViewById<TextView>(R.id.tvNotesToggle)
        val tvNotesContent = findViewById<TextView>(R.id.tvNotesContent)
        val etWeight = findViewById<TextInputEditText>(R.id.etWeight)
        val etComments = findViewById<TextInputEditText>(R.id.etComments)
        val etObservedRpe = findViewById<TextInputEditText>(R.id.etObservedRpe)
        val btnDone = findViewById<MaterialButton>(R.id.btnDone)
        val btnSkip = findViewById<MaterialButton>(R.id.btnSkip)

        // Setup exercise list adapter
        exerciseListAdapter = ExerciseListAdapter(
            onExerciseSelected = { exercise ->
                viewModel.selectedExercise.value = exercise
                viewModel.showTable.value = false
            },
            onShowTable = {
                viewModel.selectedExercise.value = null
                viewModel.showTable.value = true
            }
        )
        rvExerciseList.layoutManager = LinearLayoutManager(this)
        rvExerciseList.adapter = exerciseListAdapter

        // Setup table adapter
        exerciseTableAdapter = ExerciseTableAdapter()
        rvTable.layoutManager = LinearLayoutManager(this)
        rvTable.adapter = exerciseTableAdapter

        setupWeekSpinner(spinnerWeek)
        setupDaySpinner(spinnerDay)

        // Observe exercises for both the list and table
        viewModel.exercises.observe(this) { exercises ->
            exerciseListAdapter.submitList(exercises)
            exerciseTableAdapter.submitList(exercises)
        }

        // Observe logs for status icons in the list
        viewModel.exerciseLogs.observe(this) { logs ->
            exerciseListAdapter.updateLogs(logs)
        }

        // Toggle detail vs table panel
        viewModel.showTable.observe(this) { showTable ->
            if (showTable) {
                detailPanel.visibility = View.GONE
                tablePanel.visibility = View.VISIBLE
            } else {
                tablePanel.visibility = View.GONE
                val hasSelection = viewModel.selectedExercise.value != null
                detailPanel.visibility = if (hasSelection) View.VISIBLE else View.GONE
            }
        }

        // Populate detail panel when an exercise is selected
        viewModel.selectedExercise.observe(this) { exercise ->
            if (exercise == null) {
                detailPanel.visibility = View.GONE
                return@observe
            }
            if (viewModel.showTable.value == true) return@observe

            detailPanel.visibility = View.VISIBLE

            tvDetailName.text = "${exercise.orderIndex}. ${exercise.exerciseName}"

            val rpeStr = if (exercise.rpe.isNotBlank()) " | RPE: ${exercise.rpe}" else ""
            tvDetailInfo.text = "${exercise.sets} sets \u00D7 ${exercise.reps}$rpeStr"

            // Notes
            if (exercise.notes.isBlank()) {
                tvNotesToggle.visibility = View.GONE
                tvNotesContent.visibility = View.GONE
            } else {
                tvNotesToggle.visibility = View.VISIBLE
                tvNotesContent.text = exercise.notes
                tvNotesContent.visibility = View.GONE
                tvNotesToggle.text = "Notes \u25B6"
            }

            // Clear input fields
            etWeight.setText("")
            etComments.setText("")
            etObservedRpe.setText("")
        }

        // Notes toggle
        tvNotesToggle.setOnClickListener {
            if (tvNotesContent.visibility == View.GONE) {
                tvNotesContent.visibility = View.VISIBLE
                tvNotesToggle.text = "Notes \u25BC"
            } else {
                tvNotesContent.visibility = View.GONE
                tvNotesToggle.text = "Notes \u25B6"
            }
        }

        // Done button
        btnDone.setOnClickListener {
            val exercise = viewModel.selectedExercise.value ?: return@setOnClickListener
            val weight = etWeight.text?.toString() ?: ""
            val comments = etComments.text?.toString() ?: ""
            val observedRpe = etObservedRpe.text?.toString() ?: ""
            viewModel.markDone(exercise, weight, comments, observedRpe)
            advanceToNextExercise()
        }

        // Skip button
        btnSkip.setOnClickListener {
            val exercise = viewModel.selectedExercise.value ?: return@setOnClickListener
            viewModel.markSkipped(exercise)
            advanceToNextExercise()
        }

        // Reset selection when day changes
        viewModel.days.observe(this) {
            viewModel.selectedExercise.value = null
            viewModel.showTable.value = false
            exerciseListAdapter.setSelectedPosition(-1)
        }
    }

    private fun advanceToNextExercise() {
        val exercises = viewModel.exercises.value ?: return
        val current = viewModel.selectedExercise.value ?: return
        val currentIdx = exercises.indexOfFirst { it.id == current.id }
        if (currentIdx < 0) return

        val nextIdx = if (currentIdx + 1 < exercises.size) currentIdx + 1 else currentIdx
        val nextExercise = exercises[nextIdx]
        viewModel.selectedExercise.value = nextExercise
        exerciseListAdapter.setSelectedPosition(nextIdx)
    }

    private fun setupWeekSpinner(spinner: Spinner) {
        viewModel.weeks.observe(this) { weeks ->
            if (weeks.isNullOrEmpty()) return@observe

            val labels = weeks.map { "Week $it" }
            val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, labels)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

            suppressWeekSelection = true
            spinner.adapter = adapter

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

            val currentDay = viewModel.selectedDay.value
            val idx = if (currentDay != null) days.indexOf(currentDay) else -1
            if (idx >= 0) {
                spinner.setSelection(idx)
            } else {
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
