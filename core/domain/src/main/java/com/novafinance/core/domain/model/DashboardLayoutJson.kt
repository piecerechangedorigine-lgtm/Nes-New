package com.novafinance.core.domain.model

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val dashboardLayoutJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

fun DashboardLayout.toJson(): String = dashboardLayoutJson.encodeToString(this)

/**
 * Returns `null` rather than throwing on anything unparseable — unlike
 * a Backup file (which the person explicitly chose to import and
 * should be told clearly if it's invalid), a corrupted or
 * schema-mismatched persisted layout should just fall back to a fresh
 * default layout silently. Losing a custom widget arrangement to a
 * parse error is much less costly than losing financial data, and
 * doesn't deserve the same "explain what went wrong" treatment
 * [parseBackupPayload] gives.
 */
fun parseDashboardLayoutOrNull(json: String): DashboardLayout? =
    runCatching { dashboardLayoutJson.decodeFromString<DashboardLayout>(json) }.getOrNull()
