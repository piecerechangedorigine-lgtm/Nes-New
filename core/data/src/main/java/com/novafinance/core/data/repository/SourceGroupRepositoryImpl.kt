package com.novafinance.core.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.novafinance.core.domain.model.SourceGroup
import com.novafinance.core.domain.model.parseSourceGroupsOrEmpty
import com.novafinance.core.domain.model.toJson
import com.novafinance.core.domain.repository.SourceGroupRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val SOURCE_GROUPS_KEY = stringPreferencesKey("source_groups_json")

/**
 * DataStore-backed rather than a Room table — groups are a lightweight
 * organizational label with no referential integrity to actually
 * enforce (see [SourceGroup]'s own doc), the same reasoning
 * `DashboardRepositoryImpl` already applied to the widget layout in
 * Phase 9. JSON (de)serialization lives in `core:domain`
 * (`List<SourceGroup>.toJson()` / `parseSourceGroupsOrEmpty()`), not
 * here, so this module never needs `kotlinx.serialization` as a direct
 * dependency.
 */
class SourceGroupRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : SourceGroupRepository {

    override fun observeGroups(): Flow<List<SourceGroup>> = dataStore.data.map { prefs ->
        prefs[SOURCE_GROUPS_KEY]?.let { parseSourceGroupsOrEmpty(it) } ?: emptyList()
    }

    override suspend fun addGroup(group: SourceGroup) {
        val current = observeGroups().first()
        persist(current.filterNot { it.id == group.id } + group)
    }

    override suspend fun deleteGroup(groupId: String) {
        val current = observeGroups().first()
        persist(current.filterNot { it.id == groupId })
    }

    private suspend fun persist(groups: List<SourceGroup>) {
        dataStore.edit { it[SOURCE_GROUPS_KEY] = groups.toJson() }
    }
}
