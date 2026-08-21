package com.ecomap.app.data.remote

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataSeeder @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    suspend fun seedCategories() {
        val categories = listOf(
            mapOf("codigo" to "PLASTICO", "nombre" to "Plástico", "precio_base" to 1.5, "color" to "#4CAF50"),
            mapOf("codigo" to "METAL", "nombre" to "Metal", "precio_base" to 2.0, "color" to "#F44336"),
            mapOf("codigo" to "VIDRIO", "nombre" to "Vidrio", "precio_base" to 0.5, "color" to "#2196F3"),
            mapOf("codigo" to "PAPEL_CARTON", "nombre" to "Papel y Cartón", "precio_base" to 1.0, "color" to "#FFC107"),
            mapOf("codigo" to "ELECTRONICO", "nombre" to "Desecho Electrónico", "precio_base" to 3.0, "color" to "#9C27B0"),
            mapOf("codigo" to "TEXTIL", "nombre" to "Textil", "precio_base" to 0.8, "color" to "#795548"),
            mapOf("codigo" to "OTRO", "nombre" to "Otro", "precio_base" to 0.0, "color" to "#607D8B")
        )

        val collection = firestore.collection("categoria_material")
        val existing = collection.get().await()
        
        if (existing.isEmpty) {
            categories.forEach { cat ->
                collection.document(cat["codigo"] as String).set(cat).await()
            }
        }
    }
}
