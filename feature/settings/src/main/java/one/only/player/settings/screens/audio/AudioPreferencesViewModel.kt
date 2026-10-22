package one.only.player.settings.screens.audio

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import one.only.player.core.data.repository.PreferencesRepository
import one.only.player.core.model.AudioEqualizerBand
import one.only.player.core.model.AudioEqualizerBuiltInPreset
import one.only.player.core.model.AudioEqualizerPreset
import one.only.player.core.model.PlayerPreferences
import one.only.player.core.model.toAudioEqualizerPreset
import one.only.player.core.model.withAudioEqualizerBandLevel
import one.only.player.core.model.withAudioEqualizerBuiltInPresetApplied
import one.only.player.core.model.withAudioEqualizerPresetApplied
import one.only.player.core.model.withAudioEqualizerPresetDeleted
import one.only.player.core.model.withAudioEqualizerPresetSaved

@HiltViewModel
class AudioPreferencesViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
) : ViewModel() {

    private val uiStateInternal = MutableStateFlow(
        AudioPreferencesUiState(
            preferences = preferencesRepository.playerPreferences.value,
        ),
    )
    val uiState = uiStateInternal.asStateFlow()

    init {
        viewModelScope.launch {
            preferencesRepository.playerPreferences.collect { preferences ->
                uiStateInternal.update { currentState ->
                    currentState.copy(preferences = preferences)
                }
            }
        }
    }

    fun onEvent(event: AudioPreferencesUiEvent) {
        when (event) {
            is AudioPreferencesUiEvent.ShowDialog -> showDialog(event.value)
            is AudioPreferencesUiEvent.UpdateAudioLanguage -> updateAudioLanguage(event.value)
            is AudioPreferencesUiEvent.UpdateMaxInitialPlayerVolume -> updateMaxInitialPlayerVolume(event.value)
            AudioPreferencesUiEvent.TogglePauseOnHeadsetDisconnect -> togglePauseOnHeadsetDisconnect()
            AudioPreferencesUiEvent.ToggleShowSystemVolumePanel -> toggleShowSystemVolumePanel()
            AudioPreferencesUiEvent.ToggleRequireAudioFocus -> toggleRequireAudioFocus()
            AudioPreferencesUiEvent.ToggleRememberPlayerVolume -> toggleRememberPlayerVolume()
            AudioPreferencesUiEvent.ToggleRememberAudioTrack -> toggleRememberAudioTrack()
            AudioPreferencesUiEvent.ToggleVolumeNormalization -> toggleVolumeNormalization()
            AudioPreferencesUiEvent.ToggleVolumeBoost -> toggleVolumeBoost()
            AudioPreferencesUiEvent.ToggleSpatialAudio -> toggleSpatialAudio()
            AudioPreferencesUiEvent.ToggleAudioEqualizer -> toggleAudioEqualizer()
            is AudioPreferencesUiEvent.UpdateAudioEqualizerBand -> updateAudioEqualizerBand(event.band, event.levelDb)
            is AudioPreferencesUiEvent.ApplyAudioEqualizerBuiltInPreset -> applyAudioEqualizerBuiltInPreset(event.preset)
            is AudioPreferencesUiEvent.ApplyAudioEqualizerPreset -> applyAudioEqualizerPreset(event.preset)
            is AudioPreferencesUiEvent.SaveAudioEqualizerPreset -> saveAudioEqualizerPreset(event.name)
            is AudioPreferencesUiEvent.DeleteAudioEqualizerPreset -> deleteAudioEqualizerPreset(event.preset)
        }
    }

    private fun showDialog(value: AudioPreferenceDialog?) {
        uiStateInternal.update {
            it.copy(showDialog = value)
        }
    }

    private fun updateAudioLanguage(value: String) {
        viewModelScope.launch {
            preferencesRepository.updatePlayerPreferences {
                it.copy(preferredAudioLanguage = value)
            }
        }
    }

    private fun togglePauseOnHeadsetDisconnect() {
        viewModelScope.launch {
            preferencesRepository.updatePlayerPreferences {
                it.copy(shouldPauseOnHeadsetDisconnect = !it.shouldPauseOnHeadsetDisconnect)
            }
        }
    }

    private fun toggleShowSystemVolumePanel() {
        viewModelScope.launch {
            preferencesRepository.updatePlayerPreferences {
                it.copy(shouldShowSystemVolumePanel = !it.shouldShowSystemVolumePanel)
            }
        }
    }

    private fun toggleRequireAudioFocus() {
        viewModelScope.launch {
            preferencesRepository.updatePlayerPreferences {
                it.copy(shouldRequireAudioFocus = !it.shouldRequireAudioFocus)
            }
        }
    }

    private fun toggleRememberPlayerVolume() {
        viewModelScope.launch {
            preferencesRepository.updatePlayerPreferences {
                it.copy(shouldRememberPlayerVolume = !it.shouldRememberPlayerVolume)
            }
        }
    }

    private fun toggleRememberAudioTrack() {
        viewModelScope.launch {
            preferencesRepository.updatePlayerPreferences {
                it.copy(shouldRememberAudioTrack = !it.shouldRememberAudioTrack)
            }
        }
    }

    private fun updateMaxInitialPlayerVolume(value: Int) {
        viewModelScope.launch {
            preferencesRepository.updatePlayerPreferences {
                it.copy(
                    maxInitialPlayerVolumePercentage = value.coerceIn(
                        PlayerPreferences.MIN_INITIAL_PLAYER_VOLUME_PERCENTAGE,
                        PlayerPreferences.MAX_INITIAL_PLAYER_VOLUME_PERCENTAGE,
                    ),
                )
            }
        }
    }

    private fun toggleSpatialAudio() {
        viewModelScope.launch {
            preferencesRepository.updatePlayerPreferences {
                it.copy(isSpatialAudioEnabled = !it.isSpatialAudioEnabled)
            }
        }
    }

    private fun toggleVolumeNormalization() {
        viewModelScope.launch {
            preferencesRepository.updatePlayerPreferences {
                it.copy(isVolumeNormalizationEnabled = !it.isVolumeNormalizationEnabled)
            }
        }
    }

    private fun toggleVolumeBoost() {
        viewModelScope.launch {
            preferencesRepository.updatePlayerPreferences {
                it.copy(isVolumeBoostEnabled = !it.isVolumeBoostEnabled)
            }
        }
    }

    private fun toggleAudioEqualizer() {
        viewModelScope.launch {
            preferencesRepository.updatePlayerPreferences {
                it.copy(shouldApplyAudioEqualizer = !it.shouldApplyAudioEqualizer)
            }
        }
    }

    private fun updateAudioEqualizerBand(
        band: AudioEqualizerBand,
        levelDb: Int,
    ) {
        viewModelScope.launch {
            preferencesRepository.updatePlayerPreferences {
                if (!it.shouldApplyAudioEqualizer) return@updatePlayerPreferences it
                it.withAudioEqualizerBandLevel(band, levelDb)
            }
        }
    }

    private fun applyAudioEqualizerBuiltInPreset(preset: AudioEqualizerBuiltInPreset) {
        viewModelScope.launch {
            preferencesRepository.updatePlayerPreferences {
                it.withAudioEqualizerBuiltInPresetApplied(preset)
            }
        }
    }

    private fun applyAudioEqualizerPreset(preset: AudioEqualizerPreset) {
        viewModelScope.launch {
            preferencesRepository.updatePlayerPreferences {
                it.withAudioEqualizerPresetApplied(preset)
            }
        }
    }

    private fun saveAudioEqualizerPreset(name: String) {
        viewModelScope.launch {
            preferencesRepository.updatePlayerPreferences {
                it.withAudioEqualizerPresetSaved(it.toAudioEqualizerPreset(name, System.currentTimeMillis()))
            }
        }
    }

    private fun deleteAudioEqualizerPreset(preset: AudioEqualizerPreset) {
        viewModelScope.launch {
            preferencesRepository.updatePlayerPreferences {
                it.withAudioEqualizerPresetDeleted(preset)
            }
        }
    }
}

