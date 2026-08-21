package com.ecomap.app.presentation.ui.auth

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ecomap.app.domain.model.RoleCode
import com.ecomap.app.domain.model.Usuario
import com.ecomap.app.domain.repository.UsuarioRepository
import com.ecomap.app.data.datastore.SessionDataStore
import com.ecomap.app.data.repository.UsuarioRepositoryImpl
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
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
                // Fetch full data from firestore via repo
                val user = repository.currentUser() // Needs to be full fetch in repo
                // Update: I'll use a specific method for this
                (repository as? UsuarioRepositoryImpl)?.let { repo ->
                   // We don't have a direct getById, let's just use signIn logic or similar
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
        viewModelScope.launch {
            uiState = AuthUiState.Loading
            Log.d("ECOMAP_AUTH", "register llamado: $email rol=${role.name}")
            val user = Usuario(id = "", email = email, firebaseUid = "", nombre = name, rolId = role.name)
            val result = repository.register(user, pass)
            if (result.isSuccess) {
                val registeredUser = result.getOrNull()!!
                currentUserData = registeredUser
                sessionStore.saveSession(registeredUser.firebaseUid, registeredUser.rolId)
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

    fun updateProfile(updates: Map<String, Any?>, onDone: () -> Unit) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        viewModelScope.launch {
            uiState = AuthUiState.Loading
            val result = (repository as? UsuarioRepositoryImpl)?.updateProfile(uid, updates) 
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
                Log.d("ECOMAP_AUTH", "Correo de recuperación enviado")
                uiState = AuthUiState.Success
            } else {
                val errorMsg = mapError(result.exceptionOrNull())
                Log.e("ECOMAP_AUTH", "Error al resetear pass: $errorMsg")
                uiState = AuthUiState.Error(errorMsg)
            }
        }
    }

    private fun mapError(e: Throwable?): String {
        val message = e?.message ?: ""
        return when {
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
