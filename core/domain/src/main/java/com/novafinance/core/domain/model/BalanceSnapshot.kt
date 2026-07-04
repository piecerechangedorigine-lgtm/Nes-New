package com.novafinance.core.domain.model

import java.time.Instant

/**
 * One point-in-time balance record for one source. The 11.8 brief
 * frames this explicitly as forward-looking infrastructure ("Used
 * later for: Trends, Financial Twin, AI Insights") — this phase builds
 * the storage and the ability to record a snapshot on demand; it does
 * **not** build automatic daily/weekly/monthly capture (that needs a
 * scheduler — see `FINANCIAL_SOURCES_ARCHITECTURE.md` and
 * `ROADMAP_NEXT.md` for why that's an explicit, honest deferral rather
 * than something quietly half-built).
 */
data class BalanceSnapshot(
    val id: String,
    val sourceId: String,
    val balance: Money,
    val recordedAt: Instant
)
