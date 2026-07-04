package com.novafinance.core.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.novafinance.core.domain.model.AppTheme
import com.novafinance.core.domain.model.ProfileSettings
import com.novafinance.core.domain.model.SoundMode
import com.novafinance.core.domain.repository.ProfileRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private object Keys {
    val THEME = stringPreferencesKey("theme")
    val CURRENCY = stringPreferencesKey("currency")
    val BIOMETRIC_LOCK_ENABLED = booleanPreferencesKey("biometric_lock_enabled")
    val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
    val SPENDING_ALERTS_ENABLED = booleanPreferencesKey("spending_alerts_enabled")
    val SOUND_MODE = stringPreferencesKey("sound_mode")
    val SHOW_DASHBOARD_INSIGHTS = booleanPreferencesKey("show_dashboard_insights")
}

/**
 * Every read goes through [ProfileSettings]'s own defaults (via the
 * `?:` fallbacks below) rather than a second, separate set of default
 * constants here — so a fresh install and an explicit "reset to
 * defaults" action would only ever have one place defining what
 * "default" means.
 */
class ProfileRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : ProfileRepository {

    override fun observeSettings(): Flow<ProfileSettings> = dataStore.data.map { prefs ->
        val defaults = ProfileSettings()
        ProfileSettings(
            theme = prefs[Keys.THEME]?.let { runCatching { AppTheme.valueOf(it) }.getOrNull() } ?: defaults.theme,
            currency = prefs[Keys.CURRENCY] ?: defaults.currency,
            isBiometricLockEnabled = prefs[Keys.BIOMETRIC_LOCK_ENABLED] ?: defaults.isBiometricLockEnabled,
            areNotificationsEnabled = prefs[Keys.NOTIFICATIONS_ENABLED] ?: defaults.areNotificationsEnabled,
            areSpendingAlertsEnabled = prefs[Keys.SPENDING_ALERTS_ENABLED] ?: defaults.areSpendingAlertsEnabled,
            soundMode = prefs[Keys.SOUND_MODE]?.let { runCatching { SoundMode.valueOf(it) }.getOrNull() } ?: defaults.soundMode,
            showDashboardInsights = prefs[Keys.SHOW_DASHBOARD_INSIGHTS] ?: defaults.showDashboardInsights
        )
    }

    override suspend fun setTheme(theme: AppTheme) {
        dataStore.edit { it[Keys.THEME] = theme.name }
    }

    override suspend fun setCurrency(currencyCode: String) {
        dataStore.edit { it[Keys.CURRENCY] = currencyCode }
    }

    override suspend fun setBiometricLockEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.BIOMETRIC_LOCK_ENABLED] = enabled }
    }

    override suspend fun setNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.NOTIFICATIONS_ENABLED] = enabled }
    }

    override suspend fun setSpendingAlertsEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.SPENDING_ALERTS_ENABLED] = enabled }
    }

    override suspend fun setSoundMode(mode: SoundMode) {
        dataStore.edit { it[Keys.SOUND_MODE] = mode.name }
    }

    override suspend fun setShowDashboardInsights(enabled: Boolean) {
        dataStore.edit { it[Keys.SHOW_DASHBOARD_INSIGHTS] = enabled }
    }
}
