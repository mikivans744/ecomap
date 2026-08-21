package com.ecomap.app.data.repository

import com.ecomap.app.BuildConfig
import com.ecomap.app.data.remote.*
import com.ecomap.app.domain.model.*
import com.ecomap.app.domain.repository.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class MongoRepositoryImpl @Inject constructor(
    private val api: MongoDataApi
) : UsuarioRepository, MaterialRepository {
    
    private val cluster = "Cluster0" // Placeholder
    private val db = "ecomap"
    private val apiKey = BuildConfig.MONGO_API_KEY

    override suspend fun register(user: Usuario, password: String): Result<Usuario> = try {
        val response = api.insertOne(apiKey, MongoInsertRequest(
            dataSource = cluster,
            database = db,
            collection = "usuarios",
            document = mapOf(
                "nombre" to user.nombre,
                "email" to user.email,
                "rolId" to user.rolId,
                "createdAt" to System.currentTimeMillis()
            )
        ))
        Result.success(user.copy(id = response.insertedId))
    } catch (e: Exception) { Result.failure(e) }

    // Implementaciones mínimas para cumplir la interfaz
    override suspend fun signIn(e: String, p: String): Result<Usuario> = Result.failure(Exception("Not implemented for Mongo yet"))
    override suspend fun resetPassword(e: String): Result<Unit> = Result.failure(Exception("Not implemented for Mongo yet"))
    override suspend fun currentUser(): Usuario? = null
    override fun publications(): Flow<List<Publicacion>> = flow { emit(emptyList()) }
    override suspend fun publish(item: Publicacion): Result<Publicacion> = Result.success(item)
    override suspend fun syncPending(): Int = 0
}
