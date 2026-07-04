package com.novafinance.core.designsystem.sound

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.novafinance.core.domain.model.SoundMode

/**
 * One place every interaction-feedback call in the app goes through,
 * rather than screens reaching for `LocalHapticFeedback` directly and
 * each deciding for itself whether the current [SoundMode] should
 * suppress it. "No excessive sounds, premium fintech feel only" (the
 * brief's own words) means the actual sound files matter more than the
 * plumbing — and there are none to ship yet. Every [SoundMode.MINIMAL]
 * and [SoundMode.PREMIUM] method below is written to call into a real
 * player once one exists (see the `TODO` on [playAsset]), rather than
 * silently doing nothing forever. Shipping a placeholder beep here
 * would be worse than shipping nothing — exactly the kind of asset this
 * project's engineering rules don't fabricate (see the Phase 7
 * Baseline Profile deferral for the same principle applied to a
 * different kind of generated artifact).
 */
class NovaSoundManager(
    private val hapticFeedback: HapticFeedback,
    private val mode: SoundMode
) {
    fun onSuccess() {
        haptic(HapticFeedbackType.LongPress)
        if (mode == SoundMode.PREMIUM) playAsset(SoundAsset.SUCCESS)
    }

    fun onError() {
        haptic(HapticFeedbackType.LongPress)
        if (mode == SoundMode.PREMIUM || mode == SoundMode.MINIMAL) playAsset(SoundAsset.ERROR)
    }

    fun onTap() {
        haptic(HapticFeedbackType.TextHandleMove)
        if (mode == SoundMode.PREMIUM) playAsset(SoundAsset.TAP)
    }

    fun onDelete() {
        haptic(HapticFeedbackType.LongPress)
    }

    private fun haptic(type: HapticFeedbackType) {
        if (mode == SoundMode.OFF) return
        hapticFeedback.performHapticFeedback(type)
    }

    /**
     * TODO(sound-assets): wire this to a real player (MediaPlayer/SoundPool
     * reading from res/raw) once a sound designer has produced Nova's
     * actual notification/success/error tones. No-op until then — see
     * TECHNICAL_DEBT.md.
     */
    private fun playAsset(asset: SoundAsset) {
        // Intentionally empty. See TODO above.
    }
}

private enum class SoundAsset { SUCCESS, ERROR, TAP }

@Composable
fun rememberNovaSoundManager(mode: SoundMode): NovaSoundManager {
    val hapticFeedback = LocalHapticFeedback.current
    return remember(mode) { NovaSoundManager(hapticFeedback, mode) }
}
