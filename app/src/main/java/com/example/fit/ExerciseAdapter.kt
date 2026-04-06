package com.example.fit

import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.fit.data.Exercise
import com.example.fit.data.ExerciseLog

/**
 * Compact exercise list adapter: shows "1. Exercise Name" with status icon.
 * Last item is a "Show Table" option.
 */
class ExerciseListAdapter(
    private val onExerciseSelected: (Exercise) -> Unit,
    private val onShowTable: () -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var items: List<Exercise> = emptyList()
    private var logsByExerciseId: Map<Long, ExerciseLog> = emptyMap()
    private var selectedPosition: Int = -1

    companion object {
        private const val TYPE_EXERCISE = 0
        private const val TYPE_SHOW_TABLE = 1
    }

    fun submitList(newItems: List<Exercise>) {
        items = newItems
        notifyDataSetChanged()
    }

    fun updateLogs(logs: List<ExerciseLog>) {
        logsByExerciseId = logs.associateBy { it.exerciseId }
        notifyDataSetChanged()
    }

    fun setSelectedPosition(position: Int) {
        val old = selectedPosition
        selectedPosition = position
        if (old >= 0) notifyItemChanged(old)
        if (position >= 0) notifyItemChanged(position)
    }

    override fun getItemViewType(position: Int): Int =
        if (position < items.size) TYPE_EXERCISE else TYPE_SHOW_TABLE

    override fun getItemCount(): Int = items.size + 1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_EXERCISE) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_exercise_compact, parent, false)
            ExerciseViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_exercise_compact, parent, false)
            ShowTableViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is ExerciseViewHolder -> {
                val exercise = items[position]
                holder.bind(exercise, logsByExerciseId[exercise.id], position == selectedPosition)
                holder.itemView.setOnClickListener {
                    setSelectedPosition(holder.bindingAdapterPosition)
                    onExerciseSelected(items[holder.bindingAdapterPosition])
                }
            }
            is ShowTableViewHolder -> {
                holder.bind(position == selectedPosition)
                holder.itemView.setOnClickListener {
                    setSelectedPosition(holder.bindingAdapterPosition)
                    onShowTable()
                }
            }
        }
    }

    class ExerciseViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvName: TextView = view.findViewById(R.id.tvCompactName)
        private val tvStatus: TextView = view.findViewById(R.id.tvCompactStatus)

        fun bind(exercise: Exercise, log: ExerciseLog?, isSelected: Boolean) {
            tvName.text = "${exercise.orderIndex}. ${exercise.exerciseName}"
            tvName.setTypeface(null, if (isSelected) Typeface.BOLD else Typeface.NORMAL)

            tvStatus.text = when (log?.status) {
                "DONE" -> "\u2713"
                "SKIPPED" -> "\u21BB"
                else -> ""
            }
            tvStatus.setTextColor(
                when (log?.status) {
                    "DONE" -> itemView.context.getColor(R.color.success)
                    "SKIPPED" -> itemView.context.getColor(R.color.warning)
                    else -> itemView.context.getColor(R.color.textSecondary)
                }
            )

            itemView.setBackgroundColor(
                if (isSelected) itemView.context.getColor(R.color.surfaceVariant)
                else itemView.context.getColor(R.color.surface)
            )
        }
    }

    class ShowTableViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvName: TextView = view.findViewById(R.id.tvCompactName)
        private val tvStatus: TextView = view.findViewById(R.id.tvCompactStatus)

        fun bind(isSelected: Boolean) {
            tvName.text = "\uD83D\uDCCB Show Table"
            tvName.setTypeface(null, if (isSelected) Typeface.BOLD else Typeface.NORMAL)
            tvStatus.text = ""

            itemView.setBackgroundColor(
                if (isSelected) itemView.context.getColor(R.color.surfaceVariant)
                else itemView.context.getColor(R.color.surface)
            )
        }
    }
}

/**
 * Read-only table adapter: shows # | Exercise | Sets | Reps | RPE
 */
class ExerciseTableAdapter : RecyclerView.Adapter<ExerciseTableAdapter.ViewHolder>() {

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
        private val tvOrder: TextView = view.findViewById(R.id.tvOrderNumber)
        private val tvName: TextView = view.findViewById(R.id.tvExerciseName)
        private val tvSets: TextView = view.findViewById(R.id.tvSets)
        private val tvReps: TextView = view.findViewById(R.id.tvReps)
        private val tvRpe: TextView = view.findViewById(R.id.tvRpe)

        fun bind(exercise: Exercise) {
            tvOrder.text = "${exercise.orderIndex}"
            tvName.text = exercise.exerciseName
            tvSets.text = "${exercise.sets}"
            tvReps.text = exercise.reps
            tvRpe.text = exercise.rpe
        }
    }
}
