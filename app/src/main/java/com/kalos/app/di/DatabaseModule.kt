package com.kalos.app.di

import android.content.Context
import androidx.room.Room
import com.kalos.app.core.database.KalosDatabase
import com.kalos.app.core.database.dao.ExportDao
import com.kalos.app.core.database.dao.ImportDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Singleton
    @Provides
    fun provideDatabase(@ApplicationContext context: Context): KalosDatabase =
        Room.databaseBuilder(context, KalosDatabase::class.java, "kalos.db")
            .addMigrations(KalosDatabase.MIGRATION_7_8)
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun provideUserProfileDao(db: KalosDatabase) = db.userProfileDao()
    @Provides fun provideFoodDao(db: KalosDatabase) = db.foodDao()
    @Provides fun provideMealEntryDao(db: KalosDatabase) = db.mealEntryDao()
    @Provides fun provideExerciseDao(db: KalosDatabase) = db.exerciseDao()
    @Provides fun provideWorkoutTemplateDao(db: KalosDatabase) = db.workoutTemplateDao()
    @Provides fun provideWorkoutLogDao(db: KalosDatabase) = db.workoutLogDao()
    @Provides fun provideProgramDao(db: KalosDatabase) = db.programDao()
    @Provides fun provideWaterIntakeDao(db: KalosDatabase) = db.waterIntakeDao()
    @Provides fun provideExportDao(db: KalosDatabase): ExportDao = db.exportDao()
    @Provides fun provideImportDao(db: KalosDatabase): ImportDao = db.importDao()
}
