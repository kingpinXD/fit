package com.example.fit.ui

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fit.ProgrammeViewModel
import com.example.fit.data.Exercise
import com.example.fit.data.ExerciseHistoryEntry
import com.example.fit.data.ExerciseLog
import com.example.fit.data.ProgrammeNameNormalizer
import com.example.fit.ui.theme.LocalFitSizing
import com.example.fit.ui.theme.RpePurple
import com.example.fit.ui.theme.EquipmentGreen
import com.example.fit.ui.theme.SkipBlue
import com.example.fit.ui.theme.SuccessGreen
import com.example.fit.ui.theme.TextSecondary

@Composable
fun ProgrammeScreen(
    viewModel: ProgrammeViewModel = viewModel(),
    onNavigateToSettings: () -> Unit = {}
) {
    val hasProgramme by viewModel.hasProgramme.observeAsState(false)

    if (!hasProgramme) {
        ImportScreen(viewModel = viewModel)
        return
    }

    val programmeNameValue by viewModel.programmeName.observeAsState("")
    val weeks by viewModel.weeks.observeAsState(emptyList())
    val completedWeeks by viewModel.completedWeeks.observeAsState(emptyList())
    val days by viewModel.days.observeAsState(emptyList())
    val completedDays by viewModel.completedDays.observeAsState(emptyList())
    val exercises by viewModel.exercises.observeAsState(emptyList())
    val exerciseLogs by viewModel.exerciseLogs.observeAsState(emptyList())
    val selectedExercise by viewModel.selectedExercise.observeAsState(null)
    val selectedExerciseLog by viewModel.selectedExerciseLog.observeAsState(null)
    val exerciseHistory by viewModel.exerciseHistory.observeAsState(emptyList())
    val showTable by viewModel.showTable.observeAsState(false)
    val showHistory by viewModel.showHistory.observeAsState(false)

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
        // Top bar with title and settings icon
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = programmeNameValue.ifBlank { "Fit" },
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = onNavigateToSettings) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Settings",
                    tint = Color.White
                )
            }
        }

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
                Box(modifier = Modifier.weight(1f)) {
                    WeekDropdown(
                        weeks = weeks,
                        completedWeeks = completedWeeks,
                        selectedIndex = selectedWeekIndex,
                        onSelected = { index ->
                            selectedWeekIndex = index
                            viewModel.selectedWeek.value = weeks[index]
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    DayDropdown(
                        days = days,
                        completedDays = completedDays,
                        selectedIndex = selectedDayIndex,
                        onSelected = { index ->
                            selectedDayIndex = index
                            viewModel.selectedDay.value = days[index]
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // 2. Exercise list (2-column grid)
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(exercises.size) { index ->
                val exercise = exercises[index]
                ExerciseListItem(
                    exercise = exercise,
                    isSelected = exercise.id == selectedExercise?.id,
                    log = logsByExerciseId[exercise.id],
                    onClick = {
                        viewModel.selectedExercise.value = exercise
                        viewModel.showTable.value = false
                        viewModel.showHistory.value = false
                    },
                    onLongPress = {
                        viewModel.selectedExercise.value = exercise
                        viewModel.showTable.value = false
                        viewModel.showHistory.value = true
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
                    .weight(1f)
                    .padding(horizontal = 16.dp, vertical = 0.dp)
            )
        } else if (selectedExercise != null) {
            ExerciseDetail(
                exercise = selectedExercise!!,
                existingLog = selectedExerciseLog,
                history = exerciseHistory,
                showHistory = showHistory,
                onDone = { weight, equipment, comments, rpe ->
                    viewModel.markDone(selectedExercise!!, weight, equipment, comments, rpe)
                    viewModel.showHistory.value = false
                    advanceToNext(exercises, selectedExercise!!, viewModel)
                },
                onSkip = {
                    viewModel.markSkipped(selectedExercise!!)
                    viewModel.showHistory.value = false
                    advanceToNext(exercises, selectedExercise!!, viewModel)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun ImportScreen(viewModel: ProgrammeViewModel) {
    val context = LocalContext.current
    var importing by remember { mutableStateOf(false) }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        importing = true
        try {
            // Extract display name from content resolver
            var displayName = ""
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex >= 0) displayName = cursor.getString(nameIndex) ?: ""
                }
            }
            if (displayName.isBlank()) displayName = uri.lastPathSegment ?: ""

            // Best-effort parent folder from URI path
            val parentFolder = uri.path?.let { path ->
                val segments = path.split("/").filter { it.isNotBlank() }
                if (segments.size >= 2) segments[segments.size - 2] else ""
            } ?: ""

            val programmeName = ProgrammeNameNormalizer.normalize(displayName, parentFolder)

            val inputStream = context.contentResolver.openInputStream(uri)
            if (inputStream != null) {
                if (displayName.endsWith(".json") || displayName.contains("json")) {
                    val json = inputStream.bufferedReader().use { it.readText() }
                    viewModel.importProgramme(json, programmeName)
                } else {
                    viewModel.importProgrammeFromXlsx(inputStream, programmeName)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("ImportScreen", "Import failed", e)
        }
        importing = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Fit",
            color = Color.White,
            fontSize = 48.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Track your programme",
            color = TextSecondary,
            fontSize = 16.sp
        )

        Spacer(modifier = Modifier.height(48.dp))

        if (importing) {
            CircularProgressIndicator(
                color = Color.White,
                modifier = Modifier.size(48.dp)
            )
        } else {
            OutlinedButton(
                onClick = { filePicker.launch("*/*") },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = SolidColor(Color.White)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(
                    text = "IMPORT PROGRAMME",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
            }
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
    completedWeeks: List<Int>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedWeek = weeks.getOrNull(selectedIndex)
    val isComplete = selectedWeek != null && selectedWeek in completedWeeks
    val label = if (weeks.isNotEmpty() && selectedIndex in weeks.indices) {
        if (isComplete) "Week ${weeks[selectedIndex]} \u2713" else "Week ${weeks[selectedIndex]}"
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
            onExpandedChange = { expanded = it },
            modifier = Modifier.fillMaxWidth()
        ) {
            TextField(
                value = label,
                onValueChange = {},
                readOnly = true,
                singleLine = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                weeks.forEachIndexed { index, week ->
                    val weekComplete = week in completedWeeks
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Week $week")
                                if (weekComplete) {
                                    Text(" \u2713", color = SuccessGreen, fontWeight = FontWeight.Bold)
                                }
                            }
                        },
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
    completedDays: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedDay = days.getOrNull(selectedIndex)
    val isComplete = selectedDay != null && selectedDay in completedDays
    val label = if (days.isNotEmpty() && selectedIndex in days.indices) {
        val base = "D${selectedIndex + 1}: ${days[selectedIndex]}"
        if (isComplete) "$base \u2713" else base
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
            onExpandedChange = { expanded = it },
            modifier = Modifier.fillMaxWidth()
        ) {
            TextField(
                value = label,
                onValueChange = {},
                readOnly = true,
                singleLine = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                days.forEachIndexed { index, day ->
                    val dayComplete = day in completedDays
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Day ${index + 1}: $day")
                                if (dayComplete) {
                                    Text(" \u2713", color = SuccessGreen, fontWeight = FontWeight.Bold)
                                }
                            }
                        },
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ExerciseListItem(
    exercise: Exercise,
    isSelected: Boolean,
    log: ExerciseLog?,
    onClick: () -> Unit,
    onLongPress: () -> Unit
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
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongPress
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    if (isSelected) {
                        drawLine(
                            color = leftBorderColor,
                            start = Offset(0f, 0f),
                            end = Offset(0f, size.height),
                            strokeWidth = 3.dp.toPx()
                        )
                    }
                }
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${exercise.orderIndex}.",
                color = TextSecondary,
                fontSize = 11.sp,
                modifier = Modifier.padding(end = 4.dp)
            )
            Text(
                text = exercise.exerciseName,
                color = Color.White,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            when (log?.status) {
                "DONE" -> Text(
                    text = "\u2713",
                    color = SuccessGreen,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                "SKIPPED" -> Text(
                    text = "\u21BB",
                    color = SkipBlue,
                    fontSize = 12.sp,
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
    existingLog: ExerciseLog?,
    history: List<ExerciseHistoryEntry>,
    showHistory: Boolean,
    onDone: (weight: String, equipment: String, comments: String, rpe: String) -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Parse existing equipment tags
    val existingEquipment = remember(existingLog) {
        existingLog?.equipmentType?.split(",")?.map { it.trim() }?.toSet() ?: emptySet()
    }

    var weight by remember(exercise.id, existingLog) { mutableStateOf(existingLog?.userWeight ?: "") }
    var isBb by remember(exercise.id, existingLog) { mutableStateOf("Barbell" in existingEquipment) }
    var isDb by remember(exercise.id, existingLog) { mutableStateOf("Dumbbell" in existingEquipment) }
    var isMn by remember(exercise.id, existingLog) { mutableStateOf("Machine" in existingEquipment) }
    var isEs by remember(exercise.id, existingLog) { mutableStateOf("Each Side" in existingEquipment) }
    var comments by remember(exercise.id, existingLog) { mutableStateOf(existingLog?.userComments ?: "") }
    var observedRpe by remember(exercise.id, existingLog) { mutableStateOf(existingLog?.observedRpe ?: "") }
    var notesExpanded by remember(exercise.id) { mutableStateOf(false) }
    var altsExpanded by remember(exercise.id) { mutableStateOf(false) }

    val sz = LocalFitSizing.current
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
        shape = RoundedCornerShape(sz.cardCorner),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .padding(sz.cardPadding)
        ) {
            // Exercise name
            Text(
                text = exercise.exerciseName,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(sz.xxs))

            // Warmup line
            if (exercise.warmupSets != "0") {
                Text(
                    text = "Warm-up: ${exercise.warmupSets} sets \u00D7 ${exercise.reps.replace(Regex("\\s*\\(.*\\)"), "")}",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }

            // Working sets line
            val rpeStr = if (exercise.rpe.isNotBlank()) " | RPE: ${exercise.rpe}" else ""
            Text(
                text = "Working: ${exercise.sets} sets \u00D7 ${exercise.reps}$rpeStr",
                color = TextSecondary,
                fontSize = 12.sp
            )

            // Notes toggle
            if (exercise.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(sz.xxs))
                Text(
                    text = if (notesExpanded) "Notes \u25BC" else "Notes \u25B6",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .clickable { notesExpanded = !notesExpanded }
                        .padding(2.dp)
                )
                AnimatedVisibility(visible = notesExpanded) {
                    Text(
                        text = exercise.notes,
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontStyle = FontStyle.Italic,
                        modifier = Modifier.padding(horizontal = sz.xxs, vertical = 2.dp)
                    )
                }
            }

            // Alternatives toggle
            val hasAlts = exercise.sub1.isNotBlank() || exercise.sub2.isNotBlank()
            if (hasAlts) {
                Spacer(modifier = Modifier.height(sz.xxs))
                Text(
                    text = if (altsExpanded) "Alternatives \u25BC" else "Alternatives \u25B6",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .clickable { altsExpanded = !altsExpanded }
                        .padding(2.dp)
                )
                AnimatedVisibility(visible = altsExpanded) {
                    Column(modifier = Modifier.padding(horizontal = sz.xxs, vertical = 2.dp)) {
                        if (exercise.sub1.isNotBlank()) {
                            Text(
                                text = "\u2022 ${exercise.sub1}",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                        if (exercise.sub2.isNotBlank()) {
                            Text(
                                text = "\u2022 ${exercise.sub2}",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            // History section (shown on long press)
            AnimatedVisibility(visible = showHistory) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    Text(
                        text = "PREVIOUS WEEKS",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    if (history.isEmpty()) {
                        Text(
                            text = "No data available",
                            color = TextSecondary,
                            fontSize = 13.sp,
                            fontStyle = FontStyle.Italic
                        )
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            history.forEach { entry ->
                                HistoryCard(entry)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(sz.xs))

            // Row 1: Weight + RPE + Equipment grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(sz.xxs)
            ) {
                OutlinedTextField(
                    value = weight,
                    onValueChange = { weight = it },
                    label = { Text("Wt (lbs)", fontSize = 10.sp) },
                    colors = fieldColors,
                    singleLine = true,
                    modifier = Modifier.weight(0.8f)
                )
                OutlinedTextField(
                    value = observedRpe,
                    onValueChange = { observedRpe = it },
                    label = { Text("RPE (opt)", fontSize = 9.sp, maxLines = 1) },
                    colors = fieldColors,
                    singleLine = true,
                    modifier = Modifier.width(76.dp)
                )

                // Equipment type buttons: 2x2 compact grid
                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        EquipmentChip("Barbell", isBb, { isBb = it }, Modifier.weight(1f))
                        EquipmentChip("Dumbbell", isDb, { isDb = it }, Modifier.weight(1f))
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        EquipmentChip("Machine", isMn, { isMn = it }, Modifier.weight(1f))
                        EquipmentChip("Each Side", isEs, { isEs = it }, Modifier.weight(1f))
                    }
                }
            }

            Spacer(modifier = Modifier.height(sz.xxs))

            // Row 2: Comments
            OutlinedTextField(
                value = comments,
                onValueChange = { comments = it },
                label = { Text("Comments (opt)", fontSize = 10.sp) },
                colors = fieldColors,
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(sz.xs))

            // Done / Skip buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(sz.xs)
            ) {
                Button(
                    onClick = {
                        val equipment = listOfNotNull(
                            if (isBb) "Barbell" else null,
                            if (isDb) "Dumbbell" else null,
                            if (isMn) "Machine" else null,
                            if (isEs) "Each Side" else null
                        ).joinToString(",")
                        onDone(weight, equipment, comments, observedRpe)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SuccessGreen,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(sz.buttonCorner),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("DONE", fontWeight = FontWeight.Bold, fontSize = 13.sp)
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
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor = if (checked) Color.White.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
    val borderColor = if (checked) Color.White else MaterialTheme.colorScheme.outline
    val textColor = if (checked) Color.White else TextSecondary

    Row(
        horizontalArrangement = Arrangement.Center,
        modifier = modifier
            .clickable { onCheckedChange(!checked) }
            .background(bgColor, RoundedCornerShape(4.dp))
            .drawBehind {
                drawRoundRect(
                    color = borderColor,
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx()),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
                )
            }
            .padding(horizontal = 3.dp, vertical = 3.dp)
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 9.sp,
            maxLines = 1,
            fontWeight = if (checked) FontWeight.Bold else FontWeight.Normal
        )
    }
}


@Composable
private fun HistoryCard(entry: ExerciseHistoryEntry) {
    var showComments by remember { mutableStateOf(false) }
    val equipmentTags = entry.equipmentType.split(",").filter { it.isNotBlank() }
    val shortEquipment = mapOf(
        "Barbell" to "BB", "Dumbbell" to "DB",
        "Machine" to "MN", "Each Side" to "ES"
    )

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.width(120.dp)
    ) {
        Column(
            modifier = Modifier.padding(10.dp)
        ) {
            // Week label
            Text(
                text = "W${entry.weekNumber}",
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Weight
            if (entry.userWeight.isNotBlank()) {
                Text(
                    text = entry.userWeight,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            } else {
                Text(
                    text = if (entry.status == "SKIPPED") "Skipped" else "—",
                    color = TextSecondary,
                    fontSize = 14.sp,
                    fontStyle = FontStyle.Italic
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // RPE + Equipment chips + comment arrow
            Row(
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (entry.observedRpe.isNotBlank()) {
                    HistoryChip(entry.observedRpe, RpePurple)
                }
                equipmentTags.forEach { tag ->
                    HistoryChip(shortEquipment[tag] ?: tag, EquipmentGreen)
                }
                if (entry.userComments.isNotBlank()) {
                    Text(
                        text = if (showComments) "\u25BC" else "\u25B6",
                        fontSize = 10.sp,
                        color = TextSecondary,
                        modifier = Modifier.clickable { showComments = !showComments }
                    )
                }
            }

            // Expandable comments
            AnimatedVisibility(visible = showComments) {
                Text(
                    text = entry.userComments,
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontStyle = FontStyle.Italic,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun HistoryChip(label: String, color: Color) {
    Text(
        text = label,
        color = Color.White,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .background(color.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
            .padding(horizontal = 5.dp, vertical = 2.dp)
    )
}
