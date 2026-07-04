package com.novafinance.core.data.repository

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.novafinance.core.domain.model.PermissionInfo
import com.novafinance.core.domain.model.PermissionStatus
import com.novafinance.core.domain.model.PermissionType
import com.novafinance.core.domain.repository.PermissionRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.first

class PermissionRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dataStore: DataStore<Preferences>
) : PermissionRepository {

    override suspend fun checkStatuses(): List<PermissionInfo> =
        PermissionType.entries.map { type ->
            PermissionInfo(type = type, status = statusFor(type))
        }

    override suspend fun recordOsPermissionResult(type: PermissionType, granted: Boolean) {
        dataStore.edit { it[acknowledgedKey(type)] = granted }
    }

    override suspend fun acknowledge(type: PermissionType) {
        dataStore.edit { it[acknowledgedKey(type)] = true }
    }

    private suspend fun statusFor(type: PermissionType): PermissionStatus {
        if (type == PermissionType.NOTIFICATIONS) {
            return notificationStatus()
        }
        val acknowledged = dataStore.data.first()[acknowledgedKey(type)] ?: false
        return if (acknowledged) PermissionStatus.GRANTED else PermissionStatus.NOT_REQUESTED
    }

    /**
     * Below API 33, notifications never required a runtime permission at
     * all — reporting [PermissionStatus.GRANTED] there reflects that
     * accurately rather than asking the person to "grant" something the
     * OS never gated in the first place.
     */
    private fun notificationStatus(): PermissionStatus {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return PermissionStatus.GRANTED
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        return if (granted) PermissionStatus.GRANTED else PermissionStatus.DENIED
    }

    private fun acknowledgedKey(type: PermissionType) = booleanPreferencesKey("permission_ack_${type.name}")
}
