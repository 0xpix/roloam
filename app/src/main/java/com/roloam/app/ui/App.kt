package com.roloam.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.roloam.app.RoloamViewModel
import com.roloam.app.Screen

private val Cream = androidx.compose.ui.graphics.Color(0xFFF7F1E8)
private val Ink = androidx.compose.ui.graphics.Color(0xFF2C291F)
private val Muted = androidx.compose.ui.graphics.Color(0xFF827B6F)
private val Accent = androidx.compose.ui.graphics.Color(0xFF9B7832)
private val Night = androidx.compose.ui.graphics.Color(0xFF171713)

val RoloamCream = Cream
val RoloamInk = Ink
val RoloamMuted = Muted
val RoloamAccent = Accent

@Composable
fun RoloamApp(vm: RoloamViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()
    // Respect system left/right edge Back gestures, including predictive Back.
    BackHandler(enabled = state.screen != Screen.HOME) { vm.goHome() }
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) {
            darkColorScheme(
                primary = androidx.compose.ui.graphics.Color(0xFFE5D6B0),
                onPrimary = Ink,
                background = Night,
                surface = Night
            )
        } else {
            lightColorScheme(
                primary = Ink,
                onPrimary = Cream,
                background = Cream,
                surface = Cream
            )
        },
        typography = MaterialTheme.typography.copy(
            displayLarge = MaterialTheme.typography.displayLarge.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
            headlineLarge = MaterialTheme.typography.headlineLarge.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
            headlineMedium = MaterialTheme.typography.headlineMedium.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
            titleLarge = MaterialTheme.typography.titleLarge.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
            bodyLarge = MaterialTheme.typography.bodyLarge.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
            bodyMedium = MaterialTheme.typography.bodyMedium.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
            labelLarge = MaterialTheme.typography.labelLarge.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
        )
    ) {
        CompositionLocalProvider(LocalGoHome provides vm::goHome) {
        when (state.screen) {
            Screen.HOME -> HomeScreen(state, vm::open, vm::roll, vm::resumeTrip, vm::cancelTrip)
            Screen.PREFERENCES -> PreferencesScreen(state.preferences, vm::updatePreferences, vm::back)
            Screen.SETTINGS -> SettingsScreen(
                state = state,
                open = vm::open,
                back = vm::back,
                checkUpdate = vm::checkForBetaUpdate,
                downloadUpdate = vm::downloadBetaUpdate,
                installUpdate = vm::installBetaUpdate
            )
            Screen.REVEAL -> RevealScreen(state, vm::acceptTrip, vm::roll, vm::back)
            Screen.PLAN -> PlanScreen(state, vm::open, vm::selectStop, vm::back)
            Screen.MAP -> MapScreen(state, vm::back)
            Screen.STAY -> StayScreen(state, vm::back)
            Screen.PLACE -> PlaceScreen(state, vm::back)
            Screen.NOW -> NowScreen(state, vm::back)
            Screen.WEATHER -> WeatherScreen(state, vm::back)
            Screen.PACKING -> PackingScreen(state, vm::back)
        }
        }
    }
}
