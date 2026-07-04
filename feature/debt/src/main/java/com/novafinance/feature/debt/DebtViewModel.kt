package com.novafinance.feature.debt

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.novafinance.core.domain.model.Debt
import com.novafinance.core.domain.model.DebtDirection
import com.novafinance.core.domain.model.DebtSummary
import com.novafinance.core.domain.model.DebtType
import com.novafinance.core.domain.model.Money
import com.novafinance.core.domain.model.NovaResult
import com.novafinance.core.domain.repository.DebtRepository
import com.novafinance.core.domain.usecase.GetDebtSummaryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class DebtViewModel @Inject constructor(
    private val getDebtSummary: GetDebtSummaryUseCase,
    private val debtRepository: DebtRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DebtUiState())
    val uiState: StateFlow<DebtUiState> = _uiState.asStateFlow()

    private val _isFormSheetVisible = MutableStateFlow(false)
    val isFormSheetVisible: StateFlow<Boolean> = _isFormSheetVisible

    /** Non-null while the form sheet is open in edit mode; null means "add" mode. */
    private val _editingDebt = MutableStateFlow<Debt?>(null)
    val editingDebt: StateFlow<Debt?> = _editingDebt

    /** Non-null while the delete-confirmation dialog is showing for this debt. */
    private val _pendingDelete = MutableStateFlow<Debt?>(null)
    val pendingDelete: StateFlow<Debt?> = _pendingDelete

    /** Which direction "Add" was tapped for — read by the route to pick the right labels in [DebtFormSheet] while [editingDebt] is null (add mode). */
    private val _pendingDirection = MutableStateFlow(DebtDirection.I_OWE)
    val pendingDirection: StateFlow<DebtDirection> = _pendingDirection

    init {
        viewModelScope.launch {
            getDebtSummary(Unit).collect { result ->
                _uiState.value = when (result) {
                    is NovaResult.Success -> DebtUiState(isLoading = false, summary = result.data)
                    is NovaResult.Error -> DebtUiState(isLoading = false, errorMessage = result.failure.message)
                    is NovaResult.Loading -> _uiState.value.copy(isLoading = true)
                }
            }
        }
    }

    fun onAddDebtClick(direction: DebtDirection) {
        _editingDebt.value = null
        _pendingDirection.value = direction
        _isFormSheetVisible.value = true
    }

    fun onEditClick(debt: Debt) {
        _editingDebt.value = debt
        _isFormSheetVisible.value = true
    }

    fun onDismissFormSheet() {
        _isFormSheetVisible.value = false
        _editingDebt.value = null
    }

    fun submitForm(
        name: String,
        type: DebtType,
        originalAmount: Money,
        currentBalance: Money,
        interestRatePercent: Double?,
        minimumMonthlyPayment: Money?,
        dueDate: LocalDate?,
        counterpartyName: String?
    ) {
        if (name.isBlank()) return
        val editing = _editingDebt.value

        viewModelScope.launch {
            if (editing == null) {
                debtRepository.addDebt(
                    Debt(
                        id = UUID.randomUUID().toString(),
                        name = name.trim(),
                        direction = _pendingDirection.value,
                        type = type,
                        originalAmount = originalAmount,
                        currentBalance = currentBalance,
                        interestRatePercent = interestRatePercent,
                        minimumMonthlyPayment = minimumMonthlyPayment,
                        dueDate = dueDate,
                        counterpartyName = counterpartyName?.ifBlank { null },
                        createdAt = LocalDate.now()
                    )
                )
            } else {
                debtRepository.updateDebt(
                    editing.copy(
                        name = name.trim(),
                        type = type,
                        originalAmount = originalAmount,
                        currentBalance = currentBalance,
                        interestRatePercent = interestRatePercent,
                        minimumMonthlyPayment = minimumMonthlyPayment,
                        dueDate = dueDate,
                        counterpartyName = counterpartyName?.ifBlank { null }
                    )
                )
            }
            onDismissFormSheet()
        }
    }

    fun onDeleteClick(debt: Debt) {
        _pendingDelete.value = debt
    }

    fun onDismissDeleteConfirmation() {
        _pendingDelete.value = null
    }

    fun confirmDelete() {
        val debt = _pendingDelete.value ?: return
        viewModelScope.launch {
            debtRepository.deleteDebt(debt.id)
            _pendingDelete.value = null
        }
    }

    /** Marks a debt fully settled without deleting its history — sets balance to zero and deactivates it. */
    fun markSettled(debt: Debt) {
        viewModelScope.launch {
            debtRepository.updateDebt(debt.copy(currentBalance = Money.ZERO, isActive = false))
        }
    }
}

@Immutable
data class DebtUiState(
    val isLoading: Boolean = true,
    val summary: DebtSummary? = null,
    val errorMessage: String? = null
)
