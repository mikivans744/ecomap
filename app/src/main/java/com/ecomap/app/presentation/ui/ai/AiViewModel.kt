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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject

private const val MODEL_ID = "gemini-3.8-flash"
private val MODEL_FALLBACK_LIST = listOf("gemini-3.8-flash", "gemini-3.8-flash-lite", "gemini-2.5-flash")

@HiltViewModel
class AiViewModel @Inject constructor() : ViewModel() {

    private fun getGenerativeModel(modelName: String = MODEL_ID): GenerativeModel {
        val apiKey = BuildConfig.GEMINI_API_KEY.ifBlank { "REEMPLAZAR" }
        return GenerativeModel(
            modelName = modelName,
            apiKey = apiKey,
            generationConfig = generationConfig {
                temperature = 0.2f
                maxOutputTokens = 600
            }
        )
    }

    var response by mutableStateOf("")
        private set
    
    var isLoading by mutableStateOf(false)
        private set

    var aiErrorMessage by mutableStateOf<String?>(null)
        private set

    var debugTechnicalCause by mutableStateOf<String?>(null)
        private set

    private fun checkApiKeyConfigured(): Boolean {
        val key = BuildConfig.GEMINI_API_KEY
        if (key.isBlank() || key.startsWith("REEMPLAZAR")) {
            val msg = "Falta configurar GEMINI_API_KEY en local.properties"
            aiErrorMessage = msg
            response = msg
            debugTechnicalCause = "GEMINI_API_KEY no definida o con valor placeholder en BuildConfig"
            return false
        }
        return true
    }

    fun askGemini(prompt: String) {
        if (prompt.isBlank()) return
        if (!checkApiKeyConfigured()) return

        viewModelScope.launch {
            isLoading = true
            aiErrorMessage = null
            debugTechnicalCause = null
            try {
                val resultText = executeWithRetriesAndFallbacks("Eres un asistente de reciclaje para la app ECO-MAP. Responde de forma sencilla y en español: $prompt")
                response = resultText.ifBlank { "No pude entender eso, ¿puedes repetir?" }
            } catch (e: Exception) {
                Log.e("ECOMAP_IA", "Excepción completa en askGemini", e)
                val userMsg = mapAiError(e)
                aiErrorMessage = userMsg
                response = userMsg
                if (BuildConfig.DEBUG) {
                    debugTechnicalCause = "Debug [${e.javaClass.simpleName}]: ${e.localizedMessage ?: e.message}"
                }
            } finally {
                isLoading = false
            }
        }
    }

    private suspend fun executeWithRetriesAndFallbacks(promptText: String, bitmap: Bitmap? = null): String = withContext(Dispatchers.IO) {
        var lastException: Exception? = null

        for (modelName in MODEL_FALLBACK_LIST) {
            val model = getGenerativeModel(modelName)
            val contentObj = content {
                bitmap?.let { image(it) }
                text(promptText)
            }

            for (attempt in 0..2) {
                try {
                    val res = model.generateContent(contentObj).text ?: ""
                    if (res.isNotBlank()) {
                        return@withContext res
                    }
                } catch (e: Exception) {
                    lastException = e
                    Log.w("ECOMAP_IA", "Fallo modelo $modelName (intento $attempt): ${e.message}", e)

                    val is503 = e.message?.contains("503") == true || e.javaClass.simpleName.contains("ServerException") || e.message?.contains("UNAVAILABLE") == true
                    if (is503) {
                        withContext(Dispatchers.Main) {
                            aiErrorMessage = "La IA tiene alta demanda ahora (503). Reintentando…"
                        }
                    }

                    if (attempt < 2) {
                        val backoff = if (attempt == 0) 1500L else 3000L
                        delay(backoff)
                    }
                }
            }
        }

        throw lastException ?: Exception("No se pudo obtener respuesta de la IA.")
    }

