package com.novafinance.core.domain.usecase

import com.novafinance.core.domain.model.BudgetProgress
import com.novafinance.core.domain.model.Money
import com.novafinance.core.domain.model.NovaResult
import com.novafinance.core.domain.repository.BudgetRepository
import com.novafinance.core.domain.repository.TransactionRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.YearMonth
import javax.inject.Inject

/**
 * Joins each budget with actual spend for its category+month, computed
 * from the live transaction ledger rather than a cached "spent" column —
 * a budget can never disagree with the transactions that back it.
 */
class GetBudgetProgressUseCase @Inject constructor(
    private val budgetRepository: BudgetRepository,
    private val transactionRepository: TransactionRepository,
    dispatcher: CoroutineDispatcher
) : NovaFlowUseCase<YearMonth, List<BudgetProgress>>(dispatcher) {

    override fun execute(params: YearMonth): Flow<NovaResult<List<BudgetProgress>>> {
        return combine(
            budgetRepository.observeBudgets(params),
            transactionRepository.observeTransactionsForMonth(params)
        ) { budgets, transactions ->
            val progress = budgets.map { budget ->
                val spent = Money.sum(
                    transactions
                        .filter { it.category == budget.category && it.amount.isNegative }
                        .map { -it.amount }
                )
                BudgetProgress(budget = budget, spent = spent)
            }
            NovaResult.Success(progress)
        }
    }
}
