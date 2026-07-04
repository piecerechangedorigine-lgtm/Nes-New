package com.novafinance.core.domain.model

import java.time.LocalDate

enum class DebtWeatherState { SUNNY, PARTLY_CLOUDY, CLOUDY, RAINY, STORM }

/**
 * A trend proxy, not a real historical comparison — Nova doesn't
 * persist historical [DebtHealthScore] snapshots (no table exists to
 * store "what was the score last month"), so this can't measure actual
 * change over time yet. Instead it reads a signal available right now:
 * any overdue [Debt.dueDate] on an active debt reads as [WORSENING]
 * (a concrete, checkable fact — a missed date), any active debt with
 * real repayment progress and no overdue dates reads as [IMPROVING],
 * everything else is [STABLE]. Documented as a known simplification in
 * `DEBT_ARCHITECTURE.md` and `TECHNICAL_DEBT.md`, not silently passed
 * off as a real trend line.
 */
enum class DebtTrend { IMPROVING, STABLE, WORSENING }

data class DebtWeather(val state: DebtWeatherState, val trend: DebtTrend)

fun weatherFor(health: DebtHealthScore): DebtWeatherState = when {
    health.score >= 80 -> DebtWeatherState.SUNNY
    health.score >= 60 -> DebtWeatherState.PARTLY_CLOUDY
    health.score >= 40 -> DebtWeatherState.CLOUDY
    health.score >= 20 -> DebtWeatherState.RAINY
    else -> DebtWeatherState.STORM
}

fun trendFor(debts: List<Debt>, today: LocalDate = LocalDate.now()): DebtTrend {
    val owed = debts.filter { it.direction == DebtDirection.I_OWE && it.isActive && !it.isPaidOff }
    if (owed.isEmpty()) return DebtTrend.STABLE

    val hasOverdue = owed.any { it.dueDate != null && it.dueDate.isBefore(today) }
    if (hasOverdue) return DebtTrend.WORSENING

    val hasRealProgress = owed.any { it.percentPaidDown > 0.05f }
    return if (hasRealProgress) DebtTrend.IMPROVING else DebtTrend.STABLE
}

fun calculateDebtWeather(debts: List<Debt>, health: DebtHealthScore, today: LocalDate = LocalDate.now()): DebtWeather =
    DebtWeather(state = weatherFor(health), trend = trendFor(debts, today))