    fun draftPublicationWithAi(
        context: Context,
        imageUri: Uri?,
        userText: String,
        onSuccess: (titulo: String, descripcion: String, categoriaId: String, pesoKg: Double) -> Unit
    ) {
        if (!checkApiKeyConfigured()) return

        viewModelScope.launch {
            isLoading = true
            aiErrorMessage = null
            debugTechnicalCause = null
            try {
                val bitmap: Bitmap? = imageUri?.let { uriToBitmap(context, it) }
                val promptText = """
                    Eres un asistente de reciclaje para la app ECO-MAP para el rol CIUDADANO.
                    Analiza la imagen o el siguiente texto del usuario: "${userText.ifBlank { "Material reciclable" }}".
                    Genera una recomendación de publicación en formato JSON estricto sin comillas triples ni sintaxis markdown:
                    {"titulo": "...", "descripcion": "...", "categoriaId": "PLASTICO", "pesoKgEstimado": 1.0}
                    Categorías válidas: PLASTICO, METAL, VIDRIO, PAPEL_CARTON, ELECTRONICO, TEXTIL, OTRO.
                """.trimIndent()

                val resultText = executeWithRetriesAndFallbacks(promptText, bitmap)

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
                    Log.w("ECOMAP_IA", "Respuesta no es JSON válido, usando fallback texto plano", jsonErr)
                    onSuccess("Material Reciclable", resultText.ifBlank { userText }, "PLASTICO", 1.0)
                }
            } catch (e: Exception) {
                Log.e("ECOMAP_IA", "Excepción completa en draftPublicationWithAi", e)
                aiErrorMessage = mapAiError(e)
                if (BuildConfig.DEBUG) {
                    debugTechnicalCause = "Debug [${e.javaClass.simpleName}]: ${e.localizedMessage ?: e.message}"
                }
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
        if (!checkApiKeyConfigured()) return

        viewModelScope.launch {
            isLoading = true
            aiErrorMessage = null
            debugTechnicalCause = null
            try {
                val promptText = """
                    Eres el asistente de soporte técnico de ECO-MAP. Redacta un reclamo/mensaje de soporte formal y respetuoso en español basado en estos datos:
                    Usuario: ${userName.ifBlank { "Usuario ECO-MAP" }}
                    Rol: ${userRole.ifBlank { "CIUDADANO" }}
                    Contacto: ${phone.ifBlank { "No proporcionado" }}
                    Detalle del problema: ${userDetail.ifBlank { "Inconveniente en la plataforma" }}
                    Escribe solo el cuerpo del correo formal en texto plano.
                """.trimIndent()

                val resultText = executeWithRetriesAndFallbacks(promptText)

                if (resultText.isNotBlank()) {
                    onSuccess(resultText.trim())
                } else {
                    aiErrorMessage = "No se pudo generar el borrador."
                }
            } catch (e: Exception) {
                Log.e("ECOMAP_IA", "Excepción completa en draftSupportClaimWithAi", e)
                aiErrorMessage = mapAiError(e)
                if (BuildConfig.DEBUG) {
                    debugTechnicalCause = "Debug [${e.javaClass.simpleName}]: ${e.localizedMessage ?: e.message}"
                }
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
        if (!checkApiKeyConfigured()) return

        viewModelScope.launch {
            isLoading = true
            aiErrorMessage = null
            debugTechnicalCause = null
            try {
                val promptText = when (fieldKey) {
                    "zona_cobertura" -> "Sugiere en una sola frase breve (máximo 10 palabras en español) una zona o vecindario de cobertura de recolección para un recolector."
                    "materiales_aceptados" -> "Sugiere en una lista corta de 3 a 5 palabras clave separadas por comas los materiales reciclables que acepta un centro de acopio (ej: Plástico PET, Vidrio, Cartón)."
                    "capacidad_kg" -> "Sugiere un número decimal estimado de capacidad de carga en kg para un vehículo tipo $contextInfo (devuelve solo el número o cifra en kg)."
                    else -> "Sugiere un valor sugerido breve en español para el campo $fieldKey de un usuario $userRole."
                }

                val resultText = executeWithRetriesAndFallbacks(promptText)

                if (resultText.isNotBlank()) {
                    onSuccess(resultText.trim())
                } else {
                    aiErrorMessage = "No se obtuvo una sugerencia."
                }
            } catch (e: Exception) {
                Log.e("ECOMAP_IA", "Excepción completa en suggestProfileFieldWithAi", e)
                aiErrorMessage = mapAiError(e)
                if (BuildConfig.DEBUG) {
                    debugTechnicalCause = "Debug [${e.javaClass.simpleName}]: ${e.localizedMessage ?: e.message}"
                }
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
        val className = e.javaClass.simpleName
        return when {
            e is UnknownHostException || e is ConnectException || e is SocketTimeoutException || e is IOException ||
            msg.contains("UnknownHostException") || msg.contains("ConnectException") || msg.contains("Unable to resolve host") ->
                "Sin conexión a internet."
            msg.contains("401") || msg.contains("403") || msg.contains("API_KEY_INVALID") || msg.contains("PERMISSION_DENIED") ->
                "Clave de API inválida (revisa local.properties)"
            msg.contains("429") || msg.contains("RESOURCE_EXHAUSTED") || msg.contains("QuotaExceeded") ->
                "Cuota gratuita excedida, intenta en unos minutos"
            msg.contains("503") || className.contains("ServerException") || msg.contains("UNAVAILABLE") ->
                "IA con alta demanda, intenta en unos minutos"
            else ->
                "Sin conexión a internet."
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
