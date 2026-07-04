package com.novafinance.core.domain.repository

import com.novafinance.core.domain.model.DashboardLayout
import com.novafinance.core.domain.model.DreamBackground
import kotlinx.coroutines.flow.Flow

/**
 * The whole [DashboardLayout] is read and written as one unit rather
 * than exposing granular per-widget add/remove/reorder methods — a
 * reorder is fundamentally "here's the new order of everything," and
 * modeling it as N individual move operations would just make the
 * ViewModel reconstruct the same whole list before calling repository
 * methods anyway. `DashboardStudioViewModel` holds the working copy
 * in memory and calls [saveLayout] once per meaningful change.
 */
interface DashboardRepository {
    fun observeLayout(): Flow<DashboardLayout>
    suspend fun saveLayout(layout: DashboardLayout)
    suspend fun setBackground(background: DreamBackground)
}
