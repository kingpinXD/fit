package com.example.fit.ui

import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fit.ProgrammeViewModel
import com.example.fit.data.ProgrammeNameNormalizer

@Composable
fun SettingsScreen(
    viewModel: ProgrammeViewModel,
    onBack: () -> Unit,
    onDeleteProgramme: () -> Unit
) {
    val context = LocalContext.current
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showCycleDialog by remember { mutableStateOf(false) }
    var exportIdentifier by remember { mutableStateOf("") }
    var cycleIdentifier by remember { mutableStateOf("") }
    var exporting by remember { mutableStateOf(false) }
    var cycling by remember { mutableStateOf(false) }

    // File picker for importing a programme
    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        try {
            var displayName = ""
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex >= 0) displayName = cursor.getString(nameIndex) ?: ""
                }
            }
            if (displayName.isBlank()) displayName = uri.lastPathSegment ?: ""

            val parentFolder = uri.path?.let { path ->
                val segments = path.split("/").filter { it.isNotBlank() }
                if (segments.size >= 2) segments[segments.size - 2] else ""
            } ?: ""

            val programmeName = ProgrammeNameNormalizer.normalize(displayName, parentFolder)

            val inputStream = context.contentResolver.openInputStream(uri)
            if (inputStream != null) {
                val onResult = { result: com.example.fit.data.ImportResult ->
                    val msg = when (result) {
                        com.example.fit.data.ImportResult.SWITCHED -> "Switched to $programmeName"
                        com.example.fit.data.ImportResult.IMPORTED -> "Imported $programmeName"
                    }
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    onBack()
                }
                if (displayName.endsWith(".json") || displayName.contains("json")) {
                    val json = inputStream.bufferedReader().use { it.readText() }
                    viewModel.importProgramme(json, programmeName, onResult)
                } else {
                    viewModel.importProgrammeFromXlsx(inputStream, programmeName, onResult)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("SettingsScreen", "Import failed", e)
            Toast.makeText(context, "Import failed", Toast.LENGTH_SHORT).show()
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Programme") },
            text = {
                Text("Are you sure? This will delete the programme and all logged data.")
            },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    onDeleteProgramme()
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!exporting) {
                    showExportDialog = false
                    exportIdentifier = ""
                }
            },
            title = { Text("Export Programme") },
            text = {
                OutlinedTextField(
                    value = exportIdentifier,
                    onValueChange = { exportIdentifier = it },
                    label = { Text("e.g. Round 1, Jan 2026") },
                    singleLine = true,
                    enabled = !exporting
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        exporting = true
                        val identifier = exportIdentifier.trim()
                            .lowercase().replace(" ", "_")
                        viewModel.exportProgramme(identifier) { json, firebaseOk ->
                            exporting = false
                            showExportDialog = false
                            exportIdentifier = ""

                            val message = if (firebaseOk) "Exported to cloud" else "Export failed (cloud)"
                            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()

                            if (json != null) {
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "application/json"
                                    putExtra(Intent.EXTRA_TEXT, json)
                                    putExtra(Intent.EXTRA_SUBJECT, "Fit Programme Export")
                                }
                                context.startActivity(
                                    Intent.createChooser(shareIntent, "Share export")
                                )
                            }
                        }
                    },
                    enabled = !exporting
                ) {
                    Text(if (exporting) "Exporting..." else "Export")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showExportDialog = false
                        exportIdentifier = ""
                    },
                    enabled = !exporting
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showCycleDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!cycling) {
                    showCycleDialog = false
                    cycleIdentifier = ""
                }
            },
            title = { Text("Start New Cycle") },
            text = {
                Column {
                    Text(
                        "This will export your current data and reset the programme for a fresh cycle.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = cycleIdentifier,
                        onValueChange = { cycleIdentifier = it },
                        label = { Text("e.g. Round 1, Jan 2026") },
                        singleLine = true,
                        enabled = !cycling
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        cycling = true
                        val identifier = cycleIdentifier.trim()
                            .lowercase().replace(" ", "_")
                        viewModel.exportProgramme(identifier) { _, firebaseOk ->
                            if (firebaseOk) {
                                viewModel.deleteProgramme()
                                Toast.makeText(context, "Cycle exported and reset", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Export failed - cycle not reset", Toast.LENGTH_SHORT).show()
                            }
                            cycling = false
                            showCycleDialog = false
                            cycleIdentifier = ""
                        }
                    },
                    enabled = !cycling
                ) {
                    Text(if (cycling) "Working..." else "Start")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showCycleDialog = false
                        cycleIdentifier = ""
                    },
                    enabled = !cycling
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            Text(
                text = "Settings",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Import programme card
        SettingsCard(
            text = "Import Programme",
            onClick = { filePicker.launch("*/*") }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Export programme card
        SettingsCard(
            text = "Export Programme",
            onClick = { showExportDialog = true }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Start new cycle card
        SettingsCard(
            text = "Start New Cycle",
            onClick = { showCycleDialog = true }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Delete programme card
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clickable { showDeleteDialog = true }
        ) {
            Text(
                text = "Delete Programme",
                color = MaterialTheme.colorScheme.error,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(16.dp)
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // Version
        Text(
            text = "v${com.example.fit.BuildConfig.VERSION_NAME}",
            color = com.example.fit.ui.theme.TextSecondary,
            fontSize = 11.sp,
            modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 16.dp)
        )
    }
}

@Composable
private fun SettingsCard(text: String, onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable(onClick = onClick)
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(16.dp)
        )
    }
}
