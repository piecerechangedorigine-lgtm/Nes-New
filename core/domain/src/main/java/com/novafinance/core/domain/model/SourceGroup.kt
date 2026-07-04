package com.novafinance.core.domain.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * A purely organizational label — grouping is optional (11.2's own
 * "Groups are optional"), and nothing in [BalanceOverview],
 * [BalanceHealthScore], or any other Phase 11 calculation reads
 * [FinancialSource.groupId] at all. Groups exist for the person's own
 * mental model of their sources ("Daily Spending" vs "Savings" vs
 * "Travel"), not as an input to any scoring engine — see
 * `FINANCIAL_SOURCES_ARCHITECTURE.md` for why that separation is
 * deliberate.
 */
@Serializable
data class SourceGroup(
    val id: String,
    val name: String
)

/**
 * The five suggested starting points from the 11.2 brief. Genuinely
 * just suggestions — [com.novafinance.core.domain.repository.SourceGroupRepository]
 * stores whatever groups the person actually has, custom or not;
 * nothing distinguishes a "built-in" group from a custom one once
 * created, since there's no meaningful difference in how either is used.
 */
object SuggestedSourceGroups {
    val defaults: List<String> = listOf("Daily Spending", "Savings", "Investments", "Business", "Travel")
}

private val sourceGroupsJson = Json { ignoreUnknownKeys = true }

/**
 * JSON (de)serialization for the group list lives here, in
 * `core:domain`, not in `core:data`'s `SourceGroupRepositoryImpl` —
 * the same split `DashboardLayout.toJson()`/`parseDashboardLayoutOrNull()`
 * already established in Phase 9, so `core:data` never needs
 * `kotlinx.serialization` as a direct dependency of its own.
 */
fun List<SourceGroup>.toJson(): String = sourceGroupsJson.encodeToString(this)

fun parseSourceGroupsOrEmpty(json: String): List<SourceGroup> =
    runCatching { sourceGroupsJson.decodeFromString<List<SourceGroup>>(json) }.getOrDefault(emptyList())
