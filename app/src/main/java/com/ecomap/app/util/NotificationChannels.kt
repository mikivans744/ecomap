package com.ecomap.app.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

object NotificationChannels {
    const val GENERAL = "ecomap_general"
    const val NEGOTIATION = "ecomap_negotiation"
    const val PICKUP = "ecomap_pickup"

    fun create(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannels(listOf(
            NotificationChannel(GENERAL, "ECO-MAP", NotificationManager.IMPORTANCE_DEFAULT),
            NotificationChannel(NEGOTIATION, "Ofertas y negociaciones", NotificationManager.IMPORTANCE_HIGH),
            NotificationChannel(PICKUP, "Recolecciones", NotificationManager.IMPORTANCE_HIGH)
        ))
    }
}
