package com.kalos.app.di

import com.kalos.app.core.data.repository.*
import com.kalos.app.core.domain.repository.*
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds @Singleton abstract fun bindUserRepository(impl: UserRepositoryImpl): UserRepository
    @Binds @Singleton abstract fun bindFoodRepository(impl: FoodRepositoryImpl): FoodRepository
    @Binds @Singleton abstract fun bindMealRepository(impl: MealRepositoryImpl): MealRepository
    @Binds @Singleton abstract fun bindExerciseRepository(impl: ExerciseRepositoryImpl): ExerciseRepository
    @Binds @Singleton abstract fun bindWorkoutRepository(impl: WorkoutRepositoryImpl): WorkoutRepository
    @Binds @Singleton abstract fun bindProgramRepository(impl: ProgramRepositoryImpl): ProgramRepository
}
