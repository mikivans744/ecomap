package com.ecomap.app.presentation.ui.auth

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
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
    val state = viewModel.uiState

    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Registro: ${role.name.lowercase()}", style = MaterialTheme.typography.headlineMedium, color = Color.Black)
        
        OutlinedTextField(name, { name = it }, label = { Text("Nombre completo") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(email, { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Contraseña") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Text(if (passwordVisible) "👁️" else "🙈")
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
        )

        if (state is AuthUiState.Loading) {
            LottieLoading()
        } else {
            AnimatedButton(
                onClick = { if (email.isNotBlank() && password.length >= 6) viewModel.register(email, password, name, role, onSuccess) },
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
