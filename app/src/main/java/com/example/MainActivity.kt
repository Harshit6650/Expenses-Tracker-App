package com.example

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.MainActivity
import com.example.data.ExpenseDatabase
import com.example.data.ExpenseRepository
import com.example.ui.screens.ExpenseTrackerScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.ExpenseViewModel
import com.example.ui.viewmodel.ExpenseViewModelFactory
import com.example.utils.NotificationHelper

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    val appCtx = applicationContext
    // Create notification channels and schedule daily randomized reminders
    NotificationHelper.createNotificationChannels(appCtx)
    NotificationHelper.scheduleRandomReminders(appCtx)

    val database = ExpenseDatabase.getDatabase(appCtx)
    val repository = ExpenseRepository(database.expenseDao)
    val sharedPrefs = getSharedPreferences("wise_wallet_prefs", Context.MODE_PRIVATE)
    val viewModelFactory = ExpenseViewModelFactory(repository, sharedPrefs, appCtx)
    val viewModel: ExpenseViewModel by viewModels { viewModelFactory }

    setContent {
      val themeMode by viewModel.themeMode.collectAsState()
      val useDarkTheme = when (themeMode) {
        "light" -> false
        "dark" -> true
        else -> isSystemInDarkTheme()
      }

      MyApplicationTheme(darkTheme = useDarkTheme) {
        ExpenseTrackerScreen(viewModel = viewModel)
      }
    }
  }
}

@androidx.compose.runtime.Composable
fun Greeting(name: String, modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier) {
  androidx.compose.material3.Text(text = "Hello $name!", modifier = modifier)
}

