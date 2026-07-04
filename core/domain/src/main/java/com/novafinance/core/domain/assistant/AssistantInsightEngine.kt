package com.novafinance.core.domain.assistant

import com.novafinance.core.domain.model.AssistantAction
import com.novafinance.core.domain.model.AssistantActionType
import com.novafinance.core.domain.model.AssistantContext
import com.novafinance.core.domain.model.AssistantMessage
import com.novafinance.core.domain.model.AssistantSender
import com.novafinance.core.domain.model.AssistantSuggestedPrompt
import com.novafinance.core.domain.model.DebtPressureLabel
import com.novafinance.core.domain.model.formatted
import java.util.UUID
import javax.inject.Inject
import kotlin.math.roundToInt

/**
 * Answers Assistant questions with explainable, checkable logic over the
 * user's real [AssistantContext] — every branch below is a fact directly
 * traceable to a number Dashboard, Analytics or Budgets already shows,
 * the same "deliberately simple heuristics, not a model" approach
 * GetDashboardSummaryUseCase uses for its own insight banner.
 *
 * This intentionally does not call an external LLM. Nova has no backend
 * of its own yet, and a real model integration must never ship with a
 * provider API key embedded in the client — it belongs behind a server-side
 * proxy, which is out of scope for this module. [AssistantInsightEngine] is
 * the swappable seam: a future network-backed implementation can replace
 * it behind the same `respond(query, context)` contract without touching
 * [AssistantViewModel] or the screen.
 */
class AssistantInsightEngine @Inject constructor() {

    fun greeting(context: AssistantContext): AssistantMessage {
        val text = if (!context.hasActivity) {
            "Hi, I'm your Nova assistant. Add a few transactions and I'll be able to " +
                "talk through your spending, budgets and goals with real numbers."
        } else {
            "Hi, I'm your Nova assistant. Ask me about your spending, budgets, or " +
                "savings goals — I'll answer from your actual account data."
        }
        return assistantMessage(text)
    }

    fun suggestedPrompts(context: AssistantContext): List<AssistantSuggestedPrompt> {
        if (!context.hasActivity) {
            return listOf(prompt("How does this work?", "How does this work?"))
        }

        val prompts = mutableListOf(
            prompt("How's my spending this month?", "How's my spending this month?")
        )

        if (context.budgetProgress.isNotEmpty()) {
            prompts += prompt("Am I over budget anywhere?", "Am I over budget anywhere?")
        }
        if (context.goalForecasts.isNotEmpty()) {
            prompts += prompt("Am I on track for my goals?", "Am I on track for my goals?")
        }
        if (context.debtSummary.activeOwedDebts.isNotEmpty()) {
            prompts += prompt("How's my debt looking?", "How's my debt looking?")
        }
        prompts += prompt("What's my balance?", "What's my balance?")

        return prompts.take(4)
    }

    fun respond(query: String, context: AssistantContext): AssistantMessage {
        if (!context.hasActivity) {
            return assistantMessage(
                "I don't have any transactions to look at yet. Add one from the " +
                    "dashboard and I'll be able to answer this from real numbers.",
                AssistantAction("Add a transaction", AssistantActionType.ADD_TRANSACTION)
            )
        }

        val normalized = query.lowercase()

        return when {
            containsAny(normalized, "budget", "limit", "over spending", "overspend") ->
                budgetReply(context)

            containsAny(normalized, "goal", "saving", "save for", "target") ->
                goalReply(context)

            containsAny(normalized, "debt", "owe", "loan", "borrow", "lent") ->
                debtReply(context)

            containsAny(normalized, "spend", "spending", "expense", "category", "categories", "afford") ->
                spendingReply(context)

            containsAny(normalized, "balance", "income", "earn", "how much do i have") ->
                balanceReply(context)

            containsAny(normalized, "hello", "hi", "hey") ->
                greeting(context)

            containsAny(normalized, "how does this work", "what can you do") ->
                assistantMessage(
                    "Ask me things like \"How's my spending this month?\", \"Am I over " +
                        "budget anywhere?\" or \"Am I on track for my goals?\" — every " +
                        "answer comes straight from your own transactions, budgets and goals."
                )

            else -> fallbackReply(context)
        }
    }

