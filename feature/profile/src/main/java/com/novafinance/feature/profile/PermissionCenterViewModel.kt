package com.novafinance.feature.profile

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.novafinance.core.domain.model.PermissionInfo
import com.novafinance.core.domain.model.PermissionType
import com.novafinance.core.domain.repository.PermissionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class PermissionCenterViewModel @Inject constructor(
    private val permissionRepository: PermissionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PermissionCenterUiState())
    val uiState: StateFlow<PermissionCenterUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    /**
     * OS permission grants have no change-notification API to observe —
     * the screen calls this from a lifecycle resume effect after
     * returning from the system permission dialog (or Settings), since
     * that's the only reliable point a grant could have changed.
     */
    fun refresh() {
        viewModelScope.launch {
            val statuses = permissionRepository.checkStatuses()
            _uiState.value = PermissionCenterUiState(isLoading = false, permissions = statuses)
        }
    }

    fun onOsPermissionResult(type: PermissionType, granted: Boolean) {
        viewModelScope.launch {
            permissionRepository.recordOsPermissionResult(type, granted)
            refresh()
        }
    }

    fun onAcknowledge(type: PermissionType) {
        viewModelScope.launch {
            permissionRepository.acknowledge(type)
            refresh()
        }
    }
}

@Immutable
data class PermissionCenterUiState(
    val isLoading: Boolean = true,
    val permissions: List<PermissionInfo> = emptyList()
)
