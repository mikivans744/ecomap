package com.ecomap.app.presentation.ui.auth

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ecomap.app.domain.model.RoleCode
import com.ecomap.app.presentation.ui.ai.AiViewModel
import com.ecomap.app.util.ProfileCompletionCalculator
import com.google.android.gms.location.LocationServices

@Composable
fun ProfileScreen(
    onBack: () -> Unit = {},
    onNavigateToSupport: () -> Unit = {},
    onNavigateToCompleteProfile: () -> Unit = {},
    onLogout: () -> Unit = {},
    viewModel: AuthViewModel = hiltViewModel()
) {
    val user = viewModel.currentUserData
    val context = LocalContext.current

    var showPasswordDialog by remember { mutableStateOf(false) }
    var currentPassInput by remember { mutableStateOf("") }
    var newPassInput by remember { mutableStateOf("") }
    var passError by remember { mutableStateOf<String?>(null) }
    var passSuccessMsg by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        viewModel.refreshUser()
    }

    val completionPercent = ProfileCompletionCalculator.calculate(user)

    Column(
        Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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

        SuggestionChip(
            onClick = onNavigateToCompleteProfile,
            label = { Text("Perfil $completionPercent% completado (Completar ahora)") },
            icon = { Text(if (completionPercent == 100) "✅" else "⚠️") },
            modifier = Modifier.fillMaxWidth()
        )

        if (user?.rolId == RoleCode.CENTRO_ACOPIO.name) {
            Text("Empresa: ${user.nombreEmpresa ?: "No registrada"}")
            Text("Dirección: ${user.direccionFija ?: "No registrada"}")
        }

        if (user?.rolId == RoleCode.RECOLECTOR.name) {
            Text("Vehículo: ${user.tipoVehiculo ?: "-"} (${user.placaVehiculo ?: "-"})")
            Text("Capacidad: ${user.capacidadKg ?: 0.0} kg")
        }

        AnimatedButton(onClick = onNavigateToCompleteProfile, modifier = Modifier.fillMaxWidth()) {
            Text("Completar mi perfil")
        }

        AnimatedButton(onClick = { showPasswordDialog = true }, modifier = Modifier.fillMaxWidth(), outlined = true) {
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

    if (showPasswordDialog) {
        AlertDialog(
            onDismissRequest = { showPasswordDialog = false; passError = null; passSuccessMsg = null },
            title = { Text("Cambiar contraseña") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Ingresa tu contraseña actual y la nueva contraseña:", style = MaterialTheme.typography.bodyMedium)
                    
                    OutlinedTextField(
                        value = currentPassInput,
                        onValueChange = { currentPassInput = it; passError = null; passSuccessMsg = null },
                        label = { Text("Contraseña actual") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = newPassInput,
                        onValueChange = { newPassInput = it; passError = null; passSuccessMsg = null },
                        label = { Text("Nueva contraseña") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    passError?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }

                    passSuccessMsg?.let {
                        Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
                    }

                    HorizontalDivider()

                    TextButton(
                        onClick = {
                            val userEmail = user?.email ?: ""
                            if (userEmail.isNotBlank()) {
                                viewModel.resetPassword(userEmail)
                                passSuccessMsg = "Correo enviado a $userEmail. Revisa tu bandeja de entrada."
                            } else {
                                passError = "No se encontró un correo válido en la sesión."
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("📧 Enviarme correo de restablecimiento")
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.changePassword(
                            currentPass = currentPassInput,
                            newPass = newPassInput,
                            onSuccess = {
                                Toast.makeText(context, "Contraseña actualizada con éxito", Toast.LENGTH_LONG).show()
                                showPasswordDialog = false
                                currentPassInput = ""
                                newPassInput = ""
                                passError = null
                                passSuccessMsg = null
                            },
                            onError = { err ->
                                passError = err
                            }
                        )
                    }
                ) {
                    Text("Cambiar ahora")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPasswordDialog = false; passError = null; passSuccessMsg = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun CompleteProfileScreen(
    onBack: () -> Unit = {},
    viewModel: AuthViewModel = hiltViewModel(),
    aiViewModel: AiViewModel = hiltViewModel()
) {
    val user = viewModel.currentUserData
    val context = LocalContext.current
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    val roleStr = user?.rolId ?: RoleCode.CIUDADANO.name
    val completionPercent = ProfileCompletionCalculator.calculate(user)
    val animatedProgress by animateFloatAsState(targetValue = completionPercent / 100f, label = "profileProgress")

    var phoneInput by remember(user?.telefono) { mutableStateOf(user?.telefono ?: "") }
    var capacityInput by remember(user?.capacidadKg) { mutableStateOf(user?.capacidadKg?.toString() ?: "") }
    var coverageInput by remember(user?.zonaCobertura) { mutableStateOf(user?.zonaCobertura ?: "") }
    var materialsInput by remember { mutableStateOf("PLASTICO, VIDRIO, METAL") }
    var gpsStatus by remember { mutableStateOf<String?>(null) }

    Column(
        Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Completar perfil", style = MaterialTheme.typography.headlineMedium)
        
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Avance del perfil: $completionPercent%", style = MaterialTheme.typography.titleMedium)
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier.fillMaxWidth().height(12.dp)
                )
            }
        }

        if (aiViewModel.isLoading) {
            LottieLoading()
        }

        aiViewModel.aiErrorMessage?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        Text("Requisitos pendientes para tu rol (${roleStr}):", style = MaterialTheme.typography.titleSmall)

        // Item 1: Teléfono de contacto
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("1. Teléfono de contacto", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    value = phoneInput,
                    onValueChange = { phoneInput = it },
                    label = { Text("Número de teléfono") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                )
                Button(
                    onClick = {
                        viewModel.updateProfile(mapOf("telefono" to phoneInput.trim()))
                        Toast.makeText(context, "Teléfono actualizado", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Guardar teléfono")
                }
            }
        }

        // Checklist por rol
        when (roleStr.uppercase()) {
            RoleCode.RECOLECTOR.name -> {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("2. Capacidad de carga (kg)", style = MaterialTheme.typography.titleMedium)
                            SuggestionChip(
                                onClick = {
                                    aiViewModel.suggestProfileFieldWithAi("capacidad_kg", roleStr, user?.tipoVehiculo ?: "AUTOMOVIL") { sug ->
                                        capacityInput = sug.filter { c -> c.isDigit() || c == '.' }
                                        Toast.makeText(context, "IA sugiere: $sug. Presiona 'Guardar' para aplicar.", Toast.LENGTH_LONG).show()
                                    }
                                },
                                label = { Text("✨ Ayuda IA") }
                            )
                        }
                        OutlinedTextField(
                            value = capacityInput,
                            onValueChange = { capacityInput = it },
                            label = { Text("Capacidad en kg") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        Button(
                            onClick = {
                                val cap = capacityInput.toDoubleOrNull() ?: 0.0
                                viewModel.updateProfile(mapOf("capacidad_kg" to cap))
                                Toast.makeText(context, "Capacidad actualizada", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Guardar capacidad")
                        }
                    }
                }

                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("3. Zona de cobertura", style = MaterialTheme.typography.titleMedium)
                            SuggestionChip(
                                onClick = {
                                    aiViewModel.suggestProfileFieldWithAi("zona_cobertura", roleStr, "") { sug ->
                                        coverageInput = sug
                                        Toast.makeText(context, "IA sugiere: $sug. Presiona 'Guardar' para aplicar.", Toast.LENGTH_LONG).show()
                                    }
                                },
                                label = { Text("✨ Ayuda IA") }
                            )
                        }
                        OutlinedTextField(
                            value = coverageInput,
                            onValueChange = { coverageInput = it },
                            label = { Text("Vecindario o zona de operación") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(
                            onClick = {
                                viewModel.updateProfile(mapOf("zona_cobertura" to coverageInput.trim()))
                                Toast.makeText(context, "Zona actualizada", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Guardar zona")
                        }
                    }
                }
            }
            RoleCode.CENTRO_ACOPIO.name, "CENTRO" -> {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("2. Ubicación GPS fija", style = MaterialTheme.typography.titleMedium)
                        gpsStatus?.let { Text(it, color = Color.Gray) }
                        Button(
                            onClick = {
                                try {
                                    fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                                        if (loc != null) {
                                            viewModel.updateProfile(mapOf("lat" to loc.latitude, "lon" to loc.longitude))
                                            gpsStatus = "GPS obtenido: ${loc.latitude}, ${loc.longitude}"
                                            Toast.makeText(context, "Ubicación GPS guardada", Toast.LENGTH_SHORT).show()
                                        } else {
                                            gpsStatus = "No se pudo obtener la ubicación GPS"
                                        }
                                    }
                                } catch (e: SecurityException) {
                                    gpsStatus = "Permiso GPS no concedido"
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Obtener mi ubicación GPS actual")
                        }
                    }
                }

                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("3. Materiales aceptados", style = MaterialTheme.typography.titleMedium)
                            SuggestionChip(
                                onClick = {
                                    aiViewModel.suggestProfileFieldWithAi("materiales_aceptados", roleStr, "") { sug ->
                                        materialsInput = sug
                                        Toast.makeText(context, "IA sugiere: $sug. Presiona 'Guardar' para aplicar.", Toast.LENGTH_LONG).show()
                                    }
                                },
                                label = { Text("✨ Ayuda IA") }
                            )
                        }
                        OutlinedTextField(
                            value = materialsInput,
                            onValueChange = { materialsInput = it },
                            label = { Text("Categorías (separadas por coma)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(
                            onClick = {
                                viewModel.updateProfile(mapOf("materiales_aceptados" to materialsInput.trim()))
                                Toast.makeText(context, "Materiales actualizados", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Guardar materiales")
                        }
                    }
                }
            }
            else -> { // CIUDADANO
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("2. Ubicación GPS habitual", style = MaterialTheme.typography.titleMedium)
                        gpsStatus?.let { Text(it, color = Color.Gray) }
                        Button(
                            onClick = {
                                try {
                                    fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                                        if (loc != null) {
                                            viewModel.updateProfile(mapOf("lat" to loc.latitude, "lon" to loc.longitude))
                                            gpsStatus = "GPS obtenido: ${loc.latitude}, ${loc.longitude}"
                                            Toast.makeText(context, "Ubicación GPS guardada", Toast.LENGTH_SHORT).show()
                                        } else {
                                            gpsStatus = "No se pudo obtener la ubicación GPS"
                                        }
                                    }
                                } catch (e: SecurityException) {
                                    gpsStatus = "Permiso GPS no concedido"
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Obtener mi ubicación GPS actual")
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("Finalizar y Volver")
        }
    }
}

@Composable
fun SupportScreen(
    onBack: () -> Unit = {},
    aiViewModel: AiViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    var phone by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val user = authViewModel.currentUserData

    Column(
        Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Soporte Técnico", style = MaterialTheme.typography.headlineMedium)
        Text("Se responderá en un plazo máximo de 48 horas. Por favor deje su número de celular para contactarlo.", color = Color.Gray)
        
        OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Celular") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Describe tu problema") }, modifier = Modifier.fillMaxWidth(), minLines = 3)

        AnimatedButton(
            onClick = {
                aiViewModel.draftSupportClaimWithAi(
                    userName = user?.nombre ?: "Usuario",
                    userRole = user?.rolId ?: "CIUDADANO",
                    phone = phone,
                    userDetail = description
                ) { draft ->
                    description = draft
                }
            },
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("✨ Redactar reclamo con IA")
        }

        if (aiViewModel.isLoading) {
            LottieLoading()
        }

        aiViewModel.aiErrorMessage?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

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
