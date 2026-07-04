package com.novafinance.core.domain.repository

import com.novafinance.core.domain.model.AppTheme
import com.novafinance.core.domain.model.ProfileSettings
import com.novafinance.core.domain.model.SoundMode
import kotlinx.coroutines.flow.Flow

/**
 * Granular setters rather than one `updateSettings(ProfileSettings)` —
 * each Profile screen toggle writes exactly the one DataStore key it
 * owns, so two settings changed in quick succession (e.g. flipping two
 * switches) can never race and silently overwrite each other the way a
 * read-modify-write-the-whole-object pattern could.
 */
interface ProfileRepository {
    fun observeSettings(): Flow<ProfileSettings>
    suspend fun setTheme(theme: AppTheme)
    suspend fun setCurrency(currencyCode: String)
    suspend fun setBiometricLockEnabled(enabled: Boolean)
    suspend fun setNotificationsEnabled(enabled: Boolean)
    suspend fun setSpendingAlertsEnabled(enabled: Boolean)
    suspend fun setSoundMode(mode: SoundMode)
    suspend fun setShowDashboardInsights(enabled: Boolean)
}
