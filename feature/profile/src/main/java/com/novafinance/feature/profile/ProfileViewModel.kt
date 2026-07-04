package com.novafinance.feature.profile

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.novafinance.core.domain.model.NovaResult
import com.novafinance.core.domain.model.ProfileSettings
import com.novafinance.core.domain.model.SoundMode
import com.novafinance.core.domain.repository.ProfileRepository
import com.novafinance.core.domain.usecase.ExportBackupUseCase
import com.novafinance.core.domain.usecase.ImportBackupUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Every toggle here now persists via DataStore (see ProfileRepositoryImpl)
 * — this used to be session-scoped, in-memory-only state (Phase 4's
 * explicit, documented deferral). That deferral is closed as of Phase
 * 8.5; state survives process death.
 */
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val exportBackupUseCase: ExportBackupUseCase,
    private val importBackupUseCase: ImportBackupUseCase
) : ViewModel() {

    val uiState: StateFlow<ProfileUiState> = profileRepository.observeSettings()
        .map { settings -> ProfileUiState(isLoading = false, settings = settings) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ProfileUiState()
        )

    /** One-off result of an export/import attempt, for the screen to surface as a snackbar. */
    private val _backupEvents = Channel<BackupEvent>(Channel.BUFFERED)
    val backupEvents: Flow<BackupEvent> = _backupEvents.receiveAsFlow()

    fun onBiometricLockToggle(enabled: Boolean) {
        viewModelScope.launch { profileRepository.setBiometricLockEnabled(enabled) }
    }

    fun onNotificationsToggle(enabled: Boolean) {
        viewModelScope.launch { profileRepository.setNotificationsEnabled(enabled) }
    }

    fun onSpendingAlertsToggle(enabled: Boolean) {
        viewModelScope.launch { profileRepository.setSpendingAlertsEnabled(enabled) }
    }

    fun onSoundModeSelect(mode: SoundMode) {
        viewModelScope.launch { profileRepository.setSoundMode(mode) }
    }

    fun onDashboardInsightsToggle(enabled: Boolean) {
        viewModelScope.launch { profileRepository.setShowDashboardInsights(enabled) }
    }

    /**
     * [onJsonReady] receives the exported JSON to write to whatever
     * destination the screen's document-creation launcher resolved to
     * — actual file I/O needs a `ContentResolver`/`Uri`, which belongs
     * in the UI layer, not here.
     */
    fun exportBackup(onJsonReady: (String) -> Unit) {
        viewModelScope.launch {
            when (val result = exportBackupUseCase(Unit)) {
                is NovaResult.Success -> onJsonReady(result.data)
                is NovaResult.Error -> _backupEvents.send(BackupEvent.Failure(result.failure.message))
                is NovaResult.Loading -> Unit
            }
        }
    }

    fun importBackup(json: String) {
        viewModelScope.launch {
            when (val result = importBackupUseCase(json)) {
                is NovaResult.Success -> _backupEvents.send(BackupEvent.ImportSucceeded)
                is NovaResult.Error -> _backupEvents.send(BackupEvent.Failure(result.failure.message))
                is NovaResult.Loading -> Unit
            }
        }
    }
}

sealed class BackupEvent {
    data object ImportSucceeded : BackupEvent()
    data class Failure(val message: String) : BackupEvent()
}

@Immutable
data class ProfileUiState(
    val isLoading: Boolean = true,
    val settings: ProfileSettings = ProfileSettings()
)
