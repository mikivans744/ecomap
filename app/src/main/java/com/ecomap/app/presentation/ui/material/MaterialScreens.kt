package com.ecomap.app.presentation.ui.material

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.ecomap.app.domain.model.*
import com.ecomap.app.util.CameraUtils
import com.ecomap.app.presentation.ui.auth.AnimatedButton
import com.ecomap.app.presentation.ui.auth.LottieLoading
import com.ecomap.app.presentation.ui.logistics.InteractionViewModel
import com.ecomap.app.presentation.ui.logistics.InteractionUiState
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublishMaterialScreen(
    onBack: () -> Unit = {},
    viewModel: MaterialViewModel = hiltViewModel()
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(MaterialCategory.PLASTICO) }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var categoryExpanded by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    val state = viewModel.uiState

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) Log.d("ECOMAP_PUB", "Foto capturada ok") else imageUri = null
    }

    Column(Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Publicar material", style = MaterialTheme.typography.headlineMedium, color = Color.Black)
        ExposedDropdownMenuBox(expanded = categoryExpanded, onExpandedChange = { categoryExpanded = !categoryExpanded }) {
            OutlinedTextField(value = selectedCategory.name, onValueChange = {}, readOnly = true, label = { Text("Categoría") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) }, modifier = Modifier.menuAnchor().fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.Black, unfocusedTextColor = Color.Black))
            ExposedDropdownMenu(expanded = categoryExpanded, onDismissRequest = { categoryExpanded = false }) {
                MaterialCategory.values().forEach { category ->
                    DropdownMenuItem(text = { Text(category.name) }, onClick = { selectedCategory = category; categoryExpanded = false })
                }
            }
        }
        OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Título") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Descripción") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = weight, onValueChange = { weight = it }, label = { Text("Peso (kg)") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
        if (imageUri != null) {
            AsyncImage(model = imageUri, contentDescription = null, modifier = Modifier.size(200.dp))
            AnimatedButton(onClick = { imageUri = null }, outlined = true) { Text("Quitar foto") }
        } else {
            AnimatedButton(onClick = { val uri = CameraUtils.createImageUri(context); imageUri = uri; cameraLauncher.launch(uri) }, modifier = Modifier.fillMaxWidth()) { Text("Tomar foto") }
        }
        if (state is MaterialUiState.Loading) LottieLoading() else {
            AnimatedButton(onClick = { viewModel.publish(title, description, weight, selectedCategory.name, imageUri, onBack) }, modifier = Modifier.fillMaxWidth()) { Text("Publicar") }
        }
        if (state is MaterialUiState.Error) Text(state.message, color = Color.Red)
        TextButton(onClick = onBack) { Text("Cancelar") }
    }
}

