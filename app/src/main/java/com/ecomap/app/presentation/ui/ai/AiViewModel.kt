package com.ecomap.app.presentation.ui.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ecomap.app.BuildConfig
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import javax.inject.Inject

@HiltViewModel
class AiViewModel @Inject constructor() : ViewModel() {

    private val generativeModel = GenerativeModel(
        modelName = "gemini-1.5-flash",
        apiKey = BuildConfig.GEMINI_API_KEY,
        generationConfig = generationConfig {
            temperature = 0.2f
            maxOutputTokens = 600
        }
    )

    var response by mutableStateOf("")
        private set
    
    var isLoading by mutableStateOf(false)
        private set

    var aiErrorMessage by mutableStateOf<String?>(null)
        private set

    fun askGemini(prompt: String) {
        if (prompt.isBlank()) return
        viewModelScope.launch {
            isLoading = true
            aiErrorMessage = null
            try {
                val result = generativeModel.generateContent(content {
                    text("Eres un asistente de reciclaje para la app ECO-MAP. Responde de forma sencilla y en español: $prompt")
                })
                response = result.text ?: "No pude entender eso, ¿puedes repetir?"
            } catch (e: Exception) {
                Log.e("ECOMAP_AI", "Error en askGemini", e)
                val userMsg = mapAiError(e)
                aiErrorMessage = userMsg
                response = userMsg
            } finally {
                isLoading = false
            }
        }
    }

    fun draftPublicationWithAi(
        context: Context,
        imageUri: Uri?,
        userText: String,
        onSuccess: (titulo: String, descripcion: String, categoriaId: String, pesoKg: Double) -> Unit
    ) {
        viewModelScope.launch {
            isLoading = true
            aiErrorMessage = null
            try {
                val bitmap: Bitmap? = imageUri?.let { uriToBitmap(context, it) }
                val promptText = """
                    Eres un asistente de reciclaje para la app ECO-MAP para el rol CIUDADANO.
                    Analiza la imagen o el siguiente texto del usuario: "${userText.ifBlank { "Material reciclable" }}".
                    Genera una recomendación de publicación en formato JSON estricto sin comillas triples ni sintaxis markdown:
                    {"titulo": "...", "descripcion": "...", "categoriaId": "PLASTICO", "pesoKgEstimado": 1.0}
                    Categorías válidas: PLASTICO, METAL, VIDRIO, PAPEL_CARTON, ELECTRONICO, TEXTIL, OTRO.
                """.trimIndent()

                val resultText = withContext(Dispatchers.IO) {
                    val contentObj = content {
                        bitmap?.let { image(it) }
                        text(promptText)
                    }
                    generativeModel.generateContent(contentObj).text ?: ""
                }

                val cleanJson = extractJsonString(resultText)
                try {
                    val json = JSONObject(cleanJson)
                    val titulo = json.optString("titulo", "Material Reciclable")
                    val descripcion = json.optString("descripcion", userText)
                    val categoriaId = json.optString("categoriaId", "PLASTICO").uppercase()
                    val pesoKg = json.optDouble("pesoKgEstimado", 1.0)

                    val validCat = listOf("PLASTICO", "METAL", "VIDRIO", "PAPEL_CARTON", "ELECTRONICO", "TEXTIL", "OTRO")
                    val finalCat = if (validCat.contains(categoriaId)) categoriaId else "PLASTICO"

                    onSuccess(titulo, descripcion, finalCat, pesoKg)
                } catch (jsonErr: Exception) {
                    Log.w("ECOMAP_AI", "Respuesta no es JSON válido, usando fallback texto plano", jsonErr)
                    onSuccess("Material Reciclable", resultText.ifBlank { userText }, "PLASTICO", 1.0)
                }
            } catch (e: Exception) {
                Log.e("ECOMAP_AI", "Error en draftPublicationWithAi", e)
                aiErrorMessage = mapAiError(e)
            } finally {
                isLoading = false
            }
        }
    }

