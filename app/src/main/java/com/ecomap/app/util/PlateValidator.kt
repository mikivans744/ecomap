package com.ecomap.app.util

object PlateValidator {
    private val motoRegex = Regex("^[0-9]{3,4}-?[A-Z]{2,3}$", RegexOption.IGNORE_CASE)
    private val autoRegex = Regex("^[0-9]{4}-?[A-Z]{3}$", RegexOption.IGNORE_CASE)

    fun validate(tipoVehiculo: String, placa: String): Pair<Boolean, String?> {
        val cleanPlaca = placa.trim().uppercase()
        if (cleanPlaca.isBlank()) {
            return Pair(false, "Ingresa la placa del vehículo")
        }
        return when (tipoVehiculo.uppercase()) {
            "MOTOCICLETA" -> {
                if (motoRegex.matches(cleanPlaca)) {
                    Pair(true, null)
                } else {
                    Pair(false, "Formato inválido para motocicleta (ej: 123-AB o 1234-ABC)")
                }
            }
            "AUTOMOVIL", "CAMIONETA", "CAMION" -> {
                if (autoRegex.matches(cleanPlaca)) {
                    Pair(true, null)
                } else {
                    Pair(false, "Formato inválido para $tipoVehiculo (ej: 1234-ABC)")
                }
            }
            else -> {
                if (autoRegex.matches(cleanPlaca) || motoRegex.matches(cleanPlaca)) {
                    Pair(true, null)
                } else {
                    Pair(false, "Formato de placa no válido")
                }
            }
        }
    }
}
