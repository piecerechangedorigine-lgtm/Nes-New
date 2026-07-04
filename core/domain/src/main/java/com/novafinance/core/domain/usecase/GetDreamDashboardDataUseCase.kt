package com.novafinance.core.domain.usecase

import com.novafinance.core.domain.assistant.AssistantInsightEngine
import com.novafinance.core.domain.model.DreamDashboardData
import com.novafinance.core.domain.model.NovaResult
import com.novafinance.core.domain.model.calculateSourceHealth
import com.novafinance.core.domain.repository.FinancialSourceRepository
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * The one place Dream Dashboard's widgets all read from. Composed
 * entirely from use cases that already existed before Phase 9
 * (`GetDashboardSummaryUseCase`, `GetForecastSummaryUseCase`,
 * `GetBalanceOverviewUseCase`, `GetGoalForecastUseCase`,
 * `GetAssistantContextUseCase`) plus `GetDebtSummaryUseCase` (Phase 10)
 * and, as of Phase 11, [FinancialSourceRepository] directly (for
 * per-source names and health — see below) — every phase's job here
 * has been building the widget system around existing data, never
 * inventing a new calculation just for a widget to show.
 *
 * The "top insight" and "top debt recommendation" both reuse
 * [AssistantInsightEngine] exactly as the 9.7/10.10 briefs ask ("Use
 * current assistant implementation. Do not integrate LLMs.") — the
 * insight by asking it the same question a person might, the debt
 * recommendation via [AssistantInsightEngine.topDebtRecommendation],
 * which is the same engine's own dedicated single-message form for a
 * widget with room for one line rather than a full chat reply.
 *
 * [com.novafinance.core.domain.model.calculateSourceHealth] is called
 * directly here rather than injecting the whole
 * [GetFinancialSourceIntelligenceUseCase] — that use case already
 * combines six sources of its own; stacking it as a seventh dependency
 * here would mean two full intelligence pipelines running redundantly
 * for the one field (per-source health) this screen actually needs.
 * Reaching for the pure calculation function directly, the same way
 * every widget composable already calls `.forecast()`/`.health()`
 * extension functions rather than injecting use cases into a
 * `@Composable`, keeps this to exactly the data it needs.
 *
 * Seven sources need combining, two more than `combine`'s typed
 * overloads go up to (5) — nested `combine` calls (four groups) are
 * the same workaround the six-source version of this use case already
 * used, extended by one more group.
 */
class GetDreamDashboardDataUseCase @Inject constructor(
    private val getDashboardSummary: GetDashboardSummaryUseCase,
    private val getForecastSummary: GetForecastSummaryUseCase,
    private val getBalanceOverview: GetBalanceOverviewUseCase,
    private val getGoalForecast: GetGoalForecastUseCase,
    private val getAssistantContext: GetAssistantContextUseCase,
    private val getDebtSummary: GetDebtSummaryUseCase,
    private val financialSourceRepository: FinancialSourceRepository,
    private val assistantInsightEngine: AssistantInsightEngine,
    dispatcher: CoroutineDispatcher
) : NovaFlowUseCase<Unit, DreamDashboardData>(dispatcher) {

    override fun execute(params: Unit): Flow<NovaResult<DreamDashboardData>> =
        combine(
            combine(getDashboardSummary(Unit), getForecastSummary(Unit), getBalanceOverview(Unit), ::Triple),
            combine(getGoalForecast(Unit), getAssistantContext(Unit), ::Pair),
            getDebtSummary(Unit),
            financialSourceRepository.observeSources()
        ) { financeResults, otherResults, debtResult, sources ->
            val (summaryResult, forecastResult, balanceResult) = financeResults
            val (goalsResult, assistantContextResult) = otherResults

            val results = listOf(summaryResult, forecastResult, balanceResult, goalsResult, assistantContextResult, debtResult)
            val firstError = results.filterIsInstance<NovaResult.Error>().firstOrNull()

            when {
                firstError != null -> firstError
                results.any { it is NovaResult.Loading } -> NovaResult.Loading
                else -> {
                    val summary = (summaryResult as NovaResult.Success).data
                    val forecast = (forecastResult as NovaResult.Success).data
                    val balance = (balanceResult as NovaResult.Success).data
                    val goals = (goalsResult as NovaResult.Success).data
                    val assistantContext = (assistantContextResult as NovaResult.Success).data
                    val debt = (debtResult as NovaResult.Success).data

                    val topInsight = assistantInsightEngine.respond("How's my spending this month?", assistantContext)
                    val topDebtRecommendation = assistantInsightEngine.topDebtRecommendation(assistantContext)

                    val activeSources = sources.filter { it.isActive }
                    val sourceHealths = activeSources.map { calculateSourceHealth(it, summary.monthExpense, debt.debts) }

                    NovaResult.Success(
                        DreamDashboardData(
                            summary = summary,
                            forecast = forecast,
                            balanceOverview = balance,
                            goalForecasts = goals,
                            topInsightMessage = topInsight.text,
                            topInsightAction = topInsight.actions.firstOrNull(),
                            debtSummary = debt,
                            topDebtRecommendation = topDebtRecommendation?.text,
                            sources = activeSources,
                            sourceHealths = sourceHealths
                        )
                    )
                }
            }
        }
}
