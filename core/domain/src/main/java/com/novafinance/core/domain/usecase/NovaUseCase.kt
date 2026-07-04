package com.novafinance.core.domain.usecase

import com.novafinance.core.domain.model.NovaFailure
import com.novafinance.core.domain.model.NovaResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

/**
 * Base class for a single-shot domain operation (e.g. "submit a transaction").
 * Every concrete use case lives in its feature's domain package and contains
 * exactly one piece of business logic — no orchestration, no UI concerns.
 */
abstract class NovaUseCase<in Params, out Result>(
    private val dispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(params: Params): NovaResult<Result> = withContext(dispatcher) {
        try {
            NovaResult.Success(execute(params))
        } catch (t: Throwable) {
            NovaResult.Error(NovaFailure.Unknown(t))
        }
    }

    protected abstract suspend fun execute(params: Params): Result
}

/**
 * Base class for an observable domain operation (e.g. "observe account balance").
 * Wraps the underlying Flow with dispatcher confinement and failure mapping
 * so feature ViewModels only ever collect NovaResult, never raw exceptions.
 */
abstract class NovaFlowUseCase<in Params, out Result>(
    private val dispatcher: CoroutineDispatcher
) {
    operator fun invoke(params: Params): Flow<NovaResult<Result>> = execute(params)
        .catch { t -> emit(error = t) }
        .flowOn(dispatcher)

    protected abstract fun execute(params: Params): Flow<NovaResult<Result>>

    private suspend fun kotlinx.coroutines.flow.FlowCollector<NovaResult<Result>>.emit(error: Throwable) {
        emit(NovaResult.Error(NovaFailure.Unknown(error)))
    }
}
