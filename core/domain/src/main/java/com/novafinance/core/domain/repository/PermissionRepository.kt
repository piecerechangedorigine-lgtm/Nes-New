package com.novafinance.core.domain.repository

import com.novafinance.core.domain.model.PermissionInfo
import com.novafinance.core.domain.model.PermissionType

/**
 * Deliberately pull-based (`suspend fun`, not a `Flow`) — unlike every
 * other repository in this app, OS permission grants have no built-in
 * change notification to observe. The Permission Center screen re-checks
 * via [checkStatuses] on its own resume rather than this interface
 * pretending to be reactive when the underlying OS API isn't.
 */
interface PermissionRepository {
    /** Current status of every [PermissionType]. For types with [PermissionType.isRealOsPermission] false, this reflects in-app acknowledgment tracked in DataStore, not a real OS grant — see [PermissionType] doc. */
    suspend fun checkStatuses(): List<PermissionInfo>

    /** Called after a real OS permission request returns, so the acknowledgment/result gets persisted the same way every other Profile setting does. */
    suspend fun recordOsPermissionResult(type: PermissionType, granted: Boolean)

    /** For the five not-yet-real permissions — records that the person has seen and dismissed the explanation, without an actual OS grant happening. */
    suspend fun acknowledge(type: PermissionType)
}