    fun draftSupportClaimWithAi(
        userName: String,
        userRole: String,
        phone: String,
        userDetail: String,
        onSuccess: (draftText: String) -> Unit
    ) {
        viewModelScope.launch {
            isLoading = true
            aiErrorMessage = null
            try {
                val promptText = """
                    Eres el asistente de soporte técnico de ECO-MAP. Redacta un reclamo/mensaje de soporte formal y respetuoso en español basado en estos datos:
                    Usuario: ${userName.ifBlank { "Usuario ECO-MAP" }}
                    Rol: ${userRole.ifBlank { "CIUDADANO" }}
                    Contacto: ${phone.ifBlank { "No proporcionado" }}
                    Detalle del problema: ${userDetail.ifBlank { "Inconveniente en la plataforma" }}
                    Escribe solo el cuerpo del correo formal en texto plano.
                """.trimIndent()

                val resultText = withContext(Dispatchers.IO) {
                    generativeModel.generateContent(content { text(promptText) }).text ?: ""
                }

                if (resultText.isNotBlank()) {
                    onSuccess(resultText.trim())
                } else {
                    aiErrorMessage = "No se pudo generar el borrador."
                }
            } catch (e: Exception) {
                Log.e("ECOMAP_AI", "Error en draftSupportClaimWithAi", e)
                aiErrorMessage = mapAiError(e)
            } finally {
                isLoading = false
            }
        }
    }

    fun suggestProfileFieldWithAi(
        fieldKey: String,
        userRole: String,
        contextInfo: String,
        onSuccess: (suggestion: String) -> Unit
    ) {
        viewModelScope.launch {
            isLoading = true
            aiErrorMessage = null
            try {
                val promptText = when (fieldKey) {
                    "zona_cobertura" -> "Sugiere en una sola frase breve (máximo 10 palabras en español) una zona o vecindario de cobertura de recolección para un recolector."
                    "materiales_aceptados" -> "Sugiere en una lista corta de 3 a 5 palabras clave separadas por comas los materiales reciclables que acepta un centro de acopio (ej: Plástico PET, Vidrio, Cartón)."
                    "capacidad_kg" -> "Sugiere un número decimal estimado de capacidad de carga en kg para un vehículo tipo $contextInfo (devuelve solo el número o cifra en kg)."
                    else -> "Sugiere un valor sugerido breve en español para el campo $fieldKey de un usuario $userRole."
                }

                val resultText = withContext(Dispatchers.IO) {
                    generativeModel.generateContent(content { text(promptText) }).text ?: ""
                }

                if (resultText.isNotBlank()) {
                    onSuccess(resultText.trim())
                } else {
                    aiErrorMessage = "No se obtuvo una sugerencia."
                }
            } catch (e: Exception) {
                Log.e("ECOMAP_AI", "Error en suggestProfileFieldWithAi", e)
                aiErrorMessage = mapAiError(e)
            } finally {
                isLoading = false
            }
        }
    }

    private fun extractJsonString(text: String): String {
        val trimmed = text.trim()
        val startIndex = trimmed.indexOf('{')
        val endIndex = trimmed.lastIndexOf('}')
        return if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
            trimmed.substring(startIndex, endIndex + 1)
        } else {
            trimmed
        }
    }

    private fun mapAiError(e: Throwable): String {
        val msg = e.message ?: ""
        return when {
            msg.contains("UnknownHostException") || msg.contains("ConnectException") || msg.contains("Unable to resolve host") ->
                "El asistente necesita conexión a internet. Intenta nuevamente cuando tengas red."
            msg.contains("QuotaExceeded") || msg.contains("429") || msg.contains("RESOURCE_EXHAUSTED") ->
                "El servicio está ocupado (límite de cuota alcanzado). Intenta de nuevo en unos momentos."
            msg.contains("timeout") || msg.contains("SocketTimeoutException") ->
                "La respuesta tardó demasiado. Por favor reintenta."
            else ->
                "El asistente necesita conexión o no pudo procesar la solicitud. Reintenta más tarde."
        }
    }

    private fun uriToBitmap(context: Context, uri: Uri): Bitmap? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri))
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
            }
        } catch (e: Exception) {
            null
        }
    }
}
