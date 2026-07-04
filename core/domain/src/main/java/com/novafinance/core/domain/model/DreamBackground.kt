package com.novafinance.core.domain.model

import kotlinx.serialization.Serializable

/**
 * The Dashboard's optional background image (9.8). A `sealed class`
 * rather than an enum + nullable URI, so [None] is a real, distinct
 * state instead of "[DeviceImage] with a null/empty uri" being an
 * implicit, easy-to-mishandle third state.
 */
@Serializable
sealed class DreamBackground {
    @Serializable
    data object None : DreamBackground()

    /**
     * [uri] is a persisted content URI — the caller must have taken a
     * persistable permission grant on it (see
     * `ContentResolver.takePersistableUriPermission` in
     * `DashboardStudioViewModel`) or the URI stops resolving after the
     * next process restart, which is a common enough SAF footgun to
     * call out explicitly here.
     */
    @Serializable
    data class DeviceImage(val uri: String) : DreamBackground()

    /**
     * Not implemented — see 9.8's "Future AI-generated image support"
     * and ROADMAP_NEXT.md. This case exists now so the persisted schema
     * doesn't need to change shape when it is implemented; selecting it
     * anywhere in the UI would be premature since there's no generation
     * pipeline behind it yet.
     */
    @Serializable
    data object AiGenerated : DreamBackground()
}
