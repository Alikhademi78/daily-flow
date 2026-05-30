package com.example

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.feature.home.HomeScreen
import com.example.feature.tasks.TaskListScreen
import com.example.feature.home.MainViewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Optimize refresh rate for high frequency displays (90Hz / 120Hz / 144Hz)
        tryEnableHighRefreshRate()
        
        enableEdgeToEdge()

        // Instantiate our main central state ViewModel
        val viewModel = ViewModelProvider(this)[MainViewModel::class.java]

        setContent {
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            val isDark = when (themeMode) {
                "light" -> false
                "dark" -> true
                else -> {
                    val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
                    hour < 6 || hour >= 18
                }
            }

            MyApplicationTheme(darkTheme = isDark) {
                val navController = rememberNavController()
                NavHost(navController = navController, startDestination = "home") {
                    composable("home") {
                        HomeScreen(
                            viewModel = viewModel,
                            onNavigateToTasks = { navController.navigate("tasks") }
                        )
                    }
                    composable("tasks") {
                        TaskListScreen(
                            viewModel = viewModel,
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }

    private fun tryEnableHighRefreshRate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val display = display
                val maxMode = display?.supportedModes?.maxByOrNull { it.refreshRate }
                if (maxMode != null) {
                    val layoutParams = window.attributes
                    layoutParams.preferredDisplayModeId = maxMode.modeId
                    window.attributes = layoutParams
                }
            } catch (e: Exception) {
                // Safe background fallback
            }
        } else {
            try {
                val layoutParams = window.attributes
                @Suppress("DEPRECATION")
                layoutParams.preferredRefreshRate = 120f
                window.attributes = layoutParams
            } catch (e: Exception) {
                // Safe background fallback
            }
        }
    }
}
