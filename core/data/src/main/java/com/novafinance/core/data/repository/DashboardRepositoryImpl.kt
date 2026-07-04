package com.novafinance.core.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.novafinance.core.domain.model.DashboardLayout
import com.novafinance.core.domain.model.DreamBackground
import com.novafinance.core.domain.model.forecast
import com.novafinance.core.domain.model.parseDashboardLayoutOrNull
import com.novafinance.core.domain.model.toJson
import com.novafinance.core.domain.repository.DashboardRepository
import com.novafinance.core.domain.repository.GoalRepository
import com.novafinance.core.domain.usecase.DashboardLayoutDefaults
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val LAYOUT_KEY = stringPreferencesKey("dashboard_layout_json")

/**
 * Persists the whole [DashboardLayout] as one JSON blob under one
 * DataStore key — unlike `ProfileRepositoryImpl`'s many independent
 * keys, a widget list is one cohesive unit that's always read and
 * written together, so there's no risk of two keys drifting out of
 * sync the way independent boolean toggles never could anyway.
 */
class DashboardRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val goalRepository: GoalRepository
) : DashboardRepository {

    override fun observeLayout(): Flow<DashboardLayout> = dataStore.data.map { prefs ->
        prefs[LAYOUT_KEY]?.let { parseDashboardLayoutOrNull(it) }
            ?: buildInitialLayout()
    }

    override suspend fun saveLayout(layout: DashboardLayout) {
        dataStore.edit { it[LAYOUT_KEY] = layout.toJson() }
    }

    override suspend fun setBackground(background: DreamBackground) {
        val current = dataStore.data.first()[LAYOUT_KEY]?.let { parseDashboardLayoutOrNull(it) }
            ?: buildInitialLayout()
        saveLayout(current.copy(background = background))
    }

    /**
     * First-run fallback (and the fallback for a corrupted persisted
     * value) — built from the person's actual goals via the same
     * [DashboardLayoutDefaults] the Dashboard Studio's preset switcher
     * uses, so a brand-new install and a "reset to Balanced" both
     * produce identical results by construction.
     */
    private suspend fun buildInitialLayout(): DashboardLayout {
        val goals = goalRepository.observeGoals().first()
        val forecasts = goals.map { it.forecast() }
        return DashboardLayoutDefaults.initialLayout(forecasts)
    }
}
