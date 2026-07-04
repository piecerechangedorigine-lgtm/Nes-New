package com.novafinance.core.domain.usecase

import com.novafinance.core.domain.model.AnalyticsSummary
import com.novafinance.core.domain.model.AssistantContext
import com.novafinance.core.domain.model.BudgetProgress
import com.novafinance.core.domain.model.DashboardSummary
import com.novafinance.core.domain.model.DebtSummary
import com.novafinance.core.domain.model.GoalForecast
import com.novafinance.core.domain.model.NovaResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.YearMonth
import javax.inject.Inject

/**
 * Composes [GetDashboardSummaryUseCase], [GetAnalyticsSummaryUseCase],
 * [GetBudgetProgressUseCase], [GetGoalForecastUseCase], and (as of
 * Phase 10) [GetDebtSummaryUseCase] into the single [AssistantContext]
 * the Assistant reasons over. Deliberately built by combining the
 * existing use cases rather than re-querying repositories directly —
 * the engineering rule against duplicated logic applies here more than
 * anywhere else, since a second, slightly different spending
 * calculation would let the Assistant's numbers quietly drift from
 * what Dashboard, Analytics, and (now) the Debt Intelligence Center
 * already show for the same data.
 */
class GetAssistantContextUseCase @Inject constructor(
    private val getDashboardSummary: GetDashboardSummaryUseCase,
    private val getAnalyticsSummary: GetAnalyticsSummaryUseCase,
    private val getBudgetProgress: GetBudgetProgressUseCase,
    private val getGoalForecast: GetGoalForecastUseCase,
    private val getDebtSummary: GetDebtSummaryUseCase,
    dispatcher: CoroutineDispatcher
) : NovaFlowUseCase<Unit, AssistantContext>(dispatcher) {

    override fun execute(params: Unit): Flow<NovaResult<AssistantContext>> =
        combine(
            getDashboardSummary(Unit),
            getAnalyticsSummary(Unit),
            getBudgetProgress(YearMonth.now()),
            getGoalForecast(Unit),
            getDebtSummary(Unit)
        ) { dashboard, analytics, budgets, goals, debt ->
            merge(dashboard, analytics, budgets, goals, debt)
        }

    private fun merge(
        dashboard: NovaResult<DashboardSummary>,
        analytics: NovaResult<AnalyticsSummary>,
        budgets: NovaResult<List<BudgetProgress>>,
        goals: NovaResult<List<GoalForecast>>,
        debt: NovaResult<DebtSummary>
    ): NovaResult<AssistantContext> {
        val all: List<NovaResult<*>> = listOf(dashboard, analytics, budgets, goals, debt)
        all.filterIsInstance<NovaResult.Error>().firstOrNull()?.let { return it }
        if (all.any { it is NovaResult.Loading }) return NovaResult.Loading

        return NovaResult.Success(
            AssistantContext(
                dashboard = (dashboard as NovaResult.Success).data,
                analytics = (analytics as NovaResult.Success).data,
                budgetProgress = (budgets as NovaResult.Success).data,
                goalForecasts = (goals as NovaResult.Success).data,
                debtSummary = (debt as NovaResult.Success).data
            )
        )
    }
}
