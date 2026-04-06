package com.example.fit

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.fit.data.Exercise

class ExerciseAdapter : RecyclerView.Adapter<ExerciseAdapter.ViewHolder>() {

    private var items: List<Exercise> = emptyList()

    fun submitList(newItems: List<Exercise>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_exercise, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvName: TextView = view.findViewById(R.id.tvExerciseName)
        private val tvSets: TextView = view.findViewById(R.id.tvSets)
        private val tvReps: TextView = view.findViewById(R.id.tvReps)

        fun bind(exercise: Exercise) {
            tvName.text = exercise.exerciseName
            tvSets.text = "${exercise.sets}"
            tvReps.text = exercise.reps
        }
    }
}