    private fun spendingReply(context: AssistantContext): AssistantMessage {
        val topCategory = context.analytics.currentMonthBreakdown.firstOrNull()
        val changeText = context.analytics.monthOverMonthExpenseChangePercent?.let { change ->
            val direction = if (change >= 0) "up" else "down"
            " That's %s %d%% versus last month.".format(direction, kotlin.math.abs(change).roundToInt())
        }.orEmpty()

        val text = if (topCategory != null) {
            "You've spent ${topCategory.amount.formatted()} on ${topCategory.category.displayName} " +
                "so far this month, your biggest category at ${(topCategory.percentOfTotal * 100).roundToInt()}% " +
                "of total spend.$changeText"
        } else {
            "You haven't logged any spending yet this month.$changeText"
        }

        return assistantMessage(text, AssistantAction("Open analytics", AssistantActionType.OPEN_ANALYTICS))
    }

    private fun budgetReply(context: AssistantContext): AssistantMessage {
        if (context.budgetProgress.isEmpty()) {
            return assistantMessage(
                "You don't have any budgets set up yet. Setting one for your biggest " +
                    "spending category is usually the fastest way to rein in spending.",
                AssistantAction("Set up a budget", AssistantActionType.OPEN_BUDGETS)
            )
        }

        val overLimit = context.budgetProgress.filter { it.isOverLimit }
        val nearLimit = context.budgetProgress.filter { !it.isOverLimit && it.percentUsed >= 0.9f }

        val text = when {
            overLimit.isNotEmpty() -> {
                val names = overLimit.joinToString(", ") { it.budget.category.displayName }
                "You're over budget on $names this month."
            }

            nearLimit.isNotEmpty() -> {
                val names = nearLimit.joinToString(", ") { it.budget.category.displayName }
                "You're close to your limit on $names — worth keeping an eye on for the rest of the month."
            }

            else -> "All ${context.budgetProgress.size} of your budgets are within limit this month. Nicely on track."
        }

        return assistantMessage(text, AssistantAction("Open budgets", AssistantActionType.OPEN_BUDGETS))
    }

    private fun goalReply(context: AssistantContext): AssistantMessage {
        if (context.goalForecasts.isEmpty()) {
            return assistantMessage(
                "You don't have any savings goals yet. Setting a target amount and date " +
                    "lets me tell you exactly how much to set aside each month.",
                AssistantAction("Create a goal", AssistantActionType.OPEN_GOALS)
            )
        }

        val behindPace = context.goalForecasts.filter {
            it.requiredMonthlyContribution != null && it.requiredMonthlyContribution.isPositive
        }

        val text = if (behindPace.isEmpty()) {
            "All your savings goals are funded or don't have a target date yet — nothing urgent to report."
        } else {
            val lines = behindPace.take(3).joinToString(" ") { forecast ->
                val required = forecast.requiredMonthlyContribution?.formatted()
                if (required != null && forecast.monthsRemaining != null) {
                    "\"${forecast.goal.name}\" needs about $required a month for the next ${forecast.monthsRemaining} months to hit its target."
                } else {
                    "\"${forecast.goal.name}\" is ${(forecast.goal.percentComplete * 100).roundToInt()}% funded."
                }
            }
            lines
        }

        return assistantMessage(text, AssistantAction("Open goals", AssistantActionType.OPEN_GOALS))
    }

    private fun balanceReply(context: AssistantContext): AssistantMessage {
        val summary = context.dashboard
        val text = "Your total balance is ${summary.totalBalance.formatted()}. This month you've earned " +
            "${summary.monthIncome.formatted()} and spent ${summary.monthExpense.formatted()}."
        return assistantMessage(text)
    }

