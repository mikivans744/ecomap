package com.ecomap.app.data.remote

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class EcoMapMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) { getSharedPreferences("ecomap", MODE_PRIVATE).edit().putString("fcm_token", token).apply() }
    override fun onMessageReceived(message: RemoteMessage) { /* La UI crea el canal y muestra la notificación. */ }
}
