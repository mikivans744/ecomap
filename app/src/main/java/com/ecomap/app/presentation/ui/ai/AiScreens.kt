package com.ecomap.app.presentation.ui.ai

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.rememberAsyncImagePainter
import com.airbnb.lottie.compose.*
import com.ecomap.app.BuildConfig
import com.ecomap.app.presentation.theme.EcoGreen
import com.ecomap.app.presentation.ui.auth.AnimatedButton
import com.ecomap.app.presentation.ui.auth.LottieLoading
import com.ecomap.app.util.CameraUtils

data class ChatMessage(
    val text: String,
    val isUser: Boolean,
    val id: String = java.util.UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis()
)

@Composable
fun MaterialClassifierScreen(onBack: () -> Unit = {}) {
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    val context = LocalContext.current
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (!success) imageUri = null
    }
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        imageUri = uri
    }

    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Clasificar material", style = MaterialTheme.typography.headlineMedium, color = Color.Black)
        
        AnimatedButton(onClick = { galleryLauncher.launch("image/*") }, modifier = Modifier.fillMaxWidth()) { Text("Elegir de galería") }
        AnimatedButton(onClick = {
            val uri = CameraUtils.createImageUri(context)
            imageUri = uri
            cameraLauncher.launch(uri)
        }, modifier = Modifier.fillMaxWidth()) { Text("Tomar fotografía") }

        imageUri?.let {
            Image(
                painter = rememberAsyncImagePainter(it),
                contentDescription = null,
                modifier = Modifier.size(200.dp)
            )
            AnimatedButton(onClick = { imageUri = null }, outlined = true) { Text("Repetir foto") }
        }

        if (imageUri != null) LottieLoading()
        
        Text("Resultado: ${if (imageUri != null) "Identificando plástico..." else "Pendiente de imagen"}", color = Color.Black)
        
        Spacer(Modifier.weight(1f))
        AnimatedButton(onClick = onBack, modifier = Modifier.fillMaxWidth(), outlined = true) { Text("Volver") }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssistantScreen(onBack: () -> Unit = {}, viewModel: AiViewModel = hiltViewModel()) {
    var message by remember { mutableStateOf("") }
    val botComposition by rememberLottieComposition(LottieCompositionSpec.Asset("lottie/asistente_bot.json"))
    val isBotPlaying = viewModel.isLoading
    val context = LocalContext.current
    val listState = rememberLazyListState()

    val speechAvailable = remember(context) { SpeechRecognizer.isRecognitionAvailable(context) }

    val chatMessages = remember {
        mutableStateListOf(
            ChatMessage("¡Hola! Soy tu Asistente ECO-MAP 🤖. ¿En qué te puedo ayudar hoy sobre reciclaje o el uso de la app?", isUser = false)
        )
    }

    var isListening by remember { mutableStateOf(false) }
    var voiceError by remember { mutableStateOf<String?>(null) }
    var showPermissionDialog by remember { mutableStateOf(false) }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startSpeechRecognition(context,
                onResult = { text -> message = text },
                onError = { err -> voiceError = err },
                onListeningState = { listening -> isListening = listening }
            )
        } else {
            showPermissionDialog = true
        }
    }

    LaunchedEffect(chatMessages.size, viewModel.isLoading) {
        if (chatMessages.isNotEmpty()) {
            listState.animateScrollToItem(chatMessages.size - 1)
        }
    }

    LaunchedEffect(viewModel.response) {
        if (viewModel.response.isNotBlank()) {
            val last = chatMessages.lastOrNull()
            if (last == null || last.text != viewModel.response) {
                chatMessages.add(ChatMessage(viewModel.response, isUser = false))
            }
        }
    }

    fun sendMessage(prompt: String) {
        if (prompt.isBlank()) return
        chatMessages.add(ChatMessage(prompt, isUser = true))
        message = ""
        viewModel.askGemini(prompt)
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.White
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    LottieAnimation(
                        composition = botComposition,
                        iterations = if (isBotPlaying) LottieConstants.IterateForever else 1,
                        modifier = Modifier.size(60.dp)
                    )
                    Text("Asistente", style = MaterialTheme.typography.labelMedium, color = Color(0xFF1B1B1F))
                }

                Column(Modifier.weight(1f)) {
                    Text("Asistente ECO-MAP", style = MaterialTheme.typography.titleLarge, color = Color(0xFF1B1B1F))
                    Text("Inteligencia Artificial para Reciclaje", style = MaterialTheme.typography.bodySmall, color = Color(0xFF4B5563))
                }

                IconButton(onClick = onBack) {
                    Text("❌", style = MaterialTheme.typography.titleMedium)
                }
            }

            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                SuggestionChip(
                    onClick = { sendMessage("Cómo publicar vidrio") },
                    label = { Text("🍾 Publicar vidrio", color = Color(0xFF1B1B1F), style = MaterialTheme.typography.labelSmall) },
                    border = BorderStroke(1.dp, Color(0xFFD1D5DB)),
                    colors = SuggestionChipDefaults.suggestionChipColors(containerColor = Color(0xFFF3F4F6))
                )
                SuggestionChip(
                    onClick = { sendMessage("Redactar un reclamo") },
                    label = { Text("📝 Redactar reclamo", color = Color(0xFF1B1B1F), style = MaterialTheme.typography.labelSmall) },
                    border = BorderStroke(1.dp, Color(0xFFD1D5DB)),
                    colors = SuggestionChipDefaults.suggestionChipColors(containerColor = Color(0xFFF3F4F6))
                )
                SuggestionChip(
                    onClick = { sendMessage("Completar mi perfil") },
                    label = { Text("👤 Completar perfil", color = Color(0xFF1B1B1F), style = MaterialTheme.typography.labelSmall) },
                    border = BorderStroke(1.dp, Color(0xFFD1D5DB)),
                    colors = SuggestionChipDefaults.suggestionChipColors(containerColor = Color(0xFFF3F4F6))
                )
            }

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(chatMessages, key = { it.id }) { msg ->
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn() + slideInVertically(initialOffsetY = { 20 })
                    ) {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = if (msg.isUser) Alignment.CenterEnd else Alignment.CenterStart
                        ) {
                            Surface(
                                color = if (msg.isUser) Color(0xFFDCF8C6) else Color(0xFFF2F2F7),
                                shape = RoundedCornerShape(16.dp),
                                shadowElevation = 1.dp,
                                modifier = Modifier.widthIn(max = 280.dp)
                            ) {
                                Text(
                                    text = msg.text,
                                    color = Color(0xFF1B1B1F),
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    }
                }

                if (viewModel.isLoading) {
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.primary)
                            Text("Asistente está escribiendo...", style = MaterialTheme.typography.bodySmall, color = Color(0xFF4B5563))
                        }
                    }
                }
            }

            if (BuildConfig.DEBUG && viewModel.debugTechnicalCause != null) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = viewModel.debugTechnicalCause!!,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }

            voiceError?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    placeholder = { Text(if (isListening) "Escuchando voz..." else "Escribe o habla...", color = Color(0xFF6B7280)) },
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color(0xFF1B1B1F),
                        unfocusedTextColor = Color(0xFF1B1B1F),
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = Color(0xFF9CA3AF)
                    ),
                    singleLine = true
                )

                IconButton(
                    onClick = {
                        if (!speechAvailable) {
                            Toast.makeText(context, "Tu dispositivo no incluye reconocedor de voz de Google", Toast.LENGTH_LONG).show()
                            return@IconButton
                        }
                        val hasPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                        if (hasPermission) {
                            startSpeechRecognition(context,
                                onResult = { text -> message = text },
                                onError = { err -> voiceError = err },
                                onListeningState = { listening -> isListening = listening }
                            )
                        } else {
                            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    },
                    enabled = speechAvailable
                ) {
                    Text(if (isListening) "🎙️" else "🎤", style = MaterialTheme.typography.headlineSmall)
                }

                Button(
                    onClick = { sendMessage(message) },
                    enabled = message.isNotBlank() && !viewModel.isLoading
                ) {
                    Text("Enviar")
                }
            }

            if (viewModel.aiErrorMessage != null) {
                Button(
                    onClick = { sendMessage(chatMessages.lastOrNull { it.isUser }?.text ?: "Hola") },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Reintentar")
                }
            }
        }
    }

    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            title = { Text("Permiso de Micrófono Requerido") },
            text = { Text("Para dictar mensajes con tu voz, ECO-MAP requiere acceso al micrófono. Puedes habilitarlo directamente desde los Ajustes de la aplicación.") },
            confirmButton = {
                TextButton(onClick = {
                    showPermissionDialog = false
                    try {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.parse("package:${context.packageName}")
                        }
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(context, "Abre Ajustes > Aplicaciones > ECO-MAP > Permisos", Toast.LENGTH_LONG).show()
                    }
                }) {
                    Text("Abrir Ajustes")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

private fun startSpeechRecognition(
    context: Context,
    onResult: (String) -> Unit,
    onError: (String) -> Unit,
    onListeningState: (Boolean) -> Unit
) {
    if (!SpeechRecognizer.isRecognitionAvailable(context)) {
        onError("Tu dispositivo no incluye reconocedor de voz de Google.")
        return
    }
    val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es")
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "es")
    }
    recognizer.setRecognitionListener(object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) { onListeningState(true) }
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() { onListeningState(false) }
        override fun onError(error: Int) {
            onListeningState(false)
            val msg = when (error) {
                SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No se escuchó voz. Intenta de nuevo."
                SpeechRecognizer.ERROR_NETWORK -> "Error de red al dictar."
                SpeechRecognizer.ERROR_AUDIO -> "Error en el micrófono."
                else -> "No se pudo reconocer la voz."
            }
            onError(msg)
            recognizer.destroy()
        }
        override fun onResults(results: Bundle?) {
            onListeningState(false)
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            if (!matches.isNullOrEmpty()) {
                onResult(matches[0])
            }
            recognizer.destroy()
        }
        override fun onPartialResults(partialResults: Bundle?) {}
        override fun onEvent(eventType: Int, params: Bundle?) {}
    })
    recognizer.startListening(intent)
}

@Composable
fun ReportsScreen(onBack: () -> Unit = {}) {
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Reportes de Impacto", style = MaterialTheme.typography.headlineMedium, color = Color.Black)
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Ventas realizadas: 0", color = Color.Black)
                Text("Materiales recolectados: 0 kg", color = Color.Black)
                Text("C02 ahorrado: 0 kg", color = EcoGreen, style = MaterialTheme.typography.titleMedium)
            }
        }
        Spacer(Modifier.weight(1f))
        AnimatedButton(onClick = onBack, modifier = Modifier.fillMaxWidth(), outlined = true) { Text("Volver") }
    }
}
