package com.ecomap.app.presentation.ui.map

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ecomap.app.data.local.EcoMapDatabase
import com.ecomap.app.data.local.toEntity
import com.ecomap.app.data.local.toPublicacion
import com.ecomap.app.data.local.toUsuario
import com.ecomap.app.domain.model.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class MapViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val db: EcoMapDatabase
) : ViewModel() {

    private val _users = MutableStateFlow<List<Usuario>>(emptyList())
    val users: StateFlow<List<Usuario>> = _users

    private val _publications = MutableStateFlow<List<Publicacion>>(emptyList())
    val publications: StateFlow<List<Publicacion>> = _publications

    init {
        observeUsers()
        observePublications()
    }

    private fun observeUsers() {
        viewModelScope.launch(Dispatchers.IO) {
            val cachedUsers = db.usuarioDao().getAll().map { it.toUsuario() }
            if (cachedUsers.isNotEmpty()) {
                _users.value = cachedUsers
            }
        }

        firestore.collection("usuarios")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    viewModelScope.launch(Dispatchers.IO) {
                        val cachedUsers = db.usuarioDao().getAll().map { it.toUsuario() }
                        _users.value = cachedUsers
                    }
                    return@addSnapshotListener
                }

                val list = snapshot.documents.mapNotNull { doc ->
                    Usuario(
                        id = doc.getString("uid") ?: doc.id,
                        email = doc.getString("email") ?: "",
                        firebaseUid = doc.getString("uid") ?: doc.id,
                        nombre = doc.getString("nombre") ?: "",
                        rolId = doc.getString("rol") ?: "CIUDADANO",
                        telefono = doc.getString("telefono"),
                        lat = doc.getDouble("lat"),
                        lon = doc.getDouble("lon")
                    )
                }
                _users.value = list

                viewModelScope.launch(Dispatchers.IO) {
                    db.usuarioDao().upsertAll(list.map { it.toEntity() })
                }
            }
    }

    private fun observePublications() {
        viewModelScope.launch(Dispatchers.IO) {
            val cachedPubs = db.materialDao().getAll().map { it.toPublicacion() }.filter { it.estadoId == "DISPONIBLE" }
            if (cachedPubs.isNotEmpty()) {
                _publications.value = cachedPubs
            }
        }

        firestore.collection("publicaciones")
            .whereEqualTo("estado", "DISPONIBLE")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    viewModelScope.launch(Dispatchers.IO) {
                        val cachedPubs = db.materialDao().getAll().map { it.toPublicacion() }.filter { it.estadoId == "DISPONIBLE" }
                        _publications.value = cachedPubs
                    }
                    return@addSnapshotListener
                }

                val list = snapshot.documents.mapNotNull { doc ->
                    Publicacion(
                        id = doc.id,
                        usuarioId = doc.getString("usuarioId") ?: "",
                        categoriaId = doc.getString("categoriaId") ?: "",
                        estadoId = doc.getString("estado") ?: "DISPONIBLE",
                        titulo = doc.getString("titulo") ?: "",
                        descripcion = doc.getString("descripcion") ?: "",
                        pesoKg = doc.getDouble("pesoKg") ?: 0.0,
                        precioSugerido = doc.getDouble("precioSugerido") ?: 0.0,
                        calidad = doc.getString("calidad") ?: "",
                        imagenUrl = doc.getString("imagenUrl"),
                        lat = doc.getDouble("lat") ?: 0.0,
                        lon = doc.getDouble("lon") ?: 0.0
                    )
                }
                _publications.value = list

                viewModelScope.launch(Dispatchers.IO) {
                    db.materialDao().upsertAll(list.map { it.toEntity() })
                }
            }
    }

    fun updateLocation(location: Location) {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                firestore.collection("usuarios").document(uid).update(
                    "lat", location.latitude,
                    "lon", location.longitude,
                    "updatedAt", System.currentTimeMillis()
                ).await()
            } catch (e: Exception) {
                // Silent fail for background updates
            }
        }
    }
}
