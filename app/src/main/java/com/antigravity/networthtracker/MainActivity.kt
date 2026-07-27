package com.antigravity.networthtracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.navigation.compose.rememberNavController
import androidx.compose.material3.Scaffold
import androidx.compose.foundation.layout.padding
import com.antigravity.networthtracker.presentation.components.BottomNavigationBar
import com.antigravity.networthtracker.presentation.navigation.NavGraph
import com.antigravity.networthtracker.presentation.theme.NetWorthTrackerTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val prefs = getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
        val savedLang = prefs.getString("language_code", null)
        if (savedLang != null) {
            val locale = java.util.Locale(savedLang)
            java.util.Locale.setDefault(locale)
            val config = resources.configuration
            config.setLocale(locale)
            resources.updateConfiguration(config, resources.displayMetrics)
        }
        super.onCreate(savedInstanceState)
        setContent {
            NetWorthTrackerTheme {
                val currentDensity = LocalDensity.current
                CompositionLocalProvider(
                    LocalDensity provides Density(
                        density = currentDensity.density,
                        fontScale = 1f
                    )
                ) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        val navController = rememberNavController()
                        Scaffold(
                            bottomBar = {
                                BottomNavigationBar(navController = navController)
                            }
                        ) { paddingValues ->
                            NavGraph(
                                navController = navController,
                                modifier = Modifier.padding(paddingValues)
                            )
                        }
                    }
                }
            }
        }
    }
}
