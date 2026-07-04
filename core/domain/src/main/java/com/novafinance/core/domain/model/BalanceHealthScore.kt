package com.novafinance.core.domain.model

data class BalanceHealthScore(
    val score: Int,
    val label: BalanceHealthLabel
)

enum class BalanceHealthLabel { HEALTHY, STABLE, WARNING, CRITICAL }
