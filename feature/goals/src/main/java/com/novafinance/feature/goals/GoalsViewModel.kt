package com.novafinance.feature.goals

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.novafinance.core.domain.model.GoalForecast
import com.novafinance.core.domain.model.Money
import com.novafinance.core.domain.model.NovaResult
import com.novafinance.core.domain.model.SavingsGoal
import com.novafinance.core.domain.repository.GoalRepository
import com.novafinance.core.domain.usecase.GetGoalForecastUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class GoalsViewModel @Inject constructor(
    private val getGoalForecast: GetGoalForecastUseCase,
    private val goalRepository: GoalRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(GoalsUiState())
    val uiState: StateFlow<GoalsUiState> = _uiState.asStateFlow()

    private val _isFormSheetVisible = MutableStateFlow(false)
    val isFormSheetVisible: StateFlow<Boolean> = _isFormSheetVisible

    /** Non-null while the form sheet is open in edit mode; null means "add" mode. */
    private val _editingGoal = MutableStateFlow<SavingsGoal?>(null)
    val editingGoal: StateFlow<SavingsGoal?> = _editingGoal

    /** Non-null while the delete-confirmation dialog is showing for this goal. */
    private val _pendingDelete = MutableStateFlow<SavingsGoal?>(null)
    val pendingDelete: StateFlow<SavingsGoal?> = _pendingDelete

    init {
        viewModelScope.launch {
            getGoalForecast(Unit).collect { result ->
                _uiState.value = when (result) {
                    is NovaResult.Success -> GoalsUiState(isLoading = false, forecasts = result.data)
                    is NovaResult.Error -> GoalsUiState(isLoading = false, errorMessage = result.failure.message)
                    is NovaResult.Loading -> _uiState.value.copy(isLoading = true)
                }
            }
        }
    }

    fun onAddGoalClick() {
        _editingGoal.value = null
        _isFormSheetVisible.value = true
    }

    fun onEditClick(goal: SavingsGoal) {
        _editingGoal.value = goal
        _isFormSheetVisible.value = true
    }

    fun onDismissFormSheet() {
        _isFormSheetVisible.value = false
        _editingGoal.value = null
    }

    /**
     * Handles both add and edit. [SavingsGoal.percentComplete],
     * [SavingsGoal.remaining], and the forecast this screen renders are
     * all computed properties/use-case outputs derived from
     * targetAmount/currentAmount — changing targetAmount here
     * automatically recalculates progress everywhere it's shown; there's
     * no separate "recompute progress" step to remember to call.
     */
    fun submitForm(name: String, targetAmount: Money, targetDate: LocalDate?) {
        if (name.isBlank()) return
        val editing = _editingGoal.value

        viewModelScope.launch {
            if (editing == null) {
                goalRepository.addGoal(
                    SavingsGoal(
                        id = UUID.randomUUID().toString(),
                        name = name.trim(),
                        targetAmount = targetAmount,
                        currentAmount = Money.ZERO,
                        targetDate = targetDate,
                        createdAt = LocalDate.now()
                    )
                )
            } else {
                goalRepository.updateGoal(
                    editing.copy(
                        name = name.trim(),
                        targetAmount = targetAmount,
                        targetDate = targetDate
                    )
                )
            }
            onDismissFormSheet()
        }
    }

    fun contribute(goalId: String, amount: Money) {
        viewModelScope.launch {
            goalRepository.contribute(goalId, amount)
        }
    }

    fun onDeleteClick(goal: SavingsGoal) {
        _pendingDelete.value = goal
    }

    fun onDismissDeleteConfirmation() {
        _pendingDelete.value = null
    }

    fun confirmDelete() {
        val goal = _pendingDelete.value ?: return
        viewModelScope.launch {
            goalRepository.deleteGoal(goal.id)
            _pendingDelete.value = null
        }
    }
}

@Immutable
data class GoalsUiState(
    val isLoading: Boolean = true,
    val forecasts: List<GoalForecast> = emptyList(),
    val errorMessage: String? = null
)
