package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.example.simulator.NotificationHelper
import com.example.ui.screens.MainScreen
import com.example.ui.theme.FootDirectTheme
import com.example.ui.viewmodel.FootballViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: FootballViewModel by viewModels {
        FootballViewModel.Factory(application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Edge to edge immersive display
        enableEdgeToEdge()

        // Configure system notification channel for real-time live alerts
        NotificationHelper.createNotificationChannel(this)

        // If activity was opened from a notification click, we can focus on that match
        val matchIdFromNotification = intent.getIntExtra("MATCH_ID", -1)
        if (matchIdFromNotification != -1) {
            viewModel.allMatches.value.find { it.id == matchIdFromNotification }?.let { match ->
                viewModel.selectMatch(match)
            }
        }

        setContent {
            FootDirectTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainScreen(
                        viewModel = viewModel,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val matchIdFromNotification = intent.getIntExtra("MATCH_ID", -1)
        if (matchIdFromNotification != -1) {
            viewModel.allMatches.value.find { it.id == matchIdFromNotification }?.let { match ->
                viewModel.selectMatch(match)
            }
        }
    }
}
