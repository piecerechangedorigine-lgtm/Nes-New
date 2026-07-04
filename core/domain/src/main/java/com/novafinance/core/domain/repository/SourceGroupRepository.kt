package com.novafinance.core.domain.repository

import com.novafinance.core.domain.model.SourceGroup
import kotlinx.coroutines.flow.Flow

interface SourceGroupRepository {
    fun observeGroups(): Flow<List<SourceGroup>>
    suspend fun addGroup(group: SourceGroup)
    suspend fun deleteGroup(groupId: String)
}
