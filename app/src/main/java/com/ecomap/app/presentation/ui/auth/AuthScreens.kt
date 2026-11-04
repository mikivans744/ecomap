package com.ecomap.app.presentation.ui.auth

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ecomap.app.domain.model.RoleCode
import com.ecomap.app.domain.model.Usuario
import com.ecomap.app.util.PlateValidator
import com.ecomap.app.presentation.theme.EcoMapTheme
import com.ecomap.app.presentation.theme.EcoGreen
import com.airbnb.lottie.compose.*

@Composable
fun AnimatedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary,
    outlined: Boolean = false,
    content: @Composable RowScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.95f else 1f)

    if (outlined) {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier.scale(scale),
            interactionSource = interactionSource,
            content = content
        )
    } else {
        Button(
            onClick = onClick,
            modifier = modifier.scale(scale),
            interactionSource = interactionSource,
            colors = ButtonDefaults.buttonColors(containerColor = containerColor, contentColor = contentColor),
            content = content
        )
    }
}

@Composable
fun LottieLoading() {
    val composition by rememberLottieComposition(LottieCompositionSpec.Asset("lottie/carga_reciclaje.json"))
    LottieAnimation(
        composition = composition,
        iterations = LottieConstants.IterateForever,
        modifier = Modifier.size(100.dp)
    )
}

@Composable
fun LoginScreen(
    onRegister: () -> Unit = {},
    onRecover: () -> Unit = {},
    onLoginSuccess: () -> Unit = {},
    viewModel: AuthViewModel = hiltViewModel()
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    val state = viewModel.uiState
    val heroComposition by rememberLottieComposition(LottieCompositionSpec.Asset("lottie/splash_reciclaje.json"))

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = Color.Black,
        unfocusedTextColor = Color.Black,
        focusedLabelColor = MaterialTheme.colorScheme.primary,
        unfocusedLabelColor = Color.Gray
    )

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LottieAnimation(heroComposition, iterations = LottieConstants.IterateForever, modifier = Modifier.size(200.dp))
        
        Text("Iniciar sesión", style = MaterialTheme.typography.headlineMedium, color = Color.Black)
        
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Correo electrónico") },
            modifier = Modifier.fillMaxWidth(),
            colors = textFieldColors,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
        )
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Contraseña") },
            modifier = Modifier.fillMaxWidth(),
            colors = textFieldColors,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Text(if (passwordVisible) "👁️" else "🙈")
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
        )
        
        if (state is AuthUiState.Loading) {
            LottieLoading()
        } else {
            AnimatedButton(
                onClick = { if (email.isNotBlank() && password.isNotBlank()) viewModel.login(email, password, onLoginSuccess) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Ingresar")
            }
        }
        
        if (state is AuthUiState.Error) {
            Text(state.message, color = MaterialTheme.colorScheme.error)
        }

        AnimatedButton(onClick = onRegister, modifier = Modifier.fillMaxWidth(), outlined = true) { Text("Crear cuenta") }
        TextButton(onClick = onRecover, modifier = Modifier.fillMaxWidth()) { Text("¿Olvidaste tu contraseña?") }
    }
}

