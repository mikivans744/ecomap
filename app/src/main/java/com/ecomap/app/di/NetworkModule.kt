package com.ecomap.app.di

import com.ecomap.app.BuildConfig
import com.ecomap.app.data.remote.MongoDataApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideMongoApi(): MongoDataApi {
        return Retrofit.Builder()
            .baseUrl(if (BuildConfig.MONGO_URL.isBlank()) "https://localhost/" else BuildConfig.MONGO_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(MongoDataApi::class.java)
    }
}
