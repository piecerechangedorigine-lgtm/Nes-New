package com.novafinance.core.domain.usecase

import com.novafinance.core.domain.model.GoalForecast
import com.novafinance.core.domain.model.NovaResult
import com.novafinance.core.domain.model.forecast
import com.novafinance.core.domain.repository.GoalRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/** Maps live goals to their linear-projection forecast — see [forecast] doc for what this does and doesn't model. */
class GetGoalForecastUseCase @Inject constructor(
    private val goalRepository: GoalRepository,
    dispatcher: CoroutineDispatcher
) : NovaFlowUseCase<Unit, List<GoalForecast>>(dispatcher) {

    override fun execute(params: Unit): Flow<NovaResult<List<GoalForecast>>> =
        goalRepository.observeGoals().map { goals ->
            NovaResult.Success(goals.map { it.forecast() })
        }
}
