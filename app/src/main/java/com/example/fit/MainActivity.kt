package com.example.fit

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fit.ui.ProgrammeScreen
import com.example.fit.ui.SettingsScreen
import com.example.fit.ui.theme.FitTheme

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FitTheme {
                val viewModel: ProgrammeViewModel = viewModel()
                var showSettings by remember { mutableStateOf(false) }

                if (showSettings) {
                    SettingsScreen(
                        onBack = { showSettings = false },
                        onDeleteProgramme = {
                            viewModel.deleteProgramme()
                            showSettings = false
                        }
                    )
                } else {
                    ProgrammeScreen(
                        viewModel = viewModel,
                        onNavigateToSettings = { showSettings = true }
                    )
                }
            }
        }
    }
}
