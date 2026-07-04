package com.novafinance.core.domain.model

/** Who authored a given [AssistantMessage] — mirrors a typical chat transcript shape. */
enum class AssistantSender { USER, ASSISTANT }

/**
 * One turn in the Assistant conversation. Assistant-authored messages may
 * carry zero or more [AssistantAction]s — concrete next steps the reply
 * itself surfaced (e.g. a spending warning offers "Open Budgets") rather
 * than making the person re-describe what they just asked about.
 */
data class AssistantMessage(
    val id: String,
    val sender: AssistantSender,
    val text: String,
    val actions: List<AssistantAction> = emptyList()
)

/**
 * Closed set of destinations an assistant reply can point to. Deliberately
 * an enum rather than a raw route string — core:domain has no dependency
 * on core:navigation, so the feature layer owns mapping each type to an
 * actual [com.novafinance.core.domain.model] navigation target.
 */
enum class AssistantActionType { OPEN_BUDGETS, OPEN_GOALS, OPEN_ANALYTICS, ADD_TRANSACTION, OPEN_DEBT }

data class AssistantAction(val label: String, val type: AssistantActionType)

/** A tappable starter question shown before the person has typed anything, or alongside a reply. */
data class AssistantSuggestedPrompt(val id: String, val label: String, val query: String)
