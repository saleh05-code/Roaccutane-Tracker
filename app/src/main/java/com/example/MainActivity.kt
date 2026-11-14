package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.google.firebase.FirebaseApp
import com.example.ui.RoaccutaneApp
import com.example.ui.RoaccutaneViewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  private val viewModel: RoaccutaneViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    
    // تفعيل السحابة فوراً لمنع الانهيار (Crash)
    FirebaseApp.initializeApp(this)

    setContent {
      val sharedPrefs = remember { getSharedPreferences("roaccutane_prefs", android.content.Context.MODE_PRIVATE) }
      var isDarkTheme by remember { mutableStateOf(sharedPrefs.getBoolean("is_dark_theme", true)) }

      MyApplicationTheme(darkTheme = isDarkTheme) {
        RoaccutaneApp(
          viewModel = viewModel,
          isDarkTheme = isDarkTheme,
          onThemeToggle = {
            val newValue = !isDarkTheme
            isDarkTheme = newValue
            sharedPrefs.edit().putBoolean("is_dark_theme", newValue).apply()
          }
        )
      }
    }
  }
}
