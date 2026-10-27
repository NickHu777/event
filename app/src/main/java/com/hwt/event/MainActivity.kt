package com.hwt.event

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.hwt.event.data.EventStore
import com.hwt.event.ui.EventApp
import com.hwt.event.ui.theme.EventPalette
import com.hwt.event.ui.theme.EventTheme

class MainActivity : ComponentActivity() {
    private val eventStore by lazy { EventStore(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val preferences = getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE)
        setContent {
            var palette by remember {
                mutableStateOf(EventPalette.fromPreference(preferences.getString(PALETTE_KEY, null)))
            }
            EventTheme(palette = palette) {
                EventApp(
                    store = eventStore,
                    selectedPalette = palette,
                    onPaletteChange = { selected ->
                        palette = selected
                        preferences.edit().putString(PALETTE_KEY, selected.preferenceValue).apply()
                    }
                )
            }
        }
    }

    private companion object {
        const val PREFERENCES_NAME = "event_preferences"
        const val PALETTE_KEY = "theme_palette"
    }
}
