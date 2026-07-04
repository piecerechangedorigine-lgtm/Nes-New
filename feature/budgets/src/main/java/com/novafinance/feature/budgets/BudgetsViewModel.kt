package com.novafinance.feature.budgets

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.novafinance.core.domain.model.Budget
import com.novafinance.core.domain.model.BudgetProgress
import com.novafinance.core.domain.model.Money
import com.novafinance.core.domain.model.NovaResult
import com.novafinance.core.domain.model.TransactionCategory
import com.novafinance.core.domain.repository.BudgetRepository
import com.novafinance.core.domain.usecase.GetBudgetProgressUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.YearMonth
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class BudgetsViewModel @Inject constructor(
    private val getBudgetProgress: GetBudgetProgressUseCase,
    private val budgetRepository: BudgetRepository
) : ViewModel() {

    private val currentMonth = YearMonth.now()

    private val _uiState = MutableStateFlow(BudgetsUiState())
    val uiState: StateFlow<BudgetsUiState> = _uiState.asStateFlow()

    private val _isFormSheetVisible = MutableStateFlow(false)
    val isFormSheetVisible: StateFlow<Boolean> = _isFormSheetVisible

    /** Non-null while the form sheet is open in edit mode; null means "add" mode. */
    private val _editingBudget = MutableStateFlow<Budget?>(null)
    val editingBudget: StateFlow<Budget?> = _editingBudget

    /** Non-null while the delete-confirmation dialog is showing for this budget. */
    private val _pendingDelete = MutableStateFlow<Budget?>(null)
    val pendingDelete: StateFlow<Budget?> = _pendingDelete

    init {
        viewModelScope.launch {
            getBudgetProgress(currentMonth).collect { result ->
                _uiState.value = when (result) {
                    is NovaResult.Success -> BudgetsUiState(isLoading = false, progress = result.data)
                    is NovaResult.Error -> BudgetsUiState(isLoading = false, errorMessage = result.failure.message)
                    is NovaResult.Loading -> _uiState.value.copy(isLoading = true)
                }
            }
        }
    }

    fun onAddBudgetClick() {
        _editingBudget.value = null
        _isFormSheetVisible.value = true
    }

    fun onEditClick(budget: Budget) {
        _editingBudget.value = budget
        _isFormSheetVisible.value = true
    }

    fun onDismissFormSheet() {
        _isFormSheetVisible.value = false
        _editingBudget.value = null
    }

    fun submitForm(category: TransactionCategory, monthlyLimit: Money) {
        viewModelScope.launch {
            budgetRepository.upsertBudget(
                Budget(
                    // Deterministic, not random: one budget per category per
                    // month by construction (also backed by a DB-level
                    // unique index — see BudgetEntity), so this upsert is
                    // simultaneously "add" for a new category and "edit"
                    // for an existing one — there's no separate update path
                    // to keep in sync with this id scheme.
                    id = "${category.name}_$currentMonth",
                    category = category,
                    monthlyLimit = monthlyLimit,
                    month = currentMonth
                )
            )
            onDismissFormSheet()
        }
    }

    fun onDeleteClick(budget: Budget) {
        _pendingDelete.value = budget
    }

    fun onDismissDeleteConfirmation() {
        _pendingDelete.value = null
    }

    fun confirmDelete() {
        val budget = _pendingDelete.value ?: return
        viewModelScope.launch {
            budgetRepository.deleteBudget(budget.id)
            _pendingDelete.value = null
        }
    }
}

@Immutable
data class BudgetsUiState(
    val isLoading: Boolean = true,
    val progress: List<BudgetProgress> = emptyList(),
    val errorMessage: String? = null
)
