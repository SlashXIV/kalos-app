package com.kalos.app.core.data.repository

import com.kalos.app.core.data.mapper.toDomain
import com.kalos.app.core.data.mapper.toEntity
import com.kalos.app.core.database.dao.UserProfileDao
import com.kalos.app.core.domain.model.NutritionGoal
import com.kalos.app.core.domain.model.UserProfile
import com.kalos.app.core.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class UserRepositoryImpl @Inject constructor(
    private val dao: UserProfileDao,
) : UserRepository {
    override fun observeProfile(): Flow<UserProfile?> = dao.observe().map { it?.toDomain() }
    override suspend fun getProfile(): UserProfile? = dao.get()?.toDomain()
    override suspend fun saveProfile(profile: UserProfile) = dao.upsert(profile.toEntity())
    override fun observeGoal(): Flow<NutritionGoal?> = dao.observeGoal().map { it?.toDomain() }
    override suspend fun getGoal(): NutritionGoal? = dao.getGoal()?.toDomain()
    override suspend fun saveGoal(goal: NutritionGoal) = dao.upsertGoal(goal.toEntity())
}
