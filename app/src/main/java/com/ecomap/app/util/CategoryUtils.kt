package com.ecomap.app.util

import com.ecomap.app.domain.model.CategoriaMaterial

object CategoryUtils {
    val defaultCategories = listOf(
        CategoriaMaterial(id = "PLASTICO", codigo = "PLASTICO", nombre = "Plástico", nombreMostrar = "Plástico"),
        CategoriaMaterial(id = "METAL", codigo = "METAL", nombre = "Metal", nombreMostrar = "Metal"),
        CategoriaMaterial(id = "VIDRIO", codigo = "VIDRIO", nombre = "Vidrio", nombreMostrar = "Vidrio"),
        CategoriaMaterial(id = "PAPEL_CARTON", codigo = "PAPEL_CARTON", nombre = "Papel y Cartón", nombreMostrar = "Papel y Cartón"),
        CategoriaMaterial(id = "ELECTRONICO", codigo = "ELECTRONICO", nombre = "Desecho Electrónico", nombreMostrar = "Desecho Electrónico"),
        CategoriaMaterial(id = "TEXTIL", codigo = "TEXTIL", nombre = "Textil", nombreMostrar = "Textil"),
        CategoriaMaterial(id = "OTRO", codigo = "OTRO", nombre = "Otro", nombreMostrar = "Otro")
    )

    private val defaultCategoryMap = mapOf(
        "PLASTICO" to "Plástico",
        "METAL" to "Metal",
        "VIDRIO" to "Vidrio",
        "PAPEL_CARTON" to "Papel y Cartón",
        "ELECTRONICO" to "Desecho Electrónico",
        "TEXTIL" to "Textil",
        "OTRO" to "Otro",
        "1" to "Plástico",
        "2" to "Metal",
        "3" to "Vidrio",
        "4" to "Papel y Cartón"
    )

    fun getDisplayName(catId: String?, loadedCategories: List<CategoriaMaterial> = emptyList()): String {
        if (catId.isNullOrBlank()) return "Otro"

        val found = loadedCategories.firstOrNull {
            it.codigo.equals(catId, ignoreCase = true) || it.id.equals(catId, ignoreCase = true)
        }
        if (found != null && found.nombreMostrar.isNotBlank()) {
            return found.nombreMostrar
        }
        if (found != null && found.nombre.isNotBlank()) {
            return found.nombre
        }

        val upperKey = catId.trim().uppercase()
        return defaultCategoryMap[upperKey] ?: catId.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }
    }
}
