package com.ecomap.app.di

import com.ecomap.app.BuildConfig
import com.ecomap.app.data.repository.*
import com.ecomap.app.domain.repository.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import javax.inject.Provider

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
    
    @Provides
    @Singleton
    fun provideUsuarioRepository(
        firestoreImpl: Provider<UsuarioRepositoryImpl>,
        mongoImpl: Provider<MongoRepositoryImpl>
    ): UsuarioRepository = if (BuildConfig.USE_MONGO) mongoImpl.get() else firestoreImpl.get()

    @Provides
    @Singleton
    fun provideMaterialRepository(
        firestoreImpl: Provider<MaterialRepositoryImpl>,
        mongoImpl: Provider<MongoRepositoryImpl>
    ): MaterialRepository = if (BuildConfig.USE_MONGO) mongoImpl.get() else firestoreImpl.get()

    @Provides
    @Singleton
    fun bindSolicitud(impl: SolicitudRepositoryImpl): SolicitudRepository = impl

    @Provides
    @Singleton
    fun bindOferta(impl: OfertaRepositoryImpl): OfertaRepository = impl

    @Provides
    @Singleton
    fun bindOfertaCentro(impl: OfertaCentroRepositoryImpl): OfertaCentroRepository = impl

    @Provides
    @Singleton
    fun bindNotificacion(impl: NotificacionRepositoryImpl): NotificacionRepository = impl
}
