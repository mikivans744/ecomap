package com.ecomap.app.presentation.ui.auth

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ecomap.app.domain.model.RoleCode

@Composable
fun ProfileScreen(
    onBack: () -> Unit = {},
    onNavigateToSupport: () -> Unit = {},
    onLogout: () -> Unit = {},
    viewModel: AuthViewModel = hiltViewModel()
) {
    val user = viewModel.currentUserData
    val state = viewModel.uiState
    val context = LocalContext.current

    Column(
        Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text(user?.nombre ?: "Usuario", style = MaterialTheme.typography.headlineSmall)
                Text(user?.email ?: "", style = MaterialTheme.typography.bodyMedium)
                AssistChip(onClick = {}, label = { Text(user?.rolId ?: "") })
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Reputación: ${user?.reputacion ?: 0.0} ⭐")
                    Spacer(Modifier.width(16.dp))
                    Text("Puntos: ${user?.puntos ?: 0}")
                }
            }
        }

        if (user?.rolId == RoleCode.CENTRO_ACOPIO.name) {
            Text("Empresa: ${user.nombreEmpresa ?: "No registrada"}")
            Text("Dirección: ${user.direccionFija ?: "No registrada"}")
        }

        if (user?.rolId == RoleCode.RECOLECTOR.name) {
            Text("Vehículo: ${user.tipoVehiculo ?: "-"} (${user.placaVehiculo ?: "-"})")
            Text("Capacidad: ${user.capacidadKg ?: 0.0} kg")
        }

        AnimatedButton(onClick = { /* GPS Logic */ }, modifier = Modifier.fillMaxWidth()) {
            Text("Actualizar mi ubicación")
        }

        AnimatedButton(onClick = { viewModel.resetPassword(user?.email ?: "") }, modifier = Modifier.fillMaxWidth(), outlined = true) {
            Text("Cambiar contraseña")
        }

        AnimatedButton(onClick = onNavigateToSupport, modifier = Modifier.fillMaxWidth(), outlined = true) {
            Text("Contactar soporte técnico")
        }

        Spacer(Modifier.weight(1f))
        
        AnimatedButton(onClick = { viewModel.logout(onLogout) }, containerColor = Color.Red, modifier = Modifier.fillMaxWidth()) {
            Text("Cerrar sesión")
        }
        
        TextButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text("Volver")
        }
    }
}

@Composable
fun SupportScreen(onBack: () -> Unit = {}) {
    var phone by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Soporte Técnico", style = MaterialTheme.typography.headlineMedium)
        Text("Se responderá en un plazo máximo de 48 horas. Por favor deje su número de celular para contactarlo.", color = Color.Gray)
        
        OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Celular") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Describe tu problema") }, modifier = Modifier.fillMaxWidth(), minLines = 3)

        AnimatedButton(onClick = {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:mikivans744@hotmail.com")
                putExtra(Intent.EXTRA_SUBJECT, "Soporte ECO-MAP")
                putExtra(Intent.EXTRA_TEXT, "Celular: $phone\n\n$description")
            }
            try {
                context.startActivity(intent)
            } catch (e: Exception) {
                clipboard.setText(AnnotatedString("mikivans744@hotmail.com"))
                Toast.makeText(context, "Email copiado al portapapeles", Toast.LENGTH_SHORT).show()
            }
        }, modifier = Modifier.fillMaxWidth()) {
            Text("Enviar correo")
        }

        TextButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text("Cancelar")
        }
    }
}
