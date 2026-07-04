package com.novafinance.feature.accounts

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.novafinance.core.domain.model.Debt
import com.novafinance.core.domain.model.FinancialSource
import com.novafinance.core.domain.model.Money
import com.novafinance.core.domain.repository.DebtRepository
import com.novafinance.core.domain.repository.FinancialSourceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class AccountsViewModel @Inject constructor(
    private val financialSourceRepository: FinancialSourceRepository,
    private val debtRepository: DebtRepository
) : ViewModel() {

    /**
     * Combines debts in specifically so [AccountsScreen] can display
     * [com.novafinance.core.domain.model.effectiveCreditCardUtilization]
     * (the reconciliation-aware figure — Phase 11.5.1) rather than a
     * linked card's own, possibly-stale
     * [FinancialSource.creditCardUtilization]. "Must produce a
     * consistent Balance Intelligence result" (11.5.1's own words)
     * means every screen that shows utilization needs to agree, not
     * just the calculations that happen to already combine debts for
     * other reasons.
     */
    val uiState: StateFlow<AccountsUiState> = combine(
        financialSourceRepository.observeSources(),
        debtRepository.observeDebts()
    ) { sources, debts ->
        val active = sources.filter { it.isActive }
        AccountsUiState(
            isLoading = false,
            sources = sources,
            debts = debts,
            totalBalance = Money.sum(active.map { it.currentBalance })
        )
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AccountsUiState()
        )

    private val _isFormSheetVisible = MutableStateFlow(false)
    val isFormSheetVisible: StateFlow<Boolean> = _isFormSheetVisible

    /** Non-null while the form sheet is open in edit mode; null means "add" mode. */
    private val _editingSource = MutableStateFlow<FinancialSource?>(null)
    val editingSource: StateFlow<FinancialSource?> = _editingSource

    /** Non-null while the delete-confirmation dialog is showing for this source. */
    private val _pendingDelete = MutableStateFlow<FinancialSource?>(null)
    val pendingDelete: StateFlow<FinancialSource?> = _pendingDelete

    fun onAddAccountClick() {
        _editingSource.value = null
        _isFormSheetVisible.value = true
    }

    fun onEditClick(source: FinancialSource) {
        _editingSource.value = source
        _isFormSheetVisible.value = true
    }

    fun onDismissFormSheet() {
        _isFormSheetVisible.value = false
        _editingSource.value = null
    }

    /** Handles both add and edit — [AccountFormSheet] doesn't need to know which one is happening. */
    fun submitForm(result: AccountFormResult) {
        if (result.name.isBlank()) return
        val editing = _editingSource.value

        viewModelScope.launch {
            if (editing == null) {
                financialSourceRepository.addSource(
                    FinancialSource(
                        id = UUID.randomUUID().toString(),
                        name = result.name.trim(),
                        type = result.type,
                        currentBalance = result.balance,
                        availableBalance = result.balance,
                        notes = result.notes,
                        creditLimit = result.creditLimit,
                        includeInLiquidity = result.includeInLiquidity,
                        includeInSpendingPower = result.includeInSpendingPower,
                        includeInForecast = result.includeInForecast,
                        includeInGoals = result.includeInGoals,
                        includeInAnalytics = result.includeInAnalytics,
                        isEmergencyReserve = result.isEmergencyReserve,
                        linkedDebtId = result.linkedDebtId,
                        createdAt = Instant.now()
                    )
                )
            } else {
                // Available balance moves by the same delta as current
                // balance for a manual edit — the two only diverge once a
                // real distinction (credit line, pending hold) is entered
                // deliberately, which this simple edit form doesn't expose.
                val delta = result.balance.minorUnits - editing.currentBalance.minorUnits
                financialSourceRepository.updateSource(
                    editing.copy(
                        name = result.name.trim(),
                        type = result.type,
                        currentBalance = result.balance,
                        availableBalance = Money(editing.availableBalance.minorUnits + delta),
                        notes = result.notes,
                        creditLimit = result.creditLimit,
                        includeInLiquidity = result.includeInLiquidity,
                        includeInSpendingPower = result.includeInSpendingPower,
                        includeInForecast = result.includeInForecast,
                        includeInGoals = result.includeInGoals,
                        includeInAnalytics = result.includeInAnalytics,
                        isEmergencyReserve = result.isEmergencyReserve,
                        linkedDebtId = result.linkedDebtId
                    )
                )
            }
            onDismissFormSheet()
        }
    }

    fun onDeleteClick(source: FinancialSource) {
        _pendingDelete.value = source
    }

    fun onDismissDeleteConfirmation() {
        _pendingDelete.value = null
    }

    fun confirmDelete() {
        val source = _pendingDelete.value ?: return
        viewModelScope.launch {
            financialSourceRepository.deleteSource(source.id)
            _pendingDelete.value = null
        }
    }
}

@Immutable
data class AccountsUiState(
    val isLoading: Boolean = true,
    val sources: List<FinancialSource> = emptyList(),
    val debts: List<Debt> = emptyList(),
    val totalBalance: Money = Money.ZERO
)
