package com.novafinance.core.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class WidgetCatalogTest {

    @Test
    fun `goal widgets are never in the catalog since they are not a singleton type`() {
        assertThat(WidgetCatalog.entries.map { it.type }).doesNotContain(DashboardWidgetType.GOAL)
    }

    @Test
    fun `every non-goal widget type has exactly one catalog entry`() {
        val nonGoalTypes = DashboardWidgetType.entries.filterNot { it == DashboardWidgetType.GOAL }

        assertThat(WidgetCatalog.entries.map { it.type }).containsExactlyElementsIn(nonGoalTypes)
    }

    @Test
    fun `blank search returns every entry`() {
        assertThat(WidgetCatalog.search("")).hasSize(WidgetCatalog.entries.size)
    }

    @Test
    fun `search matches by display name case-insensitively`() {
        val results = WidgetCatalog.search("debt coach")

        assertThat(results).hasSize(1)
        assertThat(results.single().type).isEqualTo(DashboardWidgetType.DEBT_COACH)
    }

    @Test
    fun `search matches by category name`() {
        val results = WidgetCatalog.search("forecast")

        assertThat(results.map { it.type }).containsExactly(DashboardWidgetType.FORECAST, DashboardWidgetType.BALANCE_FORECAST)
    }

    @Test
    fun `search with no matches returns an empty list, not every entry`() {
        assertThat(WidgetCatalog.search("nonexistent widget name")).isEmpty()
    }
}
