package com.ecomap.app

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import dagger.hilt.android.HiltAndroidApp
import com.ecomap.app.util.NotificationChannels
import com.ecomap.app.data.remote.DataSeeder
import com.ecomap.app.domain.repository.MaterialRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class EcoMapApplication : Application() {
    @Inject lateinit var materialRepo: MaterialRepository
    @Inject lateinit var seeder: DataSeeder
    
    override fun onCreate() { 
        super.onCreate()
        
        // e) Inicialización defensiva de Firebase
        try {
            FirebaseApp.initializeApp(this)
        } catch (e: Exception) {
            Log.e("ECOMAP_INIT", "Error initializing Firebase", e)
        }

        NotificationChannels.create(this)
        
        // c) Reintento no bloqueante y seguro
        CoroutineScope(Dispatchers.IO).launch {
            try {
                seeder.seedCategories()
                materialRepo.syncPending()
            } catch (e: Exception) {
                Log.e("ECOMAP_INIT", "Error in background tasks", e)
            }
        }
    }
}
