package com.novafinance.core.domain.model

/**
 * [NOTIFICATIONS] is the only one of these with a real corresponding
 * Android runtime permission today (`POST_NOTIFICATIONS`) — Nova's
 * push-notification toggle in Profile is a real, working feature. The
 * other five have no manifest permission declared yet on purpose: SMS
 * parsing, OCR, widgets, camera capture, and background sync are all
 * future features (see [BalanceUpdateMode.SMART] and ROADMAP_NEXT.md),
 * and this app deliberately never requests a runtime permission with no
 * feature behind it yet (see AndroidManifest's own doc comment on this).
 * Their [PermissionStatus] here tracks in-app acknowledgment, not a real
 * OS grant, until the feature they back actually ships.
 */
enum class PermissionType(val displayName: String, val description: String, val isRealOsPermission: Boolean) {
    NOTIFICATIONS(
        displayName = "Notifications",
        description = "Budget alerts and account activity reminders.",
        isRealOsPermission = true
    ),
    SMS(
        displayName = "SMS",
        description = "Read bank SMS alerts to suggest balance updates automatically. Not active yet.",
        isRealOsPermission = false
    ),
    CAMERA(
        displayName = "Camera",
        description = "Scan receipts and statements. Not active yet.",
        isRealOsPermission = false
    ),
    OCR(
        displayName = "Statement scanning",
        description = "Extract transactions from a photographed statement or receipt. Not active yet.",
        isRealOsPermission = false
    ),
    WIDGETS(
        displayName = "Home screen widgets",
        description = "Show balance and budget widgets on your home screen. Not active yet.",
        isRealOsPermission = false
    ),
    BACKGROUND_SYNC(
        displayName = "Background sync",
        description = "Keep balances current without opening the app. Not active yet.",
        isRealOsPermission = false
    )
}

enum class PermissionStatus { GRANTED, DENIED, NOT_REQUESTED, PERMANENTLY_DENIED }

data class PermissionInfo(val type: PermissionType, val status: PermissionStatus)
