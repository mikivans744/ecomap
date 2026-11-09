package com.ecomap.app.presentation.ui.auth

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ecomap.app.BuildConfig
import com.ecomap.app.data.datastore.SessionDataStore
import com.ecomap.app.data.repository.UsuarioRepositoryImpl
import com.ecomap.app.domain.model.RoleCode
import com.ecomap.app.domain.model.Usuario
import com.ecomap.app.domain.repository.UsuarioRepository
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repository: UsuarioRepository,
    private val sessionStore: SessionDataStore
) : ViewModel() {

    var uiState by mutableStateOf<AuthUiState>(AuthUiState.Idle)
        private set

    var currentUserData by mutableStateOf<Usuario?>(null)
        private set

    init {
        refreshUser()
    }

    fun refreshUser() {
        viewModelScope.launch {
            val uid = FirebaseAuth.getInstance().currentUser?.uid
            if (uid != null) {
                FirebaseFirestore.getInstance()
                    .collection("usuarios").document(uid).get()
                    .addOnSuccessListener { doc ->
                        if (doc.exists()) {
                            val user = Usuario(
                                id = uid,
                                email = doc.getString("email") ?: "",
                                firebaseUid = uid,
                                nombre = doc.getString("nombre") ?: "",
                                rolId = doc.getString("rol") ?: "CIUDADANO",
                                telefono = doc.getString("telefono"),
                                reputacion = doc.getDouble("reputacion") ?: 0.0,
                                puntos = doc.getLong("puntos")?.toInt() ?: 0,
                                activo = doc.getBoolean("activo") ?: true,
                                placaVehiculo = doc.getString("placa_vehiculo"),
                                tipoVehiculo = doc.getString("tipo_vehiculo"),
                                capacidadKg = doc.getDouble("capacidad_kg"),
                                zonaCobertura = doc.getString("zona_cobertura"),
                                nombreEmpresa = doc.getString("nombre_empresa"),
                                direccionFija = doc.getString("direccion_fija"),
                                lat = doc.getDouble("lat"),
                                lon = doc.getDouble("lon")
                            )
                            currentUserData = user
                        }
                    }
            }
        }
    }

    fun login(email: String, pass: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            uiState = AuthUiState.Loading
            Log.d("ECOMAP_AUTH", "login llamado: $email")
            val result = repository.signIn(email, pass)
            if (result.isSuccess) {
                val user = result.getOrNull()!!
                currentUserData = user
                sessionStore.saveSession(user.firebaseUid, user.rolId)
                refreshUser()
                Log.d("ECOMAP_AUTH", "Login exitoso para uid: ${user.firebaseUid}")
                onSuccess()
                uiState = AuthUiState.Idle
            } else {
                val errorMsg = mapError(result.exceptionOrNull())
                Log.e("ECOMAP_AUTH", "Login fallido: $errorMsg")
                uiState = AuthUiState.Error(errorMsg)
            }
        }
    }

    fun register(email: String, pass: String, name: String, role: RoleCode, onSuccess: () -> Unit) {
        val user = Usuario(id = "", email = email, firebaseUid = "", nombre = name, rolId = role.name)
        registerWithDetails(user, pass, onSuccess)
    }

    fun registerWithDetails(user: Usuario, pass: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            uiState = AuthUiState.Loading
            Log.d("ECOMAP_AUTH", "registerWithDetails llamado: ${user.email} rol=${user.rolId}")
            val result = repository.register(user, pass)
            if (result.isSuccess) {
                val registeredUser = result.getOrNull()!!
                currentUserData = registeredUser
                sessionStore.saveSession(registeredUser.firebaseUid, registeredUser.rolId)
                refreshUser()
                Log.d("ECOMAP_AUTH", "Registro completo exitoso: ${registeredUser.firebaseUid}")
                onSuccess()
                uiState = AuthUiState.Idle
            } else {
                val errorMsg = mapError(result.exceptionOrNull())
                Log.e("ECOMAP_AUTH", "Registro fallido: $errorMsg", result.exceptionOrNull())
                uiState = AuthUiState.Error(errorMsg)
            }
        }
    }

    fun updateProfile(updates: Map<String, Any?>, onDone: () -> Unit = {}) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        viewModelScope.launch {
            uiState = AuthUiState.Loading
            val fullUpdates = updates.toMutableMap()
            fullUpdates["updatedAt"] = System.currentTimeMillis()
            val result = (repository as? UsuarioRepositoryImpl)?.updateProfile(uid, fullUpdates) 
                ?: Result.failure(Exception("Repo type error"))
            
            if (result.isSuccess) {
                refreshUser()
                onDone()
                uiState = AuthUiState.Idle
            } else {
                uiState = AuthUiState.Error("Error al actualizar perfil")
            }
        }
    }

    fun logout(onSuccess: () -> Unit) {
        viewModelScope.launch {
            FirebaseAuth.getInstance().signOut()
            sessionStore.clearSession()
            currentUserData = null
            onSuccess()
        }
    }

    fun changePassword(currentPass: String, newPass: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val user = FirebaseAuth.getInstance().currentUser
        val email = user?.email
        if (user == null || email.isNullOrBlank()) {
            onError("Usuario no autenticado")
            return
        }
        if (newPass.length < 6) {
            onError("La nueva contraseña es muy débil (mínimo 6 caracteres)")
            return
        }
        if (currentPass == newPass) {
            onError("La nueva contraseña debe ser distinta a la actual")
            return
        }

        viewModelScope.launch {
            uiState = AuthUiState.Loading
            try {
                val credential = EmailAuthProvider.getCredential(email, currentPass)
                user.reauthenticate(credential).await()
                user.updatePassword(newPass).await()
                sendAdminSecurityAlert(email)
                uiState = AuthUiState.Idle
                onSuccess()
            } catch (e: Exception) {
                uiState = AuthUiState.Idle
                val msg = e.message ?: ""
                val mapped = when {
                    msg.contains("invalid-credential") || msg.contains("wrong-password") || msg.contains("INVALID_PASSWORD") ->
                        "La contraseña actual es incorrecta"
                    msg.contains("requires-recent-login") || msg.contains("RECENT_LOGIN") ->
                        "Por seguridad, esta acción requiere una sesión reciente. Se ha enviado un correo para cambiar tu contraseña."
                    msg.contains("weak-password") ->
                        "La nueva contraseña es muy débil (mínimo 6 caracteres)"
                    else -> "Error al cambiar contraseña: ${e.localizedMessage ?: "intenta con el enlace de recuperación"}"
                }
                onError(mapped)
            }
        }
    }

    fun resetPassword(email: String) {
        if (email.isBlank()) {
            uiState = AuthUiState.Error("Ingresa tu correo electrónico")
            return
        }
        viewModelScope.launch {
            uiState = AuthUiState.Loading
            Log.d("ECOMAP_AUTH", "resetPassword llamado para: $email")
            val result = repository.resetPassword(email)
            if (result.isSuccess) {
                sendAdminSecurityAlert(email)
                Log.d("ECOMAP_AUTH", "Correo de recuperación enviado")
                uiState = AuthUiState.Success
            } else {
                val errorMsg = mapError(result.exceptionOrNull())
                Log.e("ECOMAP_AUTH", "Error al resetear pass: $errorMsg")
                uiState = AuthUiState.Error(errorMsg)
            }
        }
    }

    private fun sendAdminSecurityAlert(userEmail: String?) {
        viewModelScope.launch {
            try {
                val data = mapOf(
                    "paraUid" to "ADMIN",
                    "titulo" to "Cambio de contraseña",
                    "cuerpo" to "${userEmail ?: "Un usuario"} cambió o restableció su contraseña",
                    "tipo" to "SEGURIDAD",
                    "createdAt" to System.currentTimeMillis(),
                    "leido" to false
                )
                FirebaseFirestore.getInstance().collection("notifications").add(data).await()
                Log.d("ECOMAP_NOTIF", "Alerta de seguridad enviada a ADMIN para $userEmail")
            } catch (e: Exception) {
                Log.w("ECOMAP_NOTIF", "No se pudo notificar alerta de seguridad a ADMIN (reglas): ${e.message}")
            }
        }
    }

    private fun mapError(e: Throwable?): String {
        val message = e?.message ?: ""
        return when {
            message.contains("Cuenta desactivada") -> message
            message.contains("CONFIGURATION_NOT_FOUND") || message.contains("operation-not-allowed") -> 
                "Proveedor no habilitado en Firebase"
            message.contains("email-already-in-use") -> "Este correo ya está registrado"
            message.contains("weak-password") -> "La contraseña es muy débil (mínimo 6 caracteres)"
            message.contains("invalid-email") -> "Correo electrónico no válido"
            message.contains("network-request-failed") -> "Sin conexión a internet"
            message.contains("user-not-found") -> "Usuario no encontrado"
            message.contains("wrong-password") -> "Contraseña incorrecta"
            else -> "Error: ${e?.localizedMessage ?: "desconocido"}"
        }
    }
}

sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    object Success : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}
