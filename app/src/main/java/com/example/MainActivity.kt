package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.ClearChatConfirmationDialog
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.SettingsDialog
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.MayaViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MayaViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = DarkBackground
                ) {
                    MayaApp(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun MayaApp(
    viewModel: MayaViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    Crossfade(
        targetState = uiState.activeScreen,
        label = "screen_crossfade",
        modifier = modifier.fillMaxSize()
    ) { screen ->
        when (screen) {
            AppScreen.HOME -> {
                HomeScreen(
                    viewModel = viewModel,
                    uiState = uiState
                )
            }
            AppScreen.CHAT -> {
                ChatScreen(
                    viewModel = viewModel,
                    uiState = uiState
                )
            }
        }
    }

    // Settings Modal Dialog
    if (uiState.showSettings) {
        SettingsDialog(
            viewModel = viewModel,
            settings = settings,
            onDismiss = { viewModel.closeSettings() }
        )
    }

    // Clear Confirmation Dialog
    if (uiState.showClearConfirmation) {
        ClearChatConfirmationDialog(
            onConfirm = { viewModel.clearChatHistory() },
            onDismiss = { viewModel.closeClearConfirmation() }
        )
    }
}

