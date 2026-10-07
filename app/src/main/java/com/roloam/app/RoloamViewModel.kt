package com.roloam.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.roloam.app.data.LocationRepository
import com.roloam.app.data.Origin
import com.roloam.app.data.TripRepository
import com.roloam.app.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class Screen { HOME, PREFERENCES, REVEAL, PLAN, MAP, STAY, PLACE, NOW }

data class UiState(
    val screen: Screen = Screen.HOME,
    val preferences: TripPreferences = TripPreferences(),
    val origin: Origin = Origin("Heidelberg", GeoPoint(49.3988, 8.6724), true),
    val rolling: Boolean = false,
    val trip: TripPlan? = null,
    val selectedStop: TripStop? = null,
    val error: String? = null
)

class RoloamViewModel(app: Application) : AndroidViewModel(app) {
    private val location = LocationRepository(app)
    private val trips = TripRepository(app)
    private val prefsStore = app.getSharedPreferences("roloam", 0)
    private val _state = MutableStateFlow(UiState(preferences = loadPreferences()))
    val state: StateFlow<UiState> = _state.asStateFlow()

    fun refreshLocation() {
        viewModelScope.launch {
            val current = location.currentOrigin()
            _state.value = _state.value.copy(origin = current)
        }
    }

    fun open(screen: Screen) {
        _state.value = _state.value.copy(screen = screen, error = null)
    }

    fun back() {
        val target = when (_state.value.screen) {
            Screen.HOME -> Screen.HOME
            Screen.PREFERENCES -> Screen.HOME
            Screen.REVEAL -> Screen.HOME
            Screen.PLAN, Screen.MAP, Screen.STAY, Screen.PLACE, Screen.NOW -> Screen.PLAN
        }
        _state.value = _state.value.copy(screen = target, selectedStop = null)
    }

    fun updatePreferences(value: TripPreferences) {
        _state.value = _state.value.copy(preferences = value)
        savePreferences(value)
    }

    fun selectStop(stop: TripStop) {
        _state.value = _state.value.copy(selectedStop = stop, screen = Screen.PLACE)
    }

    fun roll() {
        if (_state.value.rolling) return
        viewModelScope.launch {
            _state.value = _state.value.copy(rolling = true, error = null)
            runCatching { trips.roll(_state.value.origin, _state.value.preferences) }
                .onSuccess { plan ->
                    _state.value = _state.value.copy(rolling = false, trip = plan, screen = Screen.REVEAL)
                }
                .onFailure { t ->
                    _state.value = _state.value.copy(rolling = false, error = t.message ?: "Could not build a trip")
                }
        }
    }

    fun acceptTrip() {
        if (_state.value.trip != null) open(Screen.PLAN)
    }

    private fun loadPreferences(): TripPreferences {
        fun enum(name: String, default: String) = prefsStore.getString(name, default) ?: default
        return TripPreferences(
            duration = runCatching { DurationChoice.valueOf(enum("duration", "TWO")) }.getOrDefault(DurationChoice.TWO),
            transport = runCatching { TransportMode.valueOf(enum("transport", "CAR")) }.getOrDefault(TransportMode.CAR),
            budget = runCatching { Budget.valueOf(enum("budget", "CHEAP")) }.getOrDefault(Budget.CHEAP),
            stay = runCatching { StayPreference.valueOf(enum("stay", "CAMPING")) }.getOrDefault(StayPreference.CAMPING),
            style = runCatching { TripStyle.valueOf(enum("style", "BOTH")) }.getOrDefault(TripStyle.BOTH)
        )
    }

    private fun savePreferences(p: TripPreferences) {
        prefsStore.edit()
            .putString("duration", p.duration.name)
            .putString("transport", p.transport.name)
            .putString("budget", p.budget.name)
            .putString("stay", p.stay.name)
            .putString("style", p.style.name)
            .apply()
    }
}
