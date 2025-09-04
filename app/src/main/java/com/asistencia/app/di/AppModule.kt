package com.asistencia.app.di

import android.content.Context
import com.asistencia.app.database.AsistenciaDatabase
import com.asistencia.app.repository.AsistenciaRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAsistenciaDatabase(
        @ApplicationContext context: Context
    ): AsistenciaDatabase {
        return AsistenciaDatabase.getDatabase(context)
    }

    @Provides
    @Singleton
    fun provideAsistenciaRepository(
        @ApplicationContext context: Context
    ): AsistenciaRepository {
        return AsistenciaRepository(context)
    }
}
