package com.ecomap.app.di

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.ecomap.app.data.local.EcoMapDatabase
import com.ecomap.app.data.datastore.SessionDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private val Context.sessionPreferences by preferencesDataStore("ecomap_session")

@Module
@InstallIn(SingletonComponent::class)
object StorageModule {
    @Provides @Singleton fun database(@ApplicationContext context: Context): EcoMapDatabase = 
        Room.databaseBuilder(context, EcoMapDatabase::class.java, "ecomap.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides @Singleton fun sessionStore(@ApplicationContext context: Context) = SessionDataStore(context.sessionPreferences)
}