@Composable
fun MaterialsListScreen() {
    val uid = FirebaseAuth.getInstance().currentUser?.uid
    var publications by remember { mutableStateOf<List<Publicacion>>(emptyList()) }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        FirebaseFirestore.getInstance().collection("publicaciones")
            .whereEqualTo("usuarioId", uid)
            .addSnapshotListener { snapshot, _ ->
                val list = snapshot?.documents?.mapNotNull { doc ->
                    Publicacion(
                        id = doc.id,
                        usuarioId = doc.getString("usuarioId") ?: "",
                        categoriaId = doc.getString("categoriaId") ?: "",
                        estadoId = doc.getString("estado") ?: "DISPONIBLE",
                        titulo = doc.getString("titulo") ?: "",
                        descripcion = doc.getString("descripcion") ?: "",
                        pesoKg = doc.getDouble("pesoKg") ?: 0.0,
                        precioSugerido = doc.getDouble("precioSugerido") ?: 0.0,
                        calidad = doc.getString("calidad") ?: "",
                        imagenUrl = doc.getString("imagenUrl"),
                        lat = doc.getDouble("lat") ?: 0.0,
                        lon = doc.getDouble("lon") ?: 0.0
                    )
                } ?: emptyList()
                publications = list
            }
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Mis Publicaciones", style = MaterialTheme.typography.headlineMedium)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(publications) { pub ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text(pub.titulo, style = MaterialTheme.typography.titleLarge)
                        Text("${pub.categoriaId} · ${pub.pesoKg} kg")
                        Text("Estado: ${pub.estadoId}")
                        Row(Modifier.padding(top = 8.dp)) {
                            AnimatedButton(onClick = {
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    action = Intent.ACTION_SEND
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, "Ubicación de mi publicación: https://maps.google.com/?q=${pub.lat},${pub.lon}")
                                }
                                context.startActivity(Intent.createChooser(intent, "Compartir ubicación"))
                            }) { Text("Compartir ubicación") }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfferToCenterScreen(
    onBack: () -> Unit = {},
    viewModel: InteractionViewModel = hiltViewModel()
) {
    var step by remember { mutableStateOf(1) }
    var selectedCenter by remember { mutableStateOf<Usuario?>(null) }
    var centers by remember { mutableStateOf<List<Usuario>>(emptyList()) }
    
    var weight by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(MaterialCategory.VIDRIO) }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var categoryExpanded by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val state = viewModel.uiState

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (!success) imageUri = null
    }

    LaunchedEffect(Unit) {
        FirebaseFirestore.getInstance().collection("usuarios")
            .whereEqualTo("rol", RoleCode.CENTRO_ACOPIO.name)
            .get().addOnSuccessListener { snapshot ->
                centers = snapshot.documents.mapNotNull { doc ->
                    Usuario(id = doc.id, email = "", firebaseUid = doc.id, nombre = doc.getString("nombre") ?: "", rolId = "", nombreEmpresa = doc.getString("nombre_empresa"))
                }
            }
    }

    if (step == 1) {
        Column(Modifier.fillMaxSize().padding(24.dp)) {
            Text("Selecciona un Centro", style = MaterialTheme.typography.headlineMedium)
            LazyColumn {
                items(centers) { center ->
                    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { selectedCenter = center; step = 2 }) {
                        Column(Modifier.padding(16.dp)) {
                            Text(center.nombreEmpresa ?: center.nombre, style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }
            TextButton(onClick = onBack) { Text("Atrás") }
        }
    } else {
        Column(Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Ofertar a ${selectedCenter?.nombreEmpresa}", style = MaterialTheme.typography.headlineMedium, color = Color.Black)
            ExposedDropdownMenuBox(expanded = categoryExpanded, onExpandedChange = { categoryExpanded = !categoryExpanded }) {
                OutlinedTextField(value = selectedCategory.name, onValueChange = {}, readOnly = true, label = { Text("Categoría") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) }, modifier = Modifier.menuAnchor().fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.Black, unfocusedTextColor = Color.Black))
                ExposedDropdownMenu(expanded = categoryExpanded, onDismissRequest = { categoryExpanded = false }) {
                    MaterialCategory.values().forEach { category ->
                        DropdownMenuItem(text = { Text(category.name) }, onClick = { selectedCategory = category; categoryExpanded = false })
                    }
                }
            }
            OutlinedTextField(value = weight, onValueChange = { weight = it }, label = { Text("Peso total (kg)") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            OutlinedTextField(value = price, onValueChange = { price = it }, label = { Text("Precio sugerido") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            if (imageUri != null) {
                AsyncImage(model = imageUri, contentDescription = null, modifier = Modifier.size(200.dp))
                AnimatedButton(onClick = { imageUri = null }, outlined = true) { Text("Cambiar foto") }
            } else {
                AnimatedButton(onClick = { val uri = CameraUtils.createImageUri(context); imageUri = uri; cameraLauncher.launch(uri) }, modifier = Modifier.fillMaxWidth()) { Text("Foto de la carga") }
            }
            if (state is InteractionUiState.Loading) LottieLoading() else {
                AnimatedButton(onClick = { viewModel.makeOfferToCenter(selectedCenter!!.id, selectedCategory.name, weight, price, imageUri, onBack) }, modifier = Modifier.fillMaxWidth()) { Text("Enviar oferta") }
            }
            if (state is InteractionUiState.Error) Text(state.message, color = Color.Red)
            TextButton(onClick = { step = 1 }) { Text("Volver a centros") }
        }
    }
}

@Composable
fun NegotiationScreen() { /* ... */ }
