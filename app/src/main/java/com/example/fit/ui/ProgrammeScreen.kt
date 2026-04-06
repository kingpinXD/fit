package com.example.fit.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fit.ProgrammeViewModel
import com.example.fit.data.Exercise
import com.example.fit.data.ExerciseLog
import com.example.fit.ui.theme.SkipBlue
import com.example.fit.ui.theme.SuccessGreen
import com.example.fit.ui.theme.TextSecondary

@Composable
fun ProgrammeScreen(viewModel: ProgrammeViewModel = viewModel()) {
    val weeks by viewModel.weeks.observeAsState(emptyList())
    val days by viewModel.days.observeAsState(emptyList())
    val exercises by viewModel.exercises.observeAsState(emptyList())
    val exerciseLogs by viewModel.exerciseLogs.observeAsState(emptyList())
    val selectedExercise by viewModel.selectedExercise.observeAsState(null)
    val showTable by viewModel.showTable.observeAsState(false)

    val logsByExerciseId = remember(exerciseLogs) {
        exerciseLogs.associateBy { it.exerciseId }
    }

    var selectedWeekIndex by remember { mutableIntStateOf(0) }
    var selectedDayIndex by remember { mutableIntStateOf(0) }

    // When weeks load, select the first one
    LaunchedEffect(weeks) {
        if (weeks.isNotEmpty() && viewModel.selectedWeek.value == null) {
            viewModel.selectedWeek.value = weeks[0]
            selectedWeekIndex = 0
        }
    }

    // When days change, reset to first day
    LaunchedEffect(days) {
        if (days.isNotEmpty()) {
            selectedDayIndex = 0
            viewModel.selectedDay.value = days[0]
            viewModel.selectedExercise.value = null
            viewModel.showTable.value = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // 1. Dropdown row
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                WeekDropdown(
                    weeks = weeks,
                    selectedIndex = selectedWeekIndex,
                    onSelected = { index ->
                        selectedWeekIndex = index
                        viewModel.selectedWeek.value = weeks[index]
                    },
                    modifier = Modifier.weight(1f)
                )
                DayDropdown(
                    days = days,
                    selectedIndex = selectedDayIndex,
                    onSelected = { index ->
                        selectedDayIndex = index
                        viewModel.selectedDay.value = days[index]
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 2. Exercise list
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(exercises) { _, exercise ->
                ExerciseListItem(
                    exercise = exercise,
                    isSelected = exercise.id == selectedExercise?.id,
                    log = logsByExerciseId[exercise.id],
                    onClick = {
                        viewModel.selectedExercise.value = exercise
                        viewModel.showTable.value = false
                    }
                )
            }
            item {
                ShowTableButton(
                    isSelected = showTable,
                    onClick = {
                        viewModel.selectedExercise.value = null
                        viewModel.showTable.value = true
                    }
                )
            }
        }

        // 3. Detail panel or table
        if (showTable) {
            ExerciseTable(
                exercises = exercises,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(2f)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
        } else if (selectedExercise != null) {
            ExerciseDetail(
                exercise = selectedExercise!!,
                onDone = { weight, comments, rpe ->
                    viewModel.markDone(selectedExercise!!, weight, comments, rpe)
                    advanceToNext(exercises, selectedExercise!!, viewModel)
                },
                onSkip = {
                    viewModel.markSkipped(selectedExercise!!)
                    advanceToNext(exercises, selectedExercise!!, viewModel)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(2f)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
    }
}

private fun advanceToNext(
    exercises: List<Exercise>,
    current: Exercise,
    viewModel: ProgrammeViewModel
) {
    val currentIdx = exercises.indexOfFirst { it.id == current.id }
    if (currentIdx < 0) return
    val nextIdx = if (currentIdx + 1 < exercises.size) currentIdx + 1 else currentIdx
    viewModel.selectedExercise.value = exercises[nextIdx]
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WeekDropdown(
    weeks: List<Int>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val label = if (weeks.isNotEmpty() && selectedIndex in weeks.indices) {
        "Week ${weeks[selectedIndex]}"
    } else {
        "Week"
    }

    Column(modifier = modifier) {
        Text(
            text = "WEEK",
            color = TextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it }
        ) {
            TextField(
                value = label,
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.menuAnchor()
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                weeks.forEachIndexed { index, week ->
                    DropdownMenuItem(
                        text = { Text("Week $week") },
                        onClick = {
                            onSelected(index)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DayDropdown(
    days: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val label = if (days.isNotEmpty() && selectedIndex in days.indices) {
        "Day ${selectedIndex + 1}: ${days[selectedIndex]}"
    } else {
        "Day"
    }

    Column(modifier = modifier) {
        Text(
            text = "DAY",
            color = TextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it }
        ) {
            TextField(
                value = label,
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.menuAnchor()
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                days.forEachIndexed { index, day ->
                    DropdownMenuItem(
                        text = { Text("Day ${index + 1}: $day") },
                        onClick = {
                            onSelected(index)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ExerciseListItem(
    exercise: Exercise,
    isSelected: Boolean,
    log: ExerciseLog?,
    onClick: () -> Unit
) {
    val bgColor = if (isSelected) {
        MaterialTheme.colorScheme.surfaceVariant
    } else {
        MaterialTheme.colorScheme.surface
    }

    val accentColor = Color.White
    val leftBorderColor = if (isSelected) accentColor else Color.Transparent

    Card(
        colors = CardDefaults.cardColors(containerColor = bgColor),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    // Left accent border for selected state
                    if (isSelected) {
                        drawLine(
                            color = leftBorderColor,
                            start = Offset(0f, 0f),
                            end = Offset(0f, size.height),
                            strokeWidth = 3.dp.toPx()
                        )
                    }
                }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${exercise.orderIndex}.",
                color = TextSecondary,
                fontSize = 15.sp,
                modifier = Modifier.padding(end = 8.dp)
            )
            Text(
                text = exercise.exerciseName,
                color = Color.White,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                fontSize = 15.sp,
                modifier = Modifier.weight(1f)
            )

            when (log?.status) {
                "DONE" -> Text(
                    text = "\u2713",
                    color = SuccessGreen,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                "SKIPPED" -> Text(
                    text = "\u21BB",
                    color = SkipBlue,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun ShowTableButton(
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Text(
        text = "Show Table",
        color = if (isSelected) Color.White else TextSecondary,
        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
        fontSize = 14.sp,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    )
}

@Composable
private fun ExerciseDetail(
    exercise: Exercise,
    onDone: (weight: String, comments: String, rpe: String) -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    var weight by remember(exercise.id) { mutableStateOf("") }
    var isBb by remember(exercise.id) { mutableStateOf(false) }
    var isDb by remember(exercise.id) { mutableStateOf(false) }
    var isMn by remember(exercise.id) { mutableStateOf(false) }
    var isEs by remember(exercise.id) { mutableStateOf(false) }
    var comments by remember(exercise.id) { mutableStateOf("") }
    var observedRpe by remember(exercise.id) { mutableStateOf("") }
    var notesExpanded by remember(exercise.id) { mutableStateOf(false) }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = Color.White,
        cursorColor = Color.White,
        focusedLabelColor = Color.White,
        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        focusedTextColor = Color.White,
        unfocusedTextColor = Color.White
    )

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Exercise name
            Text(
                text = exercise.exerciseName,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Warmup line
            if (exercise.warmupSets != "0") {
                Text(
                    text = "Warm-up: ${exercise.warmupSets} sets \u00D7 ${exercise.reps}",
                    color = TextSecondary,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
            }

            // Working sets line
            val rpeStr = if (exercise.rpe.isNotBlank()) " | RPE: ${exercise.rpe}" else ""
            Text(
                text = "Working: ${exercise.sets} sets \u00D7 ${exercise.reps}$rpeStr",
                color = TextSecondary,
                fontSize = 14.sp
            )

            // Notes toggle
            if (exercise.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (notesExpanded) "Notes \u25BC" else "Notes \u25B6",
                    color = TextSecondary,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .clickable { notesExpanded = !notesExpanded }
                        .padding(4.dp)
                )
                AnimatedVisibility(visible = notesExpanded) {
                    Text(
                        text = exercise.notes,
                        color = TextSecondary,
                        fontSize = 13.sp,
                        fontStyle = FontStyle.Italic,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Weight input + equipment checkboxes
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = weight,
                    onValueChange = { weight = it },
                    label = { Text("Weight") },
                    colors = fieldColors,
                    singleLine = true,
                    modifier = Modifier.width(120.dp)
                )

                // Equipment type checkboxes: 2x2 grid
                Column(
                    verticalArrangement = Arrangement.spacedBy(0.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        EquipmentChip("BB", isBb, { isBb = it })
                        EquipmentChip("DB", isDb, { isDb = it })
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        EquipmentChip("MN", isMn, { isMn = it })
                        EquipmentChip("ES", isEs, { isEs = it })
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Comments input
            OutlinedTextField(
                value = comments,
                onValueChange = { comments = it },
                label = { Text("Comments (optional)") },
                colors = fieldColors,
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Observed RPE input
            OutlinedTextField(
                value = observedRpe,
                onValueChange = { observedRpe = it },
                label = { Text("Observed RPE (optional)") },
                colors = fieldColors,
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Done / Skip buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { onDone(weight, comments, observedRpe) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SuccessGreen,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("DONE", fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onSkip,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SkipBlue),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = SolidColor(SkipBlue)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("SKIP", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ExerciseTable(
    exercises: List<Exercise>,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Column {
            // Header row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("#", fontWeight = FontWeight.Bold, color = TextSecondary, fontSize = 12.sp, modifier = Modifier.width(32.dp))
                Text("EXERCISE", fontWeight = FontWeight.Bold, color = TextSecondary, fontSize = 12.sp, modifier = Modifier.weight(1f))
                Text("SETS", fontWeight = FontWeight.Bold, color = TextSecondary, fontSize = 12.sp, modifier = Modifier.width(48.dp))
                Text("REPS", fontWeight = FontWeight.Bold, color = TextSecondary, fontSize = 12.sp, modifier = Modifier.width(72.dp))
                Text("RPE", fontWeight = FontWeight.Bold, color = TextSecondary, fontSize = 12.sp, modifier = Modifier.width(56.dp))
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline)

            LazyColumn {
                itemsIndexed(exercises) { _, exercise ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("${exercise.orderIndex}", color = TextSecondary, fontSize = 14.sp, modifier = Modifier.width(32.dp))
                        Text(exercise.exerciseName, color = Color.White, fontSize = 14.sp, modifier = Modifier.weight(1f))
                        Text("${exercise.sets}", color = Color.White, fontSize = 14.sp, modifier = Modifier.width(48.dp))
                        Text(exercise.reps, color = Color.White, fontSize = 14.sp, modifier = Modifier.width(72.dp))
                        Text(exercise.rpe, color = TextSecondary, fontSize = 14.sp, modifier = Modifier.width(56.dp))
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                }
            }
        }
    }
}

@Composable
private fun EquipmentChip(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val bgColor = if (checked) Color.White.copy(alpha = 0.15f) else Color.Transparent
    val borderColor = if (checked) Color.White else MaterialTheme.colorScheme.outline
    val textColor = if (checked) Color.White else TextSecondary

    Text(
        text = label,
        color = textColor,
        fontSize = 12.sp,
        fontWeight = if (checked) FontWeight.Bold else FontWeight.Normal,
        modifier = Modifier
            .clickable { onCheckedChange(!checked) }
            .background(bgColor, RoundedCornerShape(6.dp))
            .drawBehind {
                drawRoundRect(
                    color = borderColor,
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx()),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
                )
            }
            .padding(horizontal = 12.dp, vertical = 6.dp)
    )
}
