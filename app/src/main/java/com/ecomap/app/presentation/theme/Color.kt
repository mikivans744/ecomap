package com.ecomap.app.presentation.theme

import androidx.compose.ui.graphics.Color
import com.ecomap.app.domain.model.RoleCode

val EcoGreen = Color(0xFF2E7D32)
val EcoGreenDark = Color(0xFF005005)
val EcoGreenLight = Color(0xFF60AD5E)
val EcoCream = Color(0xFFF8FAF5)
val EcoDarkSurface = Color(0xFF101510)

val RoleCitizenColor = Color(0xFF2E7D32)   // EcoGreen para Ciudadano
val RoleCollectorColor = Color(0xFFE65100) // Deep Orange para Recolector
val RoleCenterColor = Color(0xFF0277BD)    // Light Blue para Centro de Acopio
val RoleAdminColor = Color(0xFF6A1B9A)     // Purple para Administrador

fun getRoleColor(roleStr: String?): Color {
    return when (roleStr?.uppercase()) {
        RoleCode.CIUDADANO.name -> RoleCitizenColor
        RoleCode.RECOLECTOR.name -> RoleCollectorColor
        RoleCode.CENTRO_ACOPIO.name, "CENTRO" -> RoleCenterColor
        RoleCode.ADMINISTRADOR.name -> RoleAdminColor
        else -> RoleCitizenColor
    }
}
