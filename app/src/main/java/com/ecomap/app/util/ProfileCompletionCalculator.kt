package com.ecomap.app.util

import com.ecomap.app.domain.model.RoleCode
import com.ecomap.app.domain.model.Usuario

object ProfileCompletionCalculator {
    fun calculate(user: Usuario?, docData: Map<String, Any?>? = null): Int {
        if (user == null && docData == null) return 0

        val rol = user?.rolId ?: (docData?.get("rol") as? String) ?: RoleCode.CIUDADANO.name
        val telefono = user?.telefono ?: (docData?.get("telefono") as? String)
        val lat = user?.lat ?: (docData?.get("lat") as? Double)
        val capacidad = user?.capacidadKg ?: (docData?.get("capacidad_kg") as? Double)
        val zona = user?.zonaCobertura ?: (docData?.get("zona_cobertura") as? String)
        val materiales = docData?.get("materiales_aceptados")

        var total = 0
        var completed = 0

        when (rol.uppercase()) {
            RoleCode.CIUDADANO.name -> {
                total = 2
                if (!telefono.isNullOrBlank()) completed++
                if (lat != null && lat != 0.0) completed++
            }
            RoleCode.RECOLECTOR.name -> {
                total = 3
                if (!telefono.isNullOrBlank()) completed++
                if (capacidad != null && capacidad > 0.0) completed++
                if (!zona.isNullOrBlank()) completed++
            }
            RoleCode.CENTRO_ACOPIO.name, "CENTRO" -> {
                total = 3
                if (!telefono.isNullOrBlank()) completed++
                if (lat != null && lat != 0.0) completed++
                val hasMat = when (materiales) {
                    is String -> materiales.isNotBlank()
                    is List<*> -> materiales.isNotEmpty()
                    else -> false
                }
                if (hasMat) completed++
            }
            else -> {
                total = 2
                if (!telefono.isNullOrBlank()) completed++
                if (lat != null && lat != 0.0) completed++
            }
        }

        return if (total == 0) 100 else (completed * 100) / total
    }
}
