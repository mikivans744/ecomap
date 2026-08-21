package com.ecomap.app.presentation.ui.logistics

import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ecomap.app.domain.model.*
import com.ecomap.app.domain.repository.*
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InteractionViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val ofertaRepo: OfertaRepository,
    private val solicitudRepo: SolicitudRepository,
    private val ofertaCentroRepo: OfertaCentroRepository
) : ViewModel() {

    var uiState by mutableStateOf<InteractionUiState>(InteractionUiState.Idle)
        private set

    private val _notifications = MutableStateFlow<List<NotificationItem>>(emptyList())
    val notifications: StateFlow<List<NotificationItem>> = _notifications

    fun makeOfferToCenter(
        centroId: String,
        category: String,
        weight: String,
        price: String,
        imageUri: Uri?,
        onSuccess: () -> Unit
    ) {
        val w = weight.toDoubleOrNull()
        val p = price.toDoubleOrNull()
        if (w == null || p == null) {
            uiState = InteractionUiState.Error("Ingresa peso y precio válidos")
            return
        }

        viewModelScope.launch {
            uiState = InteractionUiState.Loading
            val offer = OfertaCentro(
                id = "",
                recolectorId = auth.currentUser?.uid ?: "",
                centroId = centroId,
                categoria = category,
                pesoKg = w,
                precioSugerido = p,
                fotoUrl = imageUri?.toString(),
                estado = CenterOfferStatus.OFERTADA,
                lat = 0.0,
                lon = 0.0
            )
            val result = ofertaCentroRepo.create(offer)
            if (result.isSuccess) {
                onSuccess()
                uiState = InteractionUiState.Idle
            } else {
                uiState = InteractionUiState.Error("Fallo al enviar oferta al centro")
            }
        }
    }

    fun makeOffer(publicationId: String, amount: Double) {
        viewModelScope.launch {
            val offer = Oferta(
                id = "",
                publicacionId = publicationId,
                ofertanteId = auth.currentUser?.uid ?: "",
                monto = amount,
                mensaje = "Nueva oferta",
                estado = NegotiationStatus.OFERTA
            )
            ofertaRepo.create(offer)
        }
    }

    fun requestPickup(publicationId: String) {
        viewModelScope.launch {
            val request = Solicitud(
                id = "",
                publicacionId = publicationId,
                ciudadanoId = auth.currentUser?.uid ?: "",
                estado = RequestStatus.SOLICITADA
            )
            solicitudRepo.create(request)
        }
    }
}

sealed class InteractionUiState {
    object Idle : InteractionUiState()
    object Loading : InteractionUiState()
    data class Error(val message: String) : InteractionUiState()
}

data class NotificationItem(val title: String, val body: String)
