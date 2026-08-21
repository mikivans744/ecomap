package com.ecomap.app.presentation.ui.ai

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ecomap.app.BuildConfig
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AiViewModel @Inject constructor() : ViewModel() {

    private val generativeModel = GenerativeModel(
        modelName = "gemini-1.5-flash",
        apiKey = BuildConfig.GEMINI_API_KEY
    )

    var response by mutableStateOf("")
        private set
    
    var isLoading by mutableStateOf(false)
        private set

    fun askGemini(prompt: String) {
        if (prompt.isBlank()) return
        viewModelScope.launch {
            isLoading = true
            try {
                val result = generativeModel.generateContent(content {
                    text("Eres un asistente de reciclaje para la app ECO-MAP. Responde de forma sencilla y en español: $prompt")
                })
                response = result.text ?: "No pude entender eso, ¿puedes repetir?"
            } catch (e: Exception) {
                response = "Lo siento, ahora estoy fuera de línea. FAQ: Para publicar toca el botón + en el mapa."
            } finally {
                isLoading = false
            }
        }
    }
}
