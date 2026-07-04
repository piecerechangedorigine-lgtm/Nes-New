package com.novafinance.core.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.novafinance.core.domain.model.DashboardPreset
import com.novafinance.core.domain.model.DashboardWidgetType
import com.novafinance.core.domain.model.GoalForecast
import com.novafinance.core.domain.model.Money
import com.novafinance.core.domain.model.SavingsGoal
import com.novafinance.core.domain.model.WidgetSize
import java.time.LocalDate
import org.junit.Test

class DashboardLayoutDefaultsTest {

    private fun goalForecast(id: String, healthyProgress: Boolean) = GoalForecast(
        goal = SavingsGoal(
            id = id,
            name = "Goal $id",
            targetAmount = Money.fromMajor(1000.0),
            currentAmount = Money.fromMajor(if (healthyProgress) 900.0 else 10.0),
            targetDate = null,
            createdAt = LocalDate.now()
        ),
        requiredMonthlyContribution = null,
        monthsRemaining = null
    )

    @Test
    fun `balanced preset includes every widget type at medium or small size`() {
        val widgets = DashboardLayoutDefaults.buildWidgets(DashboardPreset.BALANCED, emptyList())

        val types = widgets.map { it.type }.toSet()
        assertThat(types).containsExactly(
            DashboardWidgetType.FORECAST,
            DashboardWidgetType.DEBT_OVERVIEW,
            DashboardWidgetType.FINANCIAL_OVERVIEW,
            DashboardWidgetType.AI_INSIGHTS,
            DashboardWidgetType.RECENT_ACTIVITY
        )
    }

    @Test
    fun `dream focus hides recent activity and sizes goals large`() {
        val goals = listOf(goalForecast("1", true))
        val widgets = DashboardLayoutDefaults.buildWidgets(DashboardPreset.DREAM_FOCUS, goals)

        assertThat(widgets.map { it.type }).doesNotContain(DashboardWidgetType.RECENT_ACTIVITY)
        val goalWidget = widgets.single { it.type == DashboardWidgetType.GOAL }
        assertThat(goalWidget.size).isEqualTo(WidgetSize.LARGE)
    }

    @Test
    fun `minimal preset hides AI insights entirely`() {
        val widgets = DashboardLayoutDefaults.buildWidgets(DashboardPreset.MINIMAL, emptyList())

        assertThat(widgets.map { it.type }).doesNotContain(DashboardWidgetType.AI_INSIGHTS)
    }

    @Test
    fun `minimal preset caps goal widgets to the single most at-risk goal`() {
        val healthy = goalForecast("healthy", healthyProgress = true)
        val atRisk = goalForecast("at-risk", healthyProgress = false)

        val widgets = DashboardLayoutDefaults.buildWidgets(DashboardPreset.MINIMAL, listOf(healthy, atRisk))

        val goalWidgets = widgets.filter { it.type == DashboardWidgetType.GOAL }
        assertThat(goalWidgets).hasSize(1)
        assertThat(goalWidgets.single().goalId).isEqualTo("at-risk")
    }

    @Test
    fun `balanced preset shows every goal uncapped`() {
        val goals = (1..5).map { goalForecast(it.toString(), healthyProgress = true) }

        val widgets = DashboardLayoutDefaults.buildWidgets(DashboardPreset.BALANCED, goals)

        assertThat(widgets.count { it.type == DashboardWidgetType.GOAL }).isEqualTo(5)
    }

    @Test
    fun `goal widget ids are deterministic so reapplying a preset does not duplicate cards`() {
        val goals = listOf(goalForecast("g1", true))

        val first = DashboardLayoutDefaults.buildWidgets(DashboardPreset.BALANCED, goals)
        val second = DashboardLayoutDefaults.buildWidgets(DashboardPreset.BALANCED, goals)

        assertThat(first.single { it.type == DashboardWidgetType.GOAL }.id)
            .isEqualTo(second.single { it.type == DashboardWidgetType.GOAL }.id)
    }

    @Test
    fun `initial layout is balanced with no background`() {
        val layout = DashboardLayoutDefaults.initialLayout(emptyList())

        assertThat(layout.activePreset).isEqualTo(DashboardPreset.BALANCED)
    }

    @Test
    fun `finance focus surfaces every debt widget type, not just the overview`() {
        val widgets = DashboardLayoutDefaults.buildWidgets(DashboardPreset.FINANCE_FOCUS, emptyList())

        val types = widgets.map { it.type }.toSet()
        assertThat(types).containsAtLeast(
            DashboardWidgetType.DEBT_OVERVIEW,
            DashboardWidgetType.DEBT_WEATHER,
            DashboardWidgetType.DEBT_COACH,
            DashboardWidgetType.DEBT_RECOVERY
        )
    }

    @Test
    fun `dream focus and minimal only surface the debt overview, not the specialized debt widgets`() {
        val dreamFocus = DashboardLayoutDefaults.buildWidgets(DashboardPreset.DREAM_FOCUS, emptyList())
        val minimal = DashboardLayoutDefaults.buildWidgets(DashboardPreset.MINIMAL, emptyList())

        for (widgets in listOf(dreamFocus, minimal)) {
            val types = widgets.map { it.type }
            assertThat(types).contains(DashboardWidgetType.DEBT_OVERVIEW)
            assertThat(types).containsNoneOf(
                DashboardWidgetType.DEBT_WEATHER,
                DashboardWidgetType.DEBT_COACH,
                DashboardWidgetType.DEBT_RECOVERY
            )
        }
    }
}
