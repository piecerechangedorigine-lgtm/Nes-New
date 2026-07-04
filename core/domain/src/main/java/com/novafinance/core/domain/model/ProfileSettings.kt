package com.novafinance.core.domain.model

/**
 * Every persisted personalization setting in one place. Previously
 * in-memory only (see ProfileViewModel's original doc comment) — now
 * backed by DataStore (ProfileRepositoryImpl), so this survives process
 * death, which the in-memory version explicitly couldn't.
 */
data class ProfileSettings(
    val theme: AppTheme = AppTheme.DARK,
    /** ISO 4217 currency code. Persisted from day one, but not yet threaded through [Money.formatted]'s call sites app-wide — see TECHNICAL_DEBT.md. */
    val currency: String = "USD",
    val isBiometricLockEnabled: Boolean = false,
    val areNotificationsEnabled: Boolean = true,
    val areSpendingAlertsEnabled: Boolean = true,
    val soundMode: SoundMode = SoundMode.MINIMAL,
    /** Dashboard preference example — whether the insight banner shows at all. Deliberately a single flag rather than a bag of unrelated dashboard toggles until there's a second one to justify a richer shape. */
    val showDashboardInsights: Boolean = true
)

/**
 * Nova is dark-first by brand (see NovaTheme doc in core:designsystem) —
 * [LIGHT] and [SYSTEM] are both modeled here even though only [DARK] is
 * actually implemented today, so the persisted setting doesn't need a
 * schema change once a light palette ships.
 */
enum class AppTheme { SYSTEM, LIGHT, DARK }

/**
 * Foundation for the Sound System (Phase 8.5.9). [OFF] and [HAPTIC_ONLY]
 * need no audio assets at all; [MINIMAL] and [PREMIUM] are ready for
 * real sound assets once a sound designer produces them — see
 * NovaSoundManager's doc for why no placeholder audio ships with this.
 */
enum class SoundMode { OFF, HAPTIC_ONLY, MINIMAL, PREMIUM }
