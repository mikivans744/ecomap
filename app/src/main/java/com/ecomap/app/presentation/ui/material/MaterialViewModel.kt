package com.ecomap.app.presentation.ui.material

import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ecomap.app.domain.model.Publicacion
import com.ecomap.app.domain.repository.MaterialRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MaterialViewModel @Inject constructor(
    private val repository: MaterialRepository
) : ViewModel() {

    var uiState by mutableStateOf<MaterialUiState>(MaterialUiState.Idle)
        private set

    fun publish(
        title: String, 
        description: String, 
        weight: String, 
        category: String,
        imageUri: Uri?, 
        onSuccess: () -> Unit
    ) {
        if (title.isBlank() || weight.toDoubleOrNull() == null) {
            uiState = MaterialUiState.Error("Por favor ingresa título y peso válido")
            return
        }
        viewModelScope.launch {
            uiState = MaterialUiState.Loading
            Log.d("ECOMAP_PUB", "Iniciando publicación: $title, cat: $category, uri: $imageUri")
            
            val publication = Publicacion(
                id = "", 
                usuarioId = "", 
                categoriaId = category, 
                estadoId = "DISPONIBLE",
                titulo = title, 
                descripcion = description, 
                pesoKg = weight.toDouble(),
                precioSugerido = 0.0, 
                calidad = "Normal", 
                imagenUrl = imageUri?.toString(),
                lat = 0.0, 
                lon = 0.0
            )
            
            val result = repository.publish(publication)
            
            if (result.isSuccess) {
                Log.d("ECOMAP_PUB", "Publicación exitosa")
                onSuccess()
                uiState = MaterialUiState.Idle
            } else {
                val error = result.exceptionOrNull()
                Log.e("ECOMAP_PUB", "Error al publicar: ${error?.message}", error)
                uiState = MaterialUiState.Error(mapError(error))
            }
        }
    }

    private fun mapError(e: Throwable?): String {
        val msg = e?.message ?: ""
        return when {
            msg.contains("permission-denied") -> "No tienes permisos para realizar esta acción"
            msg.contains("network") -> "Error de red. Se intentará subir la foto más tarde."
            else -> "Error al publicar: ${e?.localizedMessage ?: "desconocido"}"
        }
    }
}

sealed class MaterialUiState {
    object Idle : MaterialUiState()
    object Loading : MaterialUiState()
    data class Error(val message: String) : MaterialUiState()
}
