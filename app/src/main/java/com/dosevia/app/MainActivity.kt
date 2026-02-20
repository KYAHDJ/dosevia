package com.dosevia.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DoseviaApp()
        }
    }
}

@Composable
fun DoseviaApp() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            val viewModel: AppViewModel = viewModel(
                factory = AppViewModelFactory(
                    androidx.compose.ui.platform.LocalContext.current
                )
            )
            HomeScreen(viewModel = viewModel)
        }
    }
}
