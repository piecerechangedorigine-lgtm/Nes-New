package com.novafinance.core.domain.model

/**
 * Fixed category set for v1. Each category carries its own accent color
 * (a hex string, not a Compose Color — this module has no UI dependency)
 * so category "icons" throughout the app are just colored swatches rather
 * than a second bespoke icon set to maintain; see NovaCategorySwatch in
 * core:designsystem for where this color actually gets drawn.
 */
enum class TransactionCategory(val displayName: String, val colorHex: String, val isIncomeCategory: Boolean) {
    INCOME("Income", "#3DD68C", isIncomeCategory = true),
    FOOD("Food & Drink", "#F5A623", isIncomeCategory = false),
    TRANSPORT("Transport", "#5B6CFF", isIncomeCategory = false),
    SHOPPING("Shopping", "#F6B8C4", isIncomeCategory = false),
    BILLS("Bills & Utilities", "#8C8CFF", isIncomeCategory = false),
    ENTERTAINMENT("Entertainment", "#9FE8D6", isIncomeCategory = false),
    HEALTH("Health", "#FF5C5C", isIncomeCategory = false),
    TRANSFER("Transfer", "#8C8C96", isIncomeCategory = false),
    OTHER("Other", "#5E5E68", isIncomeCategory = false)
}