@Stable
data class AudioPreferencesUiState(
    val showDialog: AudioPreferenceDialog? = null,
    val preferences: PlayerPreferences = PlayerPreferences(),
)

sealed interface AudioPreferenceDialog {
    data object AudioLanguageDialog : AudioPreferenceDialog
    data object AudioEqualizerPresets : AudioPreferenceDialog
    data object SaveAudioEqualizerPreset : AudioPreferenceDialog
}

sealed interface AudioPreferencesUiEvent {
    data class ShowDialog(val value: AudioPreferenceDialog?) : AudioPreferencesUiEvent
    data class UpdateAudioLanguage(val value: String) : AudioPreferencesUiEvent
    data class UpdateMaxInitialPlayerVolume(val value: Int) : AudioPreferencesUiEvent
    data class UpdateAudioEqualizerBand(val band: AudioEqualizerBand, val levelDb: Int) : AudioPreferencesUiEvent
    data class ApplyAudioEqualizerBuiltInPreset(val preset: AudioEqualizerBuiltInPreset) : AudioPreferencesUiEvent
    data class ApplyAudioEqualizerPreset(val preset: AudioEqualizerPreset) : AudioPreferencesUiEvent
    data class SaveAudioEqualizerPreset(val name: String) : AudioPreferencesUiEvent
    data class DeleteAudioEqualizerPreset(val preset: AudioEqualizerPreset) : AudioPreferencesUiEvent
    data object TogglePauseOnHeadsetDisconnect : AudioPreferencesUiEvent
    data object ToggleShowSystemVolumePanel : AudioPreferencesUiEvent
    data object ToggleRequireAudioFocus : AudioPreferencesUiEvent
    data object ToggleRememberPlayerVolume : AudioPreferencesUiEvent
    data object ToggleRememberAudioTrack : AudioPreferencesUiEvent
    data object ToggleVolumeNormalization : AudioPreferencesUiEvent
    data object ToggleVolumeBoost : AudioPreferencesUiEvent
    data object ToggleSpatialAudio : AudioPreferencesUiEvent
    data object ToggleAudioEqualizer : AudioPreferencesUiEvent
}
