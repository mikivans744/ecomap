package com.ecomap.app.data.remote

import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface MongoDataApi {
    @POST("action/insertOne")
    suspend fun insertOne(
        @Header("api-key") apiKey: String,
        @Body body: MongoInsertRequest
    ): MongoInsertResponse

    @POST("action/findOne")
    suspend fun findOne(
        @Header("api-key") apiKey: String,
        @Body body: MongoFindRequest
    ): MongoFindResponse
}

data class MongoInsertRequest(
    val dataSource: String,
    val database: String,
    val collection: String,
    val document: Map<String, Any?>
)

data class MongoInsertResponse(val insertedId: String)

data class MongoFindRequest(
    val dataSource: String,
    val database: String,
    val collection: String,
    val filter: Map<String, Any?>
)

data class MongoFindResponse(val document: Map<String, Any?>?)
