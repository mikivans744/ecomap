package com.ecomap.app.presentation.ui.logistics

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun RequestPickupScreen(onBack: () -> Unit = {}) {
    var address by remember { mutableStateOf("") }
    val textFieldColors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.Black, unfocusedTextColor = Color.Black)

    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Solicitar recolección", style = MaterialTheme.typography.headlineMedium, color = Color.Black)
        OutlinedTextField(
            value = address, 
            onValueChange = { address = it }, 
            label = { Text("Dirección de recolección") },
            modifier = Modifier.fillMaxWidth(),
            colors = textFieldColors
        )
        Text("Estado: SOLICITADA", color = Color.Blue)
        Button(onClick = { /* ViewModel */ }, modifier = Modifier.fillMaxWidth()) { Text("Solicitar recolector cercano") }
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Volver") }
    }
}

@Composable
fun ActiveRouteScreen(onBack: () -> Unit = {}) {
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Ruta activa", style = MaterialTheme.typography.headlineMedium, color = Color.Black)
        Text("Estado: EN_RUTA", color = Color.Green)
        Text("Sigue las indicaciones para llegar al punto de recolección.", color = Color.DarkGray)
        Button(onClick = { /* ViewModel */ }, modifier = Modifier.fillMaxWidth()) { Text("Marcar material recolectado") }
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Volver") }
    }
}

@Composable
fun RegisterDeliveryScreen(onBack: () -> Unit = {}) {
    val textFieldColors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.Black, unfocusedTextColor = Color.Black)

    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Registrar entrega", style = MaterialTheme.typography.headlineMedium, color = Color.Black)
        Text("Confirma el peso y adjunta evidencia fotográfica.", color = Color.DarkGray)
        OutlinedTextField(
            value = "", 
            onValueChange = {}, 
            label = { Text("Peso recibido en kg") },
            modifier = Modifier.fillMaxWidth(),
            colors = textFieldColors
        )
        Button(onClick = { /* Cámara */ }, modifier = Modifier.fillMaxWidth()) { Text("Adjuntar fotografía") }
        Button(onClick = { /* ViewModel */ }, modifier = Modifier.fillMaxWidth()) { Text("Confirmar entrega") }
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Volver") }
    }
}