@Composable
fun RegisterScreen(
    onBack: () -> Unit = {},
    onRegisterCitizen: () -> Unit = {},
    onRegisterCollector: () -> Unit = {},
    onRegisterCenter: () -> Unit = {}
) {
    val heroComposition by rememberLottieComposition(LottieCompositionSpec.Asset("lottie/splash_reciclaje.json"))
    
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LottieAnimation(heroComposition, iterations = LottieConstants.IterateForever, modifier = Modifier.size(200.dp))
        
        Text("Crear cuenta", style = MaterialTheme.typography.headlineMedium, color = Color.Black)
        Text("Selecciona tu rol y completa tus datos para participar en ECO-MAP.", color = Color.DarkGray)
        
        AnimatedButton(onClick = onRegisterCitizen, modifier = Modifier.fillMaxWidth()) { Text("Registrarme como ciudadano") }
        AnimatedButton(onClick = onRegisterCollector, modifier = Modifier.fillMaxWidth()) { Text("Registrarme como recolector") }
        AnimatedButton(onClick = onRegisterCenter, modifier = Modifier.fillMaxWidth()) { Text("Registrarme como centro de acopio") }
        AnimatedButton(onClick = onBack, modifier = Modifier.fillMaxWidth(), outlined = true) { Text("Volver") }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterFormScreen(
    role: RoleCode,
    onBack: () -> Unit,
    onSuccess: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    var vehicleType by remember { mutableStateOf("AUTOMOVIL") }
    var plateNumber by remember { mutableStateOf("") }
    var vehicleDropdownExpanded by remember { mutableStateOf(false) }
    val vehicleTypes = listOf("MOTOCICLETA", "AUTOMOVIL", "CAMIONETA", "CAMION")

    var companyName by remember { mutableStateOf("") }
    var fixedAddress by remember { mutableStateOf("") }

    var validationError by remember { mutableStateOf<String?>(null) }
    val state = viewModel.uiState

    Column(
        Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Registro: ${role.name.lowercase().replace("_", " ")}", style = MaterialTheme.typography.headlineMedium, color = Color.Black)
        
        OutlinedTextField(
            value = name,
            onValueChange = { name = it; validationError = null },
            label = { Text("Nombre completo") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        OutlinedTextField(
            value = email,
            onValueChange = { email = it; validationError = null },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            singleLine = true
        )
        OutlinedTextField(
            value = password,
            onValueChange = { password = it; validationError = null },
            label = { Text("Contraseña") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Text(if (passwordVisible) "👁️" else "🙈")
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true
        )

        when (role) {
            RoleCode.RECOLECTOR -> {
                ExposedDropdownMenuBox(
                    expanded = vehicleDropdownExpanded,
                    onExpandedChange = { vehicleDropdownExpanded = !vehicleDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = vehicleType,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Tipo de vehículo") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = vehicleDropdownExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = vehicleDropdownExpanded,
                        onDismissRequest = { vehicleDropdownExpanded = false }
                    ) {
                        vehicleTypes.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type) },
                                onClick = {
                                    vehicleType = type
                                    vehicleDropdownExpanded = false
                                    validationError = null
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = plateNumber,
                    onValueChange = { plateNumber = it.uppercase(); validationError = null },
                    label = { Text("Placa del vehículo") },
                    placeholder = {
                        Text(if (vehicleType == "MOTOCICLETA") "Ej: 123-AB o 1234-ABC" else "Ej: 1234-ABC")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
            RoleCode.CENTRO_ACOPIO -> {
                OutlinedTextField(
                    value = companyName,
                    onValueChange = { companyName = it; validationError = null },
                    label = { Text("Nombre de la empresa") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = fixedAddress,
                    onValueChange = { fixedAddress = it; validationError = null },
                    label = { Text("Dirección fija") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
            else -> {}
        }

        validationError?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        if (state is AuthUiState.Loading) {
            LottieLoading()
        } else {
            AnimatedButton(
                onClick = {
                    if (name.isBlank()) {
                        validationError = "Ingresa tu nombre completo"
                        return@AnimatedButton
                    }
                    if (email.isBlank() || !email.contains("@")) {
                        validationError = "Ingresa un correo electrónico válido"
                        return@AnimatedButton
                    }
                    if (password.length < 6) {
                        validationError = "La contraseña debe tener al menos 6 caracteres"
                        return@AnimatedButton
                    }

                    if (role == RoleCode.RECOLECTOR) {
                        val (isValid, errorMsg) = PlateValidator.validate(vehicleType, plateNumber)
                        if (!isValid) {
                            validationError = errorMsg
                            return@AnimatedButton
                        }
                    }

                    if (role == RoleCode.CENTRO_ACOPIO) {
                        if (companyName.isBlank()) {
                            validationError = "Ingresa el nombre de la empresa"
                            return@AnimatedButton
                        }
                        if (fixedAddress.isBlank()) {
                            validationError = "Ingresa la dirección fija"
                            return@AnimatedButton
                        }
                    }

                    val extraUser = Usuario(
                        id = "",
                        email = email.trim(),
                        firebaseUid = "",
                        nombre = name.trim(),
                        rolId = role.name,
                        placaVehiculo = if (role == RoleCode.RECOLECTOR) plateNumber.trim() else null,
                        tipoVehiculo = if (role == RoleCode.RECOLECTOR) vehicleType else null,
                        nombreEmpresa = if (role == RoleCode.CENTRO_ACOPIO) companyName.trim() else null,
                        direccionFija = if (role == RoleCode.CENTRO_ACOPIO) fixedAddress.trim() else null
                    )

                    viewModel.registerWithDetails(extraUser, password, onSuccess)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Registrarme")
            }
        }
        
        if (state is AuthUiState.Error) {
            Text(state.message, color = MaterialTheme.colorScheme.error)
        }
        
        AnimatedButton(onClick = onBack, modifier = Modifier.fillMaxWidth(), outlined = true) { Text("Volver") }
    }
}

@Composable
fun RecoverPasswordScreen(onBack: () -> Unit = {}, viewModel: AuthViewModel = hiltViewModel()) {
    var email by remember { mutableStateOf("") }
    val state = viewModel.uiState

    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Recuperar contraseña", style = MaterialTheme.typography.headlineMedium, color = Color.Black)
        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Correo electrónico") }, modifier = Modifier.fillMaxWidth())
        
        if (state is AuthUiState.Loading) {
            LottieLoading()
        } else {
            AnimatedButton(onClick = { viewModel.resetPassword(email) }, modifier = Modifier.fillMaxWidth()) {
                Text("Enviar enlace")
            }
        }

        if (state is AuthUiState.Error) {
            Text(state.message, color = MaterialTheme.colorScheme.error)
        }
        
        if (state is AuthUiState.Success) {
            Text("Enlace enviado correctamente.", color = EcoGreen)
        }

        TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Volver") }
    }
}
