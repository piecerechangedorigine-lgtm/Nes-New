package com.novafinance.core.domain.model

/**
 * A single monthly projection: "at current pace, here's roughly where
 * you'll end the month." Deliberately a linear extrapolation from
 * spend-so-far, not a trend fit against history — the same
 * "deliberately simple heuristics, not a model" approach
 * GetDashboardSummaryUseCase's own insight banner and
 * AssistantInsightEngine already use elsewhere in this codebase.
 */
data class ForecastSummary(
    /** Projected total balance at month end if the current daily spending pace holds for the rest of the month. */
    val projectedEndOfMonthBalance: Money,
    /** Positive means a projected surplus, negative a projected deficit. Same value either way — [status] is what turns this into "save" vs "deficit" language. */
    val projectedSurplusOrDeficit: Money,
    val status: ForecastStatus,
    val confidence: ForecastConfidence,
    /** Ready-to-render sentence, e.g. "At current spending pace you will save 10,000 DZD." Built once here so every surface (Dashboard, Assistant, a future widget) renders identical wording for the same numbers. */
    val message: String
)

enum class ForecastStatus { SURPLUS, ON_TRACK, DEFICIT }

/**
 * How much to trust [ForecastSummary.projectedEndOfMonthBalance]. Early
 * in the month, a handful of transactions extrapolated across 30 days
 * is a much shakier projection than the same pace held for three weeks
 * — [confidence] exists so a surface can choose to hedge the message
 * ("early days, but...") rather than stating a shaky number as fact.
 */
enum class ForecastConfidence { LOW, MEDIUM, HIGH }
