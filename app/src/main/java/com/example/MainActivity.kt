package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.data.ExpenseDatabase
import com.example.data.ExpenseRepository
import com.example.ui.screens.ExpenseTrackerScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.ExpenseViewModel
import com.example.ui.viewmodel.ExpenseViewModelFactory

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    val database = ExpenseDatabase.getDatabase(applicationContext)
    val repository = ExpenseRepository(database.expenseDao)
    val viewModelFactory = ExpenseViewModelFactory(repository)

    setContent {
      MyApplicationTheme {
        val viewModel: ExpenseViewModel by viewModels { viewModelFactory }
        ExpenseTrackerScreen(viewModel = viewModel)
      }
    }
  }
}

@androidx.compose.runtime.Composable
fun Greeting(name: String, modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier) {
  androidx.compose.material3.Text(text = "Hello $name!", modifier = modifier)
}

