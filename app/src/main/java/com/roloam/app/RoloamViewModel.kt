package com.roloam.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.roloam.app.data.LocationRepository
import com.roloam.app.data.Origin
import com.roloam.app.data.TripRepository
import com.roloam.app.data.TripStore
import com.roloam.app.model.*
import com.roloam.app.update.BetaUpdate
import com.roloam.app.update.BetaUpdateManager
import com.roloam.app.update.UpdateCheck
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class Screen { HOME, PREFERENCES, SETTINGS, REVEAL, PLAN, MAP, STAY, PLACE, NOW, WEATHER, PACKING }

data class UiState(
    val screen: Screen = Screen.HOME,
    val preferences: TripPreferences = TripPreferences(),
    val origin: Origin = Origin("Heidelberg", GeoPoint(49.3988, 8.6724), true),
    val rolling: Boolean = false,
    val trip: TripPlan? = null,
    val savedTrip: TripPlan? = null,
    val savedTripPreferences: TripPreferences? = null,
    val selectedStop: TripStop? = null,
    val error: String? = null,
    val updateChecking: Boolean = false,
    val updateAvailable: BetaUpdate? = null,
    val updateMessage: String? = null,
    val updateDownloadProgress: Int? = null,
    val downloadedUpdate: File? = null
)

class RoloamViewModel(app: Application) : AndroidViewModel(app) {
    private val location = LocationRepository(app)
    private val trips = TripRepository(app)
    private val tripStore = TripStore(app)
    private val updates = BetaUpdateManager(app)
    private val prefsStore = app.getSharedPreferences("roloam", 0)
    private val _state = MutableStateFlow(UiState(
        preferences = loadPreferences(),
        savedTrip = tripStore.load(),
        savedTripPreferences = tripStore.loadPreferences()
    ))
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
            Screen.PREFERENCES -> Screen.SETTINGS
            Screen.SETTINGS -> Screen.HOME
            Screen.REVEAL -> Screen.HOME
            Screen.PLAN -> Screen.REVEAL
            Screen.MAP, Screen.STAY, Screen.PLACE, Screen.NOW, Screen.WEATHER, Screen.PACKING -> Screen.PLAN
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
        val origin = _state.value.origin
        val preferences = _state.value.preferences
        viewModelScope.launch {
            _state.value = _state.value.copy(rolling = true, error = null)
            runCatching { trips.roll(origin, preferences) }
                .onSuccess { plan ->
                    _state.value = _state.value.copy(rolling = false, trip = plan, screen = Screen.REVEAL)
                }
                .onFailure { t ->
                    _state.value = _state.value.copy(rolling = false, error = t.message ?: "Could not build a trip")
                }
        }
    }

    fun acceptTrip() {
        val plan = _state.value.trip ?: return
        val preferences = _state.value.preferences
        tripStore.save(plan, preferences)
        _state.value = _state.value.copy(
            savedTrip = plan, savedTripPreferences = preferences, screen = Screen.HOME, error = null
        )
    }

    fun resumeTrip() {
        val plan = _state.value.savedTrip ?: return
        _state.value = _state.value.copy(
            trip = plan,
            preferences = _state.value.savedTripPreferences ?: _state.value.preferences,
            screen = Screen.PLAN, error = null
        )
    }

    /** Cancel only the accepted journey; don't delete global trip preferences. */
    fun cancelTrip() {
        val previousId = _state.value.savedTrip?.generatedAtEpochMs
        tripStore.clear()
        if (previousId != null) {
            getApplication<Application>()
                .getSharedPreferences("roloam_progress", 0)
                .edit().remove("visited_$previousId").apply()
            getApplication<Application>()
                .getSharedPreferences("roloam_packing", 0)
                .edit().remove("packed_$previousId").apply()
        }
        _state.value = _state.value.copy(
            savedTrip = null, savedTripPreferences = null, trip = null,
            selectedStop = null, screen = Screen.HOME, error = null
        )
    }

    /** Android Back is triggered by the system's edge swipe from either side. */
    fun goHome() {
        if (_state.value.screen != Screen.HOME) {
            _state.value = _state.value.copy(screen = Screen.HOME, selectedStop = null, error = null)
        }
    }

    fun checkForBetaUpdate() {
        if (!BuildConfig.BETA_CHANNEL || _state.value.updateChecking) return
        viewModelScope.launch {
            _state.value = _state.value.copy(
                updateChecking = true,
                updateMessage = "Checking GitHub…",
                updateAvailable = null,
                downloadedUpdate = null,
                updateDownloadProgress = null
            )

            when (val result = updates.check()) {
                is UpdateCheck.Available -> {
                    _state.value = _state.value.copy(
                        updateChecking = false,
                        updateAvailable = result.update,
                        updateMessage = "Beta " + result.update.version + " is available."
                    )
                }
                is UpdateCheck.Current -> {
                    _state.value = _state.value.copy(
                        updateChecking = false,
                        updateMessage = "You're on the newest beta."
                    )
                }
                is UpdateCheck.Failed -> {
                    _state.value = _state.value.copy(
                        updateChecking = false,
                        updateMessage = result.message
                    )
                }
            }
        }
    }

    fun downloadBetaUpdate() {
        if (!BuildConfig.BETA_CHANNEL) return
        val update = _state.value.updateAvailable ?: return
        if (_state.value.updateDownloadProgress != null) return

        viewModelScope.launch {
            _state.value = _state.value.copy(
                updateDownloadProgress = 0,
                updateMessage = "Downloading " + update.version + "…"
            )
            runCatching {
                updates.download(update) { progress ->
                    _state.value = _state.value.copy(updateDownloadProgress = progress)
                }
            }.onSuccess { file ->
                _state.value = _state.value.copy(
                    updateDownloadProgress = 100,
                    downloadedUpdate = file,
                    updateMessage = "Update verified. Ready to install."
                )
            }.onFailure { error ->
                _state.value = _state.value.copy(
                    updateDownloadProgress = null,
                    downloadedUpdate = null,
                    updateMessage = error.message ?: "Update download failed"
                )
            }
        }
    }

    fun installBetaUpdate() {
        if (!BuildConfig.BETA_CHANNEL) return
        val file = _state.value.downloadedUpdate ?: return
        runCatching { updates.install(file) }
            .onFailure { error ->
                _state.value = _state.value.copy(
                    updateMessage = error.message ?: "Could not open Android installer"
                )
            }
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
