package com.shemim.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.shemim.app.ui.ThemeShowcaseScreen
import com.shemim.core.datastore.ThemePrefs
import com.shemim.core.datastore.ThemePrefsRepository
import com.shemim.core.designsystem.theme.ShemimTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            // تا معرفی DI در WP-0.5، مخزن تنظیمات مستقیماً ساخته می‌شود.
            val repo = remember { ThemePrefsRepository(applicationContext) }
            val prefs by repo.prefs.collectAsState(initial = ThemePrefs.Default)
            val scope = rememberCoroutineScope()

            ShemimTheme(
                themeKey = prefs.themeKey,
                darkMode = prefs.darkMode,
                dynamicColor = prefs.dynamicColor,
            ) {
                ThemeShowcaseScreen(
                    prefs = prefs,
                    onThemeChange = { key -> scope.launch { repo.setTheme(key) } },
                    onDarkModeChange = { mode -> scope.launch { repo.setDarkMode(mode) } },
                    onDynamicColorChange = { enabled -> scope.launch { repo.setDynamicColor(enabled) } },
                )
            }
        }
    }
}
