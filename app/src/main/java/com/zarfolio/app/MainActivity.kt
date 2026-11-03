package com.zarfolio.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.SideEffect
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.compose.foundation.isSystemInDarkTheme
import dagger.hilt.android.AndroidEntryPoint
import com.zarfolio.core.designsystem.theme.ZarfolioTheme
import com.zarfolio.app.navigation.ZarfolioRoot

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val preferences = getSharedPreferences("zarfolio_appearance", MODE_PRIVATE)
            var darkTheme by remember { mutableStateOf<Boolean?>(preferences.takeIf { it.contains("dark_theme") }?.getBoolean("dark_theme", false)) }
            val useDark = darkTheme ?: isSystemInDarkTheme()
            ZarfolioTheme(darkTheme = useDark, dynamicColor = false) {
                SideEffect {
                    WindowCompat.getInsetsController(window, window.decorView).apply {
                        isAppearanceLightStatusBars = !useDark
                        isAppearanceLightNavigationBars = !useDark
                    }
                }
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    ZarfolioRoot(
                        isDarkTheme = useDark,
                        onToggleTheme = {
                            val next = !useDark
                            darkTheme = next
                            preferences.edit().putBoolean("dark_theme", next).apply()
                        },
                    )
                }
            }
        }
    }
}
