package com.ecomap.app.presentation.ui.ai

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.rememberAsyncImagePainter
import com.ecomap.app.util.CameraUtils
import com.ecomap.app.presentation.ui.auth.AnimatedButton
import com.ecomap.app.presentation.ui.auth.LottieLoading
import com.airbnb.lottie.compose.*

import com.ecomap.app.presentation.theme.EcoGreen

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

@Composable
fun AssistantScreen(onBack: () -> Unit = {}, viewModel: AiViewModel = hiltViewModel()) {
    var message by remember { mutableStateOf("") }
    val botComposition by rememberLottieComposition(LottieCompositionSpec.Asset("lottie/asistente_bot.json"))
    val isBotPlaying = viewModel.isLoading

    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        LottieAnimation(
            composition = botComposition,
            iterations = if (isBotPlaying) LottieConstants.IterateForever else 1,
            modifier = Modifier.size(150.dp)
        )
        
        Text("Asistente ECO-MAP", style = MaterialTheme.typography.headlineMedium, color = Color.Black)
        
        Card(modifier = Modifier.fillMaxWidth().weight(1f)) {
            Box(Modifier.padding(16.dp)) {
                Text(if (viewModel.response.isEmpty()) "Hola, soy tu asistente de reciclaje. ¿En qué puedo ayudarte hoy?" else viewModel.response, color = Color.Black)
            }
        }
        
        OutlinedTextField(
            value = message, 
            onValueChange = { message = it }, 
            label = { Text("Escribe o usa tu voz") },
            modifier = Modifier.fillMaxWidth()
        )
        
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AnimatedButton(onClick = { viewModel.askGemini(message); message = "" }, modifier = Modifier.weight(1f)) { Text("Enviar") }
            IconButton(onClick = { /* Speech recognition placeholder */ }) {
                Text("🎤", style = MaterialTheme.typography.headlineSmall)
            }
        }
        
        AnimatedButton(onClick = onBack, modifier = Modifier.fillMaxWidth(), outlined = true) { Text("Volver") }
    }
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
