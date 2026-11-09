package com.ecomap.app.presentation.ui.material

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.ecomap.app.domain.model.*
import com.ecomap.app.util.CameraUtils
import com.ecomap.app.util.CategoryUtils
import com.ecomap.app.util.NetworkUtils
import com.ecomap.app.util.OfflineBanner
import com.ecomap.app.util.PublicationCardSkeleton
import com.ecomap.app.presentation.ui.auth.AnimatedButton
import com.ecomap.app.presentation.ui.auth.LottieLoading
import com.ecomap.app.presentation.ui.ai.AiViewModel
import com.ecomap.app.presentation.ui.logistics.InteractionViewModel
import com.ecomap.app.presentation.ui.logistics.InteractionUiState
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublishMaterialScreen(
    onBack: () -> Unit = {},
    viewModel: MaterialViewModel = hiltViewModel(),
    aiViewModel: AiViewModel = hiltViewModel()
) {
    val categories by viewModel.categories.collectAsState()
    var selectedCategoryCode by remember { mutableStateOf(categories.firstOrNull()?.codigo ?: "PLASTICO") }
    var selectedCategoryDisplay by remember { mutableStateOf(categories.firstOrNull()?.nombreMostrar ?: "Plástico") }
    
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var categoryExpanded by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    val state = viewModel.uiState

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) Log.d("ECOMAP_PUB", "Foto capturada ok") else imageUri = null
    }

    Column(Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Publicar material", style = MaterialTheme.typography.headlineMedium, color = Color(0xFF111827))
        
        AnimatedButton(
            onClick = {
                aiViewModel.draftPublicationWithAi(
                    context = context,
                    imageUri = imageUri,
                    userText = description.ifBlank { title }
                ) { newTitle, newDesc, newCat, newWeight ->
                    title = newTitle
                    description = newDesc
                    weight = newWeight.toString()
                    selectedCategoryCode = newCat
                    selectedCategoryDisplay = CategoryUtils.getDisplayName(newCat, categories)
                }
            },
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("✨ Redactar con IA")
        }

        if (aiViewModel.isLoading) {
            LottieLoading()
        }

        aiViewModel.aiErrorMessage?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        ExposedDropdownMenuBox(expanded = categoryExpanded, onExpandedChange = { categoryExpanded = !categoryExpanded }) {
            OutlinedTextField(
                value = selectedCategoryDisplay,
                onValueChange = {},
                readOnly = true,
                label = { Text("Categoría") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color(0xFF111827), unfocusedTextColor = Color(0xFF111827))
            )
            ExposedDropdownMenu(expanded = categoryExpanded, onDismissRequest = { categoryExpanded = false }) {
                categories.forEach { category ->
                    DropdownMenuItem(
                        text = { Text(category.nombreMostrar.ifBlank { category.nombre }) },
                        onClick = {
                            selectedCategoryCode = category.codigo
                            selectedCategoryDisplay = category.nombreMostrar.ifBlank { category.nombre }
                            categoryExpanded = false
                        }
                    )
                }
            }
        }
        OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Título") }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color(0xFF111827), unfocusedTextColor = Color(0xFF111827)))
        OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Descripción") }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color(0xFF111827), unfocusedTextColor = Color(0xFF111827)))
        OutlinedTextField(value = weight, onValueChange = { weight = it }, label = { Text("Peso (kg)") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color(0xFF111827), unfocusedTextColor = Color(0xFF111827)))
        if (imageUri != null) {
            AsyncImage(model = imageUri, contentDescription = null, modifier = Modifier.size(200.dp))
            AnimatedButton(onClick = { imageUri = null }, outlined = true) { Text("Quitar foto") }
        } else {
            AnimatedButton(onClick = { val uri = CameraUtils.createImageUri(context); imageUri = uri; cameraLauncher.launch(uri) }, modifier = Modifier.fillMaxWidth()) { Text("Tomar foto") }
        }
        if (state is MaterialUiState.Loading) LottieLoading() else {
            AnimatedButton(onClick = { viewModel.publish(title, description, weight, selectedCategoryCode, imageUri, onBack) }, modifier = Modifier.fillMaxWidth()) { Text("Publicar") }
        }
        if (state is MaterialUiState.Error) Text(state.message, color = Color.Red)
        TextButton(onClick = onBack) { Text("Cancelar") }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaterialsListScreen(
    viewModel: MaterialViewModel = hiltViewModel()
) {
    val uid = FirebaseAuth.getInstance().currentUser?.uid
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    val categories by viewModel.categories.collectAsState()
    val allPublications by viewModel.publications.collectAsState(initial = emptyList())
    val userPublications = remember(allPublications, uid) {
        if (uid.isNullOrBlank()) allPublications else allPublications.filter { it.usuarioId == uid || it.usuarioId.isEmpty() }
    }

    var isRefreshing by remember { mutableStateOf(false) }
    var expandedPubId by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize()) {
        OfflineBanner(visible = !NetworkUtils.isNetworkAvailable(context))

        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                coroutineScope.launch {
                    isRefreshing = true
                    delay(1000)
                    isRefreshing = false
                }
            },
            modifier = Modifier.fillMaxSize()
        ) {
            Column(Modifier.fillMaxSize().padding(16.dp)) {
                Text("Mis Publicaciones", style = MaterialTheme.typography.headlineMedium, color = Color(0xFF111827))
                Spacer(Modifier.height(12.dp))

                if (userPublications.isEmpty() && isRefreshing) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        repeat(3) { PublicationCardSkeleton() }
                    }
                } else if (userPublications.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No tienes publicaciones registradas.", color = Color(0xFF6B7280))
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(userPublications) { pub ->
                            val isExpanded = expandedPubId == pub.id
                            val animatedElevation by animateDpAsState(targetValue = if (isExpanded) 8.dp else 2.dp, label = "cardElevation")
                            val catDisplayName = remember(pub.categoriaId, categories) {
                                CategoryUtils.getDisplayName(pub.categoriaId, categories)
                            }

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { expandedPubId = if (isExpanded) null else pub.id },
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(defaultElevation = animatedElevation)
                            ) {
                                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(pub.titulo, style = MaterialTheme.typography.titleLarge, color = Color(0xFF111827), fontWeight = FontWeight.Bold)
                                        AssistChip(
                                            onClick = {},
                                            label = { Text(catDisplayName) },
                                            colors = AssistChipDefaults.assistChipColors(
                                                containerColor = MaterialTheme.colorScheme.primaryContainer
                                            )
                                        )
                                    }

                                    Text("${pub.pesoKg} kg · Estado: ${pub.estadoId}", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF4B5563))

                                    AnimatedVisibility(visible = isExpanded) {
                                        Column(
                                            modifier = Modifier.padding(top = 8.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            if (pub.descripcion.isNotBlank()) {
                                                Text("Descripción: ${pub.descripcion}", style = MaterialTheme.typography.bodySmall, color = Color(0xFF374151))
                                            }
                                            if (!pub.imagenUrl.isNullOrBlank()) {
                                                AsyncImage(
                                                    model = pub.imagenUrl,
                                                    contentDescription = null,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(180.dp)
                                                )
                                            }
                                            Row(Modifier.padding(top = 8.dp)) {
                                                AnimatedButton(onClick = {
                                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                                        action = Intent.ACTION_SEND
                                                        type = "text/plain"
                                                        putExtra(Intent.EXTRA_TEXT, "Ubicación de mi publicación: https://maps.google.com/?q=${pub.lat},${pub.lon}")
                                                    }
                                                    context.startActivity(Intent.createChooser(intent, "Compartir ubicación GPS"))
                                                }) { Text("Compartir ubicación GPS") }
                                            }
                                        }
                                    }
                                }
                            }
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
    viewModel: InteractionViewModel = hiltViewModel(),
    aiViewModel: AiViewModel = hiltViewModel()
) {
    var step by remember { mutableStateOf(1) }
    var selectedCenter by remember { mutableStateOf<Usuario?>(null) }
    var centers by remember { mutableStateOf<List<Usuario>>(emptyList()) }
    var aiAdvice by remember { mutableStateOf<String?>(null) }
    
    var weight by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("VIDRIO") }
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
        Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Selecciona un Centro", style = MaterialTheme.typography.headlineMedium, color = Color(0xFF111827))
                SuggestionChip(
                    onClick = {
                        aiViewModel.suggestProfileFieldWithAi("centros", "RECOLECTOR", "") { sug ->
                            aiAdvice = "💡 Guía IA: Selecciona un Centro de Acopio autorizado. Ingresa la categoría, peso y precio sugerido. El centro validará el peso y confirmará la entrega."
                        }
                    },
                    label = { Text("✨ Ayuda IA") }
                )
            }

            aiAdvice?.let {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
                ) {
                    Text(it, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onTertiaryContainer)
                }
            }

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(centers) { center ->
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { selectedCenter = center; step = 2 },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text(center.nombreEmpresa ?: center.nombre, style = MaterialTheme.typography.titleMedium, color = Color(0xFF111827), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            TextButton(onClick = onBack) { Text("Atrás") }
        }
    } else {
        Column(Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Ofertar a ${selectedCenter?.nombreEmpresa}", style = MaterialTheme.typography.headlineMedium, color = Color(0xFF111827))
            ExposedDropdownMenuBox(expanded = categoryExpanded, onExpandedChange = { categoryExpanded = !categoryExpanded }) {
                OutlinedTextField(value = CategoryUtils.getDisplayName(selectedCategory), onValueChange = {}, readOnly = true, label = { Text("Categoría") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) }, modifier = Modifier.menuAnchor().fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color(0xFF111827), unfocusedTextColor = Color(0xFF111827)))
                ExposedDropdownMenu(expanded = categoryExpanded, onDismissRequest = { categoryExpanded = false }) {
                    CategoryUtils.defaultCategories.forEach { category ->
                        DropdownMenuItem(text = { Text(category.nombreMostrar) }, onClick = { selectedCategory = category.codigo; categoryExpanded = false })
                    }
                }
            }
            OutlinedTextField(value = weight, onValueChange = { weight = it }, label = { Text("Peso total (kg)") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color(0xFF111827), unfocusedTextColor = Color(0xFF111827)))
            OutlinedTextField(value = price, onValueChange = { price = it }, label = { Text("Precio sugerido") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color(0xFF111827), unfocusedTextColor = Color(0xFF111827)))
            if (imageUri != null) {
                AsyncImage(model = imageUri, contentDescription = null, modifier = Modifier.size(200.dp))
                AnimatedButton(onClick = { imageUri = null }, outlined = true) { Text("Cambiar foto") }
            } else {
                AnimatedButton(onClick = { val uri = CameraUtils.createImageUri(context); imageUri = uri; cameraLauncher.launch(uri) }, modifier = Modifier.fillMaxWidth()) { Text("Foto de la carga") }
            }
            if (state is InteractionUiState.Loading) LottieLoading() else {
                AnimatedButton(onClick = { viewModel.makeOfferToCenter(selectedCenter!!.id, selectedCategory, weight, price, imageUri, onBack) }, modifier = Modifier.fillMaxWidth()) { Text("Enviar oferta") }
            }
            if (state is InteractionUiState.Error) Text(state.message, color = Color.Red)
            TextButton(onClick = { step = 1 }) { Text("Volver a centros") }
        }
    }
}

@Composable
fun NegotiationScreen() { /* ... */ }
