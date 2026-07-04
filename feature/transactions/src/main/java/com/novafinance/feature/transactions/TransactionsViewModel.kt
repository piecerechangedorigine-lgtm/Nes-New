package com.novafinance.feature.transactions

import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.novafinance.core.domain.model.FinancialSource
import com.novafinance.core.domain.model.Money
import com.novafinance.core.domain.model.Transaction
import com.novafinance.core.domain.model.TransactionCategory
import com.novafinance.core.domain.repository.FinancialSourceRepository
import com.novafinance.core.domain.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class TransactionsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val financialSourceRepository: FinancialSourceRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val accountIdFilter: String? = savedStateHandle["accountId"]

    private val _filters = MutableStateFlow(TransactionFilters())
    val filters: StateFlow<TransactionFilters> = _filters

    private val _isFormSheetVisible = MutableStateFlow(false)
    val isFormSheetVisible: StateFlow<Boolean> = _isFormSheetVisible

    /** Non-null while the form sheet is open in edit mode; null means "add" mode. */
    private val _editingTransaction = MutableStateFlow<Transaction?>(null)
    val editingTransaction: StateFlow<Transaction?> = _editingTransaction

    /** Non-null while the delete-confirmation dialog is showing for this transaction. */
    private val _pendingDelete = MutableStateFlow<Transaction?>(null)
    val pendingDelete: StateFlow<Transaction?> = _pendingDelete

    val uiState: StateFlow<TransactionsUiState> = combine(
        transactionRepository.observeTransactions(accountIdFilter),
        _filters,
        financialSourceRepository.observeSources()
    ) { transactions, filters, sources ->
        val filtered = transactions
            .filter { filters.category == null || it.category == filters.category }
            .filter { filters.query.isBlank() || it.merchant.contains(filters.query, ignoreCase = true) }
        TransactionsUiState(
            isLoading = false,
            isAccountScoped = accountIdFilter != null,
            transactions = filtered,
            filters = filters,
            sources = sources
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TransactionsUiState(isAccountScoped = accountIdFilter != null)
    )

    fun onQueryChange(query: String) {
        _filters.update { it.copy(query = query) }
    }

    fun onCategorySelect(category: TransactionCategory?) {
        _filters.update { it.copy(category = category) }
    }

    fun onAddTransactionClick() {
        _editingTransaction.value = null
        _isFormSheetVisible.value = true
    }

    fun onEditClick(transaction: Transaction) {
        _editingTransaction.value = transaction
        _isFormSheetVisible.value = true
    }

    fun onDismissFormSheet() {
        _isFormSheetVisible.value = false
        _editingTransaction.value = null
    }

    /**
     * Handles both add and edit. Editing preserves the transaction's
     * original [Transaction.id] and [Transaction.date] — only merchant,
     * category, amount, and source can change from the form — and always
     * writes a real [TransactionCategory] value, never null, so a stale
     * or partially-edited row can never end up in an uncategorized state.
     * Analytics, Budgets, the Dashboard, and the Assistant all read
     * through the same reactive Room `Flow` this write updates, so none
     * of them need a separate refresh call — they recompute automatically
     * the moment this transaction changes.
     */
    fun submitForm(sourceId: String, merchant: String, category: TransactionCategory, amount: Money) {
        if (merchant.isBlank()) return
        val editing = _editingTransaction.value

        viewModelScope.launch {
            if (editing == null) {
                transactionRepository.addTransaction(
                    Transaction(
                        id = UUID.randomUUID().toString(),
                        accountId = sourceId,
                        merchant = merchant.trim(),
                        category = category,
                        amount = amount,
                        date = LocalDate.now()
                    )
                )
            } else {
                transactionRepository.updateTransaction(
                    editing.copy(
                        accountId = sourceId,
                        merchant = merchant.trim(),
                        category = category,
                        amount = amount
                    )
                )
            }
            onDismissFormSheet()
        }
    }

    fun onDeleteClick(transaction: Transaction) {
        _pendingDelete.value = transaction
    }

    fun onDismissDeleteConfirmation() {
        _pendingDelete.value = null
    }

    fun confirmDelete() {
        val transaction = _pendingDelete.value ?: return
        viewModelScope.launch {
            transactionRepository.deleteTransaction(transaction.id)
            _pendingDelete.value = null
        }
    }
}

data class TransactionFilters(
    val query: String = "",
    val category: TransactionCategory? = null
)

@Immutable
data class TransactionsUiState(
    val isLoading: Boolean = true,
    val isAccountScoped: Boolean = false,
    val transactions: List<Transaction> = emptyList(),
    val filters: TransactionFilters = TransactionFilters(),
    val sources: List<FinancialSource> = emptyList()
)
