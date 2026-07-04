package com.novafinance.feature.debt

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.novafinance.core.domain.model.Debt
import com.novafinance.core.domain.model.DebtDirection
import com.novafinance.core.domain.model.DebtScenarioAdjustment
import com.novafinance.core.domain.model.DebtSimulationResult
import com.novafinance.core.domain.model.Money
import com.novafinance.core.domain.model.simulateDebtScenario
import com.novafinance.core.domain.repository.DebtRepository
import com.novafinance.core.domain.repository.GoalRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * Six reactive inputs feed the result (debts, goals, selected debt,
 * scenario type, amount, delay) — one more than `combine`'s typed
 * overloads go up to (5). Nested pairs of typed `combine` calls, the
 * same workaround `GetDreamDashboardDataUseCase` already uses for its
 * own six-source combine, keeps every value's real type intact end to
 * end rather than reaching for the untyped `vararg Flow<*>` overload
 * and unchecked-casting values back out of an `Array<*>`.
 */
@HiltViewModel
class DebtSimulatorViewModel @Inject constructor(
    debtRepository: DebtRepository,
    goalRepository: GoalRepository
) : ViewModel() {

    private val _selectedDebtId = MutableStateFlow<String?>(null)
    private val _scenarioType = MutableStateFlow(ScenarioType.INCREASE_PAYMENT)
    private val _amountText = MutableStateFlow("")
    private val _delayMonthsText = MutableStateFlow("")

    val uiState: StateFlow<DebtSimulatorUiState> = combine(
        combine(debtRepository.observeDebts(), goalRepository.observeGoals(), ::Pair),
        combine(_selectedDebtId, _scenarioType, ::Pair),
        combine(_amountText, _delayMonthsText, ::Pair)
    ) { dataInputs, selectionInputs, textInputs ->
        val (debts, goals) = dataInputs
        val (selectedDebtId, scenarioType) = selectionInputs
        val (amountText, delayMonthsText) = textInputs

        val owedDebts = debts.filter { it.direction == DebtDirection.I_OWE && it.isActive && !it.isPaidOff }
        val effectiveSelectedId = selectedDebtId ?: owedDebts.firstOrNull()?.id

        val result = effectiveSelectedId?.let { debtId ->
            buildAdjustment(debtId, scenarioType, amountText, delayMonthsText)?.let { adjustment ->
                simulateDebtScenario(debts, listOf(adjustment), goals)
            }
        }

        DebtSimulatorUiState(
            isLoading = false,
            owedDebts = owedDebts,
            selectedDebtId = effectiveSelectedId,
            scenarioType = scenarioType,
            amountText = amountText,
            delayMonthsText = delayMonthsText,
            result = result
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DebtSimulatorUiState())

    private fun buildAdjustment(debtId: String, type: ScenarioType, amountText: String, delayMonthsText: String): DebtScenarioAdjustment? = when (type) {
        ScenarioType.INCREASE_PAYMENT -> amountText.toDoubleOrNull()?.let {
            DebtScenarioAdjustment.IncreasePayment(debtId, Money.fromMajor(it))
        }
        ScenarioType.DELAY_PAYMENT -> delayMonthsText.toIntOrNull()?.let {
            DebtScenarioAdjustment.DelayPayment(debtId, it)
        }
    }

    fun onSelectDebt(debtId: String) {
        _selectedDebtId.value = debtId
    }

    fun onSelectScenarioType(type: ScenarioType) {
        _scenarioType.value = type
    }

    fun onAmountChange(text: String) {
        _amountText.value = text
    }

    fun onDelayMonthsChange(text: String) {
        _delayMonthsText.value = text
    }
}

enum class ScenarioType { INCREASE_PAYMENT, DELAY_PAYMENT }

@Immutable
data class DebtSimulatorUiState(
    val isLoading: Boolean = true,
    val owedDebts: List<Debt> = emptyList(),
    val selectedDebtId: String? = null,
    val scenarioType: ScenarioType = ScenarioType.INCREASE_PAYMENT,
    val amountText: String = "",
    val delayMonthsText: String = "",
    val result: DebtSimulationResult? = null
)
