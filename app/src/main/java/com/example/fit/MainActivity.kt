package com.example.fit

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import com.example.fit.ui.ProgrammeScreen
import com.example.fit.ui.theme.FitTheme

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FitTheme {
                ProgrammeScreen()
            }
        }
    }
}