    private fun debtReply(context: AssistantContext): AssistantMessage {
        val debt = context.debtSummary

        if (debt.activeOwedDebts.isEmpty() && debt.activeReceivables.isEmpty()) {
            return assistantMessage(
                "You don't have any debts tracked. If you're paying something off or owed " +
                    "money by someone, add it to see your Debt Health and a payoff projection.",
                AssistantAction("Open debt center", AssistantActionType.OPEN_DEBT)
            )
        }

        val text = buildString {
            if (debt.activeOwedDebts.isNotEmpty()) {
                append("Your debt health is ${debt.health.label.name.lowercase().replace('_', ' ')} ")
                append("(${debt.health.score}/100). ")
                val freedom = debt.freedomProjection
                if (freedom.debtFreeDate != null) {
                    append("At your current minimum payments, you're projected to be debt-free around ")
                    append(freedom.debtFreeDate.toString())
                    append(". ")
                } else {
                    append("At your current minimum payments, your debt-free date can't be projected yet — ")
                    append("check that every debt has a monthly payment entered. ")
                }
            }
            if (debt.activeReceivables.isNotEmpty()) {
                append("You're also owed ${debt.totalOwedToMe.formatted()} across ${debt.activeReceivables.size} " +
                    "outstanding loan${if (debt.activeReceivables.size == 1) "" else "s"} to others.")
            }
        }.trim()

        return assistantMessage(text, AssistantAction("Open debt center", AssistantActionType.OPEN_DEBT))
    }

    /**
     * The Debt Coach widget's single "top recommendation" (10.11) —
     * deliberately returns at most one message, prioritized by
     * urgency, rather than the full [debtReply] conversational answer.
     * A widget has room for one line; the Assistant chat has room for
     * a fuller answer.
     */
    fun topDebtRecommendation(context: AssistantContext): AssistantMessage? {
        val debt = context.debtSummary
        val owed = debt.activeOwedDebts
        if (owed.isEmpty()) return null

        val overdue = owed.filter { it.dueDate != null && it.dueDate.isBefore(java.time.LocalDate.now()) }
        if (overdue.isNotEmpty()) {
            val worst = overdue.maxBy { it.currentBalance.minorUnits }
            return assistantMessage(
                "\"${worst.name}\" is past its due date — worth addressing first.",
                AssistantAction("Open debt center", AssistantActionType.OPEN_DEBT)
            )
        }

        if (debt.pressure.label == DebtPressureLabel.HIGH || debt.pressure.label == DebtPressureLabel.EXTREME) {
            return assistantMessage(
                "Your debt payments are taking up a large share of your income right now — " +
                    "worth reviewing before taking on anything new.",
                AssistantAction("Open debt center", AssistantActionType.OPEN_DEBT)
            )
        }

        val smallest = owed.minByOrNull { it.currentBalance.minorUnits } ?: return null
        return assistantMessage(
            "You could eliminate \"${smallest.name}\" (${smallest.currentBalance.formatted()} left) " +
                "fastest of everything you're carrying right now.",
            AssistantAction("Open debt center", AssistantActionType.OPEN_DEBT)
        )
    }

    private fun fallbackReply(context: AssistantContext): AssistantMessage {
        val insight = context.dashboard.insight?.message
        val text = if (insight != null) {
            "$insight Ask me about your spending, budgets, or goals for more detail."
        } else {
            "I can help with spending, budgets, and savings goals — try asking \"How's my spending this month?\""
        }
        return assistantMessage(text)
    }

    private fun assistantMessage(text: String, vararg actions: AssistantAction): AssistantMessage =
        AssistantMessage(
            id = UUID.randomUUID().toString(),
            sender = AssistantSender.ASSISTANT,
            text = text,
            actions = actions.toList()
        )

    private fun prompt(label: String, query: String): AssistantSuggestedPrompt =
        AssistantSuggestedPrompt(id = UUID.randomUUID().toString(), label = label, query = query)

    private fun containsAny(haystack: String, vararg needles: String): Boolean =
        needles.any { haystack.contains(it) }
}
