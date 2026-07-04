package com.novafinance.core.navigation

/**
 * Every feature's route lives here rather than as a string literal inside
 * that feature's own NavGraphProvider. Cross-feature navigation (Dashboard's
 * "Add Transaction" quick action opening feature:transactions, for example)
 * needs a shared constant neither module has to own — putting it on the
 * other would create exactly the module coupling NovaNavGraphProvider
 * exists to avoid.
 */
object NovaRoutes {
    const val DASHBOARD = "dashboard"
    const val ACCOUNTS = "accounts"
    const val ANALYTICS = "analytics"
    const val PROFILE = "profile"

    const val TRANSACTIONS = "transactions"
    const val TRANSACTIONS_ARG_ACCOUNT_ID = "accountId"
    const val TRANSACTIONS_ROUTE_PATTERN = "transactions?accountId={accountId}"
    fun transactions(accountId: String? = null): String =
        if (accountId != null) "transactions?accountId=$accountId" else TRANSACTIONS

    const val BUDGETS = "budgets"
    const val GOALS = "goals"
    const val ASSISTANT = "assistant"
    const val PERMISSIONS = "permissions"
    const val DASHBOARD_STUDIO = "dashboard_studio"
    const val DEBT = "debt"
    const val DEBT_SIMULATOR = "debt_simulator"
}
