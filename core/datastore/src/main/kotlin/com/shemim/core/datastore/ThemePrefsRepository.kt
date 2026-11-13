package com.shemim.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import android.content.Context
import com.shemim.core.designsystem.theme.DarkModePreference
import com.shemim.core.designsystem.theme.ShemimThemeKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * تنظیمات نمایش کاربر — WP-0.2.
 *
 * فقط تنظیمات «دستگاه» اینجا نگه داشته می‌شود (تم، حالت تیره، رنگ پویا).
 * انتخاب تمِ کاربر بعد از لاگین با سرور هم sync می‌شود (`updateMyTheme`) — WP-1.7.
 */
data class ThemePrefs(
    val themeKey: ShemimThemeKey,
    val darkMode: DarkModePreference,
    val dynamicColor: Boolean,
) {
    companion object {
        val Default = ThemePrefs(
            themeKey = ShemimThemeKey.DEFAULT,
            darkMode = DarkModePreference.SYSTEM,
            dynamicColor = false,
        )
    }
}

private val Context.themeDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "shemim_theme",
)

class ThemePrefsRepository(private val context: Context) {

    private object Keys {
        val THEME = stringPreferencesKey("theme_key") // همان کلید سرور
        val DARK_MODE = stringPreferencesKey("dark_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
    }

    val prefs: Flow<ThemePrefs> = context.themeDataStore.data.map { p ->
        ThemePrefs(
            themeKey = ShemimThemeKey.fromKey(p[Keys.THEME]),
            darkMode = p[Keys.DARK_MODE]?.let { runCatching { DarkModePreference.valueOf(it) }.getOrNull() }
                ?: ThemePrefs.Default.darkMode,
            dynamicColor = p[Keys.DYNAMIC_COLOR] ?: false,
        )
    }

    /** ثبت انتخاب تم — هم‌ارز کاری که وب با updateMyTheme روی سرور می‌کند (sync در WP-1.7). */
    suspend fun setTheme(themeKey: ShemimThemeKey) {
        context.themeDataStore.edit { it[Keys.THEME] = themeKey.key }
    }

    suspend fun setDarkMode(mode: DarkModePreference) {
        context.themeDataStore.edit { it[Keys.DARK_MODE] = mode.name }
    }

    suspend fun setDynamicColor(enabled: Boolean) {
        context.themeDataStore.edit { it[Keys.DYNAMIC_COLOR] = enabled }
    }
}
